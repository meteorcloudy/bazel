package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.FileValue;
import com.google.devtools.build.lib.bazel.bzlmod.Module;
import com.google.devtools.build.lib.bazel.bzlmod.ModuleFileValue;
import com.google.devtools.build.lib.bazel.bzlmod.ModuleKey;
import com.google.devtools.build.lib.bazel.bzlmod.NonRegistryOverride;
import com.google.devtools.build.lib.bazel.bzlmod.RegistryOverride;
import com.google.devtools.build.lib.bazel.bzlmod.SelectionValue;
import com.google.devtools.build.lib.bazel.bzlmod.SingleVersionOverride;
import com.google.devtools.build.lib.pkgcache.PathPackageLocator;
import com.google.devtools.build.lib.skyframe.PrecomputedValue;
import com.google.devtools.build.lib.starlarkbuildapi.repository.StarlarkOverrideApi;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyFunctionException.Transience;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class RepoSpecsFunction implements SkyFunction {

  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws SkyFunctionException, InterruptedException {

    if (skyKey == RepoSpecsValue.KEY_FOR_OVERRIDE_DEP) {
      return computeForOverrideDep(env);
    } else if (skyKey == RepoSpecsValue.KEY_FOR_BAZEL_MODULE) {
      return computeForBazelModule(env);
    } else if (skyKey == RepoSpecsValue.KEY_FOR_MODULE_RULE) {
      return computeForModuleRule(env);
    }
    throw new IllegalArgumentException("Unrecognized key: " + skyKey.toString());
  }

  @Nullable
  private SkyValue computeForOverrideDep(Environment env)
      throws SkyFunctionException, InterruptedException {
    ModuleFileValue root = (ModuleFileValue) env.getValue(ModuleFileValue.keyForRootModule());
    if (root == null) {
      return null;
    }
    ImmutableMap.Builder<String, RepoSpec> repositories = ImmutableMap.builder();

    HashMap<String, String> moduleNameToRepoName = new HashMap<>();
    for (Map.Entry<String, ModuleKey> entry : root.getModule().getDeps().entrySet()) {
      moduleNameToRepoName.put(entry.getValue().getName(), entry.getKey());
    }

    for (Map.Entry<String, StarlarkOverrideApi> entry : root.getOverrides().entrySet()) {
      if (entry.getValue() instanceof RegistryOverride) {
        continue;
      }
      // TODO: refactor repo name calculate
      String repoName = moduleNameToRepoName.getOrDefault(entry.getKey(), entry.getKey());
      NonRegistryOverride nonRegistryOverride = (NonRegistryOverride) entry.getValue();
      repositories.put(repoName, nonRegistryOverride.getRepoSpec(repoName));
    }
    return new RepoSpecsValue(repositories.build());
  }

  @Nullable
  private SkyValue computeForBazelModule(Environment env)
      throws SkyFunctionException, InterruptedException {
    SelectionValue selectionValue = (SelectionValue) env.getValue(SelectionValue.KEY);
    if (selectionValue == null) {
      return null;
    }

    ImmutableMap.Builder<String, RepoSpec> repositories = ImmutableMap.builder();

    for (ModuleKey moduleKey : selectionValue.getDepGraph().keySet()) {
      if (moduleKey.getName().equals(selectionValue.getRootModuleName())) {
        continue;
      }
      try {
        Module module = selectionValue.getDepGraph().get(moduleKey);
        // TODO: calculate the real repo name
        String repoName = moduleKey.getName();
        RepoSpec repoSpec;
        if (module.getRepoSpec() != null) {
          repoSpec = module.getRepoSpec();
        } else {
          repoSpec = module.getRegistry().getRepoSpec(moduleKey, repoName, env.getListener());
        }
        // We may need to apply an extra set of patches here when the module has a single version
        // override with patches.
        repoSpec = maybeAppendAdditionalPatches(repoSpec,
            selectionValue.getOverrides().get(moduleKey.getName()));
        repositories.put(repoName, repoSpec);
      } catch (IOException e) {
        throw new RepoSpecsFunctionException(e, Transience.PERSISTENT);
      }
    }
    return new RepoSpecsValue(repositories.build());
  }

  private RepoSpec maybeAppendAdditionalPatches(RepoSpec repoSpec, StarlarkOverrideApi override) {
    if (!(override instanceof SingleVersionOverride)) {
      return repoSpec;
    }
    SingleVersionOverride singleVersion = (SingleVersionOverride) override;
    if (singleVersion.getPatches().isEmpty()) {
      return repoSpec;
    }
    HashMap<String, Object> newAttrs = new HashMap<>(repoSpec.getAttributes().size());
    newAttrs.putAll(repoSpec.getAttributes());
    newAttrs.put("patches", singleVersion.getPatches());
    newAttrs.put("patch_args", ImmutableList.of("-p" + singleVersion.getPatchStrip()));
    return new RepoSpec(repoSpec.getRuleClass(), ImmutableMap.copyOf(newAttrs));
  }

  @Nullable
  private SkyValue computeForModuleRule(Environment env)
      throws SkyFunctionException, InterruptedException {
    PathPackageLocator packageLocator = PrecomputedValue.PATH_PACKAGE_LOCATOR.get(env);
    ImmutableList<Root> packagePath = packageLocator.getPathEntries();

    // In Bazel, there should be only one workspace root.
    Root workspaceRoot = packagePath.get(0);
    RootedPath lockFile = RootedPath.toRootedPath(workspaceRoot,
        PathFragment.create("module_rule_repos.json"));

    FileValue fileValue = (FileValue) env.getValue(FileValue.key(lockFile));
    if (env.valuesMissing()) {
      return null;
    }

    if (!fileValue.exists() || !fileValue.isFile()) {
//      throw new ResolvedModuleRuleRepositoriesFunctionException(
//          new IOException("Expect lock file module_rule_repos.json to exist at workspace root."),
//          Transience.TRANSIENT);
      return new RepoSpecsValue(ImmutableMap.of());
    }

    Path lockFilePath = lockFile.asPath();
    byte[] bytes;
    try {
      bytes = FileSystemUtils.readWithKnownFileSize(lockFilePath, lockFilePath.getFileSize());
    } catch (IOException ex) {
      throw new RepoSpecsFunctionException(ex, Transience.TRANSIENT);
    }

    return new RepoSpecsValue(LockFileParser.loadRepoSpecs(new String(bytes)));
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }

  private static final class RepoSpecsFunctionException extends SkyFunctionException {

    RepoSpecsFunctionException(IOException e, Transience transience) {
      super(e, transience);
    }
  }
}
