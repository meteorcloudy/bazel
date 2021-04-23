package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.vfs.Path;

public interface Fetcher {

  Path fetch(String repoName, Path vendorDir);

  String getRuleClass();

  ImmutableMap<String, Object> getRuleAttrs(String repoName);
}
