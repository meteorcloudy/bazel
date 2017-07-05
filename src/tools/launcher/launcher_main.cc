#include <iostream>

#include "src/tools/launcher/data_parser.h"
#include "src/tools/launcher/launcher.h"
#include "src/tools/launcher/python_launcher.h"

using namespace std;

int main(int argc, char* args[]) {
  LaunchDataParser data_parser(args[0]);
  // DataSize data_size = data_parser.GetDataSize();
  // cout << "Data Size: " << data_size <<endl;

  // char* launch_data = new char[data_size];
  // data_parser.GetLaunchData(launch_data, data_size);
  // printf("\nData:\n%s\n", launch_data);

  LaunchInfo launch_info;
  data_parser.GetLaunchInfo(&launch_info);
  data_parser.Close();
  PythonBinaryLauncherBase python_launcher(&launch_info, argc, args);

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
