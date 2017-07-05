#include <iostream>
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

DataSize LaunchDataParser::GetDataSize() {
  DataSize data_size;
  binary_file->seekg(0 - sizeof(data_size), binary_file->end);
  binary_file->read(reinterpret_cast<char*>(&data_size), sizeof(data_size));
  return data_size;
}

void LaunchDataParser::GetLaunchData(char* launch_data,
                                     DataSize data_size) {
  binary_file->seekg(0 - data_size - sizeof(data_size), binary_file->end);
  binary_file->read(launch_data, data_size);
}

void LaunchDataParser::ParseLaunchData(LaunchInfo* launch_info,
                                       const char* launch_data,
                                       DataSize data_len) {
  DataSize start, end, colon;
  start = 0;
  while (start < data_len) {
    // Move start to point to the next non-newline character.
    while (launch_data[start] == '\n') {
      start++;
    }
    if (start >= data_len) {
      break;
    }
    // Move end to the next \n or data_len,
    // also find the first colon appears.
    end = start + 1;
    colon = -1;
    while (launch_data[end] != '\n' && end < data_len) {
      if (colon == -1 && launch_data[end] == ':') {
        colon = end;
      }
      end++;
    }
    if (colon == -1) {
      std::cerr << "Cannot find colon in line";
      return;
    }
    std::string key(launch_data + start, colon - start);
    std::string value(launch_data + colon + 1, end - colon - 1);
    launch_info->insert(std::make_pair(key, value));
    start = end;
  }
}

void LaunchDataParser::GetLaunchInfo(LaunchInfo* launch_info) {
  DataSize data_size = GetDataSize();
  char* launch_data = new char[data_size];
  GetLaunchData(launch_data, data_size);
  DataSize data_len = data_size - 1;
  ParseLaunchData(launch_info, launch_data, data_len);
  delete launch_data;
}
