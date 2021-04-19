package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.skyframe.SkyFunctions;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.starlarkbuildapi.repository.StarlarkOverrideApi;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import java.util.Map;

@AutoValue
public abstract class DiscoveryValue implements SkyValue {

  @AutoCodec
  public static final SkyKey KEY = () -> SkyFunctions.DISCOVERY;

  public static DiscoveryValue create(String rootModuleName,
      ImmutableMap<ModuleKey, Module> depGraph,
      ImmutableMap<String, StarlarkOverrideApi> overrides) {
    return new AutoValue_DiscoveryValue(rootModuleName, depGraph, overrides);
  }

  public abstract String getRootModuleName();

  public abstract ImmutableMap<ModuleKey, Module> getDepGraph();

  public abstract ImmutableMap<String, StarlarkOverrideApi> getOverrides();
}
