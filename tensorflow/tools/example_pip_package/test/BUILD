package(default_visibility = ["//visibility:private"])

sh_test(
    name = "pywrap_example_test",
    srcs = ["pywrap_example_test.sh"],
    data = ["//tensorflow/tools/example_pip_package:all_files",
            "test-setup.sh"],
)

sh_test(
    name = "build_example_pip_package_test",
    srcs = ["build_example_pip_package_test.sh"],
    data = ["//tensorflow/tools/example_pip_package:all_files",
            "test-setup.sh"],
)
