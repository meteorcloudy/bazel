#include <iostream>
#include <fstream>
#include <cstring>

using namespace std;

int main(int argc, char* argv[]) {
  if (argc < 4) {
    printf("Usage: input_file output_file_name string\n");
    return 0;
  }

  ifstream input;
  input.open(argv[1], ios::in | ios::binary);
  if (input.is_open()) {
    ofstream output(argv[2], ios::out | ios::binary);
    output << input.rdbuf();
    input.close();
    output << argv[3];
    char null = '\0';
    output.write(reinterpret_cast<char*>(&null), sizeof(null));
    std::streamsize i = strlen(argv[3]) + sizeof(null);
    output.write(reinterpret_cast<char*>(&i), sizeof(i));
    output.close();
  } else {
    cerr << "Cannot open file: " << argv[1] <<endl;
    cerr << "Error: " << strerror(errno) <<endl;
    return 1;
  }
  return 0;
}