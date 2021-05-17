package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.FileValue;
import com.google.devtools.build.lib.bazel.bzlmod.repo.RepoSpec;
import com.google.devtools.build.lib.cmdline.RepositoryName;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.events.ExtendedEventHandler;
import com.google.devtools.build.lib.packages.NoSuchThingException;
import com.google.devtools.build.lib.rules.repository.RepositoryDirectoryValue;
import com.google.devtools.build.lib.skyframe.PrecomputedValue;
import com.google.devtools.build.lib.skyframe.PrecomputedValue.Precomputed;
import com.google.devtools.build.lib.starlarkbuildapi.repository.StarlarkOverrideApi;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Mutability;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.syntax.ParserInput;
import net.starlark.java.syntax.Program;
import net.starlark.java.syntax.StarlarkFile;
import net.starlark.java.syntax.SyntaxError;

public class ModuleFileFunction implements SkyFunction {

  public static final Precomputed<List<String>> REGISTRIES = new Precomputed<>("registries");

  private final RegistryFactory registryFactory;
  private final Path workspaceRoot;

  public ModuleFileFunction(RegistryFactory registryFactory, Path workspaceRoot) {
    this.registryFactory = registryFactory;
    this.workspaceRoot = workspaceRoot;
  }

  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws SkyFunctionException, InterruptedException {
    StarlarkSemantics starlarkSemantics = PrecomputedValue.STARLARK_SEMANTICS.get(env);
    if (starlarkSemantics == null) {
      return null;
    }

    if (skyKey.equals(ModuleFileValue.keyForRootModule())) {
      return computeForRootModule(starlarkSemantics, env);
    }

    ModuleFileValue rootModule = (ModuleFileValue) env.getValue(ModuleFileValue.keyForRootModule());
    if (rootModule == null) {
      return null;
    }
    ModuleKey moduleKey = (ModuleKey) skyKey.argument();
    if (moduleKey.getName().equals(rootModule.getModule().getName())) {
      // Special case: Someone has a dependency on the root module.
      return rootModule;
    }

    // Grab the module file.
    Optional<GetModuleFileResult> optGetModuleFileResult = getModuleFile(moduleKey,
        rootModule.getOverrides().get(moduleKey.getName()), env);
    if (env.valuesMissing()) {
      return null;
    }
    if (!optGetModuleFileResult.isPresent()) {
      throw errorf("module not found in registries: %s", moduleKey);
    }
    GetModuleFileResult getModuleFileResult = optGetModuleFileResult.get();

    // Execute the module file.
    ModuleFileGlobals moduleFileGlobals = execModuleFile(getModuleFileResult.moduleFileContents,
        moduleKey, starlarkSemantics, env);

    // Perform some sanity checks.
    Module module = moduleFileGlobals.buildModule(getModuleFileResult.repoSpec,
        getModuleFileResult.registry);
    if (!module.getName().equals(moduleKey.getName())) {
      throw errorf("the MODULE.bazel file of %s declares a different name (%s)", moduleKey,
          module.getName());
    }
    if (!moduleKey.getVersion().isEmpty() && !module.getVersion().equals(moduleKey.getVersion())) {
      throw errorf("the MODULE.bazel file of %s declares a different version (%s)", moduleKey,
          module.getVersion());
    }

    return new ModuleFileValue(module, null);
    // Note that we don't need to bother with returning the overrides here, because only the
    // overrides specified by the root module take any effect.
  }

  private SkyValue computeForRootModule(StarlarkSemantics starlarkSemantics, Environment env)
      throws SkyFunctionException, InterruptedException {
    RootedPath moduleFilePath =
        RootedPath.toRootedPath(Root.fromPath(workspaceRoot), PathFragment.create("MODULE.bazel"));
    if (env.getValue(FileValue.key(moduleFilePath)) == null) {
      return null;
    }
    byte[] moduleFile = readFile(moduleFilePath.asPath());
    ModuleFileGlobals moduleFileGlobals = execModuleFile(moduleFile,
        ModuleFileValue.ROOT_MODULE_KEY, starlarkSemantics, env);
    // TODO: should we add a fetcher for root module?
    Module module = moduleFileGlobals.buildModule(null, null);

    // Check that overrides don't contain the root itself (we need to set the override for the root
    // module to "local path" of the workspace root).
    ImmutableMap<String, StarlarkOverrideApi> overrides = moduleFileGlobals.buildOverrides();
    StarlarkOverrideApi rootOverride = overrides.get(module.getName());
    if (rootOverride != null) {
      throw errorf("invalid override for the root module found: %s", rootOverride);
    }
    ImmutableMap<String, StarlarkOverrideApi> overridesWithRoot =
        ImmutableMap.<String, StarlarkOverrideApi>builder()
            .putAll(overrides)
            .put(module.getName(), LocalPathOverride.create(""))
            .build();

    return new ModuleFileValue(module, overridesWithRoot);
  }

