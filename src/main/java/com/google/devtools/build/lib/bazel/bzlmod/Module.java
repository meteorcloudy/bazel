package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

@AutoValue
public abstract class Module {

  public abstract String getName();

  public abstract String getVersion();

  public abstract ImmutableMap<String, ModuleKey> getDeps();

  public abstract Builder toBuilder();

  static Builder builder() {
    return new AutoValue_Module.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setName(String value);

    public abstract Builder setVersion(String value);

    public abstract Builder setDeps(ImmutableMap<String, ModuleKey> value);

    abstract ImmutableMap.Builder<String, ModuleKey> depsBuilder();

    public Builder addDep(String depRepoName, ModuleKey depKey) {
      depsBuilder().put(depRepoName, depKey);
      return this;
    }

    public abstract Module build();
  }
}
