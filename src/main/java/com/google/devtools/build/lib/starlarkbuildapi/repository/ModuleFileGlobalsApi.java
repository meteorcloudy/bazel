// Copyright 2019 The Bazel Authors. All rights reserved.
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
//

package com.google.devtools.build.lib.starlarkbuildapi.repository;

import com.google.devtools.build.docgen.annot.DocumentMethods;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkInt;

/**
 * A collection of global Starlark build API functions that apply to WORKSPACE files.
 */
@DocumentMethods
public interface ModuleFileGlobalsApi<ModuleFileFunctionExceptionT extends Exception> {

  @StarlarkMethod(
      name = "module",
      doc = "TODO",
      parameters = {
          @Param(
              name = "name",
              doc = "the name of the module.",
              named = true,
              positional = false,
              defaultValue = "''"),
          @Param(
              name = "version",
              doc = "the version of the module.",
              named = true,
              positional = false,
              defaultValue = "''"),
          // TODO(wyv): compatibility_level, bazel_compatibility, module_rule_exports, toolchains
          //   & platforms
      },
      extraKeywords = @Param(name = "kwargs"))
  void module(String name, String version, Dict<String, Object> kwargs)
      throws EvalException, InterruptedException;

  @StarlarkMethod(
      name = "bazel_dep",
      doc = "TODO",
      parameters = {
          @Param(
              name = "name",
              doc = "the name of the module.",
              named = true,
              positional = false),
          @Param(
              name = "version",
              doc = "the version of the module.",
              named = true,
              positional = false),
          @Param(
              name = "repo_name",
              doc = "the name of the resultant repo.",
              named = true,
              positional = false,
              defaultValue = "''"),
      })
  void bazelDep(String name, String version, String repoName)
      throws EvalException, InterruptedException;

  @StarlarkMethod(
      name = "override_dep",
      doc = "TODO",
      parameters = {
          @Param(
              name = "name",
              doc = "the name of the Bazel module dependency to apply an override to.",
              named = true,
              positional = false),
          @Param(
              name = "override",
              doc = "the actual override to apply.",
              named = true,
              positional = false),
      })
  void overrideDep(String name, StarlarkOverrideApi override)
      throws EvalException, InterruptedException;

  @StarlarkMethod(
      name = "single_version_override",
      doc = "TODO",
      parameters = {
          @Param(
              name = "version",
              doc = "TODO",
              named = true,
              positional = false,
              defaultValue = "''"),
          @Param(
              name = "registry",
              doc = "TODO",
              named = true,
              positional = false,
              defaultValue = "''"),
          @Param(
              name = "patches",
              doc = "TODO",
              named = true,
              positional = false,
              defaultValue = "[]"),
          @Param(
              name = "patch_strip",
              doc = "TODO",
              named = true,
              positional = false,
              defaultValue = "0"),
      })
  StarlarkOverrideApi singleVersionOverride(String version, String registry, Iterable<?> patches,
      StarlarkInt patchStrip) throws EvalException;

  @StarlarkMethod(
      name = "archive_override",
      doc = "TODO",
      parameters = {
          @Param(
              name = "urls",
              allowedTypes = {
                  @ParamType(type = String.class),
                  @ParamType(type = Iterable.class, generic1 = String.class),
              },
              doc = "TODO",
              named = true,
              positional = false),
          @Param(
              name = "integrity",
              doc = "TODO",
              named = true,
              positional = false,
              defaultValue = "''"),
          @Param(
              name = "strip_prefix",
              doc = "TODO",
              named = true,
              positional = false,
              defaultValue = "''"),
          @Param(
              name = "patches",
              doc = "TODO",
              named = true,
              positional = false,
              defaultValue = "[]"),
          @Param(
              name = "patch_strip",
              doc = "TODO",
              named = true,
              positional = false,
              defaultValue = "0"),
      })
  StarlarkOverrideApi archiveOverride(Object urls, String integrity, String stripPrefix,
      Iterable<?> patches, StarlarkInt patchStrip)
      throws EvalException, ModuleFileFunctionExceptionT;

  @StarlarkMethod(
      name = "git_override",
      doc = "TODO",
      parameters = {
          @Param(
              name = "remote",
              doc = "TODO",
              named = true,
              positional = false),
          @Param(
              name = "commit",
              doc = "TODO",
              named = true,
              positional = false,
              defaultValue = "''"),
          @Param(
              name = "patches",
              doc = "TODO",
              named = true,
              positional = false,
              defaultValue = "[]"),
          @Param(
              name = "patch_strip",
              doc = "TODO",
              named = true,
              positional = false,
              defaultValue = "0"),
      })
  StarlarkOverrideApi gitOverride(String remote, String commit, Iterable<?> patches,
      StarlarkInt patchStrip) throws EvalException, ModuleFileFunctionExceptionT;

  @StarlarkMethod(
      name = "local_path_override",
      doc = "TODO",
      parameters = {
          @Param(
              name = "path",
              doc = "TODO",
              named = true,
              positional = false),
      })
  StarlarkOverrideApi localPathOverride(String path);

  // TODO: multiple_version_override, git_override
}
