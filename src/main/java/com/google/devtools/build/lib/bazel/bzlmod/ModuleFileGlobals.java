package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.starlarkbuildapi.repository.ModuleFileGlobalsApi;
import com.google.devtools.build.lib.starlarkbuildapi.repository.StarlarkOverrideApi;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;

public class ModuleFileGlobals implements ModuleFileGlobalsApi {

  private boolean moduleCalled = false;
  private final Module.Builder module = Module.builder();
  private final Map<String, ModuleKey> deps = new LinkedHashMap<>();
  private final Map<String, StarlarkOverrideApi> overrides = new HashMap<>();

  public ModuleFileGlobals() {
  }

  @Override
  public void module(String name, String version)
      throws EvalException {
    if (moduleCalled) {
      throw Starlark.errorf("the module() directive can only be called once");
    }
    moduleCalled = true;
    module.setName(name).setVersion(version);
  }

  @Override
  public void bazelDep(String name, String version, String repoName)
      throws EvalException {
    if (repoName.isEmpty()) {
      repoName = name;
    }
    if (deps.putIfAbsent(repoName, ModuleKey.create(name, version)) != null) {
      throw Starlark.errorf("a bazel_dep with the repo name %s already exists", repoName);
    }
  }

  @Override
  public void overrideDep(String name, StarlarkOverrideApi override)
      throws EvalException {
    StarlarkOverrideApi existingOverride = overrides.putIfAbsent(name, override);
    if (existingOverride != null) {
      throw Starlark.errorf("multiple overrides for dep %s found", name);
    }
  }

  @Override
  public StarlarkOverrideApi singleVersionOverride(String version, String registry) {
    return SingleVersionOverride.create(version, registry);
  }

  @Override
  public StarlarkOverrideApi archiveOverride(String url, String integrity) {
    return ArchiveOverride.create(url, integrity);
  }

  @Override
  public StarlarkOverrideApi localPathOverride(String path) {
    return LocalPathOverride.create(path);
  }

  public Module buildModule() {
    return module.setDeps(ImmutableMap.copyOf(deps)).build();
  }

  public ImmutableMap<String, StarlarkOverrideApi> buildOverrides() {
    return ImmutableMap.copyOf(overrides);
  }
}
