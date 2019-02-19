package com.google.devtools.build.lib.vfs;

import com.google.devtools.build.lib.vfs.DigestHashFunction.DefaultHashFunctionNotSetException;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.windows.WindowsFileSystem;

import java.io.File;
import java.io.IOException;

/**
 * Created by pcloudy on 2/15/2019.
 */
public class Delete {
  public static void main(String[] args) throws DefaultHashFunctionNotSetException, IOException {
      Path directory = new WindowsFileSystem(DigestHashFunction.DEFAULT_HASH_FOR_TESTS).getPath(new File(args[0]).getAbsolutePath());

      System.out.println("Deleting " + args[0]);
      FileSystemUtils.deleteTree(directory);
  }
}
