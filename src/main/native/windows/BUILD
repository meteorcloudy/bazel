package(default_visibility = ["//visibility:private"])

filegroup(
    name = "srcs",
    srcs = glob(["**"]),
    visibility = ["//src/main/native:__pkg__"],
)

filegroup(
    name = "embedded_tools",
    srcs = glob([
        "*.cc",
        "*.h",
    ]) + [
        "BUILD",
    ],
    visibility = ["//src/main/native:__pkg__"],
)

cc_library(
    name = "lib-file",
    srcs = ["file.cc"],
    hdrs = ["file.h"],
    visibility = [
        "//src/main/cpp:__subpackages__",
        "//src/test/cpp:__subpackages__",
        "//src/test/native:__subpackages__",
        "//tools/test:__pkg__",
    ],
    deps = [":lib-util"],
)

cc_library(
    name = "lib-util",
    srcs = ["util.cc"],
    hdrs = ["util.h"],
    visibility = [
        "//src/tools/launcher/util:__pkg__",
        "//tools/test:__pkg__",
    ],
)

cc_binary(
    name = "windows_jni.dll",
    srcs = [
        "file-jni.cc",
        "jni-util.cc",
        "jni-util.h",
        "processes-jni.cc",
        "//src/main/native:jni.h",
        "//src/main/native:jni_md.h",
    ],
    linkshared = 1,
    visibility = ["//visibility:public"],
    linkopts = ["-DEFAULTLIB:Shlwapi.lib"],
    deps = [
        ":lib-file",
        ":lib-util",
    ],
)

# Directly re-export resources.o so it can be consumed in the client.
cc_library(
    name = "resources",
    srcs = ["resources.o"],
    visibility = ["//src/main/cpp:__pkg__"],
)
