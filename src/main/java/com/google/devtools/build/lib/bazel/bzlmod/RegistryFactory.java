package com.google.devtools.build.lib.bazel.bzlmod;

public interface RegistryFactory {

  Registry getRegistryWithUrl(String url);
}
