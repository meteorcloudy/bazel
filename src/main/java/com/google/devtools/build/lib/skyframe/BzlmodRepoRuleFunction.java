package com.google.devtools.build.lib.skyframe;

import com.google.common.base.Preconditions;
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
import com.google.devtools.build.lib.cmdline.LabelConstants;
import com.google.devtools.build.lib.cmdline.PackageIdentifier;
import com.google.devtools.build.lib.events.StoredEventHandler;
import com.google.devtools.build.lib.packages.NoSuchPackageException;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.PackageFactory;
import com.google.devtools.build.lib.packages.Rule;
import com.google.devtools.build.lib.packages.RuleClass;
import com.google.devtools.build.lib.packages.RuleClassProvider;
import com.google.devtools.build.lib.packages.RuleFactory;
import com.google.devtools.build.lib.packages.RuleFactory.BuildLangTypedAttributeValuesMap;
import com.google.devtools.build.lib.packages.RuleFactory.InvalidRuleException;
import com.google.devtools.build.lib.server.FailureDetails.PackageLoading;
import com.google.devtools.build.lib.util.Pair;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionException;
import com.google.devtools.build.skyframe.SkyFunctionException.Transience;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

import net.starlark.java.eval.Module;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread.CallStackEntry;
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
    boolean forModuleRuleResolution = ((BzlmodRepoRuleKey) skyKey).isForModuleRuleResolution();
    RepositoryInfo repositoryInfo = null;

    // Look for repositories derived from native Bazel Modules
    // TODO(pcloudy): Implement this lookup logic properly after we have the actual resolution
    //  implemented.
    for (RepositoryInfo info : ResolvedBazelModuleRepositoriesValue.getRepo()) {
      if (info.getName().equals(repositoryName)) {
        repositoryInfo = info;
      }
    }

    // Look for repositories derived from module rules if the repo is not requested for module rule
    // resolution.
    // TODO(pcloudy): Implement this lookup logic properly after we have the actual resolution
    //  implemented.
    if (repositoryInfo == null && !forModuleRuleResolution) {
      for (RepositoryInfo info : ResolvedModuleRuleRepositoriesValue.getRepo()) {
        if (info.getName().equals(repositoryName)) {
          repositoryInfo = info;
        }
      }
    }

    if (repositoryInfo == null) {
      return BzlmodRepoRuleValue.REPO_RULE_NOT_FOUND_VALUE;
    }

    if (isNativeRepoRule(repositoryInfo)) {
      return getNativeRepoRule(repositoryInfo, starlarkSemantics, env);
    } else {
      return getStarlarkRepoRule(repositoryInfo, starlarkSemantics, env);
    }
  }

  private boolean isNativeRepoRule(RepositoryInfo info) {
    // For native repo rule, the rule class name is just <rule class name>;
    // For Starlark repo rule, the rule class name is <label for bzl file>%<rule class name>
    // eg. Native: local_repository
    //     Starlark: //:repo.bzl%my_repo
    return !info.getRuleClass().contains("%");
  }

  // Create the external package builder, which is only for the convenience of creating
  // repository rules.
  private Package.Builder getExternalPackageBuilder(StarlarkSemantics semantics) {
    RootedPath bzlmodFile =
        RootedPath.toRootedPath(
            Root.fromPath(directories.getWorkspace()), LabelConstants.MODULE_DOT_BAZEL_FILE_NAME);

    Package.Builder pkg =
        packageFactory.newExternalPackageBuilder(
            bzlmodFile, ruleClassProvider.getRunfilesPrefix(), semantics);
    return pkg;
  }

  private BzlmodRepoRuleValue getNativeRepoRule(
      RepositoryInfo repositoryInfo, StarlarkSemantics semantics, Environment env)
      throws InterruptedException, BzlmodRepoRuleFunctionException {
    RuleClass ruleClass = ruleClassProvider.getRuleClassMap().get(repositoryInfo.getRuleClass());
    BuildLangTypedAttributeValuesMap attributeValues =
        new BuildLangTypedAttributeValuesMap(repositoryInfo.getAttributes());
    ImmutableList.Builder<CallStackEntry> callStack = ImmutableList.builder();
    callStack.add(new CallStackEntry("BzlmodRepoRuleFunction.getNativeRepoRule", Location.BUILTIN));
    Rule rule;
    try {
      Package.Builder pkg = getExternalPackageBuilder(semantics);
      rule = RuleFactory.createRule(
          pkg, ruleClass, attributeValues, env.getListener(), semantics, callStack.build());
      // We need to actually build the package so that the rule has the correct package reference.
      pkg.build();
    } catch (InvalidRuleException e) {
      throw new BzlmodRepoRuleFunctionException(e, Transience.PERSISTENT);
    } catch (NoSuchPackageException e) {
      throw new BzlmodRepoRuleFunctionException(e, Transience.PERSISTENT);
    }
    return new BzlmodRepoRuleValue(rule);
  }

  private BzlmodRepoRuleValue getStarlarkRepoRule(
      RepositoryInfo repositoryInfo, StarlarkSemantics semantics, Environment env)
      throws InterruptedException, BzlmodRepoRuleFunctionException {
    int pos = repositoryInfo.getRuleClass().indexOf("%");
    String bzlFile = repositoryInfo.getRuleClass().substring(0, pos);
    String ruleName = repositoryInfo.getRuleClass().substring(pos + 1);

    ImmutableList<Pair<String, Location>> programLoads =
        ImmutableList.of(Pair.of(bzlFile, Location.BUILTIN));

    ImmutableList<Label> loadLabels =
        BzlLoadFunction.getLoadLabels(
            env.getListener(), programLoads, rootPackage, /*repoMapping=*/ ImmutableMap.of());
    if (loadLabels == null) {
      NoSuchPackageException e =
          PackageFunction.PackageFunctionException.builder()
              .setType(PackageFunction.PackageFunctionException.Type.BUILD_FILE_CONTAINS_ERRORS)
              .setPackageIdentifier(rootPackage)
              .setMessage("malformed load statements")
              .setPackageLoadingCode(PackageLoading.Code.IMPORT_STARLARK_FILE_ERROR)
              .buildCause();
      throw new BzlmodRepoRuleFunctionException(e, Transience.PERSISTENT);
    }

    Preconditions.checkArgument(loadLabels.size() == 1);
    ImmutableList<BzlLoadValue.Key> keys =
        ImmutableList.of(BzlLoadValue.keyForBzlmod(loadLabels.get(0)));

    // Load the .bzl module.
    ImmutableMap<String, Module> loadedModules;
    try {
      loadedModules =
          PackageFunction.loadBzlModules(env, rootPackage, programLoads, keys, null);
    } catch (NoSuchPackageException e) {
      throw new BzlmodRepoRuleFunctionException(e, Transience.PERSISTENT);
    }
    if (loadedModules == null) {
      return null;
    }

    Object o = loadedModules.get(bzlFile).getGlobal(ruleName);
    BzlmodRepoRuleCreator repoRuleCreator;
    if (o instanceof BzlmodRepoRuleCreator) {
      repoRuleCreator = (BzlmodRepoRuleCreator) o;
    } else {
      InvalidRuleException e =
          new InvalidRuleException("Invalid repository rule " + repositoryInfo.getRuleClass());
      throw new BzlmodRepoRuleFunctionException(e, Transience.PERSISTENT);
    }

    Rule rule;
    Package.Builder pkg = getExternalPackageBuilder(semantics);
    StoredEventHandler eventHandler = new StoredEventHandler();
    try {
      rule = repoRuleCreator.createRule(pkg, semantics, repositoryInfo.getAttributes(), eventHandler);
      // We need to actually build the package so that the rule has the correct package reference.
      pkg.build();
    } catch (InvalidRuleException e) {
      // TODO: provide more info about where the rule definition come from.
      throw new BzlmodRepoRuleFunctionException(e, Transience.PERSISTENT);
    } catch (NoSuchPackageException e) {
      throw new BzlmodRepoRuleFunctionException(e, Transience.PERSISTENT);
    }

    return new BzlmodRepoRuleValue(rule);
  }

  @Nullable
  @Override
  public String extractTag(SkyKey skyKey) {
    return null;
  }

  private static final class BzlmodRepoRuleFunctionException extends SkyFunctionException {
    BzlmodRepoRuleFunctionException(InvalidRuleException e, Transience transience) {
      super(e, transience);
    }

    BzlmodRepoRuleFunctionException(NoSuchPackageException e, Transience transience) {
      super(e, transience);
    }
  }
}
