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

/**
 * The options to use when detecting urls. This enum is used as a bit mask to be able to set multiple options at once.
 */
public enum UrlDetectorOptions {
  /**
   * Default options, no special checks.
   */
  Default(0),

  /**
   * Matches quotes in the beginning and end of string.
   * If a string starts with a quote, then the ending quote will be eliminated. For example,
   * "http://linkedin.com" will pull out just 'http://linkedin.com' instead of 'http://linkedin.com"'
   */
  QUOTE_MATCH(1), // 00000001

  /**
   * Matches single quotes in the beginning and end of a string.
   */
  SINGLE_QUOTE_MATCH(2), // 00000010

  /**
   * Matches brackets and closes on the second one.
   * Same as quote matching but works for brackets such as (), {}, [].
   */
  BRACKET_MATCH(4), // 000000100

  /**
   * Checks for bracket characters and more importantly quotes to start and end strings.
   */
  JSON(5), //00000101

  /**
   * Checks JSON format or but also looks for a single quote.
   */
  JAVASCRIPT(7), //00000111

  /**
   * Checks for xml characters and uses them as ending characters as well as quotes.
   * This also includes quote_matching.
   */
  XML(9), //00001001

  /**
   * Checks all of the rules besides brackets. This is XML but also can contain javascript.
   */
  HTML(27), //00011011

  /**
   * Checks for single level domains as well. Ex: go/, http://localhost
   */
  ALLOW_SINGLE_LEVEL_DOMAIN(32); //00100000

  /**
   * The numeric value.
   */
  private int _value;

  /**
   * Creates a new Options enum
   * @param value The numeric value of the enum
   */
  UrlDetectorOptions(int value) {
    this._value = value;
  }

  /**
   * Checks if the current options have the specified flag.
   * @param flag The flag to check for.
   * @return True if this flag is active, else false.
   */
  public boolean hasFlag(UrlDetectorOptions flag) {
    return (_value & flag._value) == flag._value;
  }

  /**
   * Gets the numeric value of the enum
   * @return The numeric value of the enum
   */
  public int getValue() {
    return _value;
  }
}
