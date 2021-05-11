package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.bazel.bzlmod.repo.RepoSpec;

@AutoValue
public abstract class GitOverride implements NonRegistryOverride {

  private static final String GIT_REPOSITORY_RULE_CLASS =
      "@bazel_tools//tools/build_defs/repo:git.bzl%git_repository";

  public static GitOverride create(String remote, String commit, ImmutableList<String> patches, int patchStrip) {
    return new AutoValue_GitOverride(remote, commit, patches, patchStrip);
  }

  public abstract String getRemote();
  public abstract String getCommit();
  public abstract ImmutableList<String> getPatches();
  public abstract int getPatchStrip();

  @Override
  public RepoSpec getRepoSpec(String repoName) {
    ImmutableMap.Builder<String, Object> attrBuilder = ImmutableMap.builder();
    attrBuilder.put("name", repoName)
        .put("remote", getRemote())
        .put("commit", getCommit())
        .put("patches", getPatches())
        .put("patch_args", ImmutableList.of("-p" + getPatchStrip()));
    return new RepoSpec(GIT_REPOSITORY_RULE_CLASS, attrBuilder.build());
  }
}
