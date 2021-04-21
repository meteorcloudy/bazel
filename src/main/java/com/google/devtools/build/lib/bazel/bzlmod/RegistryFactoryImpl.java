package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.devtools.build.lib.bazel.repository.downloader.HttpDownloader;
import com.google.devtools.build.lib.vfs.Path;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class RegistryFactoryImpl implements RegistryFactory {

  private final HttpDownloader httpDownloader;
  private final Map<String, String> clientEnv;
  private final FetcherFactory fetcherFactory;

  public RegistryFactoryImpl(HttpDownloader httpDownloader, Map<String, String> clientEnv,
      FetcherFactory fetcherFactory) {
    this.httpDownloader = httpDownloader;
    this.clientEnv = clientEnv;
    this.fetcherFactory = fetcherFactory;
  }

  @Override
  public Registry getRegistryWithUrl(String url) throws URISyntaxException {
    URI uri = new URI(url);
    switch (uri.getScheme()) {
      case "http":
      case "https":
      case "file":
        return new IndexRegistry(uri, httpDownloader, clientEnv, fetcherFactory);
    }
    return null;
  }
}
