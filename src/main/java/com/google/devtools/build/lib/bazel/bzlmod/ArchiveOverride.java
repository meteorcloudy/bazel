package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.bazel.bzlmod.repo.RepoSpec;

@AutoValue
public abstract class ArchiveOverride implements NonRegistryOverride {

  public static ArchiveOverride create(ImmutableList<String> urls, ImmutableList<String> patches,
      String integrity, String stripPrefix, int patchStrip) {
    return new AutoValue_ArchiveOverride(urls, patches, integrity, stripPrefix, patchStrip);
  }

  public abstract ImmutableList<String> getUrls();

  public abstract ImmutableList<String> getPatches();

  public abstract String getIntegrity();

  public abstract String getStripPrefix();

  public abstract int getPatchStrip();

  @Override
  public RepoSpec getRepoSpec(String repoName) {
    return IndexRegistry.getRepoSpecForArchive(
        /* repoName= */ repoName,
        /* urls= */ getUrls(),
        /* integrity= */ getIntegrity(),
        /* stripPrefix= */ getStripPrefix(),
        /* patches= */ getPatches(),
        /* patchStrip= */ getPatchStrip(),
        /* remotePatches= */ ImmutableMap.of(),
        /* remotePatchStrip= */ 0);
  }
}
