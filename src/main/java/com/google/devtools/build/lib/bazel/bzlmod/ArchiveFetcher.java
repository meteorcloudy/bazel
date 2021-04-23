package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.vfs.Path;

import java.net.URL;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class ArchiveFetcher implements EarlyFetcher {

  private static final String HTTP_ARCHIVE_RULE_CLASS =
      "@bazel_tools//tools/build_defs/repo:http.bzl%http_archive";

  private final ImmutableList<URL> urls;
  private final ImmutableList<URL> patches;
  private final String integrity;
  private final String stripPrefix;
  private final int patchStrip;

  ArchiveFetcher(ImmutableList<URL> urls, ImmutableList<URL> patches, @Nullable String integrity,
      @Nullable String stripPrefix, int patchStrip) {
    this.urls = urls;
    this.patches = patches;
    this.integrity = integrity;
    this.stripPrefix = stripPrefix;
    this.patchStrip = patchStrip;
  }

  @Override
  public Path earlyFetch() {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public Path fetch(String repoName, Path vendorDir) {
    throw new UnsupportedOperationException("not implemented yet");
  }

  @Override
  public String getRuleClass() {
    return HTTP_ARCHIVE_RULE_CLASS;
  }

  @Override
  public ImmutableMap<String, Object> getRuleAttrs(String repoName) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    builder.put("name", repoName)
        .put("urls", urls.stream().map(URL::toString).collect(Collectors.toList()))
        // TODO: implement integrity attribute in http_archive
        .put("sha256", integrity.substring(7));

    if (patches != null) {
      builder.put(
          "remote_patches",
          patches.stream().map(URL::toString).collect(Collectors.toList()));
    }

    builder.put("patch_args", ImmutableList.of("-p" + patchStrip));
    if (stripPrefix != null) {
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
        Objects.equals(stripPrefix, that.stripPrefix) &&
        patchStrip == that.patchStrip;
  }

  @Override
  public int hashCode() {
    return Objects.hash(urls, patches, integrity, stripPrefix, patchStrip);
  }

  @Override
  public String toString() {
    return "ArchiveFetcher{" +
        "urls=" + urls +
        ", patches=" + patches +
        ", integrity='" + integrity + '\'' +
        ", stripPrefix='" + stripPrefix + '\'' +
        ", patchStrip=" + patchStrip +
        '}';
  }
}
