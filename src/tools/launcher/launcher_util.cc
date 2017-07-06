#include "src/tools/launcher/launcher_util.h"

string GetBinaryPathWithExe(const string binary_name) {
  if (binary_name.substr(binary_name.length() - 4) == ".exe") {
    return binary_name;
  }
  return binary_name + ".exe";
}

string GetBinaryPathWithExe(const char* binary_name) {
  return GetBinaryPathWithExe(string(binary_name));
}
