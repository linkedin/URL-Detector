/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.linkedin.urls.detection;

import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;


public class CharUtils {

  /**
   * Checks if character is a valid hex character.
   */
  public static boolean isHex(char a) {
    return (a >= '0' && a <= '9') || (a >= 'a' && a <= 'f') || (a >= 'A' && a <= 'F');
  }

  /**
   * Checks if character is a valid alphabetic character.
   */
  public static boolean isAlpha(char a) {
    return ((a >= 'a' && a <= 'z') || (a >= 'A' && a <= 'Z'));
  }

  /**
   * Checks if character is a valid numeric character.
   */
  public static boolean isNumeric(char a) {
    return a >= '0' && a <= '9';
  }

  /**
   * Checks if character is a valid alphanumeric character.
   */
  public static boolean isAlphaNumeric(char a) {
    return isAlpha(a) || isNumeric(a);
  }

  /**
   * Checks if character is a valid unreserved character. This is defined by the RFC 3986 ABNF
   */
  public static boolean isUnreserved(char a) {
    return isAlphaNumeric(a) || a == '-' || a == '.' || a == '_' || a == '~';
  }

  /**
   * Checks if character is a dot. Heres the doc:
   * http://docs.oracle.com/javase/6/docs/api/java/net/IDN.html#toASCII%28java.lang.String,%20int%29
   */
  public static boolean isDot(char a) {
    return (a == '.' || a == '\u3002' || a == '\uFF0E' || a == '\uFF61');
  }

  public static boolean isWhiteSpace(char a) {
    return (a == '\n' || a == '\t' || a == '\r' || a == ' ');
  }

  /**
   * Splits a string without the use of a regex, which could split either by isDot() or %2e
   * @param input the input string that will be split by dot
   * @return an array of strings that is a partition of the original string split by dot
   */
  public static String[] splitByDot(String input) {
    ArrayList<String> splitList = new ArrayList<String>();
    StringBuilder section = new StringBuilder();
    if (StringUtils.isEmpty(input)) {
      return new String[] { "" };
    }
    InputTextReader reader = new InputTextReader(input);
    while (!reader.eof()) {
      char curr = reader.read();
      if (isDot(curr)) {
        splitList.add(section.toString());
        section.setLength(0);
      } else if (curr == '%' && reader.canReadChars(2) && reader.peek(2).equalsIgnoreCase("2e")) {
        reader.read();
        reader.read(); //advance past the 2e
        splitList.add(section.toString());
        section.setLength(0);
      } else {
        section.append(curr);
      }
    }
    splitList.add(section.toString());
    return splitList.toArray(new String[splitList.size()]);
  }

  private CharUtils() { }
}
