Project: /_project.yaml
Book: /_book.yaml

# Release Model

{% dynamic setvar source_file "site/en/release/index.md" %}
{% include "_buttons.html" %}

Bazel 4.0 and higher provides support for two release tracks: rolling releases
and long term support (LTS) releases. This page covers Bazel release versioning,
types of releases, and the benefits of those releases for Bazel users and
contributors.

## Release Versioning {:#bazel-versioning}

Bazel uses a _major.minor.patch_ [semantic versioning](https://semver.org/)
scheme.

* A _major release_ contains features that are not backward compatible with the
  previous release. Each major Bazel version is a LTS release.
* A _minor release_ contains bug fixes and backward-compatible features
  back-ported from the main branch.
* A _patch release_ contains critical bug fixes.

Additionally, pre-release versions are indicated by appending a hyphen and a
suffix to the next major version number.

For example, a new release of each type would result in these version numbers:

* Major: 6.0.0
* Minor: 6.1.0
* Patch: 6.1.2
* Pre-release: 7.0.0-pre.20230502.1

## Support Stages {:#support-stages}

For each major Bazel version, there are four support stages:

* **Rolling**: This major version is still in pre-release, the Bazel team
  publishes rolling releases from HEAD.
* **Active**: This major version is the current active LTS release. The Bazel
  team will backport important features and bug fixes into its minor/patch
  releases.
* **Maintenance**: This major version is an old LTS release in maintenance mode.
  The Bazel team only promises to backport critical bug fixes for security
  issues and OS-compatibility issues into this LTS release.
* **Deprecated**: The Bazel team no longer provides support for this major
  version, all users should migrate to newer Bazel LTS releases.

## Release Cadence {:#release-cadence}

### Rolling releases

* Rolling releases are coordinated with Google Blaze release and are released
  from HEAD around every two weeks.
* Rolling releases can ship incompatible changes. Incompatible flags are
  recommended for major breaking changes, rolling out incompatible changes
  should follow our [backward compatibility
  policy](/release/backward-compatibility).

### LTS releases

* _major release_: New LTS release is expected to be cut from HEAD once every 12
  months. Once a new LTS release is out, it immediately enters the Active stage,
  and the previous LTS release enters the Maintenance stage.
* _minor release_: New minor releases for the Active LTS release are expected to
  be released once every 2 months.
* _patch release_: New patch releases for LTS releases in Active and Maintenance
  stages are expected to be released on demand for critical bug fixes.
* A Bazel LTS release enters the Deprecated stage after being in ​​the
  Maintenance stage for 2 years.

## Support Matrix

| LTS release | Support stage | Latest version | End of support |
| ----------- | ------------- | -------------- | -------------- |
| Bazel 7     | Rolling       | 7.0.0-pre.20230502.1 | N/A      |
| Bazel 6     | Active        | 6.1.1          | Dec 2025       |
| Bazel 5     | Maintenance   | 5.2.1          | Dec 2024       |
| Bazel 4     | Deprecated    | 4.2.1          | Dec 2023       |










## Release Policy

Bazel maintains a
[Long Term Support (LTS)](/release/versioning)
release model, where a major version is released every nine months and minor
versions are released monthly. This page covers the Bazel release policy,
including the release candidates, timelines, announcements, and testing.

Bazel releases can be found on
[GitHub](https://github.com/bazelbuild/bazel/releases){: .external}.

## Release candidates {:#release-candidates}

A release candidate for a new version of Bazel is usually created at the
beginning of every month. The work is tracked by a
[release bug on GitHub](https://github.com/bazelbuild/bazel/issues?q=is%3Aissue+is%3Aopen+label%3Arelease){: .external}
indicating a target release date, and is assigned to the current Release manager.
Release candidates should pass all Bazel unit tests, and show no unwanted
regression in the projects tested on [Buildkite](https://buildkite.com/bazel){: .external}.

Release candidates are announced on
[bazel-discuss](https://groups.google.com/g/bazel-discuss){: .external}.
Over the next days, the Bazel team monitors community bug reports for any
regressions in the candidates.

### Releasing {:#releasing}

If no regressions are discovered, the candidate is officially released after
one week. However, regressions can delay the release of a release candidate. If
regressions are found, the Bazel team applies corresponding cherry-picks to the
release candidate to fix those regressions. If no further regressions are found
for two consecutive business days beginning after one week since the first
release candidate, the candidate is released.

New features are not cherry-picked into a release candidate after it is cut.
Moreover, if a new feature is buggy, the feature may be rolled back from a
release candidate. Only bugs that have the potential to highly impact or break
the release build are fixed in a release candidate after it is cut.

A release is only released on a day where the next day is a business day.

If a critical issue is found in the latest release, the Bazel team creates a
patch release by applying the fix to the release. Because this patch updates an
existing release instead of creating a new one, the patch release candidate can
be released after two business days.

### Testing {:#testing}

A nightly build of all projects running on
[ci.bazel.build](https://github.com/bazelbuild/continuous-integration/blob/master/buildkite/README.md){: .external} is run, using Bazel
binaries built at head, and release binaries. Projects going to be impacted by a
breaking change are notified.

When a release candidate is issued, other Google projects like
[TensorFlow](https://tensorflow.org){: .external} are tested on their complete
test suite using the release candidate binaries. If you have a critical project
using Bazel, we recommend that you establish an automated testing process that
tracks the current release candidate, and report any regressions.



## Rule compatibility
