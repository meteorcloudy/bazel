package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.devtools.build.lib.vfs.Path;

public interface Fetcher {

  Path fetch(String repoName, Path vendorDir);
}
