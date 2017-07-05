
cc_binary(
  name = "launcher",
  srcs = ["launcher_main.cc"],
  deps = [
    ":launcher_lib",
    # ":java_launcher",
    # ":python_launcher",
    # ":shell_launcher",
  ],
)

cc_library(
  name = "data_parser",
  srcs = [
    "data_parser.cc",
  ],
  hdrs = [
    "data_parser.h",
  ],
)

cc_library(
  name = "launcher_lib",
  srcs = [
    "launcher.cc",
  ],
  hdrs = [
    "launcher.h",
  ],
  deps = [
    ":data_parser",
  ],
)

cc_library(
  name = "java_launcher",
  srcs = [
    "java_launcher.cc",
  ],
  hdrs = [
    "java_launcher.h"
  ],
  deps = [
    ":launcher_lib",
  ],
)

cc_library(
  name = "python_launcher",
  srcs = [
    "python_launcher.cc",
  ],
  hdrs = [
    "python_launcher.h"
  ],
  deps = [
    ":launcher_lib",
  ],
)

cc_library(
  name = "shell_launcher",
  srcs = [
    "shell_launcher.cc",
  ],
  hdrs = [
    "shell_launcher.h"
  ],
  deps = [
    ":launcher_lib",
  ],
)

cc_binary(
  name = "append_data",
  srcs = [
    "append_data.cc",
  ],
)

genrule(
  name = "launcher_with_data",
  srcs = [
    ":launcher",
  ],
  outs = [
    ":launcher_with_data.exe",
  ],
  cmd = "$(location :append_data) $< $@ \"I am the Data\"",
  tools = [
    ":append_data",
  ],
  executable = 1,
)