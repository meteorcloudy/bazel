#ifndef BAZEL_SRC_TOOLS_LAUNCHER_JAVA_LAUNCHER_H_
#define BAZEL_SRC_TOOLS_LAUNCHER_JAVA_LAUNCHER_H_

#include <string>

#include "src/tools/launcher/launcher.h"
#include "src/tools/launcher/data_parser.h"

class JavaBinaryLauncher : public BinaryLauncherBase {
 public:
  JavaBinaryLauncher(const LaunchInfo* launch_info, int argc, char* args[])
                          : BinaryLauncherBase(launch_info, argc, args) {};
  void Launch();
};

#endif // BAZEL_SRC_TOOLS_LAUNCHER_JAVA_LAUNCHER_H_
