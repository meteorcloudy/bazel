package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.vfs.Path;
import java.net.URL;

public class FetcherFactory {

  private final Path workspaceRoot;

  public FetcherFactory(Path workspaceRoot) {
    this.workspaceRoot = workspaceRoot;
  }

  public EarlyFetcher createArchiveFetcher(ImmutableList<URL> urls, ImmutableList<URL> patches,
      String integrity, String stripPrefix, int patchStrip) {
    return new ArchiveFetcher(urls, patches, integrity, stripPrefix, patchStrip);
  }

  public LocalPathFetcher createLocalPathFetcher(String path) {
    return new LocalPathFetcher(workspaceRoot.getRelative(path));
  }
}
