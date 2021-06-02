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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;

@AutoValue
public abstract class RepoSpec {

  public static RepoSpec create(String bzlFile, String ruleClassName, ImmutableMap<String, Object> attributes) {
    return new AutoValue_RepoSpec(bzlFile, ruleClassName, attributes);
  }

  public static RepoSpec create(String ruleClassName, ImmutableMap<String, Object> attributes) {
    return new AutoValue_RepoSpec(null, ruleClassName, attributes);
  }

  // The label string for the bzl file this repository rule is defined in, null for native repo rule
  @Nullable
  public abstract String getBzlFile();

  public abstract String getRuleClassName();

  public abstract ImmutableMap<String, Object> getAttributes();

  public boolean isNativeRepoRule() {
    return getBzlFile() == null;
  }

  // Return a string representing the rule class
  // eg. Native repo rule: local_repository
  //     Starlark repo rule: //:repo.bzl%my_repo
  public String getRuleClass() {
    return (isNativeRepoRule() ? "" : getBzlFile() + "%") + getRuleClassName();
  }
}
