#!/bin/bash

set -x

export PROTOC=/usr/bin/protoc
export GRPC_JAVA_PLUGIN=/usr/bin/grpc_java_plugin
export OUTPUT_DIR=derived

mkdir -p "$OUTPUT_DIR"/src

# Parse third_party/googleapis/BUILD.bazel to find the proto files we need to compile from googleapis
GOOGLE_API_PROTOS="$(grep -o '".*\.proto"' third_party/googleapis/BUILD.bazel | sed 's/"//g' | sed 's|^|third_party/googleapis/|g')"
PROTO_FILES=$(find third_party/remoteapis ${GOOGLE_API_PROTOS} third_party/pprof src/main/protobuf src/main/java/com/google/devtools/build/lib/buildeventstream/proto src/main/java/com/google/devtools/build/skyframe src/main/java/com/google/devtools/build/lib/skyframe/proto src/main/java/com/google/devtools/build/lib/bazel/debug src/main/java/com/google/devtools/build/lib/starlarkdebug/proto -name "*.proto")

for f in $PROTO_FILES ; do
    run "${PROTOC}" \
        -I. \
        -Isrc/main/protobuf/ \
        -Isrc/main/java/com/google/devtools/build/lib/buildeventstream/proto/ \
        -Isrc/main/java/com/google/devtools/build/lib/skyframe/proto/ \
        -Isrc/main/java/com/google/devtools/build/skyframe/ \
        -Isrc/main/java/com/google/devtools/build/lib/bazel/debug/ \
        -Isrc/main/java/com/google/devtools/build/lib/starlarkdebug/proto/ \
        -Ithird_party/remoteapis/ \
        -Ithird_party/googleapis/ \
        -Ithird_party/pprof/ \
        --java_out=${OUTPUT_DIR}/src \
        --plugin=protoc-gen-grpc="${GRPC_JAVA_PLUGIN-}" \
        --grpc_out=${OUTPUT_DIR}/src "$f"
done
