package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.net.URL;

@AutoValue
public abstract class ArchiveOverride implements NonRegistryOverride {

  public static ArchiveOverride create(ImmutableList<URL> urls, String integrity,
      String stripPrefix) {
    return new AutoValue_ArchiveOverride(urls, integrity, stripPrefix);
  }

  public abstract ImmutableList<URL> getUrls();

  public abstract String getIntegrity();

  public abstract String getStripPrefix();

  @Override
  public EarlyFetcher toEarlyFetcher(FetcherFactory fetcherFactory) {
    return fetcherFactory.createArchiveFetcher(getUrls(), getIntegrity(), getStripPrefix());
  }
}
