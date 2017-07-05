#ifndef BAZEL_SRC_TOOLS_LAUNCHER_DATA_PARSER_H_
#define BAZEL_SRC_TOOLS_LAUNCHER_DATA_PARSER_H_

#include <fstream>
#include <string>
#include <unordered_map>

typedef std::unordered_map<std::string, std::string> LaunchDataMap;

class LaunchDataParser {
 public:
  LaunchDataParser(const char* binary_name);
  ~LaunchDataParser();
  LaunchDataMap* GetLaunchInfo();
  std::streamsize GetDataSize();

 private:
  std::ifstream * binary_file;
  LaunchDataMap* ParseLaunchData(const char* launch_data, std::streamsize data_size);
};

#endif // BAZEL_SRC_TOOLS_LAUNCHER_DATA_PARSER_H_