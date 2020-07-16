#!/bin/bash

set -x

# Revert files before patching
git checkout WORKSPACE
git checkout src/main/java/com/google/devtools/build/lib/runtime/commands/license/BUILD
git checkout src/java_tools/buildjar

# Apply patch
patch -p1 < debian/patches/debian.patch
patch -p1 < debian/patches/remove_javac.patch

# Delete derived directory in case it exists
rm -rf derived
rm -f ./grpc-java-plugin

# Ensure packages build with no Internet access
export http_proxy=127.0.0.1:9
export https_proxy=127.0.0.1:9

#    --override_repository=bazel_skylib=$PWD/debian/missing-sources/mock_repos/bazel_skylib \
export EXTRA_BAZEL_ARGS="\
    --define=distribution=debian \
    --distdir=debian/distdir \
    --host_javabase=@local_jdk//:jdk \
    --override_repository=com_google_protobuf=$PWD/tools/distributions/debian/protobuf \
    --override_repository=remote_java_tools_linux=$PWD/debian/missing-sources/mock_repos/remote_java_tools_linux \
    --override_repository=io_bazel_skydoc=$PWD/debian/missing-sources/mock_repos/bazel_skydoc \
    --override_repository=rules_pkg=$PWD/debian/missing-sources/mock_repos/rules_pkg \
    --override_repository=rules_cc=$PWD/debian/missing-sources/mock_repos/rules_cc \
    --override_repository=rules_java=$PWD/debian/missing-sources/mock_repos/rules_java \
    --override_repository=rules_proto=$PWD/debian/missing-sources/mock_repos/rules_proto \
    --override_repository=platforms=$PWD/debian/missing-sources/mock_repos/platforms \
    "

# Only for testing, to make sure we don't use any repository cache. Can be removed.
export EXTRA_BAZEL_ARGS="${EXTRA_BAZEL_ARGS} --repository_cache="

export PROTOC=/usr/bin/protoc

export GRPC_JAVA_PLUGIN=/usr/bin/grpc_java_plugin

export VERBOSE=yes

./compile.sh

# Revert files after build
git checkout WORKSPACE
git checkout src/main/java/com/google/devtools/build/lib/runtime/commands/license/BUILD
git checkout src/java_tools/buildjar

# Remove files after build
rm -rf derived
rm -f ./grpc-java-plugin
