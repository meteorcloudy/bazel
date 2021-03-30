package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.devtools.build.lib.events.ExtendedEventHandler;
import java.io.IOException;
import java.util.Optional;

public interface Registry {

  String getUrl();

  /**
   * Returns Optional.empty() when the module is not found in this registry.
   */
  Optional<byte[]> getModuleFile(ModuleKey key, ExtendedEventHandler eventHandler)
      throws IOException, InterruptedException;

  Fetcher getFetcher(ModuleKey key, ExtendedEventHandler eventHandler)
      throws IOException, InterruptedException;
}
