# Copyright 2018 The Bazel Authors. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Utils for manipulating external repositories, once fetched.

### Setup

These utility are intended to be used by other repository rules. They
can be loaded as follows.

```python
load(
    "@bazel_tools//tools/build_defs/repo:utils.bzl",
    "workspace_and_buildfile",
    "patch",
    "update_attrs",
)
```
"""

def workspace_and_buildfile(ctx):
    """Utility function for writing WORKSPACE and, if requested, a BUILD file.

    This rule is inteded to be used in the implementation function of a
    repository rule.
    It assumes the parameters `name`, `build_file`, `build_file_contents`,
    `workspace_file`, and `workspace_file_content` to be
    present in `ctx.attr`, the latter four possibly with value None.

    Args:
      ctx: The repository context of the repository rule calling this utility
        function.
    """
    if ctx.attr.build_file and ctx.attr.build_file_content:
        ctx.fail("Only one of build_file and build_file_content can be provided.")

    if ctx.attr.workspace_file and ctx.attr.workspace_file_content:
        ctx.fail("Only one of workspace_file and workspace_file_content can be provided.")

    if ctx.attr.workspace_file:
        ctx.delete("WORKSPACE")
        ctx.symlink(ctx.attr.workspace_file, "WORKSPACE")
    elif ctx.attr.workspace_file_content:
        ctx.delete("WORKSPACE")
        ctx.file("WORKSPACE", ctx.attr.workspace_file_content)
    else:
        ctx.file("WORKSPACE", "workspace(name = \"{name}\")\n".format(name = ctx.name))

    if ctx.attr.build_file:
        ctx.delete("BUILD.bazel")
        ctx.symlink(ctx.attr.build_file, "BUILD.bazel")
    elif ctx.attr.build_file_content:
        ctx.delete("BUILD.bazel")
        ctx.file("BUILD.bazel", ctx.attr.build_file_content)

def _is_windows(ctx):
    return ctx.os.name.lower().find("windows") != -1

def _get_windows_patch(ctx):
    patch_tool_dir = ".bazel_tmp/patch/"
    ctx.download_and_extract([
        # TODO(pcloudy): Add patch-2.5.9-7-bin.zip to bazel mirror.
        "https://kent.dl.sourceforge.net/project/gnuwin32/patch/2.5.9-7/patch-2.5.9-7-bin.zip"],
        patch_tool_dir,
        "fabd6517e7bd88e067db9bf630d69bb3a38a08e044fa73d13a704ab5f8dd110b",
    )

    # Rename the patch tool to pt.exe so that it doesn't require admin right.
    patch = patch_tool_dir + "bin/pt.exe"
    ctx.symlink(patch_tool_dir + "bin/patch.exe", patch)
    return patch

def patch(ctx):
    """Implementation of patching an already extracted repository.

    This rule is inteded to be used in the implementation function of a
    repository rule. It assuumes that the parameters `patches`, `patchtool`,
    `patch_args`, and `patch_cmds` are present in `ctx.attr`.

    Args:
      ctx: The repository context of the repository rule calling this utility
        function.
    """
    bash_exe = ctx.os.environ["BAZEL_SH"] if "BAZEL_SH" in ctx.os.environ else "bash"
    powershell_exe = ctx.os.environ["BAZEL_POWERSHELL"] if "BAZEL_POWERSHELL" in ctx.os.environ else "powershell.exe"
    if len(ctx.attr.patches) > 0 or len(ctx.attr.patch_cmds) > 0:
        ctx.report_progress("Patching repository")

    patch_tool = ctx.attr.patch_tool
    if patch_tool == "auto":
        patch_tool = _get_windows_patch(ctx) if _is_windows(ctx) else "patch"

    for patchfile in ctx.attr.patches:
        if _is_windows(ctx):
            command = "Get-Content {patchfile} | {patchtool} {patch_args}".format(
                patchtool = patch_tool,
                patchfile = ctx.path(patchfile),
                patch_args = " ".join([
                    "'%s'" % arg
                    for arg in ctx.attr.patch_args
                ]),
            )
            st = ctx.execute([powershell_exe, "/c", command])
            if st.return_code:
                fail("Error applying patch %s:\n%s%s" %
                     (str(patchfile), st.stderr, st.stdout))
        else:
            command = "{patchtool} {patch_args} < {patchfile}".format(
                patchtool = patch_tool,
                patchfile = ctx.path(patchfile),
                patch_args = " ".join([
                    "'%s'" % arg
                    for arg in ctx.attr.patch_args
                ]),
            )
            st = ctx.execute([bash_exe, "-c", command])
            if st.return_code:
                fail("Error applying patch %s:\n%s%s" %
                     (str(patchfile), st.stderr, st.stdout))
    if _is_windows(ctx) and hasattr(ctx.attr, "patch_cmds_win"):
        for cmd in ctx.attr.patch_cmds_win:
            st = ctx.execute([powershell_exe, "/c", cmd])
            if st.return_code:
                fail("Error applying patch command %s:\n%s%s" %
                     (cmd, st.stdout, st.stderr))
    else:
        for cmd in ctx.attr.patch_cmds:
            st = ctx.execute([bash_exe, "-c", cmd])
            if st.return_code:
                fail("Error applying patch command %s:\n%s%s" %
                     (cmd, st.stdout, st.stderr))

def update_attrs(orig, keys, override):
    """Utility function for altering and adding the specified attributes to a particular repository rule invocation.

     This is used to make a rule reproducible.

    Args:
        orig: dict of actually set attributes (either explicitly or implicitly)
            by a particular rule invocation
        keys: complete set of attributes defined on this rule
        override: dict of attributes to override or add to orig

    Returns:
        dict of attributes with the keys from override inserted/updated
    """
    result = {}
    for key in keys:
        if getattr(orig, key) != None:
            result[key] = getattr(orig, key)
    result["name"] = orig.name
    result.update(override)
    return result
