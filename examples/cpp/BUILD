package(default_visibility = ["//visibility:public"])

load("@rules_cc//cc:defs.bzl", "cc_binary", "cc_library", "cc_test")
load("@bazel_skylib//lib:selects.bzl", "selects")

# For selecting macos x86 sources
config_setting(
    name = "macos_x86",
    values = {
        "apple_platform_type": "macos",
        "cpu": "darwin",
    },
    visibility = ["//visibility:public"],
)

# For selecting macos arm64 sources
config_setting(
    name = "macos_arm64",
    values = {
        "apple_platform_type": "macos",
        "cpu": "darwin_arm64",
    },
    visibility = ["//visibility:public"],
)

# For selecting macos arm64e sources
config_setting(
    name = "macos_arm64e",
    values = {
        "apple_platform_type": "macos",
        "cpu": "darwin_arm64e",
    },
    visibility = ["//visibility:public"],
)

# For selecting general macos sources
selects.config_setting_group(
    name = "macos",
    match_any = [
        ":macos_x86",
        ":macos_arm64",
        ":macos_arm64e",
    ],
)

cc_library(
    name = "hello-lib",
    srcs = select({
        ":macos": ["hello-lib-macos.cc"],
        "//conditions:default": ["hello-lib.cc"],
    }),
    hdrs = ["hello-lib.h"],
)

cc_binary(
    name = "hello-world",
    srcs = select({
        ":macos_x86": ["hello-world-macos-x86.cc"],
        ":macos_arm64": ["hello-world-macos-arm64.cc"],
        ":macos_arm64e": ["hello-world-macos-arm64e.cc"],
        "//conditions:default": ["hello-world.cc"],
    }),
    deps = [":hello-lib"],
)

cc_test(
    name = "hello-success_test",
    srcs = ["hello-world.cc"],
    deps = [":hello-lib"],
)

cc_test(
    name = "hello-fail_test",
    srcs = ["hello-fail.cc"],
    deps = [":hello-lib"],
)

filegroup(
    name = "srcs",
    srcs = glob(["**"]),
)
