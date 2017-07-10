
cc_binary(
  name = "launcher",
  srcs = ["launcher_main.cc"],
  deps = [
    ":launcher_lib",
    ":java_launcher",
    ":python_launcher",
    ":bash_launcher",
    ":data_parser",
    ":util",
  ],
)

genrule(
  name = "launcher_with_null_data",
  srcs = [":launcher"],
  outs = [":launcher_with_null_data.exe"],
  cmd = "$(location :append_data) $< $@",
  tools = [":append_data"],
  executable = 1,
)

genrule(
  name = "launcher_with_data",
  srcs = [":launcher"],
  outs = [":launcher_with_data.exe"],
  cmd = "$(location :append_data) $< $@  \"BINARY_TYPE=python\" \"PYTHON_BIN=C:\\Program Files\\Anaconda3\\python.exe\"",
  tools = [":append_data"],
  executable = 1,
)

cc_binary(
  name = "append_data",
  srcs = ["append_data.cc"],
)

cc_library(
  name = "util",
  srcs = ["launcher_util.cc"],
  hdrs = ["launcher_util.h"],
)

cc_library(
  name = "data_parser",
  srcs = ["data_parser.cc"],
  hdrs = ["data_parser.h"],
  deps = ["//src/main/cpp/util:errors"],
)

cc_library(
  name = "launcher_lib",
  srcs = [
    "launcher.cc",
    "data_parser.h",
  ],
  deps = ["//src/main/cpp/util:errors"],
  hdrs = ["launcher.h"],
)

cc_library(
  name = "java_launcher",
  srcs = ["java_launcher.cc"],
  hdrs = ["java_launcher.h"],
  deps = [":launcher_lib"],
)

cc_library(
  name = "python_launcher",
  srcs = ["python_launcher.cc"],
  hdrs = ["python_launcher.h"],
  deps = [":launcher_lib", ":util"],
)

cc_library(
  name = "bash_launcher",
  srcs = ["bash_launcher.cc"],
  hdrs = ["bash_launcher.h"],
  deps = [":launcher_lib"],
)
