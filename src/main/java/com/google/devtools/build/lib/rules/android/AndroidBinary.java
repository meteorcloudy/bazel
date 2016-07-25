// Copyright 2015 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.rules.android;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.devtools.build.lib.analysis.OutputGroupProvider.INTERNAL_SUFFIX;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.devtools.build.lib.actions.Action;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.FailAction;
import com.google.devtools.build.lib.actions.ParameterFile.ParameterFileType;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.FilesToRunProvider;
import com.google.devtools.build.lib.analysis.RuleConfiguredTarget.Mode;
import com.google.devtools.build.lib.analysis.RuleConfiguredTargetBuilder;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.Runfiles;
import com.google.devtools.build.lib.analysis.RunfilesProvider;
import com.google.devtools.build.lib.analysis.TransitiveInfoCollection;
import com.google.devtools.build.lib.analysis.actions.CommandLine;
import com.google.devtools.build.lib.analysis.actions.CustomCommandLine;
import com.google.devtools.build.lib.analysis.actions.SpawnAction;
import com.google.devtools.build.lib.analysis.actions.SpawnAction.Builder;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.collect.IterablesChain;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.collect.nestedset.Order;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.packages.BuildType;
import com.google.devtools.build.lib.packages.TriState;
import com.google.devtools.build.lib.rules.RuleConfiguredTargetFactory;
import com.google.devtools.build.lib.rules.android.AndroidConfiguration.AndroidBinaryType;
import com.google.devtools.build.lib.rules.android.AndroidRuleClasses.MultidexMode;
import com.google.devtools.build.lib.rules.cpp.CcToolchainProvider;
import com.google.devtools.build.lib.rules.cpp.CppHelper;
import com.google.devtools.build.lib.rules.java.DeployArchiveBuilder;
import com.google.devtools.build.lib.rules.java.JavaCommon;
import com.google.devtools.build.lib.rules.java.JavaCompilationArgsProvider;
import com.google.devtools.build.lib.rules.java.JavaConfiguration;
import com.google.devtools.build.lib.rules.java.JavaConfiguration.JavaOptimizationMode;
import com.google.devtools.build.lib.rules.java.JavaSemantics;
import com.google.devtools.build.lib.rules.java.JavaSourceInfoProvider;
import com.google.devtools.build.lib.rules.java.JavaTargetAttributes;
import com.google.devtools.build.lib.rules.java.ProguardHelper;
import com.google.devtools.build.lib.rules.java.ProguardHelper.ProguardOutput;
import com.google.devtools.build.lib.syntax.Type;
import com.google.devtools.build.lib.util.Preconditions;
import com.google.devtools.build.lib.vfs.PathFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * An implementation for the "android_binary" rule.
 */
public abstract class AndroidBinary implements RuleConfiguredTargetFactory {

  protected abstract JavaSemantics createJavaSemantics();
  protected abstract AndroidSemantics createAndroidSemantics();

  @Override
  public ConfiguredTarget create(RuleContext ruleContext)
      throws InterruptedException, RuleErrorException {
    JavaSemantics javaSemantics = createJavaSemantics();
    AndroidSemantics androidSemantics = createAndroidSemantics();
    if (!AndroidSdkProvider.verifyPresence(ruleContext)) {
      return null;
    }

    NestedSetBuilder<Artifact> filesBuilder = NestedSetBuilder.stableOrder();
    ImmutableList<TransitiveInfoCollection> deps = ImmutableList.<TransitiveInfoCollection>copyOf(
        ruleContext.getPrerequisites("deps", Mode.TARGET));
    JavaCommon javaCommon = new JavaCommon(
        ruleContext, javaSemantics, deps, deps, deps);
    javaSemantics.checkRule(ruleContext, javaCommon);
    javaSemantics.checkForProtoLibraryAndJavaProtoLibraryOnSameProto(ruleContext, javaCommon);

    AndroidCommon androidCommon = new AndroidCommon(
        javaCommon, true /* asNeverLink */, true /* exportDeps */);
    ResourceDependencies resourceDeps = LocalResourceContainer.definesAndroidResources(
        ruleContext.attributes())
        ? ResourceDependencies.fromRuleDeps(ruleContext, false /* neverlink */)
        : ResourceDependencies.fromRuleResourceAndDeps(ruleContext, false /* neverlink */);
    RuleConfiguredTargetBuilder builder = init(
        ruleContext,
        filesBuilder,
        resourceDeps,
        javaCommon,
        androidCommon,
        javaSemantics,
        androidSemantics);
    return builder.build();
  }

  private static RuleConfiguredTargetBuilder init(
      RuleContext ruleContext,
      NestedSetBuilder<Artifact> filesBuilder,
      ResourceDependencies resourceDeps,
      JavaCommon javaCommon,
      AndroidCommon androidCommon,
      JavaSemantics javaSemantics,
      AndroidSemantics androidSemantics)
      throws InterruptedException, RuleErrorException {

    if (getMultidexMode(ruleContext) != MultidexMode.LEGACY
        && ruleContext.attributes().isAttributeValueExplicitlySpecified(
            "main_dex_proguard_specs")) {
      ruleContext.throwWithAttributeError("main_dex_proguard_specs", "The "
          + "'main_dex_proguard_specs' attribute is only allowed if 'multidex' is set to 'legacy'");
    }

    if (ruleContext.attributes().isAttributeValueExplicitlySpecified("proguard_apply_mapping")
        && ruleContext.attributes()
            .get(ProguardHelper.PROGUARD_SPECS, BuildType.LABEL_LIST)
            .isEmpty()) {
      ruleContext.throwWithAttributeError("proguard_apply_mapping",
          "'proguard_apply_mapping' can only be used when 'proguard_specs' is also set");
    }

    // TODO(bazel-team): Find a way to simplify this code.
    // treeKeys() means that the resulting map sorts the entries by key, which is necessary to
    // ensure determinism.
    Multimap<String, TransitiveInfoCollection> depsByArchitecture =
        MultimapBuilder.treeKeys().arrayListValues().build();
    AndroidConfiguration androidConfig = ruleContext.getFragment(AndroidConfiguration.class);
    for (Map.Entry<Optional<String>, ? extends List<? extends TransitiveInfoCollection>> entry :
        ruleContext.getSplitPrerequisites("deps").entrySet()) {
      String cpu = entry.getKey().or(androidConfig.getCpu());
      depsByArchitecture.putAll(cpu, entry.getValue());
    }
    Map<String, BuildConfiguration> configurationMap = new LinkedHashMap<>();
    Map<String, CcToolchainProvider> toolchainMap = new LinkedHashMap<>();
    for (Map.Entry<Optional<String>, ? extends List<? extends TransitiveInfoCollection>> entry :
        ruleContext.getSplitPrerequisites(":cc_toolchain_split").entrySet()) {
      String cpu = entry.getKey().or(androidConfig.getCpu());
      TransitiveInfoCollection dep = Iterables.getOnlyElement(entry.getValue());
      CcToolchainProvider toolchain = CppHelper.getToolchain(ruleContext, dep);
      configurationMap.put(cpu, dep.getConfiguration());
      toolchainMap.put(cpu, toolchain);
    }

    NativeLibs nativeLibs = shouldLinkNativeDeps(ruleContext)
        ? NativeLibs.fromLinkedNativeDeps(ruleContext, androidSemantics.getNativeDepsFileName(),
            depsByArchitecture, toolchainMap, configurationMap)
        : NativeLibs.fromPrecompiledObjects(ruleContext, depsByArchitecture);

    // TODO(bazel-team): Resolve all the different cases of resource handling so this conditional
    // can go away: recompile from android_resources, and recompile from android_binary attributes.
    ApplicationManifest applicationManifest;
    ResourceApk resourceApk;
    ResourceApk incrementalResourceApk;
    ResourceApk instantRunResourceApk;
    ResourceApk splitResourceApk;
    if (LocalResourceContainer.definesAndroidResources(ruleContext.attributes())) {
      // Retrieve and compile the resources defined on the android_binary rule.
      LocalResourceContainer.validateRuleContext(ruleContext);
      ApplicationManifest ruleManifest = androidSemantics.getManifestForRule(ruleContext);

      applicationManifest = ruleManifest.mergeWith(ruleContext, resourceDeps);

      resourceApk = applicationManifest.packWithDataAndResources(
          ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_RESOURCES_APK),
          ruleContext,
          false, /* isLibrary */
          resourceDeps,
          ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_R_TXT),
          null, /* Artifact symbolsTxt */
          ruleContext.getTokenizedStringListAttr("resource_configuration_filters"),
          ruleContext.getTokenizedStringListAttr("nocompress_extensions"),
          ruleContext.attributes().get("crunch_png", Type.BOOLEAN),
          ruleContext.getTokenizedStringListAttr("densities"),
          false, /* incremental */
          ProguardHelper.getProguardConfigArtifact(ruleContext, ""),
          createMainDexProguardSpec(ruleContext),
          ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_PROCESSED_MANIFEST),
          ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_RESOURCES_ZIP));
      ruleContext.assertNoErrors();

      incrementalResourceApk = applicationManifest
          .addMobileInstallStubApplication(ruleContext)
          .packWithDataAndResources(
              ruleContext
                  .getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_INCREMENTAL_RESOURCES_APK),
              ruleContext,
              false, /* isLibrary */
              resourceDeps,
              null, /* Artifact rTxt */
              null, /* Artifact symbolsTxt */
              ruleContext.getTokenizedStringListAttr("resource_configuration_filters"),
              ruleContext.getTokenizedStringListAttr("nocompress_extensions"),
              ruleContext.attributes().get("crunch_png", Type.BOOLEAN),
              ruleContext.getTokenizedStringListAttr("densities"),
              true, /* incremental */
              ProguardHelper.getProguardConfigArtifact(ruleContext, "incremental"),
              null, /* mainDexProguardCfg */
              null, /* manifestOut */
              null /* mergedResourcesOut */);
      ruleContext.assertNoErrors();

      instantRunResourceApk = applicationManifest
          .addInstantRunStubApplication(ruleContext)
          .packWithDataAndResources(
              getDxArtifact(ruleContext, "android_instant_run.ap_"),
              ruleContext,
              false, /* isLibrary */
              resourceDeps,
              null, /* Artifact rTxt */
              null, /* Artifact symbolsTxt */
              ruleContext.getTokenizedStringListAttr("resource_configuration_filters"),
              ruleContext.getTokenizedStringListAttr("nocompress_extensions"),
              ruleContext.attributes().get("crunch_png", Type.BOOLEAN),
              ruleContext.getTokenizedStringListAttr("densities"),
              true, /* incremental */
              ProguardHelper.getProguardConfigArtifact(ruleContext, "instant_run"),
              null, /* mainDexProguardCfg */
              null, /* manifestOut */
              null /* mergedResourcesOut */);
      ruleContext.assertNoErrors();

      splitResourceApk = applicationManifest
          .createSplitManifest(ruleContext, "android_resources", false)
          .packWithDataAndResources(
              getDxArtifact(ruleContext, "android_resources.ap_"),
              ruleContext,
              false, /* isLibrary */
              resourceDeps,
              null, /* Artifact rTxt */
              null, /* Artifact symbolsTxt */
              ruleContext.getTokenizedStringListAttr("resource_configuration_filters"),
              ruleContext.getTokenizedStringListAttr("nocompress_extensions"),
              ruleContext.attributes().get("crunch_png", Type.BOOLEAN),
              ruleContext.getTokenizedStringListAttr("densities"),
              true, /* incremental */
              ProguardHelper.getProguardConfigArtifact(ruleContext, "incremental_split"),
              null, /* mainDexProguardCfg */
              null, /* manifestOut */
              null /* mergedResourcesOut */);
      ruleContext.assertNoErrors();

    } else {

      if (!ruleContext.attributes().get("crunch_png", Type.BOOLEAN)) {
        ruleContext.throwWithRuleError("Setting crunch_png = 0 is not supported for android_binary"
            + " rules which depend on android_resources rules.");
      }

      // Retrieve the resources from the resources attribute on the android_binary rule
      // and recompile them if necessary.
      ApplicationManifest resourcesManifest = ApplicationManifest.fromResourcesRule(ruleContext);
      if (resourcesManifest == null) {
        throw new RuleErrorException();
      }
      applicationManifest = resourcesManifest.mergeWith(ruleContext, resourceDeps);

      // Always recompiling resources causes AndroidTest to fail in certain circumstances.
      if (shouldRegenerate(ruleContext, resourceDeps)) {
        resourceApk = applicationManifest.packWithResources(
            ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_RESOURCES_APK),
            ruleContext,
            resourceDeps,
            true, /* createSource */
            ProguardHelper.getProguardConfigArtifact(ruleContext, ""),
            createMainDexProguardSpec(ruleContext));
        ruleContext.assertNoErrors();
      } else {
        resourceApk = applicationManifest.useCurrentResources(
            ruleContext,
            ProguardHelper.getProguardConfigArtifact(ruleContext, ""),
            createMainDexProguardSpec(ruleContext));
        ruleContext.assertNoErrors();
      }

      incrementalResourceApk = applicationManifest
          .addMobileInstallStubApplication(ruleContext)
          .packWithResources(
              ruleContext.getImplicitOutputArtifact(
                  AndroidRuleClasses.ANDROID_INCREMENTAL_RESOURCES_APK),
              ruleContext,
              resourceDeps,
              false, /* createSource */
              ProguardHelper.getProguardConfigArtifact(ruleContext, "incremental"),
              null /* mainDexProguardConfig */);
      ruleContext.assertNoErrors();

      instantRunResourceApk = applicationManifest
          .addInstantRunStubApplication(ruleContext)
          .packWithResources(
              getDxArtifact(ruleContext, "android_instant_run.ap_"),
              ruleContext,
              resourceDeps,
              false, /* createSource */
              ProguardHelper.getProguardConfigArtifact(ruleContext, "instant_run"),
              null /* mainDexProguardConfig */);
      ruleContext.assertNoErrors();

      splitResourceApk = applicationManifest
          .createSplitManifest(ruleContext, "android_resources", false)
          .packWithResources(getDxArtifact(ruleContext, "android_resources.ap_"),
            ruleContext,
            resourceDeps,
            false, /* createSource */
            ProguardHelper.getProguardConfigArtifact(ruleContext, "incremental_split"),
            null /* mainDexProguardConfig */);
      ruleContext.assertNoErrors();
    }

    JavaTargetAttributes resourceClasses = androidCommon.init(
        javaSemantics,
        androidSemantics,
        resourceApk,
        ruleContext.getConfiguration().isCodeCoverageEnabled(),
        true /* collectJavaCompilationArgs */,
        true /* isBinary */);
    ruleContext.assertNoErrors();

    Artifact deployJar = createDeployJar(ruleContext, javaSemantics, androidCommon, resourceClasses,
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_BINARY_DEPLOY_JAR));

    Artifact proguardMapping = ruleContext.getPrerequisiteArtifact(
        "proguard_apply_mapping", Mode.TARGET);

    return createAndroidBinary(
        ruleContext,
        filesBuilder,
        deployJar,
        /* isBinaryJarFiltered */ false,
        javaCommon,
        androidCommon,
        javaSemantics,
        androidSemantics,
        nativeLibs,
        applicationManifest,
        resourceApk,
        incrementalResourceApk,
        instantRunResourceApk,
        splitResourceApk,
        /* shrinkResources */ true,
        resourceClasses,
        ImmutableList.<Artifact>of(),
        ImmutableList.<Artifact>of(),
        proguardMapping);
  }

  public static RuleConfiguredTargetBuilder createAndroidBinary(
      RuleContext ruleContext,
      NestedSetBuilder<Artifact> filesBuilder,
      Artifact binaryJar,
      boolean isBinaryJarFiltered,
      JavaCommon javaCommon,
      AndroidCommon androidCommon,
      JavaSemantics javaSemantics,
      AndroidSemantics androidSemantics,
      NativeLibs nativeLibs,
      ApplicationManifest applicationManifest,
      ResourceApk resourceApk,
      ResourceApk incrementalResourceApk,
      ResourceApk instantRunResourceApk,
      ResourceApk splitResourceApk,
      boolean shrinkResources,
      JavaTargetAttributes resourceClasses,
      ImmutableList<Artifact> apksUnderTest,
      ImmutableList<Artifact> additionalMergedManifests,
      Artifact proguardMapping)
      throws InterruptedException, RuleErrorException {

    ImmutableList<Artifact> proguardSpecs = ProguardHelper.collectTransitiveProguardSpecs(
        ruleContext, ImmutableList.of(resourceApk.getResourceProguardConfig()));

    if (shrinkResources) {
      resourceApk = shrinkResources(
          ruleContext,
          androidCommon,
          resourceApk,
          binaryJar,
          proguardSpecs);
    }
    ProguardOutput proguardOutput =
        applyProguard(
            ruleContext,
            androidCommon,
            javaSemantics,
            binaryJar,
            filesBuilder,
            proguardSpecs,
            proguardMapping);
    Artifact jarToDex = proguardOutput.getOutputJar();
    DexingOutput dexingOutput =
        shouldDexWithJack(ruleContext)
            ? dexWithJack(ruleContext, androidCommon, proguardSpecs)
            : dex(
                ruleContext,
                androidSemantics,
                binaryJar,
                jarToDex,
                isBinaryJarFiltered,
                androidCommon,
                resourceApk.getMainDexProguardConfig(),
                resourceClasses);

    Artifact unsignedApk =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_BINARY_UNSIGNED_APK);
    Artifact signedApk =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_BINARY_SIGNED_APK);

    ApkActionBuilder apkBuilder = new ApkActionBuilder(ruleContext, androidSemantics)
        .classesDex(dexingOutput.classesDexZip)
        .resourceApk(resourceApk.getArtifact())
        .javaResourceZip(dexingOutput.javaResourceJar)
        .nativeLibs(nativeLibs);

    ruleContext.registerAction(apkBuilder
        .message("Generating unsigned apk")
        .build(unsignedApk));

    ruleContext.registerAction(apkBuilder
        .message("Generating signed apk")
        .sign(true)
        .build(signedApk));

    Artifact zipAlignedApk = zipalignApk(
        ruleContext,
        signedApk,
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_BINARY_APK));

    // Don't add blacklistedApk, so it's only built if explicitly requested.
    filesBuilder.add(binaryJar);
    filesBuilder.add(unsignedApk);
    filesBuilder.add(zipAlignedApk);
    NestedSet<Artifact> filesToBuild = filesBuilder.build();

    Artifact deployInfo = ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.DEPLOY_INFO);
    AndroidDeployInfoAction.createDeployInfoAction(ruleContext,
        deployInfo,
        applicationManifest.getManifest(),
        additionalMergedManifests,
        Iterables.concat(ImmutableList.of(zipAlignedApk), apksUnderTest));

    NestedSet<Artifact> coverageMetadata = (androidCommon.getInstrumentedJar() != null)
        ? NestedSetBuilder.create(Order.STABLE_ORDER, androidCommon.getInstrumentedJar())
        : NestedSetBuilder.<Artifact>emptySet(Order.STABLE_ORDER);

    RuleConfiguredTargetBuilder builder =
        new RuleConfiguredTargetBuilder(ruleContext);

    Artifact incrementalApk =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_BINARY_INCREMENTAL_APK);

    Artifact fullDeployMarker =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.FULL_DEPLOY_MARKER);
    Artifact incrementalDeployMarker =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.INCREMENTAL_DEPLOY_MARKER);
    Artifact splitDeployMarker =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.SPLIT_DEPLOY_MARKER);

    Artifact incrementalDexManifest =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.DEX_MANIFEST);
    ruleContext.registerAction(new SpawnAction.Builder()
        .setMnemonic("AndroidDexManifest")
        .setProgressMessage("Generating incremental installation manifest for "
            + ruleContext.getLabel())
        .setExecutable(
            ruleContext.getExecutablePrerequisite("$build_incremental_dexmanifest", Mode.HOST))
        .addOutputArgument(incrementalDexManifest)
        .addInputArguments(dexingOutput.shardDexZips)
        .useParameterFile(ParameterFileType.UNQUOTED).build(ruleContext));

    Artifact stubData = ruleContext.getImplicitOutputArtifact(
        AndroidRuleClasses.MOBILE_INSTALL_STUB_APPLICATION_DATA);
    Artifact stubDex = getStubDex(ruleContext, javaSemantics, false);
    ruleContext.assertNoErrors();

    ApkActionBuilder incrementalActionBuilder = new ApkActionBuilder(ruleContext, androidSemantics)
        .classesDex(stubDex)
        .resourceApk(incrementalResourceApk.getArtifact())
        .javaResourceZip(dexingOutput.javaResourceJar)
        .sign(true)
        .javaResourceFile(stubData)
        .message("Generating incremental apk");

    if (!ruleContext.getFragment(AndroidConfiguration.class).useIncrementalNativeLibs()) {
      incrementalActionBuilder.nativeLibs(nativeLibs);
    }

    ruleContext.registerAction(incrementalActionBuilder.build(incrementalApk));

    Artifact argsArtifact =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.MOBILE_INSTALL_ARGS);
    ruleContext.registerAction(
        new WriteAdbArgsAction(ruleContext.getActionOwner(), argsArtifact));

    createInstallAction(ruleContext, false, fullDeployMarker, argsArtifact,
        incrementalDexManifest, incrementalResourceApk.getArtifact(), incrementalApk, nativeLibs,
        stubData);

    createInstallAction(ruleContext, true, incrementalDeployMarker,
        argsArtifact,
        incrementalDexManifest,
        incrementalResourceApk.getArtifact(),
        incrementalApk,
        nativeLibs,
        stubData);

    Artifact incrementalDeployInfo = ruleContext.getImplicitOutputArtifact(
        AndroidRuleClasses.DEPLOY_INFO_INCREMENTAL);

    AndroidDeployInfoAction.createDeployInfoAction(ruleContext,
        incrementalDeployInfo,
        applicationManifest.getManifest(),
        additionalMergedManifests,
        ImmutableList.<Artifact>of());

    NestedSet<Artifact> fullInstallOutputGroup = NestedSetBuilder.<Artifact>stableOrder()
        .add(fullDeployMarker)
        .add(incrementalDeployInfo)
        .build();

    NestedSet<Artifact> incrementalInstallOutputGroup = NestedSetBuilder.<Artifact>stableOrder()
        .add(incrementalDeployMarker)
        .add(incrementalDeployInfo)
        .build();

    NestedSetBuilder<Artifact> splitApkSetBuilder = NestedSetBuilder.compileOrder();

    // Put the Android resource APK first so that this split gets installed first.
    //
    // This avoids some logcat spam during installation, because otherwise the Android package
    // manager would complain about references to missing resources in the manifest during the
    // installation of each split (said references would eventually get installed, but it cannot
    // know that in advance)
    Artifact resourceSplitApk = getDxArtifact(ruleContext, "android_resources.apk");
    ruleContext.registerAction(new ApkActionBuilder(ruleContext, androidSemantics)
        .resourceApk(splitResourceApk.getArtifact())
        .sign(true)
        .message("Generating split Android resource apk")
        .build(resourceSplitApk));
    splitApkSetBuilder.add(resourceSplitApk);

    for (int i = 0; i < dexingOutput.shardDexZips.size(); i++) {
      String splitName = "dex" + (i + 1);
      Artifact splitApkResources = createSplitApkResources(
          ruleContext, applicationManifest, splitName, true);
      Artifact splitApk = getDxArtifact(ruleContext, splitName + ".apk");
      ruleContext.registerAction(new ApkActionBuilder(ruleContext, androidSemantics)
          .classesDex(dexingOutput.shardDexZips.get(i))
          .resourceApk(splitApkResources)
          .sign(true)
          .message("Generating split dex apk " + (i + 1))
          .build(splitApk));
      splitApkSetBuilder.add(splitApk);
    }

    Artifact nativeSplitApkResources = createSplitApkResources(
        ruleContext, applicationManifest, "native", false);
    Artifact nativeSplitApk = getDxArtifact(ruleContext, "native.apk");
    ruleContext.registerAction(new ApkActionBuilder(ruleContext, androidSemantics)
        .resourceApk(nativeSplitApkResources)
        .sign(true)
        .message("Generating split native apk")
        .nativeLibs(nativeLibs)
        .build(nativeSplitApk));
    splitApkSetBuilder.add(nativeSplitApk);

    Artifact javaSplitApkResources = createSplitApkResources(
        ruleContext, applicationManifest, "java_resources", false);
    Artifact javaSplitApk = getDxArtifact(ruleContext, "java_resources.apk");
    ruleContext.registerAction(new ApkActionBuilder(ruleContext, androidSemantics)
        .resourceApk(javaSplitApkResources)
        .javaResourceZip(dexingOutput.javaResourceJar)
        .sign(true)
        .message("Generating split Java resource apk")
        .build(javaSplitApk));
    splitApkSetBuilder.add(javaSplitApk);

    Artifact splitMainApkResources = getDxArtifact(ruleContext, "split_main.ap_");
    ruleContext.registerAction(new SpawnAction.Builder()
        .setMnemonic("AndroidStripResources")
        .setProgressMessage("Stripping resources from split main apk")
        .setExecutable(ruleContext.getExecutablePrerequisite("$strip_resources", Mode.HOST))
        .addArgument("--input_resource_apk")
        .addInputArgument(resourceApk.getArtifact())
        .addArgument("--output_resource_apk")
        .addOutputArgument(splitMainApkResources)
        .build(ruleContext));

    NestedSet<Artifact> splitApks = splitApkSetBuilder.build();
    Artifact splitMainApk = getDxArtifact(ruleContext, "split_main.apk");
    Artifact splitStubDex = getStubDex(ruleContext, javaSemantics, true);
    ruleContext.assertNoErrors();
    ruleContext.registerAction(new ApkActionBuilder(ruleContext, androidSemantics)
        .resourceApk(splitMainApkResources)
        .classesDex(splitStubDex)
        .sign(true)
        .message("Generating split main apk")
        .build(splitMainApk));
    splitApkSetBuilder.add(splitMainApk);
    NestedSet<Artifact> allSplitApks = splitApkSetBuilder.build();

    createSplitInstallAction(ruleContext, splitDeployMarker, argsArtifact, splitMainApk,
        splitApks, stubData);

    Artifact splitDeployInfo = ruleContext.getImplicitOutputArtifact(
        AndroidRuleClasses.DEPLOY_INFO_SPLIT);
    AndroidDeployInfoAction.createDeployInfoAction(
        ruleContext,
        splitDeployInfo,
        applicationManifest.getManifest(),
        additionalMergedManifests,
        ImmutableList.<Artifact>of());

    NestedSet<Artifact> splitInstallOutputGroup = NestedSetBuilder.<Artifact>stableOrder()
        .addTransitive(allSplitApks)
        .add(splitDeployMarker)
        .add(splitDeployInfo)
        .build();

    Artifact debugKeystore = androidSemantics.getApkDebugSigningKey(ruleContext);
    Artifact apkManifest =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.APK_MANIFEST);
    createApkManifestAction(
        ruleContext,
        apkManifest,
        false, // text proto
        androidCommon,
        resourceClasses,
        instantRunResourceApk,
        nativeLibs,
        debugKeystore);

    Artifact apkManifestText =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.APK_MANIFEST_TEXT);
    createApkManifestAction(
        ruleContext,
        apkManifestText,
        true, // text proto
        androidCommon,
        resourceClasses,
        instantRunResourceApk,
        nativeLibs,
        debugKeystore);

    androidCommon.addTransitiveInfoProviders(
        builder, androidSemantics, resourceApk, zipAlignedApk, apksUnderTest);
    androidSemantics.addTransitiveInfoProviders(
        builder, ruleContext, javaCommon, androidCommon, jarToDex);

    if (proguardOutput.getMapping() != null) {
      builder.add(ProguardMappingProvider.class,
          new ProguardMappingProvider(proguardOutput.getMapping(),
                                      proguardOutput.getProtoMapping()));
    }

    return builder
        .setFilesToBuild(filesToBuild)
        .add(
            RunfilesProvider.class,
            RunfilesProvider.simple(
                new Runfiles.Builder(
                        ruleContext.getWorkspaceName(),
                        ruleContext.getConfiguration().legacyExternalRunfiles())
                    .addRunfiles(ruleContext, RunfilesProvider.DEFAULT_RUNFILES)
                    .addTransitiveArtifacts(filesToBuild)
                    .build()))
        .add(
            JavaSourceInfoProvider.class,
            JavaSourceInfoProvider.fromJavaTargetAttributes(resourceClasses, javaSemantics))
        .add(
            ApkProvider.class,
            new ApkProvider(
                NestedSetBuilder.create(Order.STABLE_ORDER, zipAlignedApk),
                coverageMetadata,
                NestedSetBuilder.create(Order.STABLE_ORDER, applicationManifest.getManifest())))
        .add(AndroidPreDexJarProvider.class, new AndroidPreDexJarProvider(jarToDex))
        .addOutputGroup("mobile_install_full" + INTERNAL_SUFFIX, fullInstallOutputGroup)
        .addOutputGroup(
            "mobile_install_incremental" + INTERNAL_SUFFIX, incrementalInstallOutputGroup)
        .addOutputGroup("mobile_install_split" + INTERNAL_SUFFIX, splitInstallOutputGroup)
        .addOutputGroup("apk_manifest", apkManifest)
        .addOutputGroup("apk_manifest_text", apkManifestText)
        .addOutputGroup("android_deploy_info", deployInfo);
  }

  private static void createSplitInstallAction(RuleContext ruleContext,
      Artifact marker, Artifact argsArtifact, Artifact splitMainApk, NestedSet<Artifact> splitApks,
      Artifact stubDataFile) {
    FilesToRunProvider adb = AndroidSdkProvider.fromRuleContext(ruleContext).getAdb();
    SpawnAction.Builder builder = new SpawnAction.Builder()
        .setExecutable(ruleContext.getExecutablePrerequisite("$incremental_install", Mode.HOST))
        .addTool(adb)
        .executeUnconditionally()
        .setMnemonic("AndroidInstall")
        .setProgressMessage("Installing " + ruleContext.getLabel() + " using split apks")
        .setExecutionInfo(ImmutableMap.of("local", ""))
        .addArgument("--output_marker")
        .addOutputArgument(marker)
        .addArgument("--stub_datafile")
        .addInputArgument(stubDataFile)
        .addArgument("--adb")
        .addArgument(adb.getExecutable().getExecPathString())
        .addTool(adb)
        .addArgument("--flagfile")
        .addInputArgument(argsArtifact)
        .addArgument("--split_main_apk")
        .addInputArgument(splitMainApk);

    for (Artifact splitApk : splitApks) {
      builder
          .addArgument("--split_apk")
          .addInputArgument(splitApk);
    }

    ruleContext.registerAction(builder.build(ruleContext));
  }

  private static void createInstallAction(RuleContext ruleContext,
      boolean incremental, Artifact marker, Artifact argsArtifact,
      Artifact dexmanifest, Artifact resourceApk, Artifact apk, NativeLibs nativeLibs,
      Artifact stubDataFile) {
    FilesToRunProvider adb = AndroidSdkProvider.fromRuleContext(ruleContext).getAdb();
    SpawnAction.Builder builder = new SpawnAction.Builder()
        .setExecutable(ruleContext.getExecutablePrerequisite("$incremental_install", Mode.HOST))
        // We cannot know if the user connected a new device, uninstalled the app from the device
        // or did anything strange to it, so we always run this action.
        .executeUnconditionally()
        .setMnemonic("AndroidInstall")
        .setProgressMessage(
            "Installing " + ruleContext.getLabel() + (incremental ? " incrementally" : ""))
        .setExecutionInfo(ImmutableMap.of("local", ""))
        .addArgument("--output_marker")
        .addOutputArgument(marker)
        .addArgument("--dexmanifest")
        .addInputArgument(dexmanifest)
        .addArgument("--resource_apk")
        .addInputArgument(resourceApk)
        .addArgument("--stub_datafile")
        .addInputArgument(stubDataFile)
        .addArgument("--adb")
        .addArgument(adb.getExecutable().getExecPathString())
        .addTool(adb)
        .addArgument("--flagfile")
        .addInputArgument(argsArtifact);

    if (!incremental) {
      builder
          .addArgument("--apk")
          .addInputArgument(apk);
    }

    if (ruleContext.getFragment(AndroidConfiguration.class).useIncrementalNativeLibs()) {
      for (Map.Entry<String, Iterable<Artifact>> arch : nativeLibs.getMap().entrySet()) {
        for (Artifact lib : arch.getValue()) {
          builder
              .addArgument("--native_lib")
              .addArgument(arch.getKey() + ":" + lib.getExecPathString())
              .addInput(lib);
        }
      }
    }

    ruleContext.registerAction(builder.build(ruleContext));
  }

  private static Artifact getStubDex(
      RuleContext ruleContext, JavaSemantics javaSemantics, boolean split) {
    String attribute =
        split ? "$incremental_split_stub_application" : "$incremental_stub_application";

    TransitiveInfoCollection dep = ruleContext.getPrerequisite(attribute, Mode.TARGET);
    if (dep == null) {
      ruleContext.attributeError(attribute, "Stub application cannot be found");
      return null;
    }

    JavaCompilationArgsProvider provider = dep.getProvider(JavaCompilationArgsProvider.class);
    if (provider == null) {
      ruleContext.attributeError(attribute, "'" + dep.getLabel() + "' should be a Java target");
      return null;
    }

    JavaTargetAttributes attributes = new JavaTargetAttributes.Builder(javaSemantics)
        .addRuntimeClassPathEntries(provider.getJavaCompilationArgs().getRuntimeJars())
        .build();

    Artifact stubDeployJar = getDxArtifact(ruleContext,
        split ? "split_stub_deploy.jar" : "stub_deploy.jar");
    new DeployArchiveBuilder(javaSemantics, ruleContext)
        .setOutputJar(stubDeployJar)
        .setAttributes(attributes)
        .build();

    Artifact stubDex = getDxArtifact(ruleContext,
        split ? "split_stub_application.dex" : "stub_application.dex");
    AndroidCommon.createDexAction(
        ruleContext,
        stubDeployJar,
        stubDex,
        ImmutableList.<String>of(),
        false,
        null);

    return stubDex;
  }

  private static void createApkManifestAction(
      RuleContext ruleContext,
      Artifact apkManfiest,
      boolean textProto,
      AndroidCommon androidCommon,
      JavaTargetAttributes resourceClasses,
      ResourceApk resourceApk,
      NativeLibs nativeLibs,
      Artifact debugKeystore) {

    Iterable<Artifact> jars = IterablesChain.concat(
        resourceClasses.getArchiveInputs(true), androidCommon.getRuntimeJars());

    AndroidSdkProvider sdk = AndroidSdkProvider.fromRuleContext(ruleContext);

    ApkManifestAction manifestAction = new ApkManifestAction(
        ruleContext.getActionOwner(),
        apkManfiest,
        textProto,
        sdk,
        jars,
        resourceApk,
        nativeLibs,
        debugKeystore);

    ruleContext.registerAction(manifestAction);
  }

  /** Generates an uncompressed _deploy.jar of all the runtime jars. */
  public static Artifact createDeployJar(
      RuleContext ruleContext, JavaSemantics javaSemantics, AndroidCommon common,
      JavaTargetAttributes attributes, Artifact deployJar) {
    new DeployArchiveBuilder(javaSemantics, ruleContext)
        .setOutputJar(deployJar)
        .setAttributes(attributes)
        .addRuntimeJars(common.getRuntimeJars())
        .build();
    return deployJar;
  }

  private static JavaOptimizationMode getJavaOptimizationMode(RuleContext ruleContext) {
    return ruleContext.getConfiguration().getFragment(JavaConfiguration.class)
        .getJavaOptimizationMode();
  }

  /**
   * Applies the proguard specifications, and creates a ProguardedJar. Proguard's output artifacts
   * are added to the given {@code filesBuilder}.
   */
  private static ProguardOutput applyProguard(
      RuleContext ruleContext,
      AndroidCommon common,
      JavaSemantics javaSemantics,
      Artifact deployJarArtifact,
      NestedSetBuilder<Artifact> filesBuilder,
      ImmutableList<Artifact> proguardSpecs,
      Artifact proguardMapping) throws InterruptedException {
    Artifact proguardOutputJar =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.ANDROID_BINARY_PROGUARD_JAR);

    // Proguard will be only used for binaries which specify a proguard_spec
    if (proguardSpecs.isEmpty()) {
      // Although normally the Proguard jar artifact is not needed for binaries which do not specify
      // proguard_specs, targets which use a select to provide an empty list to proguard_specs will
      // still have a Proguard jar implicit output, as it is impossible to tell what a select will
      // produce at the time of implicit output determination. As a result, this artifact must
      // always be created.
      return createEmptyProguardAction(ruleContext, javaSemantics, proguardOutputJar,
                                       deployJarArtifact);
    }

    AndroidSdkProvider sdk = AndroidSdkProvider.fromRuleContext(ruleContext);
    NestedSet<Artifact> libraryJars = NestedSetBuilder.<Artifact>naiveLinkOrder()
        .add(sdk.getAndroidJar())
        .addTransitive(common.getTransitiveNeverLinkLibraries())
        .build();
    ProguardOutput result = ProguardHelper.createProguardAction(
        ruleContext,
        sdk.getProguard(),
        deployJarArtifact,
        proguardSpecs,
        proguardMapping,
        libraryJars,
        proguardOutputJar,
        javaSemantics,
        getProguardOptimizationPasses(ruleContext));
    // Since Proguard is being run, add its output artifacts to the given filesBuilder
    result.addAllToSet(filesBuilder);
    return result;
  }

  @Nullable
  private static Integer getProguardOptimizationPasses(RuleContext ruleContext) {
    if (ruleContext.attributes().has("proguard_optimization_passes", Type.INTEGER)) {
      return ruleContext.attributes().get("proguard_optimization_passes", Type.INTEGER);
    } else {
      return null;
    }
  }

  private static ProguardOutput createEmptyProguardAction(RuleContext ruleContext,
      JavaSemantics semantics, Artifact proguardOutputJar, Artifact deployJarArtifact)
          throws InterruptedException {
    NestedSetBuilder<Artifact> failures = NestedSetBuilder.<Artifact>stableOrder();
    ProguardOutput outputs =
        ProguardHelper.getProguardOutputs(proguardOutputJar, ruleContext, semantics);
    outputs.addAllToSet(failures);
    JavaOptimizationMode optMode = getJavaOptimizationMode(ruleContext);
    ruleContext.registerAction(
        new FailAction(
            ruleContext.getActionOwner(),
            failures.build(),
            String.format("Can't run Proguard %s",
                optMode == JavaOptimizationMode.LEGACY
                    ? "without proguard_specs"
                    : "in optimization mode " + optMode)));
    return new ProguardOutput(deployJarArtifact, null, null, null);
  }

  private static ResourceApk shrinkResources(
      RuleContext ruleContext,
      AndroidCommon androidCommon,
      ResourceApk resourceApk,
      Artifact deployJar,
      ImmutableList<Artifact> proguardSpecs) throws InterruptedException {

    if (ruleContext.getFragment(AndroidConfiguration.class).useAndroidResourceShrinking()
        && LocalResourceContainer.definesAndroidResources(ruleContext.attributes())
        && !proguardSpecs.isEmpty()) {

      // TODO(apell): Once ProGuard is split into multiple runs, use the Artifact from the shrinking
      // pass here instead.
      Artifact shrunkJar = ruleContext.getImplicitOutputArtifact(
          AndroidRuleClasses.ANDROID_BINARY_SHRUNK_JAR);
      AndroidSdkProvider sdk = AndroidSdkProvider.fromRuleContext(ruleContext);

      Iterable<Artifact> libraryJars = NestedSetBuilder.<Artifact>naiveLinkOrder()
          .add(sdk.getAndroidJar())
          .addTransitive(androidCommon.getTransitiveNeverLinkLibraries())
          .build();
      Builder builder = new SpawnAction.Builder()
          .addInput(deployJar)
          .addInputs(libraryJars)
          .addInputs(proguardSpecs)
          .setExecutable(sdk.getProguard())
          .setProgressMessage("Finding Resource References With Proguard")
          .setMnemonic("ProguardResourceMapping")
          .addArgument("-injars")
          .addArgument(deployJar.getExecPathString());

      for (Artifact libraryJar : libraryJars) {
        builder.addArgument("-libraryjars")
            .addArgument(libraryJar.getExecPathString());
      }

      for (Artifact proguardSpec : proguardSpecs) {
        builder.addArgument("@" + proguardSpec.getExecPathString());
      }

      builder.addArgument("-ignorewarnings")
          .addArgument("-dontnote")
          .addArgument("-forceprocessing")
          .addArgument("-dontoptimize")
          .addArgument("-dontobfuscate")
          .addArgument("-dontpreverify")
          .addArgument("-outjars")
          .addOutputArgument(shrunkJar);

      ruleContext.registerAction(builder.build(ruleContext));

      Artifact apk = new ResourceShrinkerActionBuilder(ruleContext)
          .setResourceApkOut(ruleContext.getImplicitOutputArtifact(
              AndroidRuleClasses.ANDROID_RESOURCES_SHRUNK_APK))
          .setShrunkResourcesOut(ruleContext.getImplicitOutputArtifact(
              AndroidRuleClasses.ANDROID_RESOURCES_SHRUNK_ZIP))
          .setLogOut(ruleContext.getImplicitOutputArtifact(
              AndroidRuleClasses.ANDROID_RESOURCE_SHRINKER_LOG))
          .withResourceFiles(ruleContext.getImplicitOutputArtifact(
              AndroidRuleClasses.ANDROID_RESOURCES_ZIP))
          .withShrunkJar(shrunkJar)
          .withPrimary(resourceApk.getPrimaryResource())
          .withDependencies(resourceApk.getResourceDependencies())
          .setConfigurationFilters(
              ruleContext.getTokenizedStringListAttr("resource_configuration_filters"))
          .setUncompressedExtensions(
              ruleContext.getTokenizedStringListAttr("nocompress_extensions"))
          .build();
      return new ResourceApk(apk,
          resourceApk.getResourceJavaSrcJar(),
          resourceApk.getResourceDependencies(),
          resourceApk.getPrimaryResource(),
          resourceApk.getManifest(),
          resourceApk.getResourceProguardConfig(),
          resourceApk.getMainDexProguardConfig(),
          resourceApk.isLegacy());
    }
    return resourceApk;
  }

  @Immutable
  private static final class DexingOutput {
    private final Artifact classesDexZip;
    private final Artifact javaResourceJar;
    private final ImmutableList<Artifact> shardDexZips;

    private DexingOutput(
        Artifact classesDexZip, Artifact javaResourceJar, Iterable<Artifact> shardDexZips) {
      this.classesDexZip = classesDexZip;
      this.javaResourceJar = javaResourceJar;
      this.shardDexZips = ImmutableList.copyOf(shardDexZips);
    }
  }

  static boolean shouldDexWithJack(RuleContext ruleContext) {
    return ruleContext
        .getFragment(AndroidConfiguration.class)
        .isJackUsedForDexing();
  }

  static DexingOutput dexWithJack(
      RuleContext ruleContext, AndroidCommon androidCommon, ImmutableList<Artifact> proguardSpecs) {
    Artifact classesDexZip =
        androidCommon.compileDexWithJack(
            getMultidexMode(ruleContext),
            Optional.fromNullable(
                ruleContext.getPrerequisiteArtifact("main_dex_list", Mode.TARGET)),
            proguardSpecs);
    return new DexingOutput(classesDexZip, null, ImmutableList.of(classesDexZip));
  }

  /** Creates one or more classes.dex files that correspond to {@code proguardedJar}. */
  private static DexingOutput dex(
      RuleContext ruleContext,
      AndroidSemantics androidSemantics,
      Artifact binaryJar,
      Artifact proguardedJar,
      boolean isBinaryJarFiltered,
      AndroidCommon common,
      @Nullable Artifact mainDexProguardSpec,
      JavaTargetAttributes attributes)
      throws InterruptedException, RuleErrorException {
    List<String> dexopts = ruleContext.getTokenizedStringListAttr("dexopts");
    MultidexMode multidexMode = getMultidexMode(ruleContext);
    if (!supportsMultidexMode(ruleContext, multidexMode)) {
      ruleContext.throwWithRuleError("Multidex mode \"" + multidexMode.getAttributeValue()
          + "\" not supported by this version of the Android SDK");
    }

    int dexShards = ruleContext.attributes().get("dex_shards", Type.INTEGER);
    if (dexShards > 1) {
      if (multidexMode == MultidexMode.OFF) {
        ruleContext.throwWithRuleError(".dex sharding is only available in multidex mode");
      }

      if (multidexMode == MultidexMode.MANUAL_MAIN_DEX) {
        ruleContext.throwWithRuleError(".dex sharding is not available in manual multidex mode");
      }
    }

    Artifact mainDexList = ruleContext.getPrerequisiteArtifact("main_dex_list", Mode.TARGET);
    if ((mainDexList != null && multidexMode != MultidexMode.MANUAL_MAIN_DEX)
        || (mainDexList == null && multidexMode == MultidexMode.MANUAL_MAIN_DEX)) {
      ruleContext.throwWithRuleError(
          "Both \"main_dex_list\" and \"multidex='manual_main_dex'\" must be specified.");
    }

    // Always OFF if finalJarIsDerived
    ImmutableSet<AndroidBinaryType> incrementalDexing =
        getEffectiveIncrementalDexing(ruleContext, dexopts, binaryJar != proguardedJar);
    Artifact inclusionFilterJar =
        isBinaryJarFiltered && binaryJar == proguardedJar ? binaryJar : null;
    if (multidexMode == MultidexMode.OFF) {
      // Single dex mode: generate classes.dex directly from the input jar.
      if (incrementalDexing.contains(AndroidBinaryType.MONODEX)) {
        Artifact classesDex = getDxArtifact(ruleContext, "classes.dex.zip");
        Artifact jarToDex = getDxArtifact(ruleContext, "classes.jar");
        createShuffleJarAction(ruleContext, true, (Artifact) null, ImmutableList.of(jarToDex),
            common, inclusionFilterJar, dexopts, attributes, (Artifact) null);
        createDexMergerAction(ruleContext, "off", jarToDex, classesDex, (Artifact) null, dexopts);
        return new DexingOutput(classesDex, binaryJar, ImmutableList.of(classesDex));
      } else {
        // By *not* writing a zip we get dx to drop resources on the floor.
        Artifact classesDex = getDxArtifact(ruleContext, "classes.dex");
        AndroidCommon.createDexAction(
            ruleContext, proguardedJar, classesDex, dexopts, /* multidex */ false, (Artifact) null);
        return new DexingOutput(classesDex, binaryJar, ImmutableList.of(classesDex));
      }
    } else {
      // Multidex mode: generate classes.dex.zip, where the zip contains [classes.dex,
      // classes2.dex, ... classesN.dex].

      if (multidexMode == MultidexMode.LEGACY) {
        // For legacy multidex, we need to generate a list for the dexer's --main-dex-list flag.
        mainDexList = createMainDexListAction(
            ruleContext, androidSemantics, proguardedJar, mainDexProguardSpec);
      }

      Artifact classesDex = getDxArtifact(ruleContext, "classes.dex.zip");
      if (dexShards > 1) {
        List<Artifact> shards = new ArrayList<>(dexShards);
        for (int i = 1; i <= dexShards; i++) {
          shards.add(getDxArtifact(ruleContext, "shard" + i + ".jar"));
        }

        Artifact javaResourceJar =
            createShuffleJarAction(
                ruleContext,
                incrementalDexing.contains(AndroidBinaryType.MULTIDEX_SHARDED),
                /*proguardedJar*/ binaryJar != proguardedJar ? proguardedJar : null,
                shards,
                common,
                inclusionFilterJar,
                dexopts,
                attributes,
                mainDexList);

        List<Artifact> shardDexes = new ArrayList<>(dexShards);
        for (int i = 1; i <= dexShards; i++) {
          Artifact shard = shards.get(i - 1);
          Artifact shardDex = getDxArtifact(ruleContext, "shard" + i + ".dex.zip");
          shardDexes.add(shardDex);
          if (incrementalDexing.contains(AndroidBinaryType.MULTIDEX_SHARDED)) {
            // If there's a main dex list then the first shard contains exactly those files.
            // To work with devices that lack native multi-dex support we need to make sure that
            // the main dex list becomes one dex file if at all possible.
            // Note shard here (mostly) contains of .class.dex files from shuffled dex archives,
            // instead of being a conventional Jar file with .class files.
            String multidexStrategy = mainDexList != null && i == 1 ? "minimal" : "best_effort";
            createDexMergerAction(ruleContext, multidexStrategy, shard, shardDex, (Artifact) null,
                dexopts);
          } else {
            AndroidCommon.createDexAction(
                ruleContext, shard, shardDex, dexopts, /* multidex */ true, (Artifact) null);
          }
        }

        CommandLine mergeCommandLine = CustomCommandLine.builder()
            .addBeforeEachExecPath("--input_zip", shardDexes)
            .addExecPath("--output_zip", classesDex)
            .build();
        ruleContext.registerAction(new SpawnAction.Builder()
            .setMnemonic("MergeDexZips")
            .setProgressMessage("Merging dex shards for " + ruleContext.getLabel())
            .setExecutable(ruleContext.getExecutablePrerequisite("$merge_dexzips", Mode.HOST))
            .addInputs(shardDexes)
            .addOutput(classesDex)
            .setCommandLine(mergeCommandLine)
            .build(ruleContext));
        if (incrementalDexing.contains(AndroidBinaryType.MULTIDEX_SHARDED)) {
          // Using the deploy jar for java resources gives better "bazel mobile-install" performance
          // with incremental dexing b/c bazel can create the "incremental" and "split resource"
          // APKs earlier (b/c these APKs don't depend on code being dexed here).  This is also done
          // for other multidex modes.
          javaResourceJar = binaryJar;
        }
        return new DexingOutput(classesDex, javaResourceJar, shardDexes);
      } else {
        if (incrementalDexing.contains(AndroidBinaryType.MULTIDEX_UNSHARDED)) {
          Artifact jarToDex = AndroidBinary.getDxArtifact(ruleContext, "classes.jar");
          createShuffleJarAction(ruleContext, true, (Artifact) null, ImmutableList.of(jarToDex),
              common, inclusionFilterJar, dexopts, attributes, (Artifact) null);
          createDexMergerAction(ruleContext, "minimal", jarToDex, classesDex, mainDexList, dexopts);
        } else {
          // Because the dexer also places resources into this zip, we also need to create a cleanup
          // action that removes all non-.dex files before staging for apk building.
          // Create an artifact for the intermediate zip output that includes non-.dex files.
          Artifact classesDexIntermediate = AndroidBinary.getDxArtifact(
              ruleContext, "intermediate_classes.dex.zip");
          // Have the dexer generate the intermediate file and the "cleaner" action consume this to
          // generate the final archive with only .dex files.
          AndroidCommon.createDexAction(ruleContext, proguardedJar,
              classesDexIntermediate, dexopts, /* multidex */ true, mainDexList);
          createCleanDexZipAction(ruleContext, classesDexIntermediate, classesDex);
        }
        return new DexingOutput(classesDex, binaryJar, ImmutableList.of(classesDex));
      }
    }
  }

  private static ImmutableSet<AndroidBinaryType> getEffectiveIncrementalDexing(
      RuleContext ruleContext, List<String> dexopts, boolean isBinaryProguarded) {
    TriState override = ruleContext.attributes().get("incremental_dexing", BuildType.TRISTATE);
    // Ignore --incremental_dexing_binary_types if the incremental_dexing attribute is set, but
    // raise an error if proguard is enabled (b/c incompatible with incremental dexing ATM).
    if (isBinaryProguarded && override == TriState.YES) {
      ruleContext.attributeError("incremental_dexing",
          "target cannot be incrementally dexed because it uses Proguard");
      return ImmutableSet.of();
    }
    if (isBinaryProguarded || override == TriState.NO) {
      return ImmutableSet.of();
    }
    ImmutableSet<AndroidBinaryType> result =
        override == TriState.YES
            ? ImmutableSet.copyOf(AndroidBinaryType.values())
            : AndroidCommon.getAndroidConfig(ruleContext).getIncrementalDexingBinaries();
    if (!result.isEmpty()) {
      Iterable<String> blacklistedDexopts =
          Iterables.filter(
              dexopts,
              new FlagMatcher(AndroidCommon
                  .getAndroidConfig(ruleContext)
                  .getTargetDexoptsThatPreventIncrementalDexing()));
      if (!Iterables.isEmpty(blacklistedDexopts)) {
        // target's dexopts include flags blacklisted with --non_incremental_per_target_dexopts. If
        // incremental_dexing attribute is explicitly set for this target then we'll warn and
        // incrementally dex anyway.  Otherwise, just don't incrementally dex.
        if (override == TriState.YES) {
          Iterable<String> ignored =
              Iterables.filter(
                  blacklistedDexopts,
                  Predicates.not(
                      Predicates.in(
                          AndroidCommon.getAndroidConfig(ruleContext)
                              .getDexoptsSupportedInIncrementalDexing())));
          ruleContext.attributeWarning("incremental_dexing",
              String.format("Using incremental dexing even though dexopts %s indicate this target "
                      + "may be unsuitable for incremental dexing for the moment.%s",
                  blacklistedDexopts,
                  Iterables.isEmpty(ignored) ? "" : " These will be ignored: " + ignored));
        } else {
          result = ImmutableSet.of();
        }
      }
    }
    return result;
  }

  private static void createDexMergerAction(
      RuleContext ruleContext,
      String multidexStrategy,
      Artifact inputJar,
      Artifact classesDex,
      @Nullable Artifact mainDexList,
      Collection<String> dexopts) {
    SpawnAction.Builder dexmerger = new SpawnAction.Builder()
        .setExecutable(ruleContext.getExecutablePrerequisite("$dexmerger", Mode.HOST))
        .addArgument("--input")
        .addInputArgument(inputJar)
        .addArgument("--output")
        .addOutputArgument(classesDex)
        .addArguments(DexArchiveAspect.incrementalDexopts(ruleContext, dexopts))
        .addArgument("--multidex=" + multidexStrategy)
        .setMnemonic("DexMerger")
        .setProgressMessage("Assembling dex files into " + classesDex.prettyPrint());
    if (mainDexList != null) {
      dexmerger.addArgument("--main-dex-list").addInputArgument(mainDexList);
      if (dexopts.contains("--minimal-main-dex")) {
        dexmerger.addArgument("--minimal-main-dex");
      }
    }
    ruleContext.registerAction(dexmerger.build(ruleContext));
  }

  /**
   * Returns a {@link DexArchiveProvider} of all transitively generated dex archives as well as
   * dex archives for the Jars produced by the binary target itself.
   */
  private static Function<Artifact, Artifact> collectDexArchives(
      RuleContext ruleContext, AndroidCommon common, List<String> dexopts) {
    DexArchiveProvider.Builder result = new DexArchiveProvider.Builder()
        // Use providers from all attributes that declare DexArchiveAspect
        .addTransitiveProviders(
            ruleContext.getPrerequisites("deps", Mode.TARGET, DexArchiveProvider.class));
    ImmutableSet<String> incrementalDexopts =
        DexArchiveAspect.incrementalDexopts(ruleContext, dexopts);
    for (Artifact jar : common.getJarsProducedForRuntime()) {
      // Create dex archives next to all Jars produced by AndroidCommon for this rule.  We need to
      // do this (instead of placing dex archives into the _dx subdirectory like DexArchiveAspect
      // does because for "legacy" ResourceApks, AndroidCommon produces Jars per resource dependency
      // that can theoretically have duplicate basenames, so they go into special directories, and
      // we piggyback on that naming scheme here by placing dex archives into the same directories.
      PathFragment jarPath = jar.getRootRelativePath();
      Artifact dexArchive = ruleContext.getDerivedArtifact(
          jarPath.replaceName(jarPath.getBaseName() + ".dex.zip"),
          jar.getRoot());
      DexArchiveAspect.createDexArchiveAction(ruleContext, jar, dexArchive, incrementalDexopts);
      result.addDexArchive(incrementalDexopts, dexArchive, jar);
    }
    return result.build().archivesForDexopts(incrementalDexopts);
  }

  private static Artifact createShuffleJarAction(
      RuleContext ruleContext,
      boolean useDexArchives,
      @Nullable Artifact proguardedJar,
      List<Artifact> shards,
      AndroidCommon common,
      @Nullable Artifact inclusionFilterJar,
      List<String> dexopts,
      JavaTargetAttributes attributes,
      @Nullable Artifact mainDexList)
      throws InterruptedException {
    checkArgument(mainDexList == null || shards.size() > 1);
    checkArgument(proguardedJar == null || inclusionFilterJar == null);
    Artifact javaResourceJar =
        ruleContext.getImplicitOutputArtifact(AndroidRuleClasses.JAVA_RESOURCES_JAR);

    SpawnAction.Builder shardAction = new SpawnAction.Builder()
        .setMnemonic("ShardClassesToDex")
        .setProgressMessage("Sharding classes for dexing for " + ruleContext.getLabel())
        .setExecutable(ruleContext.getExecutablePrerequisite("$shuffle_jars", Mode.HOST))
        .addOutputs(shards)
        .addOutput(javaResourceJar);

    CustomCommandLine.Builder shardCommandLine = CustomCommandLine.builder()
        .addBeforeEachExecPath("--output_jar", shards)
        .addExecPath("--output_resources", javaResourceJar);

    if (mainDexList != null) {
      shardCommandLine.addExecPath("--main_dex_filter", mainDexList);
      shardAction.addInput(mainDexList);
    }

    // If we need to run Proguard, all the class files will be in the Proguarded jar and the
    // deploy jar will already have been built (since it's the input of Proguard) and it will
    // contain all the Java resources. Otherwise, we don't want to have deploy jar creation on
    // the critical path, so we put all the jar files that constitute it on the inputs of the
    // jar shuffler.
    if (proguardedJar != null) {
      // When proguard is used we can't use dex archives, so just shuffle the proguarded jar
      checkArgument(!useDexArchives, "Dex archives are incompatible with Proguard");
      shardCommandLine.addExecPath("--input_jar", proguardedJar);
      shardAction.addInput(proguardedJar);
    } else {
      Iterable<Artifact> classpath =
          Iterables.concat(common.getRuntimeJars(), attributes.getRuntimeClassPathForArchive());
      // Check whether we can use dex archives.  Besides the --incremental_dexing flag, also
      // make sure the "dexopts" attribute on this target doesn't mention any problematic flags.
      if (useDexArchives) {
        // Use dex archives instead of their corresponding Jars wherever we can.  At this point
        // there should be very few or no Jar files that still end up in shards.  The dexing
        // step below will have to deal with those in addition to merging .dex files together.
        classpath = Iterables
            .transform(classpath, collectDexArchives(ruleContext, common, dexopts));
        shardCommandLine.add("--split_dexed_classes");
      }
      shardCommandLine.addBeforeEachExecPath("--input_jar", classpath);
      shardAction.addInputs(classpath);

      if (inclusionFilterJar != null) {
        shardCommandLine.addExecPath("--inclusion_filter_jar", inclusionFilterJar);
        shardAction.addInput(inclusionFilterJar);
      }
    }

    shardAction.setCommandLine(shardCommandLine.build());
    ruleContext.registerAction(shardAction.build(ruleContext));
    return javaResourceJar;
  }

  /**
   * Creates an action that copies a .zip file to a specified path, filtering all non-.dex files
   * out of the output.
   */
  static void createCleanDexZipAction(RuleContext ruleContext, Artifact inputZip,
      Artifact outputZip) {
    ruleContext.registerAction(new SpawnAction.Builder()
        .setExecutable(ruleContext.getExecutablePrerequisite("$zip", Mode.HOST))
        .addInput(inputZip)
        .addOutput(outputZip)
        .addArgument(inputZip.getExecPathString())
        .addArgument("--out")
        .addArgument(outputZip.getExecPathString())
        .addArgument("--copy")
        .addArgument("classes*.dex")
        .setProgressMessage("Trimming " + inputZip.getExecPath().getBaseName())
        .setMnemonic("TrimDexZip")
        .build(ruleContext));
  }

  /**
   * Creates an action that generates a list of classes to be passed to the dexer's
   * --main-dex-list flag (which specifies the classes that need to be directly in classes.dex).
   * Returns the file containing the list.
   */
  static Artifact createMainDexListAction(
      RuleContext ruleContext,
      AndroidSemantics androidSemantics,
      Artifact jar,
      @Nullable Artifact mainDexProguardSpec)
      throws InterruptedException {
    // Process the input jar through Proguard into an intermediate, streamlined jar.
    Artifact strippedJar = AndroidBinary.getDxArtifact(ruleContext, "main_dex_intermediate.jar");
    AndroidSdkProvider sdk = AndroidSdkProvider.fromRuleContext(ruleContext);
    SpawnAction.Builder streamlinedBuilder = new SpawnAction.Builder()
        .addOutput(strippedJar)
        .setExecutable(sdk.getProguard())
        .setProgressMessage("Generating streamlined input jar for main dex classes list")
        .setMnemonic("MainDexClassesIntermediate")
        .addArgument("-forceprocessing")
        .addArgument("-injars")
        .addInputArgument(jar)
        .addArgument("-libraryjars")
        .addInputArgument(sdk.getShrinkedAndroidJar())
        .addArgument("-outjars")
        .addArgument(strippedJar.getExecPathString())
        .addArgument("-dontwarn")
        .addArgument("-dontnote")
        .addArgument("-dontoptimize")
        .addArgument("-dontobfuscate")
        .addArgument("-dontpreverify");

    List<Artifact> specs = new ArrayList<>();
    specs.addAll(
        ruleContext.getPrerequisiteArtifacts("main_dex_proguard_specs", Mode.TARGET).list());
    if (specs.isEmpty()) {
      specs.add(sdk.getMainDexClasses());
    }
    if (mainDexProguardSpec != null) {
      specs.add(mainDexProguardSpec);
    }

    for (Artifact spec : specs) {
      streamlinedBuilder.addArgument("-include");
      streamlinedBuilder.addInputArgument(spec);
    }

    androidSemantics.addMainDexListActionArguments(ruleContext, streamlinedBuilder);

    ruleContext.registerAction(streamlinedBuilder.build(ruleContext));

    // Create the main dex classes list.
    Artifact mainDexList = AndroidBinary.getDxArtifact(ruleContext, "main_dex_list.txt");
    Builder builder = new Builder()
        .setMnemonic("MainDexClasses")
        .setProgressMessage("Generating main dex classes list");

    ruleContext.registerAction(builder
        .setExecutable(sdk.getMainDexListCreator())
        .addOutputArgument(mainDexList)
        .addInputArgument(strippedJar)
        .addInputArgument(jar)
        .addArguments(ruleContext.getTokenizedStringListAttr("main_dex_list_opts"))
        .build(ruleContext));
    return mainDexList;
  }

  private static Artifact createSplitApkResources(RuleContext ruleContext,
      ApplicationManifest mainManifest, String splitName, boolean hasCode) {
    Artifact splitManifest = mainManifest.createSplitManifest(ruleContext, splitName, hasCode)
        .getManifest();
    Artifact splitResources = getDxArtifact(ruleContext, "split_" + splitName + ".ap_");
    AndroidSdkProvider sdk = AndroidSdkProvider.fromRuleContext(ruleContext);
    ruleContext.registerAction(new SpawnAction.Builder()
        .setExecutable(sdk.getAapt())
        .setMnemonic("AndroidAapt")
        .setProgressMessage("Generating resource apk for split " + splitName)
        .addArgument("package")
        .addArgument("-F")
        .addOutputArgument(splitResources)
        .addArgument("-M")
        .addInputArgument(splitManifest)
        .addArgument("-I")
        .addInputArgument(sdk.getAndroidJar())
        .build(ruleContext));

    return splitResources;
  }

  @Nullable
  private static Artifact createMainDexProguardSpec(RuleContext ruleContext) {
    return AndroidSdkProvider.fromRuleContext(ruleContext).getAaptSupportsMainDexGeneration()
        ? ProguardHelper.getProguardConfigArtifact(ruleContext, "main_dex")
        : null;
  }

  /**
   * Builder class for {@link com.google.devtools.build.lib.analysis.actions.SpawnAction}s that
   * generate APKs.
   *
   * <p>Instances of this class can be reused after calling {@code build()}.
   */
  private static final class ApkActionBuilder {
    private final RuleContext ruleContext;
    private final AndroidSemantics semantics;

    private boolean sign;
    private String message;
    private Artifact classesDex;
    private Artifact resourceApk;
    private Artifact javaResourceZip;
    // javaResourceFile adds Java resources just like javaResourceZip. We should make the stub
    // manifest writer output a zip file, then we could do away with this input to APK building.
    private Artifact javaResourceFile;
    private NativeLibs nativeLibs = NativeLibs.EMPTY;

    private ApkActionBuilder(
        RuleContext ruleContext, AndroidSemantics semantics) {
      this.ruleContext = ruleContext;
      this.semantics = semantics;
    }

    /**
     * Sets the user-visible message that is displayed when the action is running.
     */
    public ApkActionBuilder message(String message) {
      this.message = message;
      return this;
    }

    /**
     * Sets the native libraries to be included in the APK.
     */
    public ApkActionBuilder nativeLibs(NativeLibs nativeLibs) {
      this.nativeLibs = nativeLibs;
      return this;
    }

    /**
     * Sets the dex file to be included in the APK.
     *
     * <p>Can be either a plain .dex or a .zip file containing dexes.
     */
    public ApkActionBuilder classesDex(Artifact classesDex) {
      this.classesDex = classesDex;
      return this;
    }

    /**
     * Sets the resource APK that contains the Android resources to be bundled into the output.
     */
    public ApkActionBuilder resourceApk(Artifact resourceApk) {
      this.resourceApk = resourceApk;
      return this;
    }

    /**
     * Sets the file where Java resources are taken.
     *
     * <p>Everything in this will will be put directly into the APK except files with the extension
     * {@code .class}.
     */
    public ApkActionBuilder javaResourceZip(Artifact javaResourcezip) {
      this.javaResourceZip = javaResourcezip;
      return this;
    }

    /**
     * Adds an individual resource file to the root directory of the APK.
     *
     * <p>This provides the same functionality as {@code javaResourceZip}, except much more hacky.
     * Will most probably won't work if there is an input artifact in the same directory as this
     * file.
     */
    public ApkActionBuilder javaResourceFile(Artifact javaResourceFile) {
      this.javaResourceFile = javaResourceFile;
      return this;
    }

    /**
     * Sets if the APK will be signed. By default, it won't be.
     */
    public ApkActionBuilder sign(boolean sign) {
      this.sign = sign;
      return this;
    }

    /**
     * Creates a generating action for {@code outApk} that builds the APK specified.
     */
    public Action[] build(Artifact outApk) {
      Builder actionBuilder = new SpawnAction.Builder()
          .setExecutable(AndroidSdkProvider.fromRuleContext(ruleContext).getApkBuilder())
          .setProgressMessage(message)
          .setMnemonic("AndroidApkBuilder")
          .addOutputArgument(outApk);

      if (javaResourceZip != null) {
        actionBuilder
            .addArgument("-rj")
            .addInputArgument(javaResourceZip);
      }

      Artifact nativeSymlinks = nativeLibs.createApkBuilderSymlinks(ruleContext);
      if (nativeSymlinks != null) {
        PathFragment nativeSymlinksDir = nativeSymlinks.getExecPath().getParentDirectory();
        actionBuilder
            .addInputManifest(nativeSymlinks, nativeSymlinksDir)
            .addInput(nativeSymlinks)
            .addInputs(nativeLibs.getAllNativeLibs())
            .addArgument("-nf")
            // If the native libs are "foo/bar/x86/foo.so", we need to pass "foo/bar" here
            .addArgument(nativeSymlinksDir.getPathString());
      }

      if (nativeLibs.getName() != null) {
        actionBuilder
            .addArgument("-rf")
            .addArgument(nativeLibs.getName().getExecPath().getParentDirectory().getPathString())
            .addInput(nativeLibs.getName());
      }

      if (javaResourceFile != null) {
        actionBuilder
            .addArgument("-rf")
            .addArgument((javaResourceFile.getExecPath().getParentDirectory().getPathString()))
            .addInput(javaResourceFile);
      }

      if (sign) {
        Artifact signingKey = semantics.getApkDebugSigningKey(ruleContext);
        actionBuilder.addArgument("-ks").addArgument(signingKey.getExecPathString());
        actionBuilder.addInput(signingKey);
      } else {
        actionBuilder.addArgument("-u");
      }

      actionBuilder
          .addArgument("-z")
          .addInputArgument(resourceApk);

      if (classesDex != null) {
        actionBuilder
            .addArgument(classesDex.getFilename().endsWith(".dex") ? "-f" : "-z")
            .addInputArgument(classesDex);
      }

      return actionBuilder.build(ruleContext);
    }
  }

  /** Last step in buildings an apk: align the zip boundaries by 4 bytes. */
  static Artifact zipalignApk(RuleContext ruleContext,
      Artifact signedApk, Artifact zipAlignedApk) {
    List<String> args = new ArrayList<>();
    // "4" is the only valid value for zipalign, according to:
    // http://developer.android.com/guide/developing/tools/zipalign.html
    args.add("4");
    args.add(signedApk.getExecPathString());
    args.add(zipAlignedApk.getExecPathString());

    ruleContext.registerAction(new SpawnAction.Builder()
        .addInput(signedApk)
        .addOutput(zipAlignedApk)
        .setExecutable(AndroidSdkProvider.fromRuleContext(ruleContext).getZipalign())
        .addArguments(args)
        .setProgressMessage("Zipaligning apk")
        .setMnemonic("AndroidZipAlign")
        .build(ruleContext));
    args.add(signedApk.getExecPathString());
    args.add(zipAlignedApk.getExecPathString());
    return zipAlignedApk;
  }

  /**
   * Tests if the resources need to be regenerated.
   *
   * <p>The resources should be regenerated (using aapt) if any of the following are true:
   * <ul>
   *    <li>There is more than one resource container
   *    <li>There are densities to filter by.
   *    <li>There are resource configuration filters.
   *    <li>There are extensions that should be compressed.
   * </ul>
   */
  public static boolean shouldRegenerate(RuleContext ruleContext,
      ResourceDependencies resourceDeps) {
    return Iterables.size(resourceDeps.getResources()) > 1
        || ruleContext.attributes().isAttributeValueExplicitlySpecified("densities")
        || ruleContext.attributes().isAttributeValueExplicitlySpecified(
            "resource_configuration_filters")
        || ruleContext.attributes().isAttributeValueExplicitlySpecified("nocompress_extensions");
  }

  /**
   * Returns whether to use NativeDepsHelper to link native dependencies.
   */
  public static boolean shouldLinkNativeDeps(RuleContext ruleContext) {
    TriState attributeValue = ruleContext.attributes().get(
        "legacy_native_support", BuildType.TRISTATE);
    if (attributeValue == TriState.AUTO) {
      return !ruleContext.getFragment(AndroidConfiguration.class).getLegacyNativeSupport();
    } else {
      return attributeValue == TriState.NO;
    }
  }

  /**
   * Returns the multidex mode to apply to this target.
   */
  public static MultidexMode getMultidexMode(RuleContext ruleContext) {
    if (ruleContext.getRule().isAttrDefined("multidex", Type.STRING)) {
      return Preconditions.checkNotNull(
          MultidexMode.fromValue(ruleContext.attributes().get("multidex", Type.STRING)));
    } else {
      return MultidexMode.OFF;
    }
  }

  /**
   * List of Android SDKs that contain runtimes that do not support the native multidexing
   * introduced in Android L. If someone tries to build an android_binary that has multidex=native
   * set with an old SDK, we will exit with an error to alert the developer that his application
   * might not run on devices that the used SDK still supports.
   */
  private static final Set<String> RUNTIMES_THAT_DONT_SUPPORT_NATIVE_MULTIDEXING = ImmutableSet.of(
      "/android_sdk_linux/platforms/android_10/", "/android_sdk_linux/platforms/android_13/",
      "/android_sdk_linux/platforms/android_15/", "/android_sdk_linux/platforms/android_16/",
      "/android_sdk_linux/platforms/android_17/", "/android_sdk_linux/platforms/android_18/",
      "/android_sdk_linux/platforms/android_19/", "/android_sdk_linux/platforms/android_20/");

  /**
   * Returns true if the runtime contained in the Android SDK used to build this rule supports the
   * given version of multidex mode specified, false otherwise.
   */
  public static boolean supportsMultidexMode(RuleContext ruleContext, MultidexMode mode) {
    if (mode == MultidexMode.NATIVE) {
      // Native mode is not supported by Android devices running Android before v21.
      String runtime =
          AndroidSdkProvider.fromRuleContext(ruleContext).getAndroidJar().getExecPathString();
      for (String blacklistedRuntime : RUNTIMES_THAT_DONT_SUPPORT_NATIVE_MULTIDEXING) {
        if (runtime.contains(blacklistedRuntime)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Returns an intermediate artifact used to support dex generation.
   */
  public static Artifact getDxArtifact(RuleContext ruleContext, String baseName) {
    return ruleContext.getUniqueDirectoryArtifact("_dx", baseName,
        ruleContext.getBinOrGenfilesDirectory());
  }

  private static class FlagMatcher implements Predicate<String> {
    private final ImmutableList<String> matching;

    FlagMatcher(ImmutableList<String> matching) {
      this.matching = matching;
    }

    @Override
    public boolean apply(String input) {
      for (String match : matching) {
        if (input.contains(match)) {
          return true;
        }
      }
      return false;
    }
  }
}
