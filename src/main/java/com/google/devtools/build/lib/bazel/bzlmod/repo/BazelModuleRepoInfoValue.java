package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

// TODO(pcloudy): implement the actual SkyFunction for this
public class BazelModuleRepoInfoValue implements SkyValue {
  public static final SkyFunctionName BAZEL_MODULE_REPO_INFO =
      SkyFunctionName.createHermetic("BAZEL_MODULE_REPO_INFO");

  @Immutable
  public static class BazelModuleRepoInfoKey implements SkyKey {
    private static final BazelModuleRepoInfoKey KEY = new BazelModuleRepoInfoKey();

    private BazelModuleRepoInfoKey() {}

    @Override
    public SkyFunctionName functionName() {
      return BAZEL_MODULE_REPO_INFO;
    }
  }

  public static BazelModuleRepoInfoKey key() {
    return BazelModuleRepoInfoKey.KEY;
  }

  private final ImmutableMap<String, RepositoryInfo> repositories;

  public BazelModuleRepoInfoValue(ImmutableMap<String, RepositoryInfo> repositories) {
    this.repositories = repositories;
  }

  public RepositoryInfo getRepository(String repositoryName) {
    return repositories.getOrDefault(repositoryName, null);
  }
}
