#include <iostream>
#include <fstream>
#include <cstring>

using namespace std;

int main(int argc, char* argv[]) {
  if (argc < 3) {
    printf("Usage: <input_file> <output_file> <key_1>=<value_1> ... <key_n>=<value_n>\n");
    return 0;
  }

  ifstream input;
  input.open(argv[1], ios::in | ios::binary);
  if (input.is_open()) {
    ofstream output(argv[2], ios::out | ios::binary);
    output << input.rdbuf();
    input.close();
    std::streamsize len = 0;
    for (int i = 3; i < argc; i++) {
      output << argv[i];
      output.put('\0');
      len += strlen(argv[i]) + sizeof('\0');
    }
    output.write(reinterpret_cast<char*>(&len), sizeof(len));
    output.close();
  } else {
    cerr << "Cannot open file: " << argv[1] <<endl;
    cerr << "Error: " << strerror(errno) <<endl;
    return 1;
  }
  return 0;
}
