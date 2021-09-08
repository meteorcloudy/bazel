workspace(name = "io_bazel")


# Protobuf expects an //external:python_headers label which would contain the
# Python headers if fast Python protos is enabled. Since we are not using fast
# Python protos, bind python_headers to a dummy target.
bind(
    name = "python_headers",
    actual = "//:dummy",
)

# Protobuf code generation for GRPC requires three external labels:
# //external:grpc-java_plugin
# //external:grpc-jar
# //external:guava
bind(
    name = "grpc-java-plugin",
    actual = "//third_party/grpc:grpc-java-plugin",
)

bind(
    name = "grpc-jar",
    actual = "//third_party/grpc:grpc-jar",
)

bind(
    name = "guava",
    actual = "//third_party:guava",
)

local_repository(
    name = "googleapis",
    path = "./third_party/googleapis/",
)

local_repository(
    name = "remoteapis",
    path = "./third_party/remoteapis/",
)

bind(
    name = "cares",
    actual = "//third_party:cares",
)

bind(
    name = "protobuf_headers",
    actual = "//third_party:protobuf_headers",
)

bind(
    name = "upb_lib",
    actual = "//third_party:upb_lib",
)

bind(
    name = "protobuf_clib",
    actual = "//third_party:protobuf_clib",
)

bind(
    name = "madler_zlib",
    actual = "//third_party:madler_zlib",
)
