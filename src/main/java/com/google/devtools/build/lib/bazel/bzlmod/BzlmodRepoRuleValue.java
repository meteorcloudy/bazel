package com.google.devtools.build.lib.bazel.bzlmod;

import com.google.common.collect.Interner;
import com.google.devtools.build.lib.concurrent.BlazeInterners;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.packages.Rule;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.skyframe.SkyFunctionName;
import com.google.devtools.build.skyframe.SkyKey;
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

  public static BzlmodRepoRuleKey key(String repositoryName, boolean forModuleRuleResolve) {
    return BzlmodRepoRuleKey.create(repositoryName, forModuleRuleResolve);
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
  @Immutable
  @AutoCodec
  public static class BzlmodRepoRuleKey implements SkyKey {
    private static final Interner<BzlmodRepoRuleValue.BzlmodRepoRuleKey> interner = BlazeInterners.newWeakInterner();

    private final String repositoryName;
    private final boolean forModuleRuleResolve;

    private BzlmodRepoRuleKey(String repositoryName, boolean forModuleRuleResolve) {
      this.repositoryName = repositoryName;
      this.forModuleRuleResolve = forModuleRuleResolve;
    }

    @AutoCodec.VisibleForSerialization
    @AutoCodec.Instantiator
    static BzlmodRepoRuleValue.BzlmodRepoRuleKey create(
        String repositoryName, boolean forModuleRuleResolve) {
      return interner.intern(
          new BzlmodRepoRuleValue.BzlmodRepoRuleKey(repositoryName, forModuleRuleResolve));
    }

    public String getRepositoryName() {
      return repositoryName;
    }

    public boolean isForModuleRuleResolve() {
      return forModuleRuleResolve;
    }

    @Override
    public SkyFunctionName functionName() {
      return BZLMOD_REPO_RULE;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof BzlmodRepoRuleValue.BzlmodRepoRuleKey)) {
        return false;
      }
      BzlmodRepoRuleValue.BzlmodRepoRuleKey other = (BzlmodRepoRuleValue.BzlmodRepoRuleKey) obj;
      return this.repositoryName.equals(other.repositoryName)
          && this.forModuleRuleResolve == other.forModuleRuleResolve;
    }

    @Override
    public int hashCode() {
      return Objects.hash(BzlmodRepoRuleKey.class, repositoryName, forModuleRuleResolve);
    }
  }
}
