package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.bazel.bzlmod.ModuleFileValue;
import com.google.devtools.build.lib.bazel.bzlmod.ModuleKey;
import com.google.devtools.build.lib.bazel.bzlmod.NonRegistryOverride;
import com.google.devtools.build.lib.bazel.bzlmod.RegistryOverride;
import com.google.devtools.build.lib.starlarkbuildapi.repository.StarlarkOverrideApi;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class OverrideDepRepoSpecFunction implements SkyFunction {

  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws SkyFunctionException, InterruptedException {
    Preconditions.checkArgument(skyKey == OverrideDepRepoSpecValue.KEY);

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
    return new OverrideDepRepoSpecValue(repositories.build());
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }
}
