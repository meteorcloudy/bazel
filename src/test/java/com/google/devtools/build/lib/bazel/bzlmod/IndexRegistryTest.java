package com.google.devtools.build.lib.bazel.bzlmod;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth8;
import com.google.devtools.build.lib.bazel.repository.downloader.HttpDownloader;
import com.google.devtools.build.lib.testutil.FoundationTestCase;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URL;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IndexRegistryTest extends FoundationTestCase {

  @Rule
  public final TestHttpServer server = new TestHttpServer();
  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  private RegistryFactory registryFactory;

  @Before
  public void setUp() throws Exception {
    registryFactory = new RegistryFactoryImpl(new HttpDownloader(),
        Suppliers.ofInstance(ImmutableMap.of()));
  }

  @Test
  public void testHttpUrl() throws Exception {
    server.serve("/myreg/modules/foo/1.0/MODULE.bazel", "lol");
    server.start();

    Registry registry = registryFactory.getRegistryWithUrl(server.getUrl() + "/myreg");
    Truth8.assertThat(registry.getModuleFile(ModuleKey.create("foo", "1.0"), reporter))
        .hasValue("lol".getBytes());
    Truth8.assertThat(registry.getModuleFile(ModuleKey.create("bar", "1.0"), reporter)).isEmpty();
  }

  @Test
  public void testFileUrl() throws Exception {
    tempFolder.newFolder("fakereg", "modules", "foo", "1.0");
    File file = tempFolder.newFile("fakereg/modules/foo/1.0/MODULE.bazel");
    try (Writer writer = new FileWriter(file)) {
      writer.write("lol");
    }

    Registry registry = registryFactory.getRegistryWithUrl(
        new File(tempFolder.getRoot(), "fakereg").toURI().toString());
    Truth8.assertThat(registry.getModuleFile(ModuleKey.create("foo", "1.0"), reporter))
        .hasValue("lol".getBytes());
    Truth8.assertThat(registry.getModuleFile(ModuleKey.create("bar", "1.0"), reporter)).isEmpty();
  }

  @Test
  public void testGetFetcher() throws Exception {
    server.serve("/bazel_registry.json", "{\n"
        + "  \"mirrors\": [\n"
        + "    \"https://mirror.bazel.build/\",\n"
        + "    \"file:///home/bazel/mymirror/\"\n"
        + "  ]\n"
        + "}");
    server.serve("/modules/foo/1.0/source.json", "{\n"
        + "  \"url\": \"http://mysite.com/thing.zip\",\n"
        + "  \"integrity\": \"sha256-blah\",\n"
        + "  \"strip_prefix\": \"pref\"\n"
        + "}");
    server.serve("/modules/bar/2.0/source.json", "{\n"
        + "  \"url\": \"https://example.com/archive.jar?with=query\",\n"
        + "  \"integrity\": \"sha256-bleh\"\n"
        + "}");
    server.start();
    // TODO: test patches

    Registry registry = registryFactory.getRegistryWithUrl(server.getUrl());
    assertThat(registry.getRepoSpec(ModuleKey.create("foo", "1.0"), "foorepo", reporter))
        .isEqualTo(IndexRegistry.getRepoSpecForArchive(
            "foorepo",
            ImmutableList.of(
                new URL("https://mirror.bazel.build/mysite.com/thing.zip"),
                new URL("file:///home/bazel/mymirror/mysite.com/thing.zip"),
                new URL("http://mysite.com/thing.zip")),
            ImmutableList.of(),
            "sha256-blah",
            "pref",
            0));
    assertThat(registry.getRepoSpec(ModuleKey.create("bar", "2.0"), "barrepo", reporter))
        .isEqualTo(IndexRegistry.getRepoSpecForArchive(
            "barrepo",
            ImmutableList.of(
                new URL("https://mirror.bazel.build/example.com/archive.jar?with=query"),
                new URL("file:///home/bazel/mymirror/example.com/archive.jar?with=query"),
                new URL("https://example.com/archive.jar?with=query")),
            ImmutableList.of(),
            "sha256-bleh",
            "",
            0));
  }
}
