Project: /_project.yaml Book: /_book.yaml

# Rule Compatibility

{% include "_buttons.html" %}

Bazel Starlark rules can break compatibility with Bazel LTS releases in the
following two scenarios:

1. The rule will break compatibility with future LTS releases because a feature
   it depends on is removed from Bazel@HEAD.
1. The rule will break compatibility with the current or older LTS releases
   because a feature it depends on is only available in newer Bazel LTS
   releases.

Meanwhile, the rule itself can ship incompatible changes for their users as
well. When combined with breaking changes in Bazel, upgrading the rule version
and Bazel version can often be a source of frustration for Bazel users.

## Manageable migration process

While it's obviously not feasible to guarantee compatibility between every
version of Bazel and every version of the rule, our aim is to ensure that the
migration process remains manageable for Bazel users. We define a manageable
migration process to be a process where **users are not forced to upgrade the
rule’s major version and Bazel's major version simultaneously**, thereby
allowing users to handle incompatible changes from one source at a time.

For example, with the following compatibility matrix:

* Migrating from rules_foo 1.x + Bazel 4.x to rules_foo 2.x + Bazel 5.x is not
  considered easily manageable, as the users need to upgrade the major version
  of rules_foo and Bazel at the same time.
* Migrating from rules_foo 2.x + Bazel 5.x to rules_foo 3.x + Bazel 6.x is
  considered manageable, as the users can first upgrade rules_foo from 2.x to
  3.x without changing the major Bazel version, then upgrade Bazel from 5.x to
  6.x.

| | rules_foo 1.x | rules_foo 2.x | rules_foo 3.x | HEAD |
| --- | --- | --- | --- | --- |
| Bazel 4.x | ✅ | ❌ | ❌ | ❌ |
| Bazel 5.x | ❌ | ✅ | ✅ | ❌ |
| Bazel 6.x | ❌ | ❌ | ✅ | ✅ |
| HEAD | ❌ | ❌ | ❌ | ✅ |

❌: No version of the major rule version is compatible with the Bazel LTS
release.

✅: At least one version of the rule is compatible with the latest version of the
Bazel LTS release.

## Best practices

As Bazel rules authors, you can ensure a manageable migration process is
possible for users by following these best practices:

1. The rule should follow Semantic Versioning, all versions of the same major
   version are backwards compatible.
1. The rule at HEAD should be compatible with the latest Bazel LTS release.
1. The rule at HEAD should be compatible with Bazel at HEAD. Add your project to
   [Bazel downstream
   pipeline](https://buildkite.com/bazel/bazel-at-head-plus-downstream), the
   Bazel team will file issues to your project if upcoming incompatible flag
   flips will break your project.
1. The latest major version of the rule must be compatible with the latest Bazel
   LTS release.
1. The initial release of a major rule version N should be compatible with the
   most recent Bazel LTS release M that is supported by the previous major rule
   version N-1. But a later minor release of the major rule version N can drop
   the compatibility with Bazel LTS release M.

Achieving 2. and 3. is the most important task since it allows achieving 4. and
5. much easier.

The Bazel team can do the following to make it easier for rule authors to keep
compatibility with both Bazel at HEAD and the latest Bazel LTS release:

* Backport features, especially new build API features, from HEAD to the current
  Active LTS release.
* Allow rule authors to detect build API changes so that the rule can branch out
  for different Bazel versions
* Use Bazel feature repo

In general, we want to make it possible for rules to migrate for Bazel
incompatible changes and depend on new Bazel features at HEAD without dropping
compatibility with the latest Bazel LTS release.
