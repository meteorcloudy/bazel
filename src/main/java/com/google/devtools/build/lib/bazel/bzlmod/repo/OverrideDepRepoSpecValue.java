package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

// TODO(pcloudy): implement the actual SkyFunction for this
public class OverrideDepRepoSpecValue implements SkyValue {
  public static final SkyFunctionName OVERRIDE_DEP_REPO_SPEC =
      SkyFunctionName.createHermetic("OVERRIDE_DEP_REPO_SPEC");

  @AutoCodec
  public static final SkyKey KEY = () -> OVERRIDE_DEP_REPO_SPEC;

  private final ImmutableMap<String, RepoSpec> repositories;

  public OverrideDepRepoSpecValue(ImmutableMap<String, RepoSpec> repositories) {
    this.repositories = repositories;
  }

  public RepoSpec getRepository(String repositoryName) {
    return repositories.getOrDefault(repositoryName, null);
  }
}
