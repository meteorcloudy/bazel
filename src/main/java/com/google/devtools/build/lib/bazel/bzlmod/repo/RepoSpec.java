package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.common.collect.ImmutableMap;

public class RepoSpec {
  private String ruleClass;
  private ImmutableMap<String, Object> attributes;

  public RepoSpec(String ruleClass, ImmutableMap<String, Object> attributes) {
    this.ruleClass = ruleClass;
    this.attributes = attributes;
  }

  public String getRuleClass() {
    return ruleClass;
  }

  public ImmutableMap<String, Object> getAttributes() {
    return attributes;
  }
}
