package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.devtools.build.lib.vfs.Path;

public class FetcherFactory {

  private final Path workspaceRoot;

  public FetcherFactory(Path workspaceRoot) {
    this.workspaceRoot = workspaceRoot;
  }

  public EarlyFetcher createArchiveFetcher(String url, String integrity) {
    return null;
  }

  public LocalPathFetcher createLocalPathFetcher(String path) {
    return new LocalPathFetcher(workspaceRoot.getRelative(path));
  }
}
