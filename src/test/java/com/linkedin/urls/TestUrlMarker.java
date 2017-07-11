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

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


@Test
public class TestUrlMarker {

  @DataProvider
  private Object[][] getUrlMarkers() {
    return new Object[][] {
        {"hello@hello.com", "http", "hello", "", "hello.com", 80, "/", "", "",
            new int[]{-1, 0, 6, -1, -1, -1, -1}},
        {"http://hello@hello.com", "http", "hello", "", "hello.com", 80, "/", "", "",
            new int[]{0, 7, 13, -1, -1, -1, -1}},
        {"hello@hello.com", "http", "hello", "", "hello.com", 80, "/", "", "",
            new int[]{-1, 0, 6, -1, -1, -1, -1}},
        {"https://user@google.com/h?hello=w#abc", "https", "user", "", "google.com", 443, "/h", "?hello=w", "#abc",
            new int[]{0, 8, 13, -1, 23, 25, 33}},
        {"www.booopp.com:20#fa", "http", "", "", "www.booopp.com", 20, "/", "", "#fa",
            new int[]{-1, -1, 0, 15, -1, -1, 17}},
        {"www.yahooo.com:20?fff#aa", "http", "", "", "www.yahooo.com", 20, "/", "?fff", "#aa",
            new int[]{-1, -1, 0, 15, -1, 17, 21}},
        {"www.google.com#fa", "http", "", "", "www.google.com", 80, "/", "", "#fa",
            new int[]{-1, -1, 0, -1, -1, -1, 14}},
        {"www.google.com?3fd#fa", "http", "", "", "www.google.com", 80, "/", "?3fd", "#fa",
            new int[]{-1, -1, 0, -1, -1, 14, 18}},
        {"//www.google.com/", "", "", "", "www.google.com", -1, "/", "", "",
            new int[]{-1, -1, 2, -1, 16, -1, -1}},
        {"http://www.google.com/", "http", "", "", "www.google.com", 80, "/", "", "",
            new int[]{0, -1, 7, -1, 21, -1, -1}},
        {"ftp://whosdere:me@google.com/", "ftp", "whosdere", "me", "google.com", 21, "/", "", "",
            new int[]{0, 6, 18, -1, 28, -1, -1}},
        {"ono:doope@fb.net:9090/dhdh", "http", "ono", "doope", "fb.net", 9090, "/dhdh", "", "",
            new int[]{-1, 0, 10, 17, 21, -1, -1}},
        {"ono:a@fboo.com:90/dhdh/@1234", "http", "ono", "a", "fboo.com", 90, "/dhdh/@1234", "", "",
            new int[]{-1, 0, 6, 15, 17, -1, -1}},
        {"fbeoo.net:990/dhdeh/@1234", "http", "", "", "fbeoo.net", 990, "/dhdeh/@1234", "", "",
            new int[]{-1, -1, 0, 10, 13, -1, -1}},
        {"fbeoo:@boop.com/dhdeh/@1234?aj=r", "http", "fbeoo", "", "boop.com", 80, "/dhdeh/@1234", "?aj=r", "",
            new int[]{-1, 0, 7, -1, 15, 27, -1}},
        {"bloop:@noooo.com/doop/@1234", "http", "bloop", "", "noooo.com", 80, "/doop/@1234", "", "",
            new int[]{-1, 0, 7, -1, 16, -1, -1}},
        {"bah.com/lala/@1234/@dfd@df?@dsf#ono", "http", "", "", "bah.com", 80, "/lala/@1234/@dfd@df", "?@dsf",
            "#ono", new int[]{-1, -1, 0, -1, 7, 26, 31}},
        {"https://dewd:dood@www.google.com:20/?why=is&this=test#?@Sdsf", "https", "dewd", "dood",
            "www.google.com", 20, "/", "?why=is&this=test", "#?@Sdsf", new int[]{0, 8, 18, 33, 35, 36, 53}}
    };
  }

  @Test(dataProvider = "getUrlMarkers")
  public void testUrlMarker(String testString, String scheme, String username, String password, String host, int port,
      String path, String query, String fragment, int[] indices) {
    UrlMarker urlMarker = new UrlMarker();
    urlMarker.setOriginalUrl(testString);
    urlMarker.setIndices(indices);
    Url url = urlMarker.createUrl();
    Assert.assertEquals(url.getHost(), host, "host, " + testString);
    Assert.assertEquals(url.getPath(), path, "path, " + testString);
    Assert.assertEquals(url.getScheme(), scheme, "scheme, " + testString);
    Assert.assertEquals(url.getUsername(), username, "username, " + testString);
    Assert.assertEquals(url.getPassword(), password, "password, " + testString);
    Assert.assertEquals(url.getPort(), port, "port, " + testString);
    Assert.assertEquals(url.getQuery(), query, "query, " + testString);
    Assert.assertEquals(url.getFragment(), fragment, "fragment, " + testString);
  }
}
