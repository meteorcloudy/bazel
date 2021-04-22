package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.vfs.Path;

import java.net.URL;
import java.util.Objects;
import java.util.stream.Collectors;

public class ArchiveFetcher implements EarlyFetcher {

  private final ImmutableList<URL> urls;
  private final ImmutableList<URL> patches;
  private final String integrity;
  private final String stripPrefix;

  ArchiveFetcher(ImmutableList<URL> urls, ImmutableList<URL> patches, String integrity,
      String stripPrefix) {
    this.urls = urls;
    this.patches = patches;
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

  public ImmutableMap<String, Object> toHttpArchiveAttrs(String repoName) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    builder.put("name", repoName)
        .put("urls", urls.stream().map(URL::toString).collect(Collectors.toList()))
        .put("sha256", integrity.substring(7));

    if (patches != null) {
      builder.put("remote_patches", patches.stream().map(URL::toString).collect(Collectors.toList()));
    }

    // TODO: add patch_strip to this class
    builder.put("patch_args", ImmutableList.of("-p0"));
    if (stripPrefix != null) {
      // TODO: implement integrity attribute in http_archive
      builder.put("strip_prefix", stripPrefix);
    }
    return builder.build();
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
        Objects.equals(patches, that.patches) &&
        Objects.equals(integrity, that.integrity) &&
        Objects.equals(stripPrefix, that.stripPrefix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(urls, patches, integrity, stripPrefix);
  }

  @Override
  public String toString() {
    return "ArchiveFetcher{" +
        "urls=" + urls +
        "patches=" + patches +
        ", integrity='" + integrity + '\'' +
        ", stripPrefix='" + stripPrefix + '\'' +
        '}';
  }
}
