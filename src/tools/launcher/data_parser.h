#ifndef BAZEL_SRC_TOOLS_LAUNCHER_DATA_PARSER_H_
#define BAZEL_SRC_TOOLS_LAUNCHER_DATA_PARSER_H_

#include <fstream>
#include <string>
#include <unordered_map>

typedef std::unordered_map<std::string, std::string> LaunchInfo;
typedef int64_t DataSize;

class LaunchDataParser {
 public:
  LaunchDataParser(const char* binary_name);
  ~LaunchDataParser();
  void Close();
  void GetLaunchInfo(LaunchInfo* launch_info);
  DataSize GetDataSize();
  void GetLaunchData(char* launch_data,
                     DataSize data_size);

 private:
  std::ifstream * binary_file;
  void ParseLaunchData(LaunchInfo* launch_info,
                       const char* launch_data,
                       DataSize data_size);
};

#endif // BAZEL_SRC_TOOLS_LAUNCHER_DATA_PARSER_H_
