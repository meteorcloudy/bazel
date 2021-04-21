package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.vfs.Path;
import java.net.URL;
import java.util.Objects;

public class ArchiveFetcher implements EarlyFetcher {

  private final ImmutableList<URL> urls;
  private final String integrity;
  private final String stripPrefix;

  ArchiveFetcher(ImmutableList<URL> urls, String integrity, String stripPrefix) {
    this.urls = urls;
    this.integrity = integrity;
    this.stripPrefix = stripPrefix;
  }

  @Override
  public Path earlyFetch() {
    return null;
  }

  @Override
  public Path fetch(String repoName, Path vendorDir) {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ArchiveFetcher that = (ArchiveFetcher) o;
    return Objects.equals(urls, that.urls) &&
        Objects.equals(integrity, that.integrity) &&
        Objects.equals(stripPrefix, that.stripPrefix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(urls, integrity, stripPrefix);
  }

  @Override
  public String toString() {
    return "ArchiveFetcher{" +
        "urls=" + urls +
        ", integrity='" + integrity + '\'' +
        ", stripPrefix='" + stripPrefix + '\'' +
        '}';
  }
}
