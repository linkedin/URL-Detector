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


public class TestUrlUtil {

  @DataProvider
  private Object[][] getDecodeStrings() {
    return new Object[][] {
        {"%%32%35", "%"},
        {"%2%35", "%"},
        {"%%325", "%"},
        {"%%32%3525", "%"},
        {"%%%32%35", "%%"},
        {"%%32%35%", "%%"},
        {"%%32%3532", "2"},
        {"%%%32%3532%%32%3535", "%"},
        {"/%25%32%35", "/%"},
        {"/%2%2%2", "/%2%2%2"},
        {"/%2%%335", "/%"},
        {"/%25%32%35%25%32%35", "/%%"},
        {"/%2525252525252525", "/%"},
        {"/asdf%25%32%35asd", "/asdf%asd"},
        {"/%%%25%32%35asd%%", "/%%%asd%%"},
        {"/%2E%73%65%63%75%72%65/%77%77%77%2E%65%62%61%79%2E%63%6F%6D/", "/.secure/www.ebay.com/"},
        {"/uploads/%20%20%20%20/", "/uploads/    /"},
        {"/%257Ea%2521b%2540c%2523d%2524e%25f%255E00%252611%252A22%252833%252944_55%252B",
            "/~a!b@c#d$e%f^00&11*22(33)44_55+"}
    };
  }

  @Test(dataProvider = "getDecodeStrings")
  public void testDecode(String input, String expectedDecodedString) {
    Assert.assertEquals(UrlUtil.decode(input), expectedDecodedString);
  }

  @DataProvider
  private Object[][] getEncodeStrings() {
    return new Object[][] {
        {"/lnjbk%", "/lnjbk%25"},
        {"/%2%2%2", "/%252%252%252"}
    };
  }

  @Test(dataProvider = "getEncodeStrings")
  public void testEncode(String input, String expectedEncodedString) {
    Assert.assertEquals(UrlUtil.encode(input), expectedEncodedString);
  }

  @DataProvider
  private Object[][] getExtraDotsStrings() {
    return new Object[][] {
        {".s..ales.....com", "s.ales.com"},
        {"33r.nEt...", "33r.nEt"},
        {"[::-34:50]...", "[::-34:50]"},
        {"asdf.[-34::192.168.34.-3]...", "asdf.[-34::192.168.34.-3]"},
        {".", ""}
    };
  }

  @Test(dataProvider = "getExtraDotsStrings")
  public void testExtraDotsHosts(String input, String expected) {
    Assert.assertEquals(UrlUtil.removeExtraDots(input), expected);
  }
}
