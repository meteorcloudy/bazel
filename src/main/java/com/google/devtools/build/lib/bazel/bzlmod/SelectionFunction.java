package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SelectionFunction implements SkyFunction {

  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws SkyFunctionException, InterruptedException {
    DiscoveryValue discovery = (DiscoveryValue) env.getValue(DiscoveryValue.KEY);
    if (discovery == null) {
      return null;
    }

    // TODO: compatibility_level, multiple_version_override

    // First figure out the best version to use for every module.
    ImmutableMap<ModuleKey, Module> depGraph = discovery.getDepGraph();
    Map<String, ParsedVersion> bestVersionForEachModule = new HashMap<>();
    for (ModuleKey key : depGraph.keySet()) {
      // TODO: deal with parse exception
      ParsedVersion parsedVersion = ParsedVersion.parse(key.getVersion());
      bestVersionForEachModule.merge(key.getName(), parsedVersion, ParsedVersion::max);
    }

    // Now build a new dep graph where deps with unselected versions are removed.
    ImmutableMap.Builder<ModuleKey, Module> newDepGraphBuilder = new ImmutableMap.Builder<>();
    for (Map.Entry<ModuleKey, Module> entry : depGraph.entrySet()) {
      ModuleKey moduleKey = entry.getKey();
      Module module = entry.getValue();
      // Remove any dep whose version isn't the best.
      String bestVersion = bestVersionForEachModule.get(moduleKey.getName()).getOriginal();
      if (!moduleKey.getVersion().equals(bestVersion)) {
        continue;
      }

      // Rewrite deps to point to the best version.
      Module.Builder moduleBuilder = module.toBuilder();
      moduleBuilder.setDeps(ImmutableMap.copyOf(Maps.transformValues(module.getDeps(), depKey ->
          ModuleKey.create(depKey.getName(),
              bestVersionForEachModule.get(depKey.getName()).getOriginal())
      )));
      newDepGraphBuilder.put(moduleKey, moduleBuilder.build());
    }
    ImmutableMap<ModuleKey, Module> newDepGraph = newDepGraphBuilder.build();

    // Further remove unreferenced modules from the graph. We can find out which modules are
    // referenced by collecting deps transitively from the root.
    Set<ModuleKey> referenced = new HashSet<>();
    collectDeps(ModuleKey.create(discovery.getRootModuleName(), ""), newDepGraph, referenced);
    ImmutableMap<ModuleKey, Module> finalDepGraph =
        ImmutableMap.copyOf(Maps.filterKeys(newDepGraph, referenced::contains));
    return SelectionValue.create(
        discovery.getRootModuleName(), finalDepGraph, discovery.getOverrides());
  }

  private void collectDeps(
      ModuleKey key, ImmutableMap<ModuleKey, Module> depGraph, Set<ModuleKey> referenced) {
    if (referenced.contains(key)) {
      return;
    }
    referenced.add(key);
    for (ModuleKey depKey : depGraph.get(key).getDeps().values()) {
      collectDeps(depKey, depGraph, referenced);
    }
  }

  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }
}
