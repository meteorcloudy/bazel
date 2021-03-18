package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.skyframe.SkyValue;

import java.io.FileNotFoundException;

// TODO(pcloudy): implement the actual SkyFunction for this
public class ResolvedBazelModuleRepositoriesValue extends ResolvedRepositoryValue implements SkyValue {
  private static ImmutableList<RepositoryInfo> repositories;

  public static ImmutableList<RepositoryInfo> getRepo() {
    if (repositories != null) {
      return repositories;
    }

    try {
      repositories = ResolvedRepositoryValue.loadRepositoryFromFile("/Users/pcloudy/workspace/bazel/native_module_repos.json");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    return repositories;
  }
}
