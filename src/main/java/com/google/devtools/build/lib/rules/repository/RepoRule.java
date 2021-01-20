package com.google.devtools.build.lib.rules.repository;

import static com.google.devtools.build.lib.packages.Attribute.attr;
import static com.google.devtools.build.lib.packages.Type.STRING;
import static com.google.devtools.build.lib.packages.Type.STRING_DICT;
import static com.google.devtools.build.lib.packages.Type.STRING_LIST;

import com.google.devtools.build.lib.analysis.RuleDefinition;
import com.google.devtools.build.lib.analysis.RuleDefinitionEnvironment;
import com.google.devtools.build.lib.packages.RuleClass;
import com.google.devtools.build.lib.packages.RuleClass.Builder;
import com.google.devtools.build.lib.packages.RuleClass.Builder.RuleClassType;

public class RepoRule implements RuleDefinition {

  public static final String NAME = "repo";
  public static final String FETCH_COMMAND = "fetch_command";
  public static final String FINGERPRINT = "fingerprint";
  public static final String REPO_DEPS = "repo_deps";
  public static final String ENVIRON = "environ";

  @Override
  public RuleClass build(Builder builder, RuleDefinitionEnvironment environment) {
    return builder
        /* <!-- #BLAZE_RULE(repo).ATTRIBUTE(fetch_command) -->
        The command that Bazel should call to fetch this repo.
        <!-- #END_BLAZE_RULE.ATTRIBUTE --> */
        .add(attr(FETCH_COMMAND, STRING_LIST).mandatory())
        .add(attr(FINGERPRINT, STRING).mandatory())
        .add(attr(REPO_DEPS, STRING_LIST))
        .add(attr(ENVIRON, STRING_DICT))
        .setWorkspaceOnly()
        .build();
  }

  @Override
  public Metadata getMetadata() {
    return RuleDefinition.Metadata.builder()
        .name(RepoRule.NAME)
        .type(RuleClassType.WORKSPACE)
        .ancestors(WorkspaceBaseRule.class)
        .factoryClass(WorkspaceConfiguredTargetFactory.class)
        .build();
  }
}
