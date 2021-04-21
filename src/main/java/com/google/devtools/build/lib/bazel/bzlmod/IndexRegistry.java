package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.bazel.repository.downloader.HttpDownloader;
import com.google.devtools.build.lib.events.ExtendedEventHandler;
import com.google.devtools.build.lib.vfs.Path;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class IndexRegistry implements Registry {

  private final URI uri;
  private final HttpDownloader httpDownloader;
  private final Map<String, String> clientEnv;
  private final FetcherFactory fetcherFactory;
  private final Gson gson;

  IndexRegistry(URI uri, HttpDownloader httpDownloader, Map<String, String> clientEnv,
      FetcherFactory fetcherFactory) {
    this.uri = uri;
    this.httpDownloader = httpDownloader;
    this.clientEnv = clientEnv;
    this.fetcherFactory = fetcherFactory;
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
    String[] patchFiles;
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
  public Fetcher getFetcher(ModuleKey key, ExtendedEventHandler eventHandler)
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
    URL sourceUrl = sourceJson.get().url;
    ImmutableList.Builder<URL> urls = new ImmutableList.Builder<>();
    if (bazelRegistryJson.isPresent()) {
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
        urls.add(new URL(url.toString()));
      }
    }
    urls.add(sourceUrl);
    return fetcherFactory.createArchiveFetcher(
        urls.build(),
        sourceJson.get().integrity,
        sourceJson.get().stripPrefix);
  }
}
