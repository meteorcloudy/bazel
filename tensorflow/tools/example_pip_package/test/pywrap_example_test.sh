set -eu

source $(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/test-setup.sh \
  || { echo "test-setup.sh not found!" >&2; exit 1; }

function test_build_pywrap_example() {
  bazel build --host_cpu=x64_windows_msvc --cpu=x64_windows_msvc //tensorflow/tools/example_pip_package:pywrap_example  --verbose_failures
  echo $TEST_SRCDIR/io_bazel/bazel-bin/tensorflow/tools/example_pip_package/_pywrap_example.dll
  test -e $TEST_SRCDIR/io_bazel/bazel-bin/tensorflow/tools/example_pip_package/_pywrap_example.dll || fail "failed to build _pywrap_example.dll"
  test -e $TEST_SRCDIR/io_bazel/bazel-bin/tensorflow/tools/example_pip_package/pywrap_example.py || fail "failed to build pywrap_example.py"
}

test_build_pywrap_example
