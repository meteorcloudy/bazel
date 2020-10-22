#!/bin/bash

set -x

# Revert files before patching
git checkout -f

# Apply patch
patch -p1 < debian/patches/WORKSPACE.patch
patch -p1 < debian/patches/remove_license_deps.patch
patch -p1 < debian/patches/remove_grpc_api.patch
patch -p1 < debian/patches/remove_javac.patch
patch -p1 < debian/patches/replace_grpc_java_plugin.patch
rm -r mock_repos
patch -p1 < debian/patches/mock_repos.patch

# Delete derived directory in case it exists
rm -rf derived

# Ensure packages build with no Internet access
export http_proxy=127.0.0.1:9
export https_proxy=127.0.0.1:9

export DEFAULT_ARGS="\
    --spawn_strategy=standalone \
    --nojava_header_compilation \
    --strategy=Javac=worker --worker_quit_after_build --ignore_unsupported_sandboxing \
    --compilation_mode=opt \
    --java_toolchain=//src/java_tools/buildjar:bootstrap_toolchain \
    --host_java_toolchain=//src/java_tools/buildjar:bootstrap_toolchain \
    --action_env=PATH \
    --host_platform=@local_config_platform//:host \
    --platforms=@local_config_platform//:host \
    "
export EXTRA_BAZEL_ARGS="\
    --define=distribution=debian \
    --host_javabase=@local_jdk//:jdk \
    --override_repository=com_google_protobuf=$PWD/tools/distributions/debian/protobuf \
    --override_repository=remote_java_tools_linux=$PWD/mock_repos/remote_java_tools_linux \
    --override_repository=bazel_skylib=$PWD/mock_repos/bazel_skylib \
    --override_repository=io_bazel_skydoc=$PWD/mock_repos/bazel_skydoc \
    --override_repository=rules_pkg=$PWD/mock_repos/rules_pkg \
    --override_repository=rules_cc=$PWD/mock_repos/rules_cc \
    --override_repository=rules_java=$PWD/mock_repos/rules_java \
    --override_repository=rules_proto=$PWD/mock_repos/rules_proto \
    --override_repository=platforms=$PWD/mock_repos/platforms \
    "

# Only for testing, to make sure we don't use any repository cache. Can be removed.
export EXTRA_BAZEL_ARGS="${EXTRA_BAZEL_ARGS} --repository_cache="


bazel build $DEFAULT_ARGS $EXTRA_BAZEL_ARGS  //src:bazel_nojdk

# Revert files after build
git checkout -f

# Remove files after build
rm -rf derived
