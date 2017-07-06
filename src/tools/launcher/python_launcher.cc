#include <string>
#include "src/tools/launcher/python_launcher.h"
#include "src/tools/launcher/launcher_util.h"

using namespace std;

void PythonBinaryLauncher::Launch() {
  string python_binary = this->GetLaunchInfoByKey("PYTHON_BIN");
  vector<string> args_vector = this->GetArgs();
  args_vector[0] = GetBinaryPathWithExe(args_vector[0]);
  args_vector[0] = args_vector[0].substr(0, args_vector[0].length() - 4) + ".zip";
  this->LaunchProcess(python_binary, args_vector);
}
