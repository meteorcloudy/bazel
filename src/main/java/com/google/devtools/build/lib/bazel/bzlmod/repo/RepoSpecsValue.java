package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

public class RepoSpecsValue implements SkyValue {
  public static final SkyFunctionName REPO_SPECS =
      SkyFunctionName.createHermetic("REPO_SPECS");

  @AutoCodec
  public static final SkyKey KEY_FOR_OVERRIDE_DEP = () -> REPO_SPECS;

  @AutoCodec
  public static final SkyKey KEY_FOR_BAZEL_MODULE = () -> REPO_SPECS;

  @AutoCodec
  public static final SkyKey KEY_FOR_MODULE_RULE = () -> REPO_SPECS;

  private final ImmutableMap<String, RepoSpec> repositories;

  public RepoSpecsValue(ImmutableMap<String, RepoSpec> repositories) {
    this.repositories = repositories;
  }

  public RepoSpec getRepository(String repositoryName) {
    return repositories.get(repositoryName);
  }
}
