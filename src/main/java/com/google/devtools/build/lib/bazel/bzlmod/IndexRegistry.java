package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.bazel.bzlmod.repo.RepoSpec;
import com.google.devtools.build.lib.bazel.repository.downloader.HttpDownloader;
import com.google.devtools.build.lib.events.ExtendedEventHandler;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.starlark.java.eval.StarlarkInt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class IndexRegistry implements Registry {

  private static final String HTTP_ARCHIVE_RULE_CLASS =
      "@bazel_tools//tools/build_defs/repo:http.bzl%http_archive";

  private final URI uri;
  private final HttpDownloader httpDownloader;
  private final Map<String, String> clientEnv;
  private final Gson gson;

  IndexRegistry(URI uri, HttpDownloader httpDownloader, Map<String, String> clientEnv) {
    this.uri = uri;
    this.httpDownloader = httpDownloader;
    this.clientEnv = clientEnv;
    this.gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
  }

  @Override
  public String getUrl() {
    return uri.toString();
  }

  private Optional<byte[]> grabFile(String relativePath, ExtendedEventHandler eventHandler)
      throws IOException, InterruptedException {
    String plainBaseUrl = getUrl();
    if (!plainBaseUrl.endsWith("/")) {
      plainBaseUrl += "/";
    }
    try {
      return Optional.of(httpDownloader
          .downloadAndReadOneUrl(new URL(plainBaseUrl + relativePath), eventHandler, clientEnv));
    } catch (FileNotFoundException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<byte[]> getModuleFile(ModuleKey key, ExtendedEventHandler eventHandler)
      throws IOException, InterruptedException {
    return grabFile(String.format("modules/%s/%s/MODULE.bazel", key.getName(), key.getVersion()),
        eventHandler);
  }

  private static class BazelRegistryJson {

    String[] mirrors;
  }

  private static class SourceJson {

    URL url;
    String integrity;
    String stripPrefix;
    Map<String, String> patches;
    int patchStrip;
  }

  private <T> Optional<T> grabJson(String relativePath, Class<T> klass,
      ExtendedEventHandler eventHandler)
      throws IOException, InterruptedException {
    Optional<byte[]> bytes = grabFile(relativePath, eventHandler);
    if (!bytes.isPresent()) {
      return Optional.empty();
    }
    String jsonString = new String(bytes.get(), StandardCharsets.UTF_8);
    return Optional.of(gson.fromJson(jsonString, klass));
  }

  @Override
  public RepoSpec getRepoSpec(ModuleKey key, String repoName, ExtendedEventHandler eventHandler)
      throws IOException, InterruptedException {
    Optional<BazelRegistryJson> bazelRegistryJson =
        grabJson("bazel_registry.json", BazelRegistryJson.class, eventHandler);
    Optional<SourceJson> sourceJson = grabJson(
        String.format("modules/%s/%s/source.json", key.getName(), key.getVersion()),
        SourceJson.class, eventHandler);
    if (!sourceJson.isPresent()) {
      throw new FileNotFoundException(
          String.format("Module %s's source information not found in registry %s", key, getUrl()));
    }
    if (sourceJson.get().integrity == null) {
      throw new IOException(String.format("Missing integrity for module %s", key));
    }
    URL sourceUrl = sourceJson.get().url;
    ImmutableList.Builder<String> urls = new ImmutableList.Builder<>();
    if (bazelRegistryJson.isPresent() && bazelRegistryJson.get().mirrors != null) {
      for (String mirror : bazelRegistryJson.get().mirrors) {
        StringBuilder url = new StringBuilder(mirror);
        if (url.charAt(url.length() - 1) != '/') {
          url.append('/');
        }
        url.append(sourceUrl.getAuthority());
        if (url.charAt(url.length() - 1) != '/' && sourceUrl.getFile().charAt(0) != '/') {
          url.append('/');
        }
        url.append(sourceUrl.getFile());
        urls.add(url.toString());
      }
    }
    urls.add(sourceUrl.toString());

    // Build remote patches as key value pair as "url" => "integrity"
    ImmutableMap.Builder<String, String> remotePatches = new ImmutableMap.Builder<>();
    if (sourceJson.get().patches != null) {
      String plainBaseUrl = getUrl();
      if (!plainBaseUrl.endsWith("/")) {
        plainBaseUrl += "/";
      }
      for (Map.Entry<String, String> entry : sourceJson.get().patches.entrySet()) {
        remotePatches.put(
            plainBaseUrl + String.format(
                "modules/%s/%s/patches/%s", key.getName(), key.getVersion(), entry.getKey()),
            entry.getValue());
      }
    }
    return getRepoSpecForArchive(
        /* repoName= */ repoName,
        /* urls= */ urls.build(),
        /* integrity= */ sourceJson.get().integrity,
        /* stripPrefix= */ Strings.nullToEmpty(sourceJson.get().stripPrefix),
        /* patches= */ ImmutableList.of(),
        /* patchStrip= */ 0,
        /* remotePatches= */ remotePatches.build(),
        /* remotePatchStrip= */ sourceJson.get().patchStrip);
  }

  public static RepoSpec getRepoSpecForArchive(String repoName, ImmutableList<String> urls,
      String integrity, String stripPrefix,
      ImmutableList<String> patches, int patchStrip,
      ImmutableMap<String, String> remotePatches, int remotePatchStrip) {
    ImmutableMap.Builder<String, Object> attrBuilder = ImmutableMap.builder();
    attrBuilder.put("name", repoName)
        .put("urls", urls)
        .put("integrity", integrity)
        .put("strip_prefix", stripPrefix)
        .put("patches", patches)
        .put("patch_args", ImmutableList.of("-p" + patchStrip))
        .put("remote_patches", remotePatches)
        .put("remote_patch_strip", StarlarkInt.of(remotePatchStrip));
    return new RepoSpec(HTTP_ARCHIVE_RULE_CLASS, attrBuilder.build());
  }
}
