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
 * Class used to read a text input character by character. This also gives the ability to backtrack.
 */
public class InputTextReader {

  /**
   * The content to read.
   */
  private final char[] _content;

  /**
   * The current position in the content we are looking at.
   */
  private int _index = 0;

  /**
   * Creates a new instance of the InputTextReader using the content to read.
   * @param content The content to read.
   */
  public InputTextReader(String content) {
    _content = content.toCharArray();
  }

  /**
   * Reads a single char from the content stream and increments the index.
   * @return The next available character.
   */
  public char read() {
    char chr = _content[_index++];
    return CharUtils.isWhiteSpace(chr) ? ' ' : chr;
  }

  /**
   * Peeks at the next number of chars and returns as a string without incrementing the current index.
   * @param numberChars The number of chars to peek.
   */
  public String peek(int numberChars) {
    return new String(_content, _index, numberChars);
  }

  /**
   * Gets the character in the array offset by the current index.
   * @param offset The number of characters to offset.
   * @return The character at the location of the index plus the provided offset.
   */
  public char peekChar(int offset) {
    if (!canReadChars(offset)) {
      throw new ArrayIndexOutOfBoundsException();
    }

    return _content[_index + offset];
  }

  /**
   * Returns true if the reader has more the specified number of chars.
   * @param numberChars The number of chars to see if we can read.
   * @return True if we can read this number of chars, else false.
   */
  public boolean canReadChars(int numberChars) {
    return _content.length >= _index + numberChars;
  }

  /**
   * Checks if the current stream is at the end.
   * @return True if the stream is at the end and no more can be read.
   */
  public boolean eof() {
    return _content.length <= _index;
  }

  /**
   * Gets the current position in the stream.
   * @return The index to the current position.
   */
  public int getPosition() {
    return _index;
  }

  /**
   * Moves the index to the specified position.
   * @param position The position to set the index to.
   */
  public void seek(int position) {
    _index = position;
  }

  /**
   * Goes back a single character.
   */
  public void goBack() {
    _index--;
  }
}
