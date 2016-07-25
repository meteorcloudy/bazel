// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.rules.android;

import static com.google.devtools.build.lib.packages.Attribute.ConfigurationTransition.HOST;
import static com.google.devtools.build.lib.packages.Attribute.attr;
import static com.google.devtools.build.lib.packages.BuildType.LABEL;
import static com.google.devtools.build.lib.syntax.Type.BOOLEAN;
import static com.google.devtools.build.lib.syntax.Type.STRING;
import static com.google.devtools.build.lib.syntax.Type.STRING_DICT;
import static com.google.devtools.build.lib.syntax.Type.STRING_LIST;

import com.google.devtools.build.lib.analysis.RuleDefinition;
import com.google.devtools.build.lib.analysis.RuleDefinitionEnvironment;
import com.google.devtools.build.lib.packages.Attribute.AllowedValueSet;
import com.google.devtools.build.lib.packages.RuleClass;
import com.google.devtools.build.lib.packages.RuleClass.Builder.RuleClassType;
import com.google.devtools.build.lib.rules.android.AndroidConfiguration.AndroidManifestMerger;
import com.google.devtools.build.lib.rules.android.AndroidRuleClasses.MultidexMode;

/**
 * Attributes for {@code android_binary} that are not present on {@code android_test}.
 */
public final class AndroidBinaryOnlyRule implements RuleDefinition {
  @Override
  public RuleClass build(RuleClass.Builder builder, final RuleDefinitionEnvironment env) {
    return builder
        /* <!-- #BLAZE_RULE(android_binary).ATTRIBUTE(manifest_merger) -->
        Select the manifest merger to use for this rule.<br/>
        Possible values:
        <ul>
            <li><code>manifest_merger = "legacy"</code>: Use the legacy manifest merger. Does not
              allow features of the android merger like placeholder substitution and tools
              attributes for defining merge behavior. Removes all
              <code>&lt;uses-permission&gt;</code> and <code>&lt;uses-permission-sdk-23&gt;</code>
              tags. Performs a tag-level merge.</li>
            <li><code>manifest_merger = "android"</code>: Use the android manifest merger. Allows
              features like placeholder substitution and tools attributes for defining merge
              behavior. Follows the semantics from
              <a href="http://tools.android.com/tech-docs/new-build-system/user-guide/manifest-merger">
              the documentation</a> except it has been modified to also remove all
              <code>&lt;uses-permission&gt;</code> and <code>&lt;uses-permission-sdk-23&gt;</code>
              tags. Performs an attribute-level merge.</li>
            <li><code>manifest_merger = "auto"</code>: Merger is controlled by the
              <a href="../blaze-user-manual.html#flag--android_manifest_merger">
              --android_manifest_merger</a> flag.</li>
        </ul>
        <!-- #END_BLAZE_RULE.ATTRIBUTE --> */
        .add(attr("manifest_merger", STRING)
            .allowedValues(new AllowedValueSet(AndroidManifestMerger.getAttributeValues()))
            .value(AndroidManifestMerger.getRuleAttributeDefault()))
        /* <!-- #BLAZE_RULE(android_binary).ATTRIBUTE(manifest_values) -->
        A dictionary of values to be overridden in the manifest. Any instance of ${name} in the
        manifest will be replaced with the value corresponding to name in this dictionary.
        applicationId, versionCode, versionName, minSdkVersion, targetSdkVersion and
        maxSdkVersion will also override the corresponding attributes of the manifest and
        uses-sdk tags. packageName will be ignored and will be set from either applicationId if
        specified or the package in manifest. When manifest_merger is set to legacy, only
        applicationId, versionCode and versionName will have any effect.
        <!-- #END_BLAZE_RULE.ATTRIBUTE --> */
        .add(attr("manifest_values", STRING_DICT))
        /* <!-- #BLAZE_RULE(android_binary).ATTRIBUTE(nocompress_extensions) -->
        A list of file extension to leave uncompressed in apk.
        <!-- #END_BLAZE_RULE.ATTRIBUTE --> */
        .add(attr("nocompress_extensions", STRING_LIST))
        /* <!-- #BLAZE_RULE(android_binary).ATTRIBUTE(crunch_png) -->
        Do PNG crunching (or not). This is independent of nine-patch processing, which is always
        done. Currently only supported for local resources (not android_resources).
        <!-- #END_BLAZE_RULE.ATTRIBUTE --> */
        .add(attr("crunch_png", BOOLEAN).value(true))
        /* <!-- #BLAZE_RULE(android_binary).ATTRIBUTE(resource_configuration_filters) -->
        A list of resource configuration filters, such 'en' that will limit the resources in the
        apk to only the ones in the 'en' configuration.
        <!-- #END_BLAZE_RULE.ATTRIBUTE --> */
        .add(attr("resource_configuration_filters", STRING_LIST))
        /* <!-- #BLAZE_RULE(android_binary).ATTRIBUTE(densities) -->
        Densities to filter for when building the apk.
        This will strip out raster drawable resources that would not be loaded by a device with
        the specified screen densities, to reduce APK size. A corresponding compatible-screens
        section will also be added to the manifest if it does not already contain a superset
        listing.
        <!-- #END_BLAZE_RULE.ATTRIBUTE --> */
        .add(attr("densities", STRING_LIST))
        .add(attr("$android_manifest_merge_tool", LABEL)
            .cfg(HOST)
            .exec()
            .value(env.getToolsLabel(AndroidRuleClasses.MANIFEST_MERGE_TOOL_LABEL)))

        /* <!-- #BLAZE_RULE(android_binary).ATTRIBUTE(multidex) -->
        Whether to split code into multiple dex files.<br/>
        Possible values:
        <ul>
          <li><code>native</code>: Split code into multiple dex files when the dex 64K index limit
            is exceeded. Assumes native platform support for loading multidex classes at runtime.
            <em class="harmful">This works with only Android L and newer</em>.</li>
          <li><code>legacy</code>: Split code into multiple dex files when the dex 64K index limit
            is exceeded. Assumes multidex classes are loaded through application code (i.e. no
            native platform support).</li>
          <li><code>manual_main_dex</code>: Split code into multiple dex files when the dex 64K
            index limit is exceeded. The content of the main dex file needs to be specified by
            providing a list of classes in a text file using the
            <a href="${link android_binary.main_dex_list}">main_dex_list</a> attribute.</li>
          <li><code>off</code>: Compile all code to a single dex file, even if it exceeds the index
            limit.</li>
        </ul>
        <!-- #END_BLAZE_RULE.ATTRIBUTE --> */
        .add(attr("multidex", STRING)
            .allowedValues(new AllowedValueSet(MultidexMode.getValidValues()))
            .value(MultidexMode.OFF.getAttributeValue()))
        .removeAttribute("data")
        .build();
  }

  @Override
  public Metadata getMetadata() {
    return RuleDefinition.Metadata.builder()
        .name("$android_binary_only")
        .type(RuleClassType.ABSTRACT)
        .ancestors(AndroidRuleClasses.AndroidBinaryBaseRule.class)
        .build();
  }
}
