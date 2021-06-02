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
// limitations under the License

package com.google.devtools.build.lib.bazel.bzlmod;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RepoSpecTest {

  @Test
  public void nativeRepoSpecTest() {
    RepoSpec repoSpec = RepoSpec.create("local_repository",
        ImmutableMap.of("path", "/foo/bar"));
    assertThat(repoSpec.isNativeRepoRule()).isTrue();
    assertThat(repoSpec.getRuleClassName()).isEqualTo("local_repository");
    assertThat(repoSpec.getRuleClass()).isEqualTo("local_repository");
    assertThat(repoSpec.getAttributes()).isEqualTo(ImmutableMap.of("path", "/foo/bar"));
  }

  @Test
  public void starlarkRepoSpecTest() {
    RepoSpec repoSpec = RepoSpec.create("//pkg:repo.bzl", "my_repo",
        ImmutableMap.of("attr1", "foo", "attr2", "bar"));
    assertThat(repoSpec.isNativeRepoRule()).isFalse();
    assertThat(repoSpec.getBzlFile()).isEqualTo("//pkg:repo.bzl");
    assertThat(repoSpec.getRuleClassName()).isEqualTo("my_repo");
    assertThat(repoSpec.getRuleClass()).isEqualTo("//pkg:repo.bzl%my_repo");
    assertThat(repoSpec.getAttributes()).isEqualTo(
        ImmutableMap.of("attr1", "foo", "attr2", "bar"));
  }
}
