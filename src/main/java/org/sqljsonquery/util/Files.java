package org.sqljsonquery.util;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;


public final class Files
{
   public static BufferedWriter newFileOrStdoutWriter(Optional<Path> outputFilePath) throws IOException
   {
      return outputFilePath.isPresent() ?
         java.nio.file.Files.newBufferedWriter(outputFilePath.get())
         : new BufferedWriter(new OutputStreamWriter(System.out));
   }

   public static OutputStream outputStream(String pathOrDash) throws IOException
   {
      if ( "-".equals(pathOrDash) )
         return System.out;
      else
         return new FileOutputStream(pathOrDash);
   }


   private Files() {}
}

