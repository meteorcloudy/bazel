package com.google.devtools.build.lib.bazel.bzlmod;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.actions.FileStateValue;
import com.google.devtools.build.lib.actions.FileValue;
import com.google.devtools.build.lib.analysis.BlazeDirectories;
import com.google.devtools.build.lib.analysis.ServerDirectories;
import com.google.devtools.build.lib.analysis.util.AnalysisMock;
import com.google.devtools.build.lib.pkgcache.PathPackageLocator;
import com.google.devtools.build.lib.skyframe.BazelSkyframeExecutorConstants;
import com.google.devtools.build.lib.skyframe.ExternalFilesHelper;
import com.google.devtools.build.lib.skyframe.ExternalFilesHelper.ExternalFileAction;
import com.google.devtools.build.lib.skyframe.FileFunction;
import com.google.devtools.build.lib.skyframe.FileStateFunction;
import com.google.devtools.build.lib.skyframe.PrecomputedFunction;
import com.google.devtools.build.lib.skyframe.PrecomputedValue;
import com.google.devtools.build.lib.skyframe.SkyFunctions;
import com.google.devtools.build.lib.testutil.FoundationTestCase;
import com.google.devtools.build.lib.util.io.TimestampGranularityMonitor;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.Root;
import com.google.devtools.build.lib.vfs.UnixGlob;
import com.google.devtools.build.skyframe.EvaluationContext;
import com.google.devtools.build.skyframe.EvaluationResult;
import com.google.devtools.build.skyframe.InMemoryMemoizingEvaluator;
import com.google.devtools.build.skyframe.MemoizingEvaluator;
import com.google.devtools.build.skyframe.RecordingDifferencer;
import com.google.devtools.build.skyframe.SequencedRecordingDifferencer;
import com.google.devtools.build.skyframe.SequentialBuildDriver;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import java.util.concurrent.atomic.AtomicReference;
import net.starlark.java.eval.StarlarkSemantics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ModuleFileFunctionTest extends FoundationTestCase {

  private SequentialBuildDriver driver;
  private RecordingDifferencer differencer;
  private EvaluationContext evaluationContext;
  private FakeRegistry.Factory registryFactory;

  @Before
  public void setup() throws Exception {
    differencer = new SequencedRecordingDifferencer();
    evaluationContext = EvaluationContext.newBuilder()
        .setNumThreads(8)
        .setEventHandler(reporter)
        .build();
    registryFactory = new FakeRegistry.Factory();
    AtomicReference<PathPackageLocator> packageLocator = new AtomicReference<>(
        new PathPackageLocator(
            outputBase,
            ImmutableList.of(Root.fromPath(rootDirectory)),
            BazelSkyframeExecutorConstants.BUILD_FILES_BY_PRIORITY));
    BlazeDirectories directories =
        new BlazeDirectories(
            new ServerDirectories(rootDirectory, outputBase, rootDirectory),
            rootDirectory,
            /* defaultSystemJavabase= */ null,
            AnalysisMock.get().getProductName());
    ExternalFilesHelper externalFilesHelper =
        ExternalFilesHelper.createForTesting(
            packageLocator,
            ExternalFileAction.DEPEND_ON_EXTERNAL_PKG_FOR_EXTERNAL_REPO_PATHS,
            directories);

    FetcherFactory fetcherFactory = new FetcherFactory(rootDirectory);
    MemoizingEvaluator evaluator = new InMemoryMemoizingEvaluator(
        ImmutableMap.<SkyFunctionName, SkyFunction>builder()
            .put(FileValue.FILE, new FileFunction(packageLocator))
            .put(FileStateValue.FILE_STATE, new FileStateFunction(
                new AtomicReference<TimestampGranularityMonitor>(),
                new AtomicReference<>(UnixGlob.DEFAULT_SYSCALLS),
                externalFilesHelper))
            .put(SkyFunctions.MODULE_FILE,
                new ModuleFileFunction(fetcherFactory, registryFactory, rootDirectory))
            .put(SkyFunctions.PRECOMPUTED, new PrecomputedFunction())
            .build(),
        differencer);
    driver = new SequentialBuildDriver(evaluator);

    PrecomputedValue.STARLARK_SEMANTICS.set(differencer, StarlarkSemantics.DEFAULT);
  }

  @Test
  public void testRootModule() throws Exception {
    scratch.file(rootDirectory.getRelative("MODULE.bazel").getPathString(),
        "module(name='A',version='0.1')",
        "bazel_dep(name='B',version='1.0')",
        "bazel_dep(name='C',version='2.0',repo_name='see')",
        "override_dep(name='D', override=single_version_override(version='18'))",
        "override_dep(name='E', override=local_path_override(path='somewhere/else'))");
    FakeRegistry registry = registryFactory.newFakeRegistry();
    ModuleFileFunction.REGISTRIES.set(differencer, ImmutableList.of(registry.getUrl()));

    EvaluationResult<ModuleFileValue> result =
        driver.evaluate(ImmutableList.of(ModuleFileValue.keyForRootModule()), evaluationContext);
    if (result.hasError()) {
      fail(result.getError().toString());
    }
    ModuleFileValue moduleFileValue = result.get(ModuleFileValue.keyForRootModule());
    assertThat(moduleFileValue.getModule()).isEqualTo(
        Module.builder()
            .setName("A")
            .setVersion("0.1")
            .addDep("B", ModuleKey.create("B", "1.0"))
            .addDep("see", ModuleKey.create("C", "2.0"))
            .build());
    assertThat(moduleFileValue.getOverrides()).containsExactly(
        "A", LocalPathOverride.create(""),
        "D", SingleVersionOverride.create("18", ""),
        "E", LocalPathOverride.create("somewhere/else"));
  }

  @Test
  public void testRootModule_BadSelfOverride() throws Exception {
    scratch.file(rootDirectory.getRelative("MODULE.bazel").getPathString(),
        "module(name='A')",
        "override_dep(name='A', override=single_version_override(version='7'))");
    FakeRegistry registry = registryFactory.newFakeRegistry();
    ModuleFileFunction.REGISTRIES.set(differencer, ImmutableList.of(registry.getUrl()));

    EvaluationResult<ModuleFileValue> result =
        driver.evaluate(ImmutableList.of(ModuleFileValue.keyForRootModule()), evaluationContext);
    assertThat(result.hasError()).isTrue();
    assertThat(result.getError().toString()).contains("invalid override for the root module");
  }

  @Test
  public void testRegistriesCascade() throws Exception {
    scratch.file(rootDirectory.getRelative("MODULE.bazel").getPathString(),
        "module(name='A',version='0.1')");
    // Registry1 has no module B@1.0; registry2 and registry3 both have it. We should be using the
    // B@1.0 from registry2.
    FakeRegistry registry1 = registryFactory.newFakeRegistry();
    FakeRegistry registry2 = registryFactory.newFakeRegistry()
        .addModule(ModuleKey.create("B", "1.0"),
            "module(name='B',version='1.0');bazel_dep(name='C',version='2.0')",
            new LocalPathFetcher(rootDirectory.getRelative("B")));
    FakeRegistry registry3 = registryFactory.newFakeRegistry()
        .addModule(ModuleKey.create("B", "1.0"),
            "module(name='B',version='1.0');bazel_dep(name='D',version='3.0')",
            new LocalPathFetcher(rootDirectory.getRelative("B")));
    ModuleFileFunction.REGISTRIES.set(differencer,
        ImmutableList.of(registry1.getUrl(), registry2.getUrl(), registry3.getUrl()));

    SkyKey skyKey = ModuleFileValue.key(ModuleKey.create("B", "1.0"));
    EvaluationResult<ModuleFileValue> result =
        driver.evaluate(ImmutableList.of(skyKey), evaluationContext);
    if (result.hasError()) {
      fail(result.getError().toString());
    }
    ModuleFileValue moduleFileValue = result.get(skyKey);
    assertThat(moduleFileValue.getModule()).isEqualTo(
        Module.builder()
            .setName("B")
            .setVersion("1.0")
            .addDep("C", ModuleKey.create("C", "2.0"))
            .build());
  }

  @Test
  public void testLocalPathOverride() throws Exception {
    scratch.file(rootDirectory.getRelative("MODULE.bazel").getPathString(),
        "module(name='A',version='0.1')",
        "override_dep(name='B',override=local_path_override(path='code_for_b'))");
    // There is an override for B to use the local path "code_for_b", so we shouldn't even be
    // looking at the registry.
    scratch.file(rootDirectory.getRelative("code_for_b/MODULE.bazel").getPathString(),
        "module(name='B',version='1.0')",
        "bazel_dep(name='C',version='2.0')");
    FakeRegistry registry = registryFactory.newFakeRegistry()
        .addModule(ModuleKey.create("B", "1.0"),
            "module(name='B',version='1.0');bazel_dep(name='C',version='3.0')",
            new LocalPathFetcher(rootDirectory.getRelative("B")));
    ModuleFileFunction.REGISTRIES.set(differencer, ImmutableList.of(registry.getUrl()));

    // The version is "" (empty) here due to the override.
    SkyKey skyKey = ModuleFileValue.key(ModuleKey.create("B", ""));
    EvaluationResult<ModuleFileValue> result =
        driver.evaluate(ImmutableList.of(skyKey), evaluationContext);
    if (result.hasError()) {
      fail(result.getError().toString());
    }
    ModuleFileValue moduleFileValue = result.get(skyKey);
    assertThat(moduleFileValue.getModule()).isEqualTo(
        Module.builder()
            .setName("B")
            .setVersion("1.0")
            .addDep("C", ModuleKey.create("C", "2.0"))
            .build());
  }

  @Test
  public void testRegistryOverride() throws Exception {
    FakeRegistry registry1 = registryFactory.newFakeRegistry()
        .addModule(ModuleKey.create("B", "1.0"),
            "module(name='B',version='1.0');bazel_dep(name='C',version='2.0')",
            new LocalPathFetcher(rootDirectory.getRelative("B")));
    FakeRegistry registry2 = registryFactory.newFakeRegistry()
        .addModule(ModuleKey.create("B", "1.0"),
            "module(name='B',version='1.0');bazel_dep(name='C',version='3.0')",
            new LocalPathFetcher(rootDirectory.getRelative("B")));
    // Override the registry for B to be registry2 (instead of the default registry1).
    scratch.file(rootDirectory.getRelative("MODULE.bazel").getPathString(),
        "module(name='A',version='0.1')",
        "override_dep(name='B',override=single_version_override(registry='" + registry2.getUrl()
            + "'))");
    ModuleFileFunction.REGISTRIES.set(differencer, ImmutableList.of(registry1.getUrl()));

    SkyKey skyKey = ModuleFileValue.key(ModuleKey.create("B", "1.0"));
    EvaluationResult<ModuleFileValue> result =
        driver.evaluate(ImmutableList.of(skyKey), evaluationContext);
    if (result.hasError()) {
      fail(result.getError().toString());
    }
    ModuleFileValue moduleFileValue = result.get(skyKey);
    assertThat(moduleFileValue.getModule()).isEqualTo(
        Module.builder()
            .setName("B")
            .setVersion("1.0")
            .addDep("C", ModuleKey.create("C", "3.0"))
            .build());
  }
}
