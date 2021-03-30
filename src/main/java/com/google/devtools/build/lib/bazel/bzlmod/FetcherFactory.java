package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.vfs.Path;
import java.net.URL;

public class FetcherFactory {

  private final Path workspaceRoot;

  public FetcherFactory(Path workspaceRoot) {
    this.workspaceRoot = workspaceRoot;
  }

  public EarlyFetcher createArchiveFetcher(ImmutableList<URL> urls, String integrity,
      String stripPrefix) {
    // TODO: add patches
    return new ArchiveFetcher(urls, integrity, stripPrefix);
  }

  public LocalPathFetcher createLocalPathFetcher(String path) {
    return new LocalPathFetcher(workspaceRoot.getRelative(path));
  }
}
