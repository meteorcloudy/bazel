package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.bazel.bzlmod.Fetcher;
import com.google.devtools.build.lib.bazel.bzlmod.Module;
import com.google.devtools.build.lib.bazel.bzlmod.ModuleKey;
import com.google.devtools.build.lib.bazel.bzlmod.SelectionValue;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyFunctionException.Transience;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

import java.io.IOException;

import javax.annotation.Nullable;

public class BazelModuleRepoSpecFunction implements SkyFunction {


  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws SkyFunctionException, InterruptedException {
    Preconditions.checkArgument(skyKey == BazelModuleRepoSpecValue.KEY);
    SelectionValue selectionValue = (SelectionValue) env.getValue(SelectionValue.KEY);
    if (selectionValue == null) {
      return null;
    }

    ImmutableMap.Builder<String, RepoSpec> repositories = ImmutableMap.builder();

    for (ModuleKey moduleKey : selectionValue.getDepGraph().keySet()) {
      if (moduleKey.getName().equals(selectionValue.getRootModuleName())) {
        // TODO: use information for root module for repo_deps and repo_mapping
        continue;
      }
      try {
        Module module = selectionValue.getDepGraph().get(moduleKey);
        Fetcher fetcher;
        if (module.getFetcher() != null) {
          fetcher = module.getFetcher();
        } else {
          fetcher = module.getRegistry().getFetcher(moduleKey, env.getListener());
        }
        // TODO: calculate the real repo name
        String repoName = moduleKey.getName();
        RepoSpec repoSpec = new RepoSpec(fetcher.getRuleClass(), fetcher.getRuleAttrs(repoName));
        repositories.put(repoName, repoSpec);
      } catch (IOException e) {
        throw new BazelModuleRepoInfoFunctionException(e, Transience.PERSISTENT);
      }
    }
    return new BazelModuleRepoSpecValue(repositories.build());
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }

  private static final class BazelModuleRepoInfoFunctionException extends SkyFunctionException {

    BazelModuleRepoInfoFunctionException(IOException e, Transience transience) {
      super(e, transience);
    }
  }
}
