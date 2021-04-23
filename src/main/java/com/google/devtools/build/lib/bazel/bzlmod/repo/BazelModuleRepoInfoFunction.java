package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.bazel.bzlmod.ArchiveFetcher;
import com.google.devtools.build.lib.bazel.bzlmod.Fetcher;
import com.google.devtools.build.lib.bazel.bzlmod.Module;
import com.google.devtools.build.lib.bazel.bzlmod.ModuleKey;
import com.google.devtools.build.lib.bazel.bzlmod.SelectionValue;
import com.google.devtools.build.lib.bazel.bzlmod.repo.RepositoryInfo.Builder;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyFunctionException.Transience;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.annotation.Nullable;

public class BazelModuleRepoInfoFunction implements SkyFunction {

  private static final String HTTP_ARCHIVE = "@bazel_tools//tools/build_defs/repo:http.bzl%http_archive";

  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws SkyFunctionException, InterruptedException {
    Preconditions.checkArgument(skyKey == BazelModuleRepoInfoValue.key());
    SelectionValue selectionValue = (SelectionValue) env.getValue(SelectionValue.KEY);
    if (selectionValue == null) {
      return null;
    }

    ImmutableMap.Builder<String, RepositoryInfo> repositories = ImmutableMap.builder();

    for (ModuleKey moduleKey : selectionValue.getDepGraph().keySet()) {
      if (!moduleKey.getName().equals(selectionValue.getRootModuleName())) {
        try {
          Module module = selectionValue.getDepGraph().get(moduleKey);
          Fetcher fetcher;
          if (module.getFetcher() != null) {
            fetcher = module.getFetcher();
          } else {
            fetcher = module.getRegistry().getFetcher(moduleKey, env.getListener());
          }
          if (fetcher instanceof ArchiveFetcher) {
            ArchiveFetcher archiveFetcher = (ArchiveFetcher) fetcher;
            RepositoryInfo.Builder builder = new Builder();
            String name = "@" + moduleKey.getName();
            RepositoryInfo repositoryInfo = builder.setName(name)
                .setRuleClass(HTTP_ARCHIVE)
                .setAttributes(archiveFetcher.toHttpArchiveAttrs(moduleKey.getName()))
                // TODO: implement repo_deps
                .setRepoDeps(ImmutableList.of())
                // TODO: implement repo_mappings
                .setRepoMappings(ImmutableMap.of())
                // TODO: impement vendor_dir
                .setVendorDir("")
                .build();
            repositories.put(name, repositoryInfo);
          }
          // TODO: implement for other fetcher type
        } catch (IOException e) {
          throw new BazelModuleRepoInfoFunctionException(e, Transience.PERSISTENT);
        }
      }
    }
    return new BazelModuleRepoInfoValue(repositories.build());
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
    BazelModuleRepoInfoFunctionException(URISyntaxException e, Transience transience) {
      super(e, transience);
    }
  }
}
