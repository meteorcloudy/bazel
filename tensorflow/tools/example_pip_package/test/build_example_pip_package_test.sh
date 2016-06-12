set -eu

source $(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/test-setup.sh \
  || { echo "test-setup.sh not found!" >&2; exit 1; }

function test_pip_package() {
  bazel build --host_cpu=x64_windows_msvc --cpu=x64_windows_msvc //tensorflow/tools/example_pip_package:build_example_pip_package  --verbose_failures
  test -e $TEST_SRCDIR/io_bazel/bazel-bin/tensorflow/tools/example_pip_package/build_example_pip_package || fail "failed to build build_example_pip_package"

  mkdir -p $TEST_SRCDIR/wheel_output
  $TEST_SRCDIR/io_bazel/bazel-bin/tensorflow/tools/example_pip_package/build_example_pip_package $TEST_SRCDIR/wheel_output
  (ls $TEST_SRCDIR/wheel_output | grep -q "\.whl$") || fail "failed to build python wheel file"
}

test_pip_package
