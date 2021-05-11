package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.bazel.bzlmod.ModuleFileFunction.ModuleFileFunctionException;
import com.google.devtools.build.lib.bazel.bzlmod.repo.RepoSpec;
import com.google.devtools.build.lib.starlarkbuildapi.repository.ModuleFileGlobalsApi;
import com.google.devtools.build.lib.starlarkbuildapi.repository.StarlarkOverrideApi;

import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.starlark.java.eval.StarlarkInt;

public class ModuleFileGlobals implements ModuleFileGlobalsApi<ModuleFileFunctionException> {

  private boolean moduleCalled = false;
  private final Module.Builder module = Module.builder();
  private final Map<String, ModuleKey> deps = new LinkedHashMap<>();
  private final Map<String, StarlarkOverrideApi> overrides = new HashMap<>();

  public ModuleFileGlobals() {
  }

  @Override
  public void module(String name, String version, Dict<String, Object> kwargs)
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
  public StarlarkOverrideApi singleVersionOverride(String version, String registry,
      Iterable<?> patches, StarlarkInt patchStrip) throws EvalException {
    return SingleVersionOverride.create(version, registry,
        ImmutableList.copyOf((Iterable<String>) patches),
        patchStrip.toInt("single_version_override.patch_strip"));
  }

  @Override
  public StarlarkOverrideApi archiveOverride(Object urls, String integrity,
      String stripPrefix, Iterable<?> patches, StarlarkInt patchStrip) throws EvalException {
    ImmutableList.Builder<String> urlList = new ImmutableList.Builder<>();
    if (urls instanceof String) {
      urlList.add((String) urls);
    } else {
      for (String urlString : (Iterable<String>) urls) {
        urlList.add(urlString);
      }
    }
    return ArchiveOverride.create(
        urlList.build(), ImmutableList.copyOf((Iterable<String>) patches), integrity, stripPrefix,
        patchStrip.toInt("archive_override.patch_strip"));
  }

  @Override
  public StarlarkOverrideApi gitOverride(String remote, String commit, Iterable<?> patches,
      StarlarkInt patchStrip) throws EvalException {
    return GitOverride.create(
        remote, commit, ImmutableList.copyOf((Iterable<String>) patches),
        patchStrip.toInt("git_override.patch_strip"));
  }

  @Override
  public StarlarkOverrideApi localPathOverride(String path) {
    return LocalPathOverride.create(path);
  }

  public Module buildModule(RepoSpec repoSpec, Registry registry) {
    return module.setDeps(ImmutableMap.copyOf(deps))
        .setRepoSpec(repoSpec)
        .setRegistry(registry)
        .build();
  }

  public ImmutableMap<String, StarlarkOverrideApi> buildOverrides() {
    return ImmutableMap.copyOf(overrides);
  }
}
