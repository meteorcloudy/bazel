# -*- Python -*-

# Parse the bazel version string from `native.bazel_version`.
def _parse_bazel_version(bazel_version):
  # Remove commit from version.
  version = bazel_version.split(" ", 1)[0]

  # Split into (release, date) parts and only return the release
  # as a tuple of integers.
  parts = version.split('-', 1)

  # Turn "release" into a tuple of strings
  version_tuple = ()
  for number in parts[0].split('.'):
    version_tuple += (str(number),)
  return version_tuple

# Check that a specific bazel version is being used.
def check_version(bazel_version):
  if "bazel_version" in dir(native) and native.bazel_version:
    current_bazel_version = _parse_bazel_version(native.bazel_version)
    minimum_bazel_version = _parse_bazel_version(bazel_version)
    if minimum_bazel_version > current_bazel_version:
      fail("\nCurrent Bazel version is {}, expected at least {}\n".format(
          native.bazel_version, bazel_version))
  pass

# Bazel rules for building swig files.
def _py_wrap_cc_impl(ctx):
  srcs = ctx.files.srcs
  if len(srcs) != 1:
    fail("Exactly one SWIG source file label must be specified.", "srcs")
  module_name = ctx.attr.module_name
  cc_out = ctx.outputs.cc_out
  py_out = ctx.outputs.py_out
  src = ctx.files.srcs[0]
  args = ["-c++", "-python"]
  args += ["-module", module_name]
  args += ["-l" + f.path for f in ctx.files.swig_includes]
  cc_include_dirs = set()
  cc_includes = set()
  for dep in ctx.attr.deps:
    cc_include_dirs += [h.dirname for h in dep.cc.transitive_headers]
    cc_includes += dep.cc.transitive_headers
  args += ["-I" + x for x in cc_include_dirs]
  args += ["-I" + ctx.label.workspace_root]
  args += ["-o", cc_out.path]
  args += ["-outdir", py_out.dirname]
  args += [src.path]
  outputs = [cc_out, py_out]
  ctx.action(executable=ctx.executable.swig_binary,
             arguments=args,
             mnemonic="PythonSwig",
             inputs=sorted(set([src]) + cc_includes + ctx.files.swig_includes +
                         ctx.attr.swig_deps.files),
             outputs=outputs,
             progress_message="SWIGing {input}".format(input=src.path))
  return struct(files=set(outputs))

_py_wrap_cc = rule(
    attrs = {
        "srcs": attr.label_list(
            mandatory = True,
            allow_files = True,
        ),
        "swig_includes": attr.label_list(
            cfg = DATA_CFG,
            allow_files = True,
        ),
        "deps": attr.label_list(
            allow_files = True,
            providers = ["cc"],
        ),
        "swig_deps": attr.label(default = Label(
           "//tensorflow/tools/example_pip_package:swig"
        )),
        "module_name": attr.string(mandatory = True),
        "py_module_name": attr.string(mandatory = True),
        "swig_binary": attr.label(
            default = Label("//tensorflow/tools/example_pip_package:swig.bat"),
            cfg = HOST_CFG,
            executable = True,
            allow_files = True,
        ),
    },
    outputs = {
        "cc_out": "%{module_name}.cc",
        "py_out": "%{py_module_name}.py",
    },
    implementation = _py_wrap_cc_impl,
)

def tf_py_wrap_cc(name, srcs, swig_includes=[], deps=[], copts=[], **kwargs):
  module_name = name.split("/")[-1]
  # Convert a rule name such as foo/bar/baz to foo/bar/_baz.so
  # and use that as the name for the rule producing the .so file.
  cc_library_name = "/".join(name.split("/")[:-1] + ["_" + module_name + ".so"])
  extra_deps = []
  _py_wrap_cc(name=name + "_py_wrap",
              srcs=srcs,
              swig_includes=swig_includes,
              deps=deps + extra_deps,
              module_name=module_name,
              py_module_name=name)
  native.cc_binary(
      name=cc_library_name,
      srcs=[module_name + ".cc"],
      copts=(copts + ["-Wno-self-assign", "-Wno-write-strings"]),
      linkopts=[],
      linkstatic=1,
      #linkshared=1,
      deps=deps)
  native.py_library(name=name,
                    srcs=[":" + name + ".py"],
                    srcs_version="PY2AND3",
                    data=[":" + cc_library_name])

