Project: /_project.yaml Book: /_book.yaml

# Bzlmod Migration Guide

{% include "_buttons.html" %}

As explained in the [overview page](/external/overview), Bzlmod is going to
replace the legacy WORKSPACE system in future Bazel releases. This guide helps
you migrate your project to Bzlmod and drop the needs of WORKSPACE.

## WORKSPACE vs Bzlmod

Bazel's WORKSPACE and Bzlmod offer similar features with different syntax. Here
are the tips for migrating from WORKSPACE functionalities to Bzlmod.

### Define the root of a Bazel workspace

The WORKSPACE file marks the source root of a Bazel project, this responsibility
is not yet replaced by Bzlmod. There should still be a `WORKSPACE` or
`WORKSPACE.bazel` file at your workspace root.

### Specify repository name for your workspace

#### WORKSPACE

The [workspace](rules/lib/globals/workspace#workspace) function is used to
specify a repository name for your workspace. This allows a target `//foo:bar`
in the workspace to be referenced as `@<workspace name>//foo:bar`. If not
specified, the default repository name for your repository is `__main__`.

**WORKSPACE example**:

```python
workspace(name = "com_foo_bar")
```

#### Bzlmod

It's recommended to reference targets in the same workspace with the `//foo:bar`
syntax without `@<repo name>`. But if you do need the old syntax for any
reasons, you can specify the module name specified by the
[module](/rules/lib/globals/module#module) function as the repository name. If
you need to keep compatibility with WORKSPACE, you can use `repo_name` attribute
of the [module](rules/lib/globals/module#module) function to override the
repository name.

**MODULE.bazel example**:

```python
module(
    name = "bar",
    repo_name = "com_foo_bar",
)
```

### Fetch Bazel projects as external dependencies

This is often achieved via [http_archive](/rules/lib/repo/http#http_archive) or
[git_repository](/rules/lib/repo/git#git_repository) repository rules.

**WORKSPACE example**:

```python
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

http_archive(
    name = "bazel_skylib",
    sha256 = "66ffd9315665bfaafc96b52278f57c7e2dd09f5ede279ea6d39b2be471e7e3aa",
    urls = ["https://github.com/bazelbuild/bazel-skylib/releases/download/1.4.2/bazel-skylib-1.4.2.tar.gz"],
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()
```

For `bazel_skylib`, it already builds with Bazel, which means it can be
migrated to Bzlmod and then be introduced as a [Bazel module](/external/module).
When it's available in [Bazel Central Registry](https://registry.bazel.build) or
your custom Bazel registry, you can simply depend on it via a
[bazel_dep](/rules/lib/globals/module#bazel_dep) directive.

**MODULE.bazel example**:

```python
bazel_dep(name = "bazel_skylib", version = "1.4.2")
```

Since Bzlmod automatically fetches transitive dependencies of Bazel modules, you
don't need to load the `bazel_skylib_workspace` macro.

### Fetch other external dependencies

**WORKSPACE example**:

```python
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_file")

http_file(
    name = "my_file",
    url = "http://example.com/file",
    sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
)
```

### Inspect the host machine environment

### Integrate third party package manager

### Introduce local repositories

### Register toolchains & execution platforms

### Bind targets

### Other custom repository rules

## Bazel module vs module extension

## Toolchains registration with Bzlmod

## Builtin default dependencies

## Hybrid mode

### WORKSPACE.bzlmod

## Migration tool

## Best practices

### For end projects

### For dependency projects

## FAQs
