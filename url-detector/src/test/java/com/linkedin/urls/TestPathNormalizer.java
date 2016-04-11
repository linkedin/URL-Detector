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


public class TestPathNormalizer {

  @DataProvider
  private Object[][] getPaths() {
    return new Object[][] {
        {"/%25%32%35", "/%25"},
        {"/%2%2%2", "/%252%252%252"},
        {"/%2%%335", "/%25"},
        {"/%25%32%35%25%32%35", "/%25%25"},
        {"/%2525252525252525", "/%25"},
        {"/asdf%25%32%35asd", "/asdf%25asd"},
        {"/%%%25%32%35asd%%", "/%25%25%25asd%25%25"},
        {"/%2E%73%65%63%75%72%65/%77%77%77%2E%65%62%61%79%2E%63%6F%6D/", "/.secure/www.ebay.com/"},
        {"/uploads/%20%20%20%20/.verify/.eBaysecure=updateuserdataxplimnbqmn-xplmvalidateinfoswqpcmlx=hgplmcx/",
            "/uploads/%20%20%20%20/.verify/.eBaysecure=updateuserdataxplimnbqmn-xplmvalidateinfoswqpcmlx=hgplmcx/"},
        {"/%257Ea%2521b%2540c%2523d%2524e%25f%255E00%252611%252A22%252833%252944_55%252B",
            "/~a!b@c%23d$e%25f^00&11*22(33)44_55+"},
        {"/lala/.././../..../", "/..../"},
        {"//asdfasdf/awef/sadf/sdf//", "/asdfasdf/awef/sadf/sdf/"},
        {"/", "/"},
        {"/a/../b/c", "/b/c"},
        {"/blah/..", "/"},
        {"../", "../"},
        {"/asdf/.", "/asdf/"},
        {"/a/b/./././././../c/d", "/a/c/d"},
        {"/a/b//////.///././././../c/d", "/a/c/d"},
        {"//../a/c/..///sdf", "/a/sdf"},
        {"/../asdf", "/asdf"},
        {"/../asdf/", "/asdf/"},
        {"/a/b/..c", "/a/b/..c"},
        {"/a/b/.././", "/a/"},
        {"/a/b/./", "/a/b/"},
        {"/a/b/../..", "/"},
        {"/a/b/../../../../../../", "/"},
        {"/a/b/../../../../../..", "/"},
        {"/a/b/../../../../../../c/d", "/c/d"},
        {"/a/b/../../../../../../c/d/", "/c/d/"},
        {"/a/b/../.", "/a/"},
        {"/a/b/..", "/a/"},
        {"/1.html", "/1.html"},
        {"/1/2.html?param=1", "/1/2.html?param=1"},
        {"/a./b.", "/a./b."},
        {"/a./b./", "/a./b./"}
    };
  }

  @Test(dataProvider = "getPaths")
  public void testPaths(String path, String expectedPath) {
    PathNormalizer pathNormalizer = new PathNormalizer();

    Assert.assertEquals(pathNormalizer.normalizePath(path), expectedPath);
  }
}
