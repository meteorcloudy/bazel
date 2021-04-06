package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.devtools.build.lib.starlarkbuildapi.repository.StarlarkOverrideApi;

public interface RegistryOverride extends StarlarkOverrideApi {

  String getRegistry();
}
