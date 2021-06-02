// Copyright 2021 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

public class RepoSpecsValue implements SkyValue {
  public static final SkyFunctionName REPO_SPECS =
      SkyFunctionName.createHermetic("REPO_SPECS");

  @AutoCodec
  public static final SkyKey KEY_FOR_OVERRIDE_DEP = () -> REPO_SPECS;

  @AutoCodec
  public static final SkyKey KEY_FOR_BAZEL_MODULE = () -> REPO_SPECS;

  @AutoCodec
  public static final SkyKey KEY_FOR_MODULE_RULE = () -> REPO_SPECS;

  private final ImmutableMap<String, RepoSpec> repositories;

  public RepoSpecsValue(ImmutableMap<String, RepoSpec> repositories) {
    this.repositories = repositories;
  }

  public RepoSpec getRepository(String repositoryName) {
    return repositories.get(repositoryName);
  }
}
