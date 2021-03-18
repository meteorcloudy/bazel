package com.google.devtools.build.lib.skyframe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.analysis.BlazeDirectories;
import com.google.devtools.build.lib.bazel.bzlmod.BzlmodRepoRuleCreator;
import com.google.devtools.build.lib.bazel.bzlmod.BzlmodRepoRuleValue;
import com.google.devtools.build.lib.bazel.bzlmod.BzlmodRepoRuleValue.BzlmodRepoRuleKey;
import com.google.devtools.build.lib.bazel.bzlmod.RepositoryInfo;
import com.google.devtools.build.lib.bazel.bzlmod.ResolvedBazelModuleRepositoriesValue;
import com.google.devtools.build.lib.bazel.bzlmod.ResolvedModuleRuleRepositoriesValue;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.packages.NoSuchPackageException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.PackageFactory;
import com.google.devtools.build.lib.packages.Rule;
import com.google.devtools.build.lib.packages.RuleClassProvider;
import com.google.devtools.build.lib.packages.RuleFactory.InvalidRuleException;
import com.google.devtools.build.lib.util.Pair;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

import net.starlark.java.eval.Module;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.syntax.Location;

import javax.annotation.Nullable;

public class BzlmodRepoRuleFunction implements SkyFunction {

  private final PackageFactory packageFactory;
  private final RuleClassProvider ruleClassProvider;
  private final BlazeDirectories directories;
  private static final PackageIdentifier rootPackage = PackageIdentifier.createInMainRepo("");

  public BzlmodRepoRuleFunction(
      PackageFactory packageFactory,
      RuleClassProvider ruleClassProvider,
      BlazeDirectories directories) {
    this.packageFactory = packageFactory;
    this.ruleClassProvider = ruleClassProvider;
    this.directories = directories;
  }

  @Nullable
  @Override
  public SkyValue compute(SkyKey skyKey, Environment env)
      throws SkyFunctionException, InterruptedException {
    StarlarkSemantics starlarkSemantics = PrecomputedValue.STARLARK_SEMANTICS.get(env);
    if (starlarkSemantics == null) {
      return null;
    }

    String repositoryName = ((BzlmodRepoRuleKey) skyKey).getRepositoryName();
    boolean forModuleRuleResolve = ((BzlmodRepoRuleKey) skyKey).isForModuleRuleResolve();
    RepositoryInfo repositoryInfo = null;

    // Look for repository derived from native Bazel Modules
    for (RepositoryInfo info : ResolvedBazelModuleRepositoriesValue.getRepo()) {
      if (info.getName().equals(repositoryName)) {
        repositoryInfo = info;
      }
    }

    // Look for repository derived from module rules
    if (repositoryInfo == null && forModuleRuleResolve) {
      for (RepositoryInfo info : ResolvedModuleRuleRepositoriesValue.getRepo()) {
        if (info.getName().equals(repositoryName)) {
          repositoryInfo = info;
        }
      }
    }

    if (repositoryInfo == null) {
      return BzlmodRepoRuleValue.REPO_RULE_NOT_FOUND_VALUE;
    }

    int pos = repositoryInfo.getRuleClass().indexOf("%");
    String bzlFile = repositoryInfo.getRuleClass().substring(0, pos);
    String ruleName = repositoryInfo.getRuleClass().substring(pos + 1);

    ImmutableList<Pair<String, Location>> programLoads = ImmutableList.of(Pair.of(bzlFile, Location.BUILTIN));

    ImmutableList<Label> loadLabels =
        BzlLoadFunction.getLoadLabels(
            env.getListener(), programLoads, rootPackage, /*repoMapping=*/ ImmutableMap.of());
    if (loadLabels == null) {
      // TODO: throw an exception
      System.out.println("Load label failed!");
    }

    RootedPath bzlmodFile =
        RootedPath.toRootedPath(
            Root.fromPath(directories.getWorkspace()), PathFragment.create("Moduel.bazel"));

    // Compute key for each load label.
    ImmutableList.Builder<BzlLoadValue.Key> keys =
        ImmutableList.builderWithExpectedSize(loadLabels.size());
    for (Label loadLabel : loadLabels) {
      keys.add(
          BzlLoadValue.keyForWorkspace(
              loadLabel,
              0,
              bzlmodFile));
    }

    // Load .bzl modules in parallel.
    ImmutableMap<String, Module> loadedModules = null;
    try {
      loadedModules =
          PackageFunction.loadBzlModules(
              env, rootPackage, programLoads, keys.build(), null);
    } catch (NoSuchPackageException | InterruptedException e) {
      // TODO: throw an exception
      System.out.println("Load bzl file failed!");
    }
    if (loadedModules == null) {
      return null;
    }

    BzlmodRepoRuleCreator repoRuleBuilder  = (BzlmodRepoRuleCreator) loadedModules.get(bzlFile).getGlobal(ruleName);

    Package.Builder pkg =
        packageFactory.newExternalPackageBuilder(
            bzlmodFile, ruleClassProvider.getRunfilesPrefix(), starlarkSemantics);

    Rule rule = null;
    try {
      rule = repoRuleBuilder.createRule(pkg, starlarkSemantics, repositoryInfo.getAttributes());
    } catch (InvalidRuleException e) {
      e.printStackTrace();
    }

    return new BzlmodRepoRuleValue(rule);
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }
}
