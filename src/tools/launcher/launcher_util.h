#ifndef BAZEL_SRC_TOOLS_LAUNCHER_LAUNCHER_UTIL_H_
#define BAZEL_SRC_TOOLS_LAUNCHER_LAUNCHER_UTIL_H_

#include <string>

using namespace std;

string GetBinaryPathWithExe(const string binary_name);

string GetBinaryPathWithExe(const char* binary_name);

#endif // BAZEL_SRC_TOOLS_LAUNCHER_LAUNCHER_UTIL_H_
