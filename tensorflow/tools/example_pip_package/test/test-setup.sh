function fail() {
  echo $1
  exit 1
}

function setup() {
  export JAVA_HOME="$(ls -d C:/Program\ Files/Java/jdk* | sort | tail -n 1)"
  export TMPDIR=c:/temp
  export BAZEL_SH=c:/tools/msys64/usr/bin/bash.exe
  export PYTHON_LIB=C:/python_27_amd64/files/libs/python27.lib
  export PYTHON_INCLUDE=C:/python_27_amd64/files/include
  export TMP=C:/tools/msys64/tmp
  export TEMP=C:/tools/msys64/tmp
}

setup
