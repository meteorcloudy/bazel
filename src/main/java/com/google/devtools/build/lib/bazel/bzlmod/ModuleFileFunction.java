package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.FileValue;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.packages.NoSuchThingException;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

  // TODO: populate this with the value of a flag.
  public static final Precomputed<List<String>> REGISTRIES = new Precomputed<>("registries");

  private final FetcherFactory fetcherFactory;
  private final RegistryFactory registryFactory;
  private final Path workspaceRoot;

  public ModuleFileFunction(FetcherFactory fetcherFactory, RegistryFactory registryFactory,
      Path workspaceRoot) {
    this.fetcherFactory = fetcherFactory;
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
    Module module = moduleFileGlobals.buildModule();
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
    Module module = moduleFileGlobals.buildModule();

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
    // Exactly one of `earlyFetcher` and `registry` is null.
    EarlyFetcher earlyFetcher;
    Registry registry;
  }

  private Optional<GetModuleFileResult> getModuleFile(ModuleKey key, StarlarkOverrideApi override,
      Environment env) throws ModuleFileFunctionException, InterruptedException {
    if (override instanceof NonRegistryOverride) {
      GetModuleFileResult result = new GetModuleFileResult();
      result.earlyFetcher = ((NonRegistryOverride) override).toEarlyFetcher(fetcherFactory);
      Path fetchPath = result.earlyFetcher.earlyFetch();
      RootedPath moduleFilePath =
          RootedPath.toRootedPath(Root.fromPath(fetchPath), PathFragment.create("MODULE.bazel"));
      if (env.getValue(FileValue.key(moduleFilePath)) == null) {
        return Optional.empty();
      }
      result.moduleFileContents = readFile(moduleFilePath.asPath());
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
    return getModuleFileFromRegistries(key,
        registries.stream().map(registryFactory::getRegistryWithUrl)
            .collect(ImmutableList.toImmutableList()));
  }

  private static byte[] readFile(Path path) throws ModuleFileFunctionException {
    try {
      // TODO: throw in a FileValue here?
      return FileSystemUtils.readWithKnownFileSize(path, path.getFileSize());
    } catch (IOException e) {
      throw new ModuleFileFunctionException(e);
    }
  }

  @VisibleForTesting
  static Optional<GetModuleFileResult> getModuleFileFromRegistries(ModuleKey key,
      List<Registry> registries) {
    GetModuleFileResult result = new GetModuleFileResult();
    for (Registry registry : registries) {
      Optional<byte[]> moduleFile = registry.getModuleFile(key);
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

  private static final class ModuleFileFunctionException extends SkyFunctionException {

    ModuleFileFunctionException(Exception cause) {
      super(cause, Transience.TRANSIENT);
    }
  }
}
