#ifndef BAZEL_SRC_TOOLS_LAUNCHER_LAUNCHER_H_
#define BAZEL_SRC_TOOLS_LAUNCHER_LAUNCHER_H_

#include <string>
#include <unordered_map>

#include "src/tools/launcher/data_parser.h"

class BinaryLauncherBase {
 public:
  BinaryLauncherBase(int argc, const char* argv[]);
  virtual void Launch() = 0;
};

#endif // BAZEL_SRC_TOOLS_LAUNCHER_LAUNCHER_H_