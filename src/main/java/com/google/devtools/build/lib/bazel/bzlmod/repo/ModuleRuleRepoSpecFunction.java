package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.FileValue;
import com.google.devtools.build.lib.pkgcache.PathPackageLocator;
import com.google.devtools.build.lib.skyframe.PrecomputedValue;
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

import javax.annotation.Nullable;

public class ModuleRuleRepoSpecFunction implements SkyFunction {

  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws SkyFunctionException, InterruptedException {
    Preconditions.checkArgument(skyKey == ModuleRuleRepoSpecValue.KEY);
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
      return new ModuleRuleRepoSpecValue(ImmutableMap.of());
    }

    Path lockFilePath = lockFile.asPath();
    byte[] bytes;
    try {
      bytes = FileSystemUtils.readWithKnownFileSize(lockFilePath, lockFilePath.getFileSize());
    } catch (IOException ex) {
      throw new ModuleRuleRepoInfoFunctionException(ex, Transience.TRANSIENT);
    }

    return new ModuleRuleRepoSpecValue(
        LockFileParser.loadRepositoryInfos(new String(bytes)));
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }

  private static final class ModuleRuleRepoInfoFunctionException extends SkyFunctionException {
    ModuleRuleRepoInfoFunctionException(IOException e, Transience transience) {
      super(e, transience);
    }
  }
}
