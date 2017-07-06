#ifndef BAZEL_SRC_TOOLS_LAUNCHER_PYTHON_LAUNCHER_H_
#define BAZEL_SRC_TOOLS_LAUNCHER_PYTHON_LAUNCHER_H_

#include <string>

#include "src/tools/launcher/launcher.h"
#include "src/tools/launcher/data_parser.h"

class PythonBinaryLauncher : public BinaryLauncherBase {
 public:
  PythonBinaryLauncher(const LaunchInfo* launch_info, int argc, char* args[])
                          : BinaryLauncherBase(launch_info, argc, args) {};
  void Launch();
};

#endif // BAZEL_SRC_TOOLS_LAUNCHER_PYTHON_LAUNCHER_H_
