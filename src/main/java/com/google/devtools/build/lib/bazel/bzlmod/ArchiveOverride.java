package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ArchiveOverride implements NonRegistryOverride {

  public static ArchiveOverride create(String url, String integrity) {
    return new AutoValue_ArchiveOverride(url, integrity);
  }

  public abstract String getUrl();

  public abstract String getIntegrity();

  @Override
  public EarlyFetcher toEarlyFetcher(FetcherFactory fetcherFactory) {
    return fetcherFactory.createArchiveFetcher(getUrl(), getIntegrity());
  }
}
