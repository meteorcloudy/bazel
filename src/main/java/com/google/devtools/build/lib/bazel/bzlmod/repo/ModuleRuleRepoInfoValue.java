package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

// TODO(pcloudy): implement the actual SkyFunction for this
public class ModuleRuleRepoInfoValue implements SkyValue {
  public static final SkyFunctionName MODULE_RULE_REPO_INFO =
      SkyFunctionName.createHermetic("MODULE_RULE_REPO_INFO");

  @Immutable
  public static class ModuleRuleRepositoriesKey implements SkyKey {
    private static final ModuleRuleRepositoriesKey KEY = new ModuleRuleRepositoriesKey();

    private ModuleRuleRepositoriesKey() {}

    @Override
    public SkyFunctionName functionName() {
      return MODULE_RULE_REPO_INFO;
    }
  }

  public static ModuleRuleRepositoriesKey key() {
    return ModuleRuleRepositoriesKey.KEY;
  }

  private final ImmutableMap<String, RepositoryInfo> repositories;

  public ModuleRuleRepoInfoValue(ImmutableMap<String, RepositoryInfo> repositories) {
    this.repositories = repositories;
  }

  public RepositoryInfo getRepository(String repositoryName) {
    return repositories.getOrDefault(repositoryName, null);
  }
}
