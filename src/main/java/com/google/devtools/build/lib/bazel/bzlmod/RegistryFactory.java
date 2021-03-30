package com.google.devtools.build.lib.bazel.bzlmod;

import java.net.URISyntaxException;

public interface RegistryFactory {

  Registry getRegistryWithUrl(String url) throws URISyntaxException;
}
