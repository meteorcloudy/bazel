#ifndef BAZEL_SRC_TOOLS_LAUNCHER_BASH_LAUNCHER_H_
#define BAZEL_SRC_TOOLS_LAUNCHER_BASH_LAUNCHER_H_

#include <string>

#include "src/tools/launcher/launcher.h"
#include "src/tools/launcher/data_parser.h"

class BashBinaryLauncher : public BinaryLauncherBase {
 public:
  BashBinaryLauncher(const LaunchInfo* launch_info, int argc, char* args[])
                     : BinaryLauncherBase(launch_info, argc, args) {};
  void Launch();
};

#endif // BAZEL_SRC_TOOLS_LAUNCHER_BASH_LAUNCHER_H_
