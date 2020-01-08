#!/bin/bash
set -e
set -x

cd ${KOKORO_ARTIFACTS_DIR}/github/bazel

# Get release name
git fetch --force origin refs/notes/*:refs/notes/*
release_name=$(source scripts/release/common.sh; get_full_release_name)
release_name=test
echo "release_name = \"${release_name}\""

# # Switch to Xcode 10.2.1 so that the Bazel release we build is still
# # compatible with macOS High Sierra.
# /usr/bin/sudo /usr/bin/xcode-select --switch /Applications/Xcode10.2.1.app
# /usr/bin/sudo /usr/bin/xcodebuild -runFirstLaunch

# Get Bazelisk
mkdir -p /tmp/tool
BAZELISK="/tmp/tool/bazelisk"
wget https://github.com/bazelbuild/bazelisk/releases/download/v1.2.1/bazelisk-darwin-amd64 -O "${BAZELISK}"
chmod +x "${BAZELISK}"

"${BAZELISK}" build //src:bazel
mkdir output
cp bazel-bin/src/bazel output/bazel

output/bazel build \
    --define IPHONE_SDK=1 \
    -c opt \
    --stamp \
    --embed_label "${release_name}" \
    --workspace_status_command=scripts/ci/build_status_command.sh \
    src/bazel \
    scripts/packages/with-jdk/install.sh

mkdir artifacts
cp "bazel-bin/src/bazel" "artifacts/bazel-${release_name}-darwin-x86_64"
cp "bazel-bin/scripts/packages/with-jdk/install.sh" "artifacts/bazel-${release_name}-installer-darwin-x86_64.sh"
