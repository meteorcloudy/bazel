package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.devtools.build.lib.util.Pair;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeRegistry implements Registry {

  private final String url;
  private final Map<ModuleKey, Pair<byte[], Fetcher>> modules = new HashMap<>();

  public FakeRegistry(String url) {
    this.url = url;
  }

  public FakeRegistry addModule(ModuleKey key, String moduleFile, Fetcher fetcher) {
    modules.put(key, Pair.of(moduleFile.getBytes(), fetcher));
    return this;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public Optional<byte[]> getModuleFile(ModuleKey key) {
    return Optional.ofNullable(modules.get(key)).map(Pair::getFirst);
  }

  @Override
  public Optional<Fetcher> getFetcher(ModuleKey key) {
    return Optional.ofNullable(modules.get(key)).map(Pair::getSecond);
  }

  public static class Factory implements RegistryFactory {

    private int numFakes = 0;
    private final Map<String, FakeRegistry> registries = new HashMap<>();

    public FakeRegistry newFakeRegistry() {
      FakeRegistry registry = new FakeRegistry("fake:" + (numFakes++));
      registries.put(registry.getUrl(), registry);
      return registry;
    }

    @Override
    public Registry getRegistryWithUrl(String url) {
      // TODO: instead of returning null, throw an exception
      return registries.get(url);
    }
  }
}
