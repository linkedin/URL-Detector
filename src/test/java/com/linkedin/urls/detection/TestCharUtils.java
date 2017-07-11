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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestCharUtils {

  @Test
  public void testCharUtilsIsHex() {
    char[] arr = { 'a', 'A', '0', '9' };
    for (char a : arr) {
      Assert.assertTrue(CharUtils.isHex(a));
    }

    char[] arr2 = { '~', ';', 'Z', 'g' };
    for (char a : arr2) {
      Assert.assertFalse(CharUtils.isHex(a));
    }
  }

  @Test
  public void testCharUtilsIsNumeric() {
    char[] arr = { '0', '4', '6', '9' };
    for (char a : arr) {
      Assert.assertTrue(CharUtils.isNumeric(a));
    }

    char[] arr2 = { 'a', '~', 'A', 0 };
    for (char a : arr2) {
      Assert.assertFalse(CharUtils.isNumeric(a));
    }
  }

  @Test
  public void testCharUtilsIsAlpha() {
    char[] arr = { 'a', 'Z', 'f', 'X' };
    for (char a : arr) {
      Assert.assertTrue(CharUtils.isAlpha(a));
    }

    char[] arr2 = { '0', '9', '[', '~' };
    for (char a : arr2) {
      Assert.assertFalse(CharUtils.isAlpha(a));
    }
  }

  @Test
  public void testCharUtilsIsAlphaNumeric() {
    char[] arr = { 'a', 'G', '3', '9' };
    for (char a : arr) {
      Assert.assertTrue(CharUtils.isAlphaNumeric(a));
    }

    char[] arr2 = { '~', '-', '_', '\n' };
    for (char a : arr2) {
      Assert.assertFalse(CharUtils.isAlphaNumeric(a));
    }
  }

  @Test
  public void testCharUtilsIsUnreserved() {
    char[] arr = { '-', '.', 'a', '9', 'Z', '_', 'f' };
    for (char a : arr) {
      Assert.assertTrue(CharUtils.isUnreserved(a));
    }

    char[] arr2 = { ' ', '!', '(', '\n' };
    for (char a : arr2) {
      Assert.assertFalse(CharUtils.isUnreserved(a));
    }
  }

  @DataProvider
  private Object[][] getSplitStrings() {
    return new Object[][] {
        {"192.168.1.1"},
        {".."},
        {"192%2e168%2e1%2e1"},
        {"asdf"},
        {"192.39%2e1%2E1"},
        {"as\uFF61awe.a3r23.lkajsf0ijr...."},
        {"%2e%2easdf"},
        {"sdoijf%2e"},
        {"ksjdfh.asdfkj.we%2"},
        {"0xc0%2e0x00%2e0x02%2e0xeb"},
        {""}
    };
  }

  @Test(dataProvider = "getSplitStrings")
  public void testSplitByDot(String stringToSplit) {
    Assert
        .assertEquals(CharUtils.splitByDot(stringToSplit), stringToSplit.split("[\\.\u3002\uFF0E\uFF61]|%2e|%2E", -1));
  }

}
