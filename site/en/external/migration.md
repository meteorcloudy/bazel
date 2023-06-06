Project: /_project.yaml Book: /_book.yaml

# Bzlmod Migration Guide

{% include "_buttons.html" %}

As explained in the [overview page](/external/overview), Bzlmod is going to
replace the WORKSPACE system in future Bazel releases. This guide helps you
understand each functionality in the WORKSPACE file and provide advice for
migrating them to Bzlmod.

## Migrate from WORKSPACE to Bzlmod

Bazel's WORKSPACE and Bzlmod offer similar features with different syntax. Here
are the tips for migrating from WORKSPACE functionalities to Bzlmod.

### Define the root of a Bazel workspace

This responsibility is yet not replaced by Bzlmod, therefore there should still
be a `WORKSPACE` or `WORKSPACE.bazel` file to indicate the root of a Bazel
workspace.

If you have fully migrated to Bzlmod, the `WORKSPACE` can be empty with a
comment like:

```python
# This file marks the root of the Bazel workspace.
# See MODULE.bazel for external dependencies setup.
```

### Specify repository name for your workspace

The [workspace](rules/lib/globals/workspace#workspace) function is used to
specify your workspace name. This allows a target `//foo:bar` in the workspace
to be referenced as `@<workspace name>//foo:bar`, if not specified, the default
workspace name is `__main__`.

**WORKSPACE example**:

```python
workspace(name = "com_foo_bar")
```

With Bzlmod, it's recommended to not use the `@<workspace name>//` prefix for
referencing targets in the same repository. But if you do need this syntax for
legacy reasons, you can use `repo_name` attribute of the
[module](rules/lib/globals/module#module) function.

**MODULE.bazel example**:

```python
module(
    name = "bar",
    repo_name = "com_foo_bar",
)
```

### Download external dependencies

One of the main functionalities of WORKSPACE is to download external dependencies. This is often achieved via [http_archive](/rules/lib/repo/http#http_archive) or [git_repository](/rules/lib/repo/git#git_repository) repository rules, but could also be done with custom repository rules.

**WORKSPACE example**:

```python
http_archive(
    name = "bazel_skylib",
    sha256 = "66ffd9315665bfaafc96b52278f57c7e2dd09f5ede279ea6d39b2be471e7e3aa",
    urls = ["https://github.com/bazelbuild/bazel-skylib/releases/download/1.4.2/bazel-skylib-1.4.2.tar.gz"],
)

load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")

bazel_skylib_workspace()
```

If the dependency already builds with Bazel, that means the dependency can be
first migrated to Bzlmod and then be introduced as a [Bazel
module](/external/module). When it's available in [Bazel Central
Registry](https://registry.bazel.build) or your custom Bazel registry, you can
simply depend on it via a [bazel_dep](/rules/lib/globals/module#bazel_dep)
directive.

**MODULE.bazel example**:

```python
bazel_dep(name = "bazel_skylib", version = "1.4.2")
```

Since Bzlmod automatically fetches transitive dependencies of Bazel modules, you
don't need to load the `bazel_skylib_workspace` macro.

### Inspect the host machine environment

### Integrate third party package manager

### Introduce local repositories

### Register toolchains & execution platforms

### Bind targets

### Other custom repository rules

## Bazel module vs module extension

## Toolchains registration with Bzlmod

## Hybrid mode

## Migration tool

## Best practices

## FAQs
