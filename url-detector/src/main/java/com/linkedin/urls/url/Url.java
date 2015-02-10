package com.linkedin.urls.url;

import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *
 * Creating own Uri class since java.net.Uri would throw parsing exceptions
 * for URL's considered ok by browsers.
 *
 * Also to avoid further conflict, this does stuff that the normal Uri object doesn't do:
 * - Converts http://google.com/a/b/.//./../c to http://google.com/a/c
 * - Decodes repeatedly so that http://host/%2525252525252525 becomes http://host/%25 while normal decoders
 *     would make it http://host/%25252525252525 (one less 25)
 * - Removes tabs and new lines: http://www.google.com/foo\tbar\rbaz\n2 becomes "http://www.google.com/foobarbaz2"
 * - Converts IP addresses: http://3279880203/blah becomes http://195.127.0.11/blah
 * - Strips fragments (anything after #)
 *
 */
public class Url {

  private static final String DEFAULT_SCHEME = "http";
  private static final Map<String, Integer> SCHEME_PORT_MAP;
  static {
    SCHEME_PORT_MAP = new HashMap<String, Integer>();
    SCHEME_PORT_MAP.put("http", 80);
    SCHEME_PORT_MAP.put("https", 443);
    SCHEME_PORT_MAP.put("ftp", 21);
  }
  private UrlMarker _urlMarker;
  private String _scheme;
  private String _username;
  private String _password;
  private String _host;
  private int _port = 0;
  private String _path;
  private String _query;
  private String _fragment;
  private String _originalUrl;

  protected Url(UrlMarker urlMarker) {
    _urlMarker = urlMarker;
    _originalUrl = urlMarker.getOriginalUrl();
  }

  /**
   * Returns a url given a single url.
   */
  public static Url create(String url)
      throws MalformedURLException {
    List<Url> urls = new UrlDetector(url, UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN).detect();
    if (urls.size() == 1) {
      return urls.get(0);
    } else if (urls.size() == 0) {
      throw new MalformedURLException("We couldn't find any urls in string: " + url);
    } else {
      throw new MalformedURLException("We found more than one url in string: " + url);
    }
  }

  @Override
  public String toString() {
    return this.getFullUrl();
  }

  /**
   * Note that this includes the fragment
   * @return Formats the url to: [scheme]://[username]:[password]@[host]:[port]/[path]?[query]#[fragment]
   */
  public String getFullUrl() {
    return getFullUrlWithoutFragment() + StringUtils.defaultString(getFragment());
  }

  /**
   *
   * @return Formats the url to: [scheme]://[username]:[password]@[host]:[port]/[path]?[query]
   */
  public String getFullUrlWithoutFragment() {
    StringBuilder url = new StringBuilder();
    if (getScheme() != null) {
      url.append(getScheme());
      url.append(":");
    }
    url.append("//");

    if (getUsername() != null) {
      url.append(getUsername());
      if (getPassword() != null) {
        url.append(":");
        url.append(getPassword());
      }
      url.append("@");
    }

    url.append(getHost());
    if (getPort() > 0 && getPort() != SCHEME_PORT_MAP.get(getScheme())) {
      url.append(":");
      url.append(getPort());
    }

    url.append(getPath());
    url.append(StringUtils.defaultString(getQuery()));

    return url.toString();
  }

  public String getScheme() {
    if (_scheme == null) {
      if (exists(UrlPart.SCHEME)) {
        _scheme = getPart(UrlPart.SCHEME);
        _scheme = _scheme.substring(0, _scheme.indexOf(":"));
      } else if (!_originalUrl.startsWith("//")) {
        _scheme = DEFAULT_SCHEME;
      }
    }
    return _scheme;
  }

  public String getUsername() {
    if (_username == null) {
      populateUsernamePassword();
    }
    return _username;
  }

  public String getPassword() {
    if (_password == null) {
      populateUsernamePassword();
    }
    return _password;
  }

  public String getHost() {
    if (_host == null) {
      _host = getPart(UrlPart.HOST);
      if (exists(UrlPart.PORT)) {
        _host = _host.substring(0, _host.length() - 1);
      }
    }
    return _host;
  }

  /**
   * port = 0 means it hasn't been set yet. port = -1 means there is no port
   */
  public int getPort() {
    if (_port == 0) {
      String portString = getPart(UrlPart.PORT);
      if (portString != null && !portString.isEmpty()) {
        _port = Integer.parseInt(portString);
      } else if (SCHEME_PORT_MAP.containsKey(getScheme())) {
        _port = SCHEME_PORT_MAP.get(getScheme());
      } else {
        _port = -1;
      }
    }
    return _port;
  }

  public String getPath() {
    if (_path == null) {
      _path = exists(UrlPart.PATH) ? getPart(UrlPart.PATH) : "/";
    }
    return _path;
  }

  public String getQuery() {
    if (_query == null) {
      _query = getPart(UrlPart.QUERY);
    }
    return _query;
  }

  public String getFragment() {
    if (_fragment == null) {
      _fragment = getPart(UrlPart.FRAGMENT);
    }
    return _fragment;
  }

  /**
   * Always returns null for non normalized urls.
   */
  public byte[] getHostBytes() {
    return null;
  }

  public String getOriginalUrl() {
    return _originalUrl;
  }

  private void populateUsernamePassword() {
    if (exists(UrlPart.USERNAME_PASSWORD)) {
      String usernamePassword = getPart(UrlPart.USERNAME_PASSWORD);
      String[] usernamePasswordParts = usernamePassword.substring(0, usernamePassword.length() - 1).split(":");
      if (usernamePasswordParts.length == 1) {
        _username = usernamePasswordParts[0];
      } else if (usernamePasswordParts.length == 2) {
        _username = usernamePasswordParts[0];
        _password = usernamePasswordParts[1];
      }
    }
  }

  /**
   * @param urlPart The url part we are checking for existence
   * @return Returns true if the part exists.
   */
  private boolean exists(UrlPart urlPart) {
    return urlPart != null && _urlMarker.indexOf(urlPart) >= 0;
  }

  /**
   * For example, in http://yahoo.com/lala/, nextExistingPart(UrlPart.HOST) would return UrlPart.PATH
   * @param urlPart The current url part
   * @return Returns the next part; if there is no existing next part, it returns null
   */
  private UrlPart nextExistingPart(UrlPart urlPart) {
    UrlPart nextPart = urlPart.getNextPart();
    if (exists(nextPart)) {
      return nextPart;
    } else if (nextPart == null) {
      return null;
    } else {
      return nextExistingPart(nextPart);
    }
  }

  /**
   * @param part The part that we want. Ex: host, path
   */
  private String getPart(UrlPart part) {
    if (!exists(part)) {
      return null;
    }

    UrlPart nextPart = nextExistingPart(part);
    if (nextPart == null) {
      return _originalUrl.substring(_urlMarker.indexOf(part));
    }
    return _originalUrl.substring(_urlMarker.indexOf(part), _urlMarker.indexOf(nextPart));
  }

  protected void setRawPath(String path) {
    _path = path;
  }

  protected void setRawHost(String host) {
    _host = host;
  }

  protected String getRawPath() {
    return _path;
  }

  protected String getRawHost() {
    return _host;
  }

  protected UrlMarker getUrlMarker() {
    return _urlMarker;
  }
}
