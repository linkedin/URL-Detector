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
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


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
  public void testUsernamePasswordUrls(String testString, String host, String path) throws MalformedURLException {
    Url url = NormalizedUrl.create(testString);
    Assert.assertNotNull(url);
    Assert.assertEquals(url.getHost(), host);
    Assert.assertEquals(url.getPath(), path);
  }

  /**
   * https://developers.google.com/safe-browsing/developers_guide_v3#Canonicalization
   * @return
   */
  @DataProvider
  private Object[][] getFullUrls() {
    return new Object[][] {
        {"http://host/%25%32%35", "http://host/%25"},
        {"http://host/%25%32%35%25%32%35", "http://host/%25%25"},
        {"http://host/%2525252525252525", "http://host/%25"},
        {"http://host/asdf%25%32%35asd", "http://host/asdf%25asd"},
        {"http://host/%%%25%32%35asd%%", "http://host/%25%25%25asd%25%25"},
        {"http://www.google.com/", "http://www.google.com/"},
        {"http://%31%36%38%2e%31%38%38%2e%39%39%2e%32%36/%2E%73%65%63%75%72%65/%77%77%77%2E%65%62%61%79%2E%63%6F%6D/",
            "http://168.188.99.26/.secure/www.ebay.com/"},
        {"http://195.127.0.11/uploads/%20%20%20%20/.verify/.eBaysecure=updateuserdataxplimnbqmn-xplmvalidateinfoswqpcmlx=hgplmcx/",
            "http://195.127.0.11/uploads/%20%20%20%20/.verify/.eBaysecure=updateuserdataxplimnbqmn-xplmvalidateinfoswqpcmlx=hgplmcx/"},
        {"http://host%23.com/%257Ea%2521b%2540c%2523d%2524e%25f%255E00%252611%252A22%252833%252944_55%252B",
            "http://host%23.com/~a!b@c%23d$e%25f^00&11*22(33)44_55+"},
        {"http://3279880203/blah", "http://195.127.0.11/blah"},
        {"http://www.google.com/blah/..", "http://www.google.com/"},
        {"www.google.com/", "http://www.google.com/"},
        {"www.google.com", "http://www.google.com/"},
        {"http://www.evil.com/blah#frag", "http://www.evil.com/blah"},
        {"http://www.GOOgle.com/", "http://www.google.com/"},
        {"http://www.google.com/foo\tbar\rbaz\n2", "http://www.google.com/foobarbaz2"},
        {"http://www.google.com/q?", "http://www.google.com/q?"},
        {"http://www.google.com/q?r?", "http://www.google.com/q?r?"},
        {"http://www.google.com/q?r?s", "http://www.google.com/q?r?s"},
        {"http://evil.com/foo#bar#baz", "http://evil.com/foo"},
        {"http://evil.com/foo;", "http://evil.com/foo;"},
        {"http://evil.com/foo?bar;", "http://evil.com/foo?bar;"},
        {"http://\\x01\\x80.com/", "http://%01%80.com/"},
        {"http://notrailingslash.com", "http://notrailingslash.com/"},
        {"http://www.gotaport.com:1234/", "http://www.gotaport.com:1234/"},
        {"  http://www.google.com/  ", "http://www.google.com/"},
        {"http:// leadingspace.com/", "http://%20leadingspace.com/"},
        {"http://%20leadingspace.com/", "http://%20leadingspace.com/"},
        {"%20leadingspace.com/", "http://%20leadingspace.com/"},
        {"https://www.securesite.com/", "https://www.securesite.com/"},
        {"http://host.com/ab%23cd", "http://host.com/ab%23cd"},
        {"http://host.com//twoslashes?more//slashes", "http://host.com/twoslashes?more//slashes"},
        {"http://go.co/a/b/../c", "http://go.co/a/c"}
    };
  }

  @Test(dataProvider = "getFullUrls")
  public void testFullUrls(String host, String expectedHost) throws MalformedURLException {
    NormalizedUrl normalizedUrl = NormalizedUrl.create(host);
    Assert.assertEquals(normalizedUrl.getFullUrlWithoutFragment(), expectedHost);
  }
}
