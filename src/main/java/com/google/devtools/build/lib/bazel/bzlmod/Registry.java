package com.google.devtools.build.lib.bazel.bzlmod;

import java.util.Optional;

public interface Registry {

  String getUrl();

  /**
   * Returns Optional.empty() when the module is not found in this registry.
   */
  Optional<byte[]> getModuleFile(ModuleKey key);

  /**
   * Returns Optional.empty() when the module is not found in this registry.
   */
  Optional<Fetcher> getFetcher(ModuleKey key);
}
