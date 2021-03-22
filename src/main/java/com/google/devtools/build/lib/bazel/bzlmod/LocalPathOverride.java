package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class LocalPathOverride implements NonRegistryOverride {
  public static LocalPathOverride create(String path) {
    return new AutoValue_LocalPathOverride(path);
  }

  public abstract String getPath();

  @Override
  public EarlyFetcher toEarlyFetcher(FetcherFactory fetcherFactory) {
    return fetcherFactory.createLocalPathFetcher(getPath());
  }
}
