/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.linkedin.urls;

import com.linkedin.urls.detection.CharUtils;
import com.linkedin.urls.detection.InputTextReader;
import java.util.Stack;


class UrlUtil {

  /**
   * Decodes the url by iteratively removing hex characters with backtracking.
   * For example: %2525252525252525 becomes %
   */
  protected static String decode(String url) {
    StringBuilder stringBuilder = new StringBuilder(url);
    Stack<Integer> nonDecodedPercentIndices = new Stack<Integer>();
    int i = 0;
    while (i < stringBuilder.length() - 2) {
      char curr = stringBuilder.charAt(i);
      if (curr == '%') {
        if (CharUtils.isHex(stringBuilder.charAt(i + 1)) && CharUtils.isHex(stringBuilder.charAt(i + 2))) {
          char decodedChar =
              String.format("%s", (char) Short.parseShort(stringBuilder.substring(i + 1, i + 3), 16)).charAt(0);
          stringBuilder.delete(i, i + 3); //delete the % and two hex digits
          stringBuilder.insert(i, decodedChar); //add decoded character

          if (decodedChar == '%') {
            i--; //backtrack one character to check for another decoding with this %.
          } else if (!nonDecodedPercentIndices.isEmpty() && CharUtils.isHex(decodedChar)
              && CharUtils.isHex(stringBuilder.charAt(i - 1)) && i - nonDecodedPercentIndices.peek() == 2) {
            //Go back to the last non-decoded percent sign if it's decodable.
            //We only need to go back if it's of form %[HEX][HEX]
            i = nonDecodedPercentIndices.pop() - 1; //backtrack to the % sign.
          } else if (!nonDecodedPercentIndices.isEmpty() && i == stringBuilder.length() - 2) {
            //special case to handle %[HEX][Unknown][end of string]
            i = nonDecodedPercentIndices.pop() - 1; //backtrack to the % sign.
          }
        } else {
          nonDecodedPercentIndices.add(i);
        }
      }
      i++;
    }
    return stringBuilder.toString();
  }

  /**
   * Removes TAB (0x09), CR (0x0d), and LF (0x0a) from the URL
   * @param urlPart The part of the url we are canonicalizing
   */
  protected static String removeSpecialSpaces(String urlPart) {
    StringBuilder stringBuilder = new StringBuilder(urlPart);
    for (int i = 0; i < stringBuilder.length(); i++) {
      char curr = stringBuilder.charAt(i);
      if (CharUtils.isWhiteSpace(curr)) {
        stringBuilder.deleteCharAt(i);
      }
    }
    return stringBuilder.toString();
  }

  /**
   * Replaces all special characters in the url with hex strings.
   */
  protected static String encode(String url) {
    StringBuilder encoder = new StringBuilder();
    for (char chr : url.toCharArray()) {
      byte chrByte = (byte) chr;
      if ((chrByte <= 32 || chrByte >= 127 || chr == '#' || chr == '%')) {
        encoder.append(String.format("%%%02X", chrByte));
      } else {
        encoder.append(chr);
      }
    }
    return encoder.toString();
  }

  /**
   * Removes all leading and trailing dots; replaces consecutive dots with a single dot
   * Ex: ".lalal.....com." -> "lalal.com"
   */
  protected static String removeExtraDots(String host) {
    StringBuilder stringBuilder = new StringBuilder();
    InputTextReader reader = new InputTextReader(host);
    while (!reader.eof()) {
      char curr = reader.read();
      stringBuilder.append(curr);
      if (curr == '.') {
        char possibleDot = curr;
        while (possibleDot == '.' && !reader.eof()) {
          possibleDot = reader.read();
        }
        if (possibleDot != '.') {
          stringBuilder.append(possibleDot);
        }
      }
    }

    if (stringBuilder.length() > 0 && stringBuilder.charAt(stringBuilder.length() - 1) == '.') {
      stringBuilder.deleteCharAt(stringBuilder.length() - 1);
    }
    if (stringBuilder.length() > 0 && stringBuilder.charAt(0) == '.') {
      stringBuilder.deleteCharAt(0);
    }

    return stringBuilder.toString();
  }

  private UrlUtil() { }
}
