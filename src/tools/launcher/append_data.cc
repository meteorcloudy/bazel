#include <iostream>
#include <fstream>

using namespace std;

int main(int argc, char* argv[]) {
  ofstream myfile;
  myfile.open(argv[1], ios::app | ios::out | ios::binary);
  if (myfile.is_open()) {
    myfile << argv[2];
    char null = '\0';
    myfile.write(reinterpret_cast<char*>(&null), sizeof(null));
    std::streamsize i = strlen(argv[2]) + 1;
    myfile.write(reinterpret_cast<char*>(&i), sizeof(i));
    myfile.close();
  } else {
    cout << "Cannot open file" <<endl;
  }
  return 0;
}