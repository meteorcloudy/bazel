#ifndef BAZEL_SRC_TOOLS_LAUNCHER_LAUNCHER_H_
#define BAZEL_SRC_TOOLS_LAUNCHER_LAUNCHER_H_

#include <string>
#include <unordered_map>
#include <windows.h>

#include "src/tools/launcher/data_parser.h"

using namespace std;

static const int MAX_CMDLINE_LENGTH = 32768;

struct CmdLine {
  char cmdline[MAX_CMDLINE_LENGTH];
};

class BinaryLauncherBase {
 public:
  BinaryLauncherBase(const LaunchInfo* launch_info, int argc, char* args[]);
  string GetLaunchInfoByKey(const string key);
  int GetArgNumber();
  vector<string> GetArgs();
  void LaunchProcess(const string& executable,
                     const vector<string>& args_vector);
  void CreateCommandLine(CmdLine* result, const string& exe,
                              const std::vector<string>& args_vector);
  virtual void Launch() = 0;
 
 private:
  const LaunchInfo* launch_info;
  int argc;
  char** args;
};

#endif // BAZEL_SRC_TOOLS_LAUNCHER_LAUNCHER_H_
