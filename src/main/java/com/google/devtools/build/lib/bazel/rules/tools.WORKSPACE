local_repository(
    name = "bazel_tools",
    path = __embedded_dir__ + "/embedded_tools",
    repo_mapping = {
        "@com_google_protobuf": "@com_google_protobuf",
        "@local_config_cc": "@local_config_cc",
        "@local_config_cc_toolchains": "@local_config_cc_toolchains",
        "@local_config_platform": "@local_config_platform",
        "@rules_cc": "@rules_cc",
        "@rules_java": "@rules_java",
        "@rules_proto": "@rules_proto",
        "@rules_python": "@rules_python",
    },
)

bind(
    name = "cc_toolchain",
    actual = "@bazel_tools//tools/cpp:default-toolchain",
)
