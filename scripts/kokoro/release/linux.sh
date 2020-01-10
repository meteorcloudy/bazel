#!/bin/bash
set -e
set -x

##### DEBUG - begin ######
echo $RELEASE_BRANCH

cd ${KOKORO_ARTIFACTS_DIR}/github/bazel
git fetch origin $RELEASE_BRANCH
git checkout $RELEASE_BRANCH

echo "Done"
exit 0
##### DEBUG - end   ######

cd ${KOKORO_ARTIFACTS_DIR}/github/bazel

# Get release name
git fetch --force origin refs/notes/*:refs/notes/*
release_name=$(source scripts/release/common.sh; get_full_release_name)
release_name=test
echo "release_name = \"${release_name}\""

# Get Bazelisk
mkdir -p /tmp/tool
BAZELISK="/tmp/tool/bazelisk"
wget https://github.com/bazelbuild/bazelisk/releases/download/v1.2.1/bazelisk-linux-amd64 -O "${BAZELISK}"
chmod +x "${BAZELISK}"

"${BAZELISK}" build --sandbox_tmpfs_path=/tmp //src:bazel
mkdir output
cp bazel-bin/src/bazel output/bazel

output/bazel build \
    -c opt \
    --stamp \
    --sandbox_tmpfs_path=/tmp \
    --embed_label "${release_name}" \
    --workspace_status_command=scripts/ci/build_status_command.sh \
    src/bazel \
    scripts/packages/with-jdk/install.sh \
    scripts/packages/debian/bazel-debian.deb \
    scripts/packages/debian/bazel.dsc \
    scripts/packages/debian/bazel.tar.gz \
    bazel-distfile.zip

mkdir artifacts
cp "bazel-bin/src/bazel" "artifacts/bazel-${release_name}-linux-x86_64"
cp "bazel-bin/scripts/packages/with-jdk/install.sh" "artifacts/bazel-${release_name}-installer-linux-x86_64.sh"
cp "bazel-bin/scripts/packages/debian/bazel-debian.deb" "artifacts/bazel_${release_name}-linux-x86_64.deb"
cp "bazel-bin/scripts/packages/debian/bazel.dsc" "artifacts/bazel_${release_name}.dsc"
cp "bazel-bin/scripts/packages/debian/bazel.tar.gz" "artifacts/bazel_${release_name}.tar.gz"
cp "bazel-bin/bazel-distfile.zip" "artifacts/bazel-${release_name}-dist.zip"
