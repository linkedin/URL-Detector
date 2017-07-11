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

import java.net.MalformedURLException;


/**
 * Returns a normalized version of a url instead of the original url string.
 */
public class NormalizedUrl extends Url {

  private boolean _isPopulated = false;
  private byte[] _hostBytes;

  public NormalizedUrl(UrlMarker urlMarker) {
    super(urlMarker);
  }

  /**
   * Returns a normalized url given a single url.
   */
  public static NormalizedUrl create(String url) throws MalformedURLException {
    return Url.create(url).normalize();
  }

  @Override
  public String getHost() {
    if (getRawHost() == null) {
      populateHostAndHostBytes();
    }
    return getRawHost();
  }

  @Override
  public String getPath() {
    if (getRawPath() == null) {
      setRawPath(new PathNormalizer().normalizePath(super.getPath()));
    }
    return getRawPath();
  }

  /**
   * Returns the byte representation of the ip address. If the host is not an ip address, it returns null.
   */
  @Override
  public byte[] getHostBytes() {
    if (_hostBytes == null) {
      populateHostAndHostBytes();
    }
    return _hostBytes;
  }

  private void populateHostAndHostBytes() {
    if (!_isPopulated) {
      HostNormalizer hostNormalizer = new HostNormalizer(super.getHost());
      setRawHost(hostNormalizer.getNormalizedHost());
      _hostBytes = hostNormalizer.getBytes();
      _isPopulated = true;
    }
  }
}
