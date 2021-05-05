package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

public class BazelModuleRepoSpecValue implements SkyValue {
  public static final SkyFunctionName BAZEL_MODULE_REPO_SPEC =
      SkyFunctionName.createHermetic("BAZEL_MODULE_REPO_SPEC");

  @AutoCodec
  public static final SkyKey KEY = () -> BAZEL_MODULE_REPO_SPEC;

  private final ImmutableMap<String, RepoSpec> repositories;

  public BazelModuleRepoSpecValue(ImmutableMap<String, RepoSpec> repositories) {
    this.repositories = repositories;
  }

  public RepoSpec getRepository(String repositoryName) {
    return repositories.getOrDefault(repositoryName, null);
  }
}
