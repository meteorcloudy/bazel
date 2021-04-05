package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

// TODO(pcloudy): implement the actual SkyFunction for this
public class ResolvedModuleRuleRepositoriesValue implements SkyValue {
  public static final SkyFunctionName RESOLVED_MODULE_RULE_REPOS =
      SkyFunctionName.createHermetic("RESOLVED_MODULE_RULE_REPOS");

  @Immutable
  public static class ResolvedModuleRuleRepositoriesKey implements SkyKey {
    private static final ResolvedModuleRuleRepositoriesKey KEY = new ResolvedModuleRuleRepositoriesKey();

    private ResolvedModuleRuleRepositoriesKey() {}

    @Override
    public SkyFunctionName functionName() {
      return RESOLVED_MODULE_RULE_REPOS;
    }
  }

  public static ResolvedModuleRuleRepositoriesKey key() {
    return ResolvedModuleRuleRepositoriesKey.KEY;
  }

  private final ImmutableMap<String, RepositoryInfo> repositories;

  public ResolvedModuleRuleRepositoriesValue(ImmutableMap<String, RepositoryInfo> repositories) {
    this.repositories = repositories;
  }

  public RepositoryInfo getRepository(String repositoryName) {
    return repositories.getOrDefault(repositoryName, null);
  }
}
