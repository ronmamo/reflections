package com.google.common.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Copyright (C) 2010 RapidPM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created by RapidPM - Team on 18.09.16.
 */
public class Files {
  private Files() {
  }

  /**
   * Writes a character sequence (such as a string) to a file using the given character set.
   *
   * @param from    the character sequence to write
   * @param to      the destination file
   * @param charset the charset used to encode the output stream; see { StandardCharsets} for
   *                helpful predefined constants
   *
   * @throws IOException if an I/O error occurs
   */
  public static void write(CharSequence from, File to, Charset charset) throws IOException {
    if (to.exists()) {
      try (FileOutputStream fileOutputStream = new FileOutputStream(to)) {
        final String string = from.toString();
        fileOutputStream.write(string.getBytes(charset));
        fileOutputStream.flush();
      }
    }
  }


}
