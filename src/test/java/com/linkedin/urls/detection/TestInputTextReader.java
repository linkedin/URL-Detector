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

import org.testng.Assert;
import org.testng.annotations.Test;


public class TestInputTextReader {
  private static final String CONTENT = "HELLO WORLD";

  @Test
  public void testSimpleRead() {
    InputTextReader reader = new InputTextReader(CONTENT);
    for (int i = 0; i < CONTENT.length(); i++) {
      Assert.assertEquals(reader.read(), CONTENT.charAt(i));
    }
  }

  @Test
  public void testEOF() {
    InputTextReader reader = new InputTextReader(CONTENT);
    for (int i = 0; i < CONTENT.length() - 1; i++) {
      reader.read();
    }

    Assert.assertFalse(reader.eof());
    reader.read();
    Assert.assertTrue(reader.eof());
  }

  @Test
  public void testGoBack() {
    InputTextReader reader = new InputTextReader(CONTENT);
    Assert.assertEquals(reader.read(), CONTENT.charAt(0));
    reader.goBack();
    Assert.assertEquals(reader.read(), CONTENT.charAt(0));
    Assert.assertEquals(reader.read(), CONTENT.charAt(1));
    Assert.assertEquals(reader.read(), CONTENT.charAt(2));
    reader.goBack();
    reader.goBack();
    Assert.assertEquals(reader.read(), CONTENT.charAt(1));
    Assert.assertEquals(reader.read(), CONTENT.charAt(2));
  }

  @Test
  public void testSeek() {
    InputTextReader reader = new InputTextReader(CONTENT);
    reader.seek(4);
    Assert.assertEquals(reader.read(), CONTENT.charAt(4));

    reader.seek(1);
    Assert.assertEquals(reader.read(), CONTENT.charAt(1));
  }

  @Test(expectedExceptions = NegativeArraySizeException.class, expectedExceptionsMessageRegExp = ".*" + CONTENT + ".*")
  public void testEndlessLoopDetection() {
    InputTextReader reader = new InputTextReader(CONTENT);
    for (int i = 0; i < InputTextReader.MAX_BACKTRACK_MULTIPLIER + 1; i++) {
      reader.seek(CONTENT.length());
      reader.seek(0);
    }
  }
}
