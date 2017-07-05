#include <iostream>

#include "src/tools/launcher/data_parser.h"

using namespace std;

int main(int argv, char* args[]) {
  LaunchDataParser data_parser(args[0]);
  cout << "Data Size: " << data_parser.GetDataSize() <<endl;
  data_parser.GetLaunchInfo();
  return 0;
}
