
set -e

function main() {
  if [ $# -lt 1 ] ; then
    echo "No destination dir provided"
    exit 1
  fi

  DEST=$1
  TMPDIR=$(mktemp -d -t tmp.XXXXXXXXXX)

  echo $(date) : "=== Using tmpdir: ${TMPDIR}"

  if [ ! -d bazel-bin/tensorflow ]; then
    echo "Could not find bazel-bin.  Did you run from the root of the build tree?"
    exit 1
  fi

  if [ -d bazel-bin/tensorflow/tools/example_pip_package/build_example_pip_package.runfiles/io_bazel/tensorflow ]; then
    cp -R \
      bazel-bin/tensorflow/tools/example_pip_package/build_example_pip_package.runfiles/io_bazel/tensorflow \
      ${TMPDIR}
  else
    cp -R \
      bazel-bin/tensorflow/tools/example_pip_package/build_example_pip_package.runfiles/__main__/tensorflow \
      ${TMPDIR}
  fi

  chmod -R 755 ${TMPDIR}/tensorflow

  cp tensorflow/tools/example_pip_package/MANIFEST.in ${TMPDIR}
  cp tensorflow/tools/example_pip_package/README.rst ${TMPDIR}
  cp tensorflow/tools/example_pip_package/LICENSE.txt ${TMPDIR}
  cp tensorflow/tools/example_pip_package/setup.py ${TMPDIR}
  cp tensorflow/tools/example_pip_package/setup.cfg ${TMPDIR}

  pushd ${TMPDIR}
  rm -f MANIFEST
  echo $(date) : "=== Building wheel"
  ${PYTHON_BIN_PATH:-python} setup.py bdist_wheel >/dev/null
  mkdir -p ${DEST}
  cp dist/* ${DEST}
  popd
  rm -rf ${TMPDIR}
  echo $(date) : "=== Output wheel file is in: ${DEST}"
}

main "$@"
