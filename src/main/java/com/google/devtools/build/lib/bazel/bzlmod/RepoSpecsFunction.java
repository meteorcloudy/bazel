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
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

import javax.annotation.Nullable;

public class RepoSpecsFunction implements SkyFunction {

  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws SkyFunctionException, InterruptedException {

    if (skyKey == RepoSpecsValue.KEY_FOR_OVERRIDE_DEP) {
      return computeForOverrideDep(env);
    } else if (skyKey == RepoSpecsValue.KEY_FOR_BAZEL_MODULE) {
      return computeForBazelModule(env);
    } else if (skyKey == RepoSpecsValue.KEY_FOR_MODULE_RULE) {
      return computeForModuleRule(env);
    }
    throw new IllegalArgumentException("Unrecognized key: " + skyKey.toString());
  }

  @Nullable
  private SkyValue computeForOverrideDep(Environment env) {
    ImmutableMap.Builder<String, RepoSpec> repositories = ImmutableMap.builder();
    return new RepoSpecsValue(repositories.build());
  }

  @Nullable
  private SkyValue computeForBazelModule(Environment env) {
    ImmutableMap.Builder<String, RepoSpec> repositories = ImmutableMap.builder();
    return new RepoSpecsValue(repositories.build());
  }

  @Nullable
  private SkyValue computeForModuleRule(Environment env) {
    ImmutableMap.Builder<String, RepoSpec> repositories = ImmutableMap.builder();
    return new RepoSpecsValue(repositories.build());
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }
}
