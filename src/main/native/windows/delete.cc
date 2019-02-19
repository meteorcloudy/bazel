#include <memory>
#include <sstream>
#include <string>

#include "src/main/native/windows/file.h"
#include "src/main/native/windows/util.h"
#include "src/main/cpp/util/path_platform.h"


std::wstring s2ws(const std::string& str)
{
    int size_needed = MultiByteToWideChar(CP_UTF8, 0, &str[0], (int)str.size(), NULL, 0);
    std::wstring wstrTo( size_needed, 0 );
    MultiByteToWideChar(CP_UTF8, 0, &str[0], (int)str.size(), &wstrTo[0], size_needed);
    return wstrTo;
}

int main(int argc, char** argv) {
  std::wstring wpath = s2ws(argv[1]);
  std::wstring error;

  std::string e;
  blaze_util::AsAbsoluteWindowsPath(s2ws(argv[1]), &wpath, &e);


  wprintf(L"deleting %s\n", wpath.c_str());
  int result = bazel::windows::DeletePath(wpath, &error);
  wprintf(L"%s\n", error.c_str());
  if (result == 1) {
    printf("Error deleting file\n");
  } else if (result == 2) {
    printf("File doesn't exist\n");
  } else if (result == 3) {
    printf("Directory Not empty\n");
  } else if (result == 4) {
    printf("Access Denied\n");
  }
  return result;
}