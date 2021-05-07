package com.google.devtools.build.lib.skyframe;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.analysis.BlazeDirectories;
import com.google.devtools.build.lib.bazel.bzlmod.repo.BzlmodRepoRuleCreator;
import com.google.devtools.build.lib.bazel.bzlmod.repo.BzlmodRepoRuleValue;
import com.google.devtools.build.lib.bazel.bzlmod.repo.BzlmodRepoRuleValue.Key;
import com.google.devtools.build.lib.bazel.bzlmod.repo.RepoSpec;
import com.google.devtools.build.lib.bazel.bzlmod.repo.RepoSpecsValue;
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

  private static final String TOOLS_REPO = "bazel_tools";

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

    String repositoryName = ((Key) skyKey).argument();
    RepoSpec repoSpec;

    // @bazel_tools is a special repo that we pull from the extracted install dir.
    if (repositoryName.equals(TOOLS_REPO)) {
      repoSpec = new RepoSpec(
          "local_repository",
          ImmutableMap.of(
              "name", "bazel_tools", "path",
              directories.getEmbeddedBinariesRoot().getChild("embedded_tools").getPathString()));
      return getRuleFromSpec(repoSpec, starlarkSemantics, env);
    }

    // Look for repositories defined by non-registry overrides.
    RepoSpecsValue overrideDepRepos =
        (RepoSpecsValue) env.getValue(RepoSpecsValue.KEY_FOR_OVERRIDE_DEP);
    if (overrideDepRepos == null) {
      return null;
    }
    repoSpec = overrideDepRepos.getRepository(repositoryName);
    if (repoSpec != null) {
      return getRuleFromSpec(repoSpec, starlarkSemantics, env);
    }

    // Look for repositories derived from native Bazel Modules
    RepoSpecsValue bazelModuleRepos =
        (RepoSpecsValue) env.getValue(RepoSpecsValue.KEY_FOR_BAZEL_MODULE);
    if (bazelModuleRepos == null) {
      return null;
    }
    repoSpec = bazelModuleRepos.getRepository(repositoryName);
    if (repoSpec != null) {
      return getRuleFromSpec(repoSpec, starlarkSemantics, env);
    }

    // Look for repositories derived from module rules if the repo is not requested for module rule
    // resolution.
    RepoSpecsValue moduleRuleRepos =
        (RepoSpecsValue) env.getValue(RepoSpecsValue.KEY_FOR_MODULE_RULE);
    if (moduleRuleRepos == null) {
      return null;
    }
    repoSpec = moduleRuleRepos.getRepository(repositoryName);
    if (repoSpec != null) {
      return getRuleFromSpec(repoSpec, starlarkSemantics, env);
    }

    return BzlmodRepoRuleValue.REPO_RULE_NOT_FOUND_VALUE;
  }

  private BzlmodRepoRuleValue getRuleFromSpec(RepoSpec repoSpec,
      StarlarkSemantics starlarkSemantics, Environment env)
      throws BzlmodRepoRuleFunctionException, InterruptedException {
    if (isNativeRepoRule(repoSpec)) {
      return getNativeRepoRule(repoSpec, starlarkSemantics, env);
    } else {
      return getStarlarkRepoRule(repoSpec, starlarkSemantics, env);
    }
  }

  private boolean isNativeRepoRule(RepoSpec info) {
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
      RepoSpec repoSpec, StarlarkSemantics semantics, Environment env)
      throws InterruptedException, BzlmodRepoRuleFunctionException {
    RuleClass ruleClass = ruleClassProvider.getRuleClassMap().get(repoSpec.getRuleClass());
    BuildLangTypedAttributeValuesMap attributeValues =
        new BuildLangTypedAttributeValuesMap(repoSpec.getAttributes());
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
      RepoSpec repoSpec, StarlarkSemantics semantics, Environment env)
      throws InterruptedException, BzlmodRepoRuleFunctionException {
    int pos = repoSpec.getRuleClass().indexOf("%");
    String bzlFile = repoSpec.getRuleClass().substring(0, pos);
    String ruleName = repoSpec.getRuleClass().substring(pos + 1);

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
          new InvalidRuleException("Invalid repository rule " + repoSpec.getRuleClass());
      throw new BzlmodRepoRuleFunctionException(e, Transience.PERSISTENT);
    }

    Rule rule;
    Package.Builder pkg = getExternalPackageBuilder(semantics);
    StoredEventHandler eventHandler = new StoredEventHandler();
    try {
      rule = repoRuleCreator.createRule(pkg, semantics, repoSpec.getAttributes(), eventHandler);
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
