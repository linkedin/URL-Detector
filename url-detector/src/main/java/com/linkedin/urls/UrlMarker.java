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

public class UrlMarker {

  private String _originalUrl;
  private int _schemeIndex = -1;
  private int _usernamePasswordIndex = -1;
  private int _hostIndex = -1;
  private int _portIndex = -1;
  private int _pathIndex = -1;
  private int _queryIndex = -1;
  private int _fragmentIndex = -1;

  public UrlMarker() {
  }

  public Url createUrl() {
    return new Url(this);
  }

  public void setOriginalUrl(String originalUrl) {
    _originalUrl = originalUrl;
  }

  public String getOriginalUrl() {
    return _originalUrl;
  }

  public void setIndex(UrlPart urlPart, int index) {
    switch (urlPart) {
      case SCHEME:
        _schemeIndex = index;
        break;
      case USERNAME_PASSWORD:
        _usernamePasswordIndex = index;
        break;
      case HOST:
        _hostIndex = index;
        break;
      case PORT:
        _portIndex = index;
        break;
      case PATH:
        _pathIndex = index;
        break;
      case QUERY:
        _queryIndex = index;
        break;
      case FRAGMENT:
        _fragmentIndex = index;
        break;
      default:
        break;
    }
  }

  /**
   * @param urlPart The part you want the index of
   * @return Returns the index of the part
   */
  public int indexOf(UrlPart urlPart) {
    switch (urlPart) {
      case SCHEME:
        return _schemeIndex;
      case USERNAME_PASSWORD:
        return _usernamePasswordIndex;
      case HOST:
        return _hostIndex;
      case PORT:
        return _portIndex;
      case PATH:
        return _pathIndex;
      case QUERY:
        return _queryIndex;
      case FRAGMENT:
        return _fragmentIndex;
      default:
        return -1;
    }
  }

  public void unsetIndex(UrlPart urlPart) {
    setIndex(urlPart, -1);
  }

  /**
   * This is used in TestUrlMarker to set indices more easily.
   * @param indices array of indices of size 7
   */
  protected UrlMarker setIndices(int[] indices) {
    if (indices == null || indices.length != 7) {
      throw new IllegalArgumentException("Malformed index array.");
    }
    setIndex(UrlPart.SCHEME, indices[0]);
    setIndex(UrlPart.USERNAME_PASSWORD, indices[1]);
    setIndex(UrlPart.HOST, indices[2]);
    setIndex(UrlPart.PORT, indices[3]);
    setIndex(UrlPart.PATH, indices[4]);
    setIndex(UrlPart.QUERY, indices[5]);
    setIndex(UrlPart.FRAGMENT, indices[6]);
    return this;
  }
}
