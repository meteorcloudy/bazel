package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.bazel.bzlmod.repo.RepoSpec;

@AutoValue
public abstract class LocalPathOverride implements NonRegistryOverride {
  public static LocalPathOverride create(String path) {
    return new AutoValue_LocalPathOverride(path);
  }

  public abstract String getPath();

  @Override
  public RepoSpec getRepoSpec(String repoName) {
    return new RepoSpec("local_repository", ImmutableMap.of("name", repoName, "path", getPath()));
  }
}
