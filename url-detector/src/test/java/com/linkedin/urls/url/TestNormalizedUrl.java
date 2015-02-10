package com.linkedin.urls.url;

import java.net.MalformedURLException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
public class TestNormalizedUrl {

  @DataProvider
  private Object[][] getHostPathUrls() {
    return new Object[][] {
        {"http://www.google.com/", "www.google.com", "/"},
        {"teeee.com", "teeee.com", "/"},
        {"[::1]", "[0:0:0:0:0:0:0:1]", "/"},
        {"yahoo.com/@1234", "yahoo.com", "/@1234"},
        {"http://[::0xfe.07.23.33]/%25%32%35", "[0:0:0:0:0:0:fe07:1721]", "/%25"},
        {"http://host.com/%2525252525252525", "host.com", "/%25"},
        {"http://[::1]/asdf%25%32%35asd", "[0:0:0:0:0:0:0:1]", "/asdf%25asd"},
        {"http://[::10]/%%%25%32%35asd%%", "[0:0:0:0:0:0:0:10]", "/%25%25%25asd%25%25"},
        {"343324381/", "20.118.182.221", "/"}
    };
  }

  @Test(dataProvider = "getHostPathUrls")
  public void testUsernamePasswordUrls(String testString, String host, String path)
      throws MalformedURLException {
    Url url = NormalizedUrl.createNormalized(testString);
    Assert.assertNotNull(url);
    Assert.assertEquals(url.getHost(), host);
    Assert.assertEquals(url.getPath(), path);
  }
}
