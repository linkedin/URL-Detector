/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.linkedin.urls.url;

import org.apache.commons.lang3.StringUtils;

class PathNormalizer {

  /**
   * Normalizes the path by doing the following:
   * remove special spaces, decoding hex encoded characters,
   * gets rid of extra dots and slashes, and re-encodes it once
   */
  protected String normalizePath(String path) {

    if (StringUtils.isEmpty(path)) {
      return path;
    }
    path = UrlUtil.removeSpecialSpaces(path);
    path = UrlUtil.decode(path);
    path = sanitizeDotsAndSlashes(path);
    return UrlUtil.encode(path);
  }

  /**
   * Replaces "/../" and "/./" with "/" recursively.
   */
  private static String sanitizeDotsAndSlashes(String path) {
    StringBuilder stringBuilder = new StringBuilder(path);
    int index = 0;
    while (index < stringBuilder.length() - 2) {
      if ( stringBuilder.charAt(index) == '/' && stringBuilder.charAt(index + 1) == '.') {
        if (index + 3 < stringBuilder.length() && stringBuilder.charAt(index + 2) == '.' &&
            stringBuilder.charAt(index + 3) == '/') {
          stringBuilder.delete(index, index + 3); // "/../" -> "/"
          index--; // backtrack so we can detect if this / is part of another replacement
        } else if (stringBuilder.charAt(index + 2) == '/') {
          stringBuilder.delete(index, index + 2); // "/./" -> "/"
          index--; // backtrack so we can detect if this / is part of another replacement
        }
      }
      index++;
    }
    return stringBuilder.toString();
  }
}
