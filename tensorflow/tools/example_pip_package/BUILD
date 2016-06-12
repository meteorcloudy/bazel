package(default_visibility = ["//visibility:public"])

load(":tensorflow.bzl", "tf_py_wrap_cc")
export_files = (["swig.bat"])

sh_binary(
    name = "build_example_pip_package",
    srcs = ["build_example_pip_package.sh"],
    data = [
        "MANIFEST.in",
        "README.rst",
        "LICENSE.txt",
        "setup.py",
        "setup.cfg",
        ":pywrap_example",
        ":pywrap_example_pyd_file"
    ],
)

genrule(
    name = "pywrap_example_pyd_file",
    outs = ["_pywrap_example.pyd"],
    srcs = [":_pywrap_example.so"],
    cmd = "cp $< $@",
)

tf_py_wrap_cc(
    name = "pywrap_example",
    srcs = ["example.i"],
    deps = [
        ":example_lib",
    ],
)

cc_library(
    name = "example_lib",
    srcs = ["example_lib.c"],
    hdrs = [
        "example_lib.h",
    ],
)

filegroup(
    name = "swig",
    srcs = ["swig.bat"],
)

filegroup(
    name = "all_files",
    srcs = [
            "BUILD", 
            "example.i",
            "example_lib.c",
            "example_lib.h",
            "MANIFEST.in",
            "README.rst",
            "LICENSE.txt",
            "setup.py",
            "setup.cfg",
            "tensorflow.bzl",
            "swig.bat",
            "build_example_pip_package.sh",
    ],
)

