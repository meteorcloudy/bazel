package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.devtools.build.lib.bazel.repository.downloader.HttpDownloader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.function.Supplier;

public class RegistryFactoryImpl implements RegistryFactory {

  private final HttpDownloader httpDownloader;
  private final Supplier<Map<String, String>> clientEnvironmentSupplier;

  public RegistryFactoryImpl(HttpDownloader httpDownloader,
      Supplier<Map<String, String>> clientEnvironmentSupplier) {
    this.httpDownloader = httpDownloader;
    this.clientEnvironmentSupplier = clientEnvironmentSupplier;
  }

  @Override
  public Registry getRegistryWithUrl(String url) throws URISyntaxException {
    URI uri = new URI(url);
    switch (uri.getScheme()) {
      case "http":
      case "https":
      case "file":
        return new IndexRegistry(uri, httpDownloader, clientEnvironmentSupplier.get());
    }
    return null;
  }
}
