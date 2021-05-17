load("@rules_java//java:defs.bzl", "java_library")

package(
    default_visibility = ["//src:__subpackages__"],
)

filegroup(
    name = "srcs",
    srcs = glob(["*"]),
    visibility = ["//src:__subpackages__"],
)

java_library(
    name = "common",
    srcs = ["ModuleKey.java"],
    deps = [
        "//third_party:auto_value",
        "//third_party:jsr305",
    ],
)

java_library(
    name = "registry",
    srcs = [
        "IndexRegistry.java",
        "Registry.java",
        "RegistryFactory.java",
        "RegistryFactoryImpl.java",
    ],
    deps = [
        ":common",
        "//src/main/java/com/google/devtools/build/lib/bazel/bzlmod/repo:repo_spec_values",
        "//src/main/java/com/google/devtools/build/lib/bazel/repository/downloader",
        "//src/main/java/com/google/devtools/build/lib/events",
        "//src/main/java/com/google/devtools/build/lib/vfs",
        "//src/main/java/net/starlark/java/eval",
        "//third_party:gson",
        "//third_party:guava",
    ],
)

java_library(
    name = "resolution",
    srcs = [
        "ArchiveOverride.java",
        "DiscoveryFunction.java",
        "DiscoveryValue.java",
        "GitOverride.java",
        "LocalPathOverride.java",
        "Module.java",
        "ModuleFileFunction.java",
        "ModuleFileGlobals.java",
        "ModuleFileValue.java",
        "NonRegistryOverride.java",
        "ParsedVersion.java",
        "RegistryOverride.java",
        "SelectionFunction.java",
        "SelectionValue.java",
        "SingleVersionOverride.java",
    ],
    deps = [
        ":common",
        ":registry",
        "//src/main/java/com/google/devtools/build/lib/actions:file_metadata",
        "//src/main/java/com/google/devtools/build/lib/bazel/bzlmod/repo:repo_spec_values",
        "//src/main/java/com/google/devtools/build/lib/cmdline:cmdline-primitives",
        "//src/main/java/com/google/devtools/build/lib/concurrent",
        "//src/main/java/com/google/devtools/build/lib/events",
        "//src/main/java/com/google/devtools/build/lib/packages",
        "//src/main/java/com/google/devtools/build/lib/rules:repository/repository_directory_value",
        "//src/main/java/com/google/devtools/build/lib/skyframe:precomputed_value",
        "//src/main/java/com/google/devtools/build/lib/skyframe:sky_functions",
        "//src/main/java/com/google/devtools/build/lib/skyframe/serialization/autocodec:autocodec-annotation",
        "//src/main/java/com/google/devtools/build/lib/starlarkbuildapi/repository",
        "//src/main/java/com/google/devtools/build/lib/vfs",
        "//src/main/java/com/google/devtools/build/lib/vfs:pathfragment",
        "//src/main/java/com/google/devtools/build/skyframe",
        "//src/main/java/com/google/devtools/build/skyframe:skyframe-objects",
        "//src/main/java/net/starlark/java/eval",
        "//src/main/java/net/starlark/java/syntax",
        "//third_party:auto_value",
        "//third_party:guava",
        "//third_party:jsr305",
    ],
)
