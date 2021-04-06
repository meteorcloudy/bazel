package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.devtools.build.lib.starlarkbuildapi.repository.StarlarkOverrideApi;

public interface NonRegistryOverride extends StarlarkOverrideApi {

  EarlyFetcher toEarlyFetcher(FetcherFactory fetcherFactory);
}
