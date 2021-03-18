package com.google.devtools.build.lib.bazel.bzlmod.repo;

import com.google.devtools.build.lib.events.EventHandler;
import com.google.devtools.build.lib.packages.Package;
import com.google.devtools.build.lib.packages.Rule;
import com.google.devtools.build.lib.packages.RuleFactory.InvalidRuleException;

import net.starlark.java.eval.StarlarkSemantics;

import java.util.Map;

public interface BzlmodRepoRuleCreator {
  Rule createRule(Package.Builder pkg, StarlarkSemantics semantics, Map<String, Object> kwargs, EventHandler handler)
      throws InterruptedException, InvalidRuleException;
}
