#include <iostream>

#include "src/tools/launcher/data_parser.h"
#include "src/tools/launcher/launcher.h"
#include "src/tools/launcher/python_launcher.h"
#include "src/tools/launcher/java_launcher.h"
#include "src/tools/launcher/bash_launcher.h"
#include "src/tools/launcher/launcher_util.h"

using namespace std;

int main(int argc, char* args[]) {
  LaunchDataParser data_parser(GetBinaryPathWithExe(args[0]).c_str());
  // DataSize data_size = data_parser.GetDataSize();
  // cout << "Data Size: " << data_size <<endl;

  // char* launch_data = new char[data_size];
  // data_parser.GetLaunchData(launch_data, data_size);
  // printf("\nData:\n%s\n", launch_data);
  // printf("Binary %s\n", GetBinaryPathWithExe(args[0]).c_str());
  LaunchInfo launch_info;
  data_parser.GetLaunchInfo(&launch_info);
  data_parser.Close();
  PythonBinaryLauncher python_launcher(&launch_info, argc, args);

  // cout << "LaunchInfo:" <<endl;
  // for (auto& x: launch_info) {
  //   cout << "^" << x.first << "=" << x.second << "$" << endl;
  // }

  // cout << "InfoByKey:" << endl;
  // cout << python_launcher.GetLaunchInfoByKey("language") << endl;
  // cout << python_launcher.GetLaunchInfoByKey("foo") << endl;

  python_launcher.Launch();
  return 0;
}
