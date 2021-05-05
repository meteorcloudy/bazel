package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Interner;
import com.google.devtools.build.lib.concurrent.BlazeInterners;
import com.google.devtools.build.lib.skyframe.SkyFunctions;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.starlarkbuildapi.repository.StarlarkOverrideApi;
import com.google.devtools.build.skyframe.AbstractSkyKey;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

public class ModuleFileValue implements SkyValue {

  public static final ModuleKey ROOT_MODULE_KEY = ModuleKey.create("", "");

  private final Module module;
  private final ImmutableMap<String, StarlarkOverrideApi> overrides;

  public ModuleFileValue(Module module, ImmutableMap<String, StarlarkOverrideApi> overrides) {
    this.module = module;
    this.overrides = overrides;
  }

  public ImmutableMap<String, StarlarkOverrideApi> getOverrides() {
    return overrides;
  }

  public Module getModule() {
    return module;
  }

  public static Key key(ModuleKey key) {
    return Key.create(key);
  }

  public static Key keyForRootModule() {
    return Key.create(ROOT_MODULE_KEY);
  }

  /**
   * {@link SkyKey} for {@link ModuleFileValue} computation.
   */
  @AutoCodec.VisibleForSerialization
  @AutoCodec
  static class Key extends AbstractSkyKey<ModuleKey> {

    private static final Interner<Key> interner = BlazeInterners.newWeakInterner();

    private Key(ModuleKey arg) {
      super(arg);
    }

    @AutoCodec.VisibleForSerialization
    @AutoCodec.Instantiator
    static Key create(ModuleKey arg) {
      return interner.intern(new Key(arg));
    }

    @Override
    public SkyFunctionName functionName() {
      return SkyFunctions.MODULE_FILE;
    }
  }
}
