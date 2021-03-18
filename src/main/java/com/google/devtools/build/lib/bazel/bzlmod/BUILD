load("@rules_java//java:defs.bzl", "java_library")

package(
    default_visibility = ["//src:__subpackages__"],
)

java_library(
    name = "repo_rule_value",
    srcs = [
        "BzlmodRepoRuleValue.java",
        "ResolvedRepositoryValue.java",
        "ResolvedBazelModuleRepositoriesValue.java",
        "ResolvedModuleRuleRepositoriesValue.java",
    ],
    deps = [
        ":repo_info",
        "//src/main/java/com/google/devtools/build/lib/concurrent",
        "//src/main/java/com/google/devtools/build/lib/packages",
        "//src/main/java/com/google/devtools/build/skyframe:skyframe-objects",
        "//src/main/java/com/google/devtools/build/lib/skyframe/serialization/autocodec",
        "//third_party:gson",
        "//third_party:guava",
    ],
)

java_library(
    name = "repo_info",
    srcs = ["RepositoryInfo.java"],
    deps = [
        "//third_party:guava",
    ],
)

java_library(
    name = "repo_rule_creator",
    srcs = ["BzlmodRepoRuleCreator.java"],
    deps = [
        "//src/main/java/com/google/devtools/build/lib/packages",
        "//src/main/java/net/starlark/java/eval",
    ]
)
