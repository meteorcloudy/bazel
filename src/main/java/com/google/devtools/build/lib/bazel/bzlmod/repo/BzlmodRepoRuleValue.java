package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.collect.Interner;
import com.google.devtools.build.lib.concurrent.BlazeInterners;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.packages.Rule;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.skyframe.AbstractSkyKey;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyValue;

import java.util.Objects;

public class BzlmodRepoRuleValue implements SkyValue {
  public static final SkyFunctionName BZLMOD_REPO_RULE =
      SkyFunctionName.createHermetic("BZLMOD_REPO_RULE");

  private final Rule rule;

  public BzlmodRepoRuleValue(Rule rule) {
    this.rule = rule;
  }

  public Rule getRule() {
    return rule;
  }

  public static Key key(String repositoryName) {
    return Key.create(repositoryName);
  }

  /** Represents an unsuccessful repository lookup. */
  public static final class RepoRuleNotFoundValue extends BzlmodRepoRuleValue {
    private RepoRuleNotFoundValue() {
      super(null);
    }

    @Override
    public Rule getRule() {
      throw new IllegalStateException();
    }
  }

  public static final RepoRuleNotFoundValue REPO_RULE_NOT_FOUND_VALUE = new RepoRuleNotFoundValue();

  /** Argument for the SkyKey to request a BzlmodRepoRuleValue. */
  @AutoCodec
  public static class Key extends AbstractSkyKey<String> {
    private static final Interner<Key> interner = BlazeInterners.newWeakInterner();

    private Key(String arg) {
      super(arg);
    }

    @AutoCodec.VisibleForSerialization
    @AutoCodec.Instantiator
    static Key create(String arg) {
      return interner.intern(new Key(arg));
    }

    @Override
    public SkyFunctionName functionName() {
      return BZLMOD_REPO_RULE;
    }
  }
}
