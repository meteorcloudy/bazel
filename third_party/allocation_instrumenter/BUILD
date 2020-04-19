load("@rules_java//java:defs.bzl", "java_import")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2.0

filegroup(
    name = "srcs",
    srcs = glob(["**"]),
)

java_import(
    name = "allocation_instrumenter",
#    jars = ["java-allocation-instrumenter-3.0.1.jar"],
    jars = ["@debian_allocation_instrumenter//:java-allocation-instrumenter-3.1.0.jar"],
)
