// Copyright 2018 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.skyframe;

import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.bazel.bzlmod.Module;
import com.google.devtools.build.lib.bazel.bzlmod.ModuleKey;
import com.google.devtools.build.lib.bazel.bzlmod.SelectionValue;
import com.google.devtools.build.lib.cmdline.LabelConstants;
import com.google.devtools.build.lib.cmdline.RepositoryName;
import com.google.devtools.build.lib.packages.BuildFileContainsErrorsException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

import java.util.Map;

import javax.annotation.Nullable;

/** {@link SkyFunction} for {@link RepositoryMappingValue}s. */
public class RepositoryMappingFunction implements SkyFunction {

  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws SkyFunctionException, InterruptedException {
    SelectionValue selectionValue = (SelectionValue) env.getValue(SelectionValue.KEY);
    if (selectionValue == null) {
      return null;
    }

    // TODO: upgrade this when implementing multiple version override and module rule.
    // Now we assume the canonical repo name is the module name.
    RepositoryName name = (RepositoryName) skyKey.argument();
    String moduleName = name.isMain() ? selectionValue.getRootModuleName() : name.strippedName();

    ImmutableMap<RepositoryName, RepositoryName> mapping = null;
    for (Map.Entry<ModuleKey, Module> entry : selectionValue.getDepGraph().entrySet()) {
      if (entry.getKey().getName().equals(moduleName)) {
        ImmutableMap.Builder<RepositoryName, RepositoryName> builder = ImmutableMap.builder();
        for (Map.Entry<String, ModuleKey> dep : entry.getValue().getDeps().entrySet()) {
          builder.put(RepositoryName.createFromValidStrippedName(dep.getKey()),
              RepositoryName.createFromValidStrippedName(dep.getValue().getName()));
        }
        mapping = builder.build();
        break;
      }
    }

    if (mapping != null) {
      return RepositoryMappingValue.withMapping(mapping);
    }

    return computeFromWorkspace(skyKey, env);
  }

  private SkyValue computeFromWorkspace(SkyKey skyKey, Environment env)
      throws InterruptedException, RepositoryMappingFunctionException {
    SkyKey externalPackageKey = PackageValue.key(LabelConstants.EXTERNAL_PACKAGE_IDENTIFIER);
    PackageValue externalPackageValue = (PackageValue) env.getValue(externalPackageKey);
    if (env.valuesMissing()) {
      return null;
    }
    Package externalPackage = externalPackageValue.getPackage();
    if (externalPackage.containsErrors()) {
      throw new RepositoryMappingFunctionException();
    }

    ImmutableMap<RepositoryName, RepositoryName> mapping =
        externalPackage.getRepositoryMapping((RepositoryName) skyKey.argument());
    return RepositoryMappingValue.withMapping(mapping);
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }

  private static class RepositoryMappingFunctionException extends SkyFunctionException {
    RepositoryMappingFunctionException() {
      super(
          new BuildFileContainsErrorsException(LabelConstants.EXTERNAL_PACKAGE_IDENTIFIER),
          Transience.PERSISTENT);
    }
  }
}
