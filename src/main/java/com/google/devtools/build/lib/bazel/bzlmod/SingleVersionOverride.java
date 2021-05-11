package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

@AutoValue
public abstract class SingleVersionOverride implements RegistryOverride {

  public static SingleVersionOverride create(String version, String registry,
      ImmutableList<String> patches, int patchStrip) {
    return new AutoValue_SingleVersionOverride(version, registry, patches, patchStrip);
  }

  public abstract String getVersion();

  @Override
  public abstract String getRegistry();

  public abstract ImmutableList<String> getPatches();

  public abstract int getPatchStrip();
}
