package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SingleVersionOverride implements RegistryOverride {

  public static SingleVersionOverride create(String version, String registry) {
    return new AutoValue_SingleVersionOverride(version, registry);
  }

  public abstract String getVersion();

  @Override
  public abstract String getRegistry();
}
