#include <iostream>
#include <sstream>
#include <vector>
#include <string>
#include <windows.h>

#include "src/tools/launcher/launcher.h"
#include "src/tools/launcher/data_parser.h"
#include "src/main/cpp/util/errors.h"

using namespace std;
using blaze_util::die;
using blaze_util::pdie;

BinaryLauncherBase::BinaryLauncherBase(const LaunchInfo* launch_info, int argc, char* args[]) {
  this->launch_info = launch_info;
  this->argc = argc;
  this->args = args;
}

string BinaryLauncherBase::GetLaunchInfoByKey(const string key) {
  LaunchInfo::const_iterator item = launch_info->find(key);
  if (item == launch_info->end()) {
    cerr << "Cannot find key \"" << key << "\" from launch data" << endl;
    return "";
  }
  return item->second;
}

int BinaryLauncherBase::GetArgNumber() {
  return this->argc;
}

char** BinaryLauncherBase::GetArgs() {
  return this->args;
}

void BinaryLauncherBase::CreateCommandLine(CmdLine* result, const string& exe,
                              const vector<string>& args_vector) {
  ostringstream cmdline;
  bool first = true;
  for (const auto& s : args_vector) {
    if (first) {
      first = false;
      // Skip first argument, instead use quoted executable name.
      cmdline << '\"' << exe << '\"';
      continue;
    } else {
      cmdline << ' ';
    }

    bool has_space = s.find(" ") != string::npos;

    if (has_space) {
      cmdline << '\"';
    }

    string::const_iterator it = s.begin();
    while (it != s.end()) {
      char ch = *it++;
      switch (ch) {
        case '"':
          // Escape double quotes
          cmdline << "\\\"";
          break;

        case '\\':
          if (it == s.end()) {
            // Backslashes at the end of the string are quoted if we add quotes
            cmdline << (has_space ? "\\\\" : "\\");
          } else {
            // Backslashes everywhere else are quoted if they are followed by a
            // quote or a backslash
            cmdline << (*it == '"' || *it == '\\' ? "\\\\" : "\\");
          }
          break;

        default:
          cmdline << ch;
      }
    }

    if (has_space) {
      cmdline << '\"';
    }
  }

  string cmdline_str = cmdline.str();
  if (cmdline_str.size() >= MAX_CMDLINE_LENGTH) {
    pdie(1, "Command line too long: %s",
         cmdline_str.c_str());

  }

  // Copy command line into a mutable buffer.
  // CreateProcess is allowed to mutate its command line argument.
  strncpy(result->cmdline, cmdline_str.c_str(), MAX_CMDLINE_LENGTH - 1);
  result->cmdline[MAX_CMDLINE_LENGTH - 1] = 0;
}

void BinaryLauncherBase::LaunchProcess(const string commandline) {
  // CmdLine cmdline;
  // CreateCommandLine(&cmdline, exe, args_vector);
  PROCESS_INFORMATION processInfo = {0};
  STARTUPINFOA startupInfo = {0};
  char cmdline[MAX_CMDLINE_LENGTH] = "python.exe";
  BOOL ok = CreateProcessA(
      /* lpApplicationName */ NULL,
      /* lpCommandLine */ cmdline,
      /* lpProcessAttributes */ NULL,
      /* lpThreadAttributes */ NULL,
      /* bInheritHandles */ TRUE,
      /* dwCreationFlags */ 0,
      /* lpEnvironment */ NULL,
      /* lpCurrentDirectory */ NULL,
      /* lpStartupInfo */ &startupInfo,
      /* lpProcessInformation */ &processInfo);
  WaitForSingleObject(processInfo.hProcess, INFINITE);
  CloseHandle(processInfo.hProcess);
  CloseHandle(processInfo.hThread);
}
