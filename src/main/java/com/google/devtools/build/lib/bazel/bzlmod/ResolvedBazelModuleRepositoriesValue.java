package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

// TODO(pcloudy): implement the actual SkyFunction for this
public class ResolvedBazelModuleRepositoriesValue implements SkyValue {
  public static final SkyFunctionName RESOLVED_BAZEL_MODULE_REPOS =
      SkyFunctionName.createHermetic("RESOLVED_BAZEL_MODULE_REPOS");

  @Immutable
  public static class ResolvedBazelModuleRepositoriesKey implements SkyKey {
    private static final ResolvedBazelModuleRepositoriesKey KEY = new ResolvedBazelModuleRepositoriesKey();

    private ResolvedBazelModuleRepositoriesKey() {}

    @Override
    public SkyFunctionName functionName() {
      return RESOLVED_BAZEL_MODULE_REPOS;
    }
  }

  public static ResolvedBazelModuleRepositoriesKey key() {
    return ResolvedBazelModuleRepositoriesKey.KEY;
  }

  private final ImmutableMap<String, RepositoryInfo> repositories;

  public ResolvedBazelModuleRepositoriesValue(ImmutableMap<String, RepositoryInfo> repositories) {
    this.repositories = repositories;
  }

  public RepositoryInfo getRepository(String repositoryName) {
    return repositories.getOrDefault(repositoryName, null);
  }
}
