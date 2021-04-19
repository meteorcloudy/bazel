package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.devtools.build.lib.starlarkbuildapi.repository.StarlarkOverrideApi;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import javax.annotation.Nullable;

public class DiscoveryFunction implements SkyFunction {

  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws SkyFunctionException, InterruptedException {
    ModuleFileValue root = (ModuleFileValue) env.getValue(ModuleFileValue.keyForRootModule());
    if (root == null) {
      return null;
    }
    ModuleKey rootModuleKey = ModuleKey.create(root.getModule().getName(), "");
    ImmutableMap<String, StarlarkOverrideApi> overrides = root.getOverrides();
    Map<ModuleKey, Module> depGraph = new HashMap<>();
    depGraph.put(rootModuleKey, rewriteDepKeys(root.getModule(), overrides));
    Queue<ModuleKey> unexpanded = new ArrayDeque<>();
    unexpanded.add(rootModuleKey);
    while (!unexpanded.isEmpty()) {
      Module module = depGraph.get(unexpanded.remove());
      for (ModuleKey depKey : module.getDeps().values()) {
        if (depGraph.containsKey(depKey)) {
          continue;
        }
        ModuleFileValue dep = (ModuleFileValue) env.getValue(ModuleFileValue.key(depKey));
        if (dep == null) {
          // Don't return yet. Try to expand any other unexpanded nodes before returning.
          depGraph.put(depKey, null);
        } else {
          depGraph.put(depKey, rewriteDepKeys(dep.getModule(), overrides));
          unexpanded.add(depKey);
        }
      }
    }
    if (env.valuesMissing()) {
      return null;
    }
    return DiscoveryValue.create(
        root.getModule().getName(), ImmutableMap.copyOf(depGraph), overrides);
  }

  private static Module rewriteDepKeys(Module module,
      ImmutableMap<String, StarlarkOverrideApi> overrides) {
    return module.toBuilder()
        .setDeps(ImmutableMap.copyOf(Maps.transformValues(module.getDeps(), depKey -> {
          @Nullable StarlarkOverrideApi override = overrides.get(depKey.getName());
          if (override instanceof NonRegistryOverride) {
            return ModuleKey.create(depKey.getName(), "");
          } else if (override instanceof SingleVersionOverride) {
            String overrideVersion = ((SingleVersionOverride) override).getVersion();
            if (!overrideVersion.isEmpty()) {
              return ModuleKey.create(depKey.getName(), overrideVersion);
            }
          }
          return depKey;
        }))).build();
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }
}
