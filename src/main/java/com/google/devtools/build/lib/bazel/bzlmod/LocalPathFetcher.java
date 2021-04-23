package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.vfs.Path;

public class LocalPathFetcher implements EarlyFetcher {
  private final Path path;

  public LocalPathFetcher(Path path) {
    this.path = path;
  }

  @Override
  public Path earlyFetch() {
    return path;
  }

  @Override
  public Path fetch(String repoName, Path vendorDir) {
    return path;
  }

  @Override
  public String getRuleClass() {
    return "local_repository";
  }

  @Override
  public ImmutableMap<String, Object> getRuleAttrs(String repoName) {
    return ImmutableMap.of("name", repoName, "path", path);
  }
}
