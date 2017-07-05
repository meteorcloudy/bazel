#include <fstream>
#include <cstdio>
#include "src/tools/launcher/data_parser.h"

LaunchDataParser::LaunchDataParser(const char* binary_name) {
  binary_file = new std::ifstream(binary_name, std::ios::binary | std::ios::in);
}

LaunchDataParser::~LaunchDataParser() {
  binary_file->close();
  delete binary_file;
}

std::streamsize LaunchDataParser::GetDataSize() {
  std::streamsize data_size;
  binary_file->seekg(0 - sizeof(data_size), binary_file->end);
  binary_file->read(reinterpret_cast<char*>(&data_size), sizeof(data_size));
  return data_size;
}

LaunchDataMap* LaunchDataParser::ParseLaunchData(const char* launch_data,
                                                 std::streamsize data_size) {
  return new LaunchDataMap({{"key", "value"}});
}

LaunchDataMap* LaunchDataParser::GetLaunchInfo() {
  std::streamsize data_size = GetDataSize();
  char* launch_data = new char[data_size];
  binary_file->seekg(0 - data_size - sizeof(data_size), binary_file->end);
  binary_file->read(launch_data, data_size);
  printf("DataStr: %s\n", launch_data);
  return ParseLaunchData(launch_data, data_size);
}