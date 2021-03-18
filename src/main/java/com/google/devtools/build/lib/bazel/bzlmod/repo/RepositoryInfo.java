package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class RepositoryInfo {
  private String name;
  private String ruleClass;
  private ImmutableMap<String, Object> attributes;
  private ImmutableList<String> repoDeps;
  private ImmutableMap<String, String> repoMappings;
  private String vendorDir;

  private RepositoryInfo(String name, String ruleClass,
      ImmutableMap<String, Object> attributes,
      ImmutableList<String> repoDeps,
      ImmutableMap<String, String> repoMappings, String vendorDir) {
    this.name = name;
    this.ruleClass = ruleClass;
    this.attributes = attributes;
    this.repoDeps = repoDeps;
    this.repoMappings = repoMappings;
    this.vendorDir = vendorDir;
  }

  public String getName() {
    return name;
  }

  public String getRuleClass() {
    return ruleClass;
  }

  public ImmutableMap<String, Object> getAttributes() {
    return attributes;
  }

  public ImmutableList<String> getRepoDeps() {
    return repoDeps;
  }

  public ImmutableMap<String, String> getRepoMappings() {
    return repoMappings;
  }

  public String getVendorDir() {
    return vendorDir;
  }

  public static class Builder {

    private String name;
    private String ruleClass;
    private ImmutableMap<String, Object> attributes;
    private ImmutableList<String> repoDeps;
    private ImmutableMap<String, String> repoMappings;
    private String vendorDir;

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setRuleClass(String ruleClass) {
      this.ruleClass = ruleClass;
      return this;
    }

    public Builder setAttributes(ImmutableMap<String, Object> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder setRepoDeps(ImmutableList<String> repoDeps) {
      this.repoDeps = repoDeps;
      return this;
    }

    public Builder setRepoMappings(ImmutableMap<String, String> repoMappings) {
      this.repoMappings = repoMappings;
      return this;
    }

    public Builder setVendorDir(String vendorDir) {
      this.vendorDir = vendorDir;
      return this;
    }

    public RepositoryInfo build() {
      return new RepositoryInfo(name, ruleClass, attributes, repoDeps, repoMappings, vendorDir);
    }
  }
}
