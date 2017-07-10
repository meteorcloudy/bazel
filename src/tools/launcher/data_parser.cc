#include <iostream>
#include <fstream>
#include <cstdio>
#include "src/tools/launcher/data_parser.h"
#include "src/main/cpp/util/errors.h"

using namespace std;
using blaze_util::PrintWarning;
using blaze_util::die;

LaunchDataParser::LaunchDataParser(const char* binary_name) {
  binary_file = new ifstream(binary_name, ios::binary | ios::in);
}

LaunchDataParser::~LaunchDataParser() {
  if (binary_file != NULL) {
    this->Close();
  }
}

void LaunchDataParser::Close() {
  binary_file->close();
  delete binary_file;
  binary_file = NULL;
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
                                       DataSize data_size) {
  DataSize start, end, equal;
  start = 0;
  while (start < data_size) {
    // Move start to point to the next non-null character.
    while (launch_data[start] == '\0' && start < data_size) {
      start++;
    }
    if (start >= data_size) {
      break;
    }
    // Move end to the next null character or end of the string,
    // also find the first equal symbol appears.
    end = start;
    equal = -1;
    while (launch_data[end] != '\0' && end < data_size) {
      if (equal == -1 && launch_data[end] == '=') {
        equal = end;
      }
      end++;
    }
    if (equal == -1) {
      PrintWarning("Cannot find equal symbol in line: %s\n",
                   string(launch_data + start, end - start).c_str());
    } else if (start == equal) {
      PrintWarning("Key is empty string in line: %s\n",
                   string(launch_data + start, end - start).c_str());
    } else {
      string key(launch_data + start, equal - start);
      string value(launch_data + equal + 1, end - equal - 1);
      launch_info->insert(make_pair(key, value));
    }
    start = end;
  }
}

void LaunchDataParser::GetLaunchInfo(LaunchInfo* launch_info) {
  DataSize data_size = GetDataSize();
  char* launch_data = new char[data_size];
  GetLaunchData(launch_data, data_size);
  if (data_size == 0) {
    Close();
    die(1, "No data appended, cannot launch anything!");
  }
  ParseLaunchData(launch_info, launch_data, data_size);
  delete launch_data;
}
