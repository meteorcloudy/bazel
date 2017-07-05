#include <iostream>

#include "src/tools/launcher/data_parser.h"

using namespace std;

int main(int argv, char* args[]) {
  LaunchDataParser data_parser(args[0]);
  DataSize data_size = data_parser.GetDataSize();
  cout << "Data Size: " << data_size <<endl;

  char* launch_data = new char[data_size];
  data_parser.GetLaunchData(launch_data, data_size);
  printf("\nData:\n%s\n", launch_data);

  LaunchInfo launch_info;
  data_parser.GetLaunchInfo(&launch_info);
  printf("LaunchInfo:\n");
  for (auto& x: launch_info) {
    cout << x.first << "=" << x.second << endl;
  }
  return 0;
}
