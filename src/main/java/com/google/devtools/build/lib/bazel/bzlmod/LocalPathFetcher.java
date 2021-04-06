package com.google.devtools.build.lib.bazel.bzlmod;

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
}