  private ModuleFileGlobals execModuleFile(byte[] moduleFile, ModuleKey moduleKey,
      StarlarkSemantics starlarkSemantics, Environment env)
      throws ModuleFileFunctionException, InterruptedException {
    StarlarkFile starlarkFile =
        StarlarkFile.parse(ParserInput.fromUTF8(moduleFile, moduleKey + "/MODULE.bazel"));
    if (!starlarkFile.ok()) {
      Event.replayEventsOn(env.getListener(), starlarkFile.errors());
      throw errorf("error parsing MODULE.bazel file for %s", moduleKey);
    }

    ModuleFileGlobals moduleFileGlobals = new ModuleFileGlobals();
    try (Mutability mu = Mutability.create("module file", moduleKey)) {
      net.starlark.java.eval.Module predeclaredEnv = getPredeclaredEnv(moduleFileGlobals,
          starlarkSemantics);
      Program program = Program.compileFile(starlarkFile, predeclaredEnv);
      // TODO: check that `program` has no `def`, `if`, etc
      StarlarkThread thread = new StarlarkThread(mu, starlarkSemantics);
      thread.setPrintHandler(Event.makeDebugPrintHandler(env.getListener()));
      Starlark.execFileProgram(program, predeclaredEnv, thread);
    } catch (SyntaxError.Exception | EvalException e) {
      throw new ModuleFileFunctionException(e);
    }
    return moduleFileGlobals;
  }

  private static class GetModuleFileResult {

    byte[] moduleFileContents;
    // Exactly one of `repoSpec` and `registry` is null.
    RepoSpec repoSpec;
    Registry registry;
  }

  @Nullable
  private Optional<GetModuleFileResult> getModuleFile(ModuleKey key, StarlarkOverrideApi override,
      Environment env) throws ModuleFileFunctionException, InterruptedException {
    if (override instanceof NonRegistryOverride) {
      // TODO: The repo name is not necessarily the same as the module name here. Fix later.
      String repoName = key.getName();
      RepositoryDirectoryValue repoDir = (RepositoryDirectoryValue) env.getValue(
          RepositoryDirectoryValue.key(RepositoryName.createFromValidStrippedName(repoName)));
      if (repoDir == null) {
        return null;
      }
      RootedPath moduleFilePath = RootedPath.toRootedPath(
          Root.fromPath(repoDir.getPath()), PathFragment.create("MODULE.bazel"));
      if (env.getValue(FileValue.key(moduleFilePath)) == null) {
        return Optional.empty();
      }
      GetModuleFileResult result = new GetModuleFileResult();
      result.moduleFileContents = readFile(moduleFilePath.asPath());
      result.repoSpec = ((NonRegistryOverride) override).getRepoSpec(repoName);
      return Optional.of(result);
    }

    List<String> registries = Objects.requireNonNull(REGISTRIES.get(env));
    if (override instanceof RegistryOverride) {
      String overrideRegistry = ((RegistryOverride) override).getRegistry();
      if (!overrideRegistry.isEmpty()) {
        registries = ImmutableList.of(overrideRegistry);
      }
    } else if (override != null) {
      throw errorf("unrecognized override type %s", override.getClass().getName());
    }
    ImmutableList.Builder<Registry> registryObjects = new ImmutableList.Builder<>();
    for (String registryUrl : registries) {
      try {
        registryObjects.add(registryFactory.getRegistryWithUrl(
            registryUrl.replace("%workspace%", workspaceRoot.getPathString())));
      } catch (URISyntaxException e) {
        throw new ModuleFileFunctionException(e);
      }
    }
    try {
      return getModuleFileFromRegistries(key, registryObjects.build(), env.getListener());
    } catch (IOException e) {
      throw new ModuleFileFunctionException(e);
    }
  }

  private static byte[] readFile(Path path) throws ModuleFileFunctionException {
    try {
      // TODO: throw in a FileValue here?
      return FileSystemUtils.readWithKnownFileSize(path, path.getFileSize());
    } catch (IOException e) {
      throw new ModuleFileFunctionException(e);
    }
  }

  private static Optional<GetModuleFileResult> getModuleFileFromRegistries(ModuleKey key,
      List<Registry> registries, ExtendedEventHandler eventHandler)
      throws IOException, InterruptedException {
    GetModuleFileResult result = new GetModuleFileResult();
    for (Registry registry : registries) {
      Optional<byte[]> moduleFile = registry.getModuleFile(key, eventHandler);
      if (!moduleFile.isPresent()) {
        continue;
      }
      result.moduleFileContents = moduleFile.get();
      result.registry = registry;
      return Optional.of(result);
    }
    return Optional.empty();
  }

  private net.starlark.java.eval.Module getPredeclaredEnv(ModuleFileGlobals moduleFileGlobals,
      StarlarkSemantics starlarkSemantics) {
    ImmutableMap.Builder<String, Object> env = ImmutableMap.builder();
    Starlark.addMethods(env, moduleFileGlobals, starlarkSemantics);
    return net.starlark.java.eval.Module.withPredeclared(starlarkSemantics, env.build());
  }

  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }

  private static ModuleFileFunctionException errorf(String format, Object... args) {
    return new ModuleFileFunctionException(new NoSuchThingException(String.format(format, args)));
  }

  static final class ModuleFileFunctionException extends SkyFunctionException {

    ModuleFileFunctionException(Exception cause) {
      super(cause, Transience.TRANSIENT);
    }
  }
}
