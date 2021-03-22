package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ModuleKey {

  public static ModuleKey create(String name, String version) {
    return new AutoValue_ModuleKey(name, version);
  }

  public abstract String getName();

  public abstract String getVersion();

  @Override
  public String toString() {
    return (getName().isEmpty() ? "_" : getName()) + "@" + (getVersion().isEmpty() ? "_"
        : getVersion());
  }
}
