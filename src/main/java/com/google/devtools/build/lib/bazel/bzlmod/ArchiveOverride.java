package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.net.URL;

@AutoValue
public abstract class ArchiveOverride implements NonRegistryOverride {

  public static ArchiveOverride create(ImmutableList<URL> urls, ImmutableList<URL> patches,
      String integrity, String stripPrefix, int patchStrip) {
    return new AutoValue_ArchiveOverride(urls, patches, integrity, stripPrefix, patchStrip);
  }

  public abstract ImmutableList<URL> getUrls();

  public abstract ImmutableList<URL> getPatches();

  public abstract String getIntegrity();

  public abstract String getStripPrefix();

  public abstract int getPatchStrip();

  @Override
  public EarlyFetcher toEarlyFetcher(FetcherFactory fetcherFactory) {
    return fetcherFactory.createArchiveFetcher(getUrls(), getPatches(), getIntegrity(),
        getStripPrefix(), getPatchStrip());
  }
}
