package(default_visibility = ["//visibility:public"])

load("@rules_cc//cc:defs.bzl", "cc_binary", "cc_library", "cc_test")

config_setting(
    name = "macos",
    values = {
        "apple_platform_type": "macos",
        "cpu": "darwin",
    },
    visibility = ["//visibility:public"],
)

genrule(
   name = "gen",
   cmd = "$(location :hello-world) > $@",
   outs = ["output"],
   tools = [":hello-world"],
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
    srcs = ["hello-world.cc"],
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
