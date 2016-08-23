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
 * The domain name reader reads input from a InputTextReader and validates if the content being read is a valid domain name.
 * After a domain name is read, the returning status is what to do next. If the domain is valid but a specific character is found,
 * the next state will be to read another part for the rest of the url. For example, if a "?" is found at the end and the
 * domain is valid, the return state will be to read a query string.
 */
public class DomainNameReader {

  /**
   * The minimum length of a ascii based top level domain.
   */
  private static final int MIN_TOP_LEVEL_DOMAIN = 2;

  /**
   * The maximum length of a ascii based top level domain.
   */
  private static final int MAX_TOP_LEVEL_DOMAIN = 22;

  /**
   * The maximum number that the url can be in a url that looks like:
   * http://123123123123/path
   */
  private static final long MAX_NUMERIC_DOMAIN_VALUE = 4294967295L;

  /**
   * The minimum number the url can be in a url that looks like:
   * http://123123123123/path
   */
  private static final long MIN_NUMERIC_DOMAIN_VALUE = 16843008L;

  /**
   * If the domain name is an ip address, for each part of the address, whats the minimum value?
   */
  private static final int MIN_IP_PART = 0;

  /**
   * If the domain name is an ip address, for each part of the address, whats the maximum value?
   */
  private static final int MAX_IP_PART = 255;

  /**
   * The start of the utf character code table which indicates that this character is an international character.
   * Everything below this value is either a-z,A-Z,0-9 or symbols that are not included in domain name.
   */
  private static final int INTERNATIONAL_CHAR_START = 192;

  /**
   * The maximum length of each label in the domain name.
   */
  private static final int MAX_LABEL_LENGTH = 64;

  /**
   * The maximum number of labels in a single domain name.
   */
  private static final int MAX_NUMBER_LABELS = 127;

  /**
   * The maximum domain name length.
   */
  private static final int MAX_DOMAIN_LENGTH = 255;

  /**
   * Encoded hex dot.
   */
  private static final String HEX_ENCODED_DOT = "2e";

  /**
   * This is the final return state of reading a domain name.
   */
  public enum ReaderNextState {
    /**
     * Trying to read the domain name caused it to be invalid.
     */
    InvalidDomainName,
    /**
     * The domain name is found to be valid.
     */
    ValidDomainName,
    /**
     * Finished reading, next step should be to read the fragment.
     */
    ReadFragment,
    /**
     * Finished reading, next step should be to read the path.
     */
    ReadPath,
    /**
     * Finished reading, next step should be to read the port.
     */
    ReadPort,
    /**
     * Finished reading, next step should be to read the query string.
     */
    ReadQueryString
  }

  /**
   * The interface that gets called for each character that's non-matching (to a valid domain name character) in to count
   * the matching quotes and parenthesis correctly.
   */
  interface CharacterHandler {
    void addCharacter(char character);
  }

  /**
   * The currently written string buffer.
   */
  private StringBuilder _buffer;

  /**
   * The domain name started with a partial domain name found. This is the original string of the domain name only.
   */
  private String _current;

  /**
   * Detection option of this reader.
   */
  private UrlDetectorOptions _options;

  /**
   * Keeps track the number of dots that were found in the domain name.
   */
  private int _dots = 0;

  /**
   * Keeps track of the number of characters since the last "."
   */
  private int _currentLabelLength = 0;

  /**
   * Keeps track of the number of characters in the top level domain.
   */
  private int _topLevelLength = 0;

  /**
   * Keeps track where the domain name started. This is non zero if the buffer starts with
   * http://username:password@...
   */
  private int _startDomainName = 0;

  /**
   * Keeps track if the entire domain name is numeric.
   */
  private boolean _numeric = false;

  /**
   * Keeps track if we are seeing an ipv6 type address.
   */
  private boolean _seenBracket = false;

  /**
   * Keeps track if we have seen a full bracket set "[....]"; used for ipv6 type address.
   */
  private boolean _seenCompleteBracketSet = false;

  /**
   * Keeps track if we have a zone index in the ipv6 address.
   */
  private boolean _zoneIndex = false;

  /**
   * Contains the input stream to read.
   */
  private final InputTextReader _reader;

  /**
   * Contains the handler for each character match.
   */
  private final CharacterHandler _characterHandler;

  /**
   * Creates a new instance of the DomainNameReader object.
   * @param reader The input stream to read.
   * @param buffer The string buffer to use for storing a domain name.
   * @param current The current string that was thought to be a domain name.
   * @param options The detector options of this reader.
   * @param characterHandler The handler to call on each non-matching character to count matching quotes and stuff.
   */
  public DomainNameReader(InputTextReader reader, StringBuilder buffer, String current, UrlDetectorOptions options,
      CharacterHandler characterHandler) {
    _buffer = buffer;
    _current = current;
    _reader = reader;
    _options = options;
    _characterHandler = characterHandler;
  }

  /**
   * Reads and parses the current string to make sure the domain name started where it was supposed to,
   * and the current domain name is correct.
   * @return The next state to use after reading the current.
   */
  private ReaderNextState readCurrent() {

    if (_current != null) {
      //Handles the case where the string is ".hello"
      if (_current.length() == 1 && CharUtils.isDot(_current.charAt(0))) {
        return ReaderNextState.InvalidDomainName;
      } else if (_current.length() == 3 && _current.equalsIgnoreCase("%" + HEX_ENCODED_DOT)) {
        return ReaderNextState.InvalidDomainName;
      }

      //The location where the domain name started.
      _startDomainName = _buffer.length() - _current.length();

      //flag that the domain is currently all numbers and/or dots.
      _numeric = true;

      //If an invalid char is found, we can just restart the domain from there.
      int newStart = 0;

      char[] currArray = _current.toCharArray();
      int length = currArray.length;

      //hex special case
      boolean isAllHexSoFar = length > 2 && (currArray[0] == '0' && (currArray[1] == 'x' || currArray[1] == 'X'));

      int index = isAllHexSoFar ? 2 : 0;
      boolean done = false;

      while (index < length && !done) {
        //get the current character and update length counts.
        char curr = currArray[index];
        _currentLabelLength++;
        _topLevelLength = _currentLabelLength;

        //Is the length of the last part > 64 (plus one since we just incremented)
        if (_currentLabelLength > MAX_LABEL_LENGTH) {
          return ReaderNextState.InvalidDomainName;
        } else if (CharUtils.isDot(curr)) {
          //found a dot. Increment dot count, and reset last length
          _dots++;
          _currentLabelLength = 0;
        } else if (curr == '[') {
          _seenBracket = true;
          _numeric = false;
        } else if (curr == '%' && index + 2 < length && CharUtils.isHex(currArray[index + 1])
            && CharUtils.isHex(currArray[index + 2])) {
          //handle url encoded dot
          if (currArray[index + 1] == '2' && currArray[index + 2] == 'e') {
            _dots++;
            _currentLabelLength = 0;
          } else {
            _numeric = false;
          }
          index += 2;
        } else if (isAllHexSoFar) {
          //if it's a valid character in the domain that is not numeric
          if (!CharUtils.isHex(curr)) {
            _numeric = false;
            isAllHexSoFar = false;
            index--; //backtrack to rerun last character knowing it isn't hex.
          }
        } else if (CharUtils.isAlpha(curr) || curr == '-' || curr >= INTERNATIONAL_CHAR_START) {
          _numeric = false;
        } else if (!CharUtils.isNumeric(curr) && !_options.hasFlag(UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN)) {
          //if its not _numeric and not alphabetical, then restart searching for a domain from this point.
          newStart = index + 1;
          _currentLabelLength = 0;
          _topLevelLength = 0;
          _numeric = true;
          _dots = 0;
          done = true;
        }
        index++;
      }

      //An invalid character for the domain was found somewhere in the current buffer.
      //cut the first part of the domain out. For example:
      // http://asdf%asdf.google.com <- asdf.google.com is still valid, so restart from the %
      if (newStart > 0) {

        //make sure the location is not at the end. Otherwise the thing is just invalid.
        if (newStart < _current.length()) {
          _buffer.replace(0, _buffer.length(), _current.substring(newStart));

          //cut out the previous part, so now the domain name has to be from here.
          _startDomainName = 0;
        }

        //now after cutting if the buffer is just "." newStart > current (last character in current is invalid)
        if (newStart >= _current.length() || _buffer.toString().equals(".")) {
          return ReaderNextState.InvalidDomainName;
        }
      }
    } else {
      _startDomainName = _buffer.length();
    }

    //all else is good, return OK
    return ReaderNextState.ValidDomainName;
  }

  /**
   * Reads the Dns and returns the next state the state machine should take in throwing this out, or continue processing
   * if this is a valid domain name.
   * @return The next state to take.
   */
  public ReaderNextState readDomainName() {

    //Read the current, and if its bad, just return.
    if (readCurrent() == ReaderNextState.InvalidDomainName) {
      return ReaderNextState.InvalidDomainName;
    }

    //while not done and not end of string keep reading.
    boolean done = false;
    while (!done && !_reader.eof()) {
      char curr = _reader.read();

      if (curr == '/') {
        //continue by reading the path
        return checkDomainNameValid(ReaderNextState.ReadPath, curr);
      } else if (curr == ':' && (!_seenBracket || _seenCompleteBracketSet)) {
        //Don't check for a port if it's in the middle of an ipv6 address
        //continue by reading the port.
        return checkDomainNameValid(ReaderNextState.ReadPort, curr);
      } else if (curr == '?') {
        //continue by reading the query string
        return checkDomainNameValid(ReaderNextState.ReadQueryString, curr);
      } else if (curr == '#') {
        //continue by reading the fragment
        return checkDomainNameValid(ReaderNextState.ReadFragment, curr);
      } else if (CharUtils.isDot(curr)
          || (curr == '%' && _reader.canReadChars(2) && _reader.peek(2).equalsIgnoreCase(HEX_ENCODED_DOT))) {
        //if the current character is a dot or a urlEncodedDot

        //handles the case: hello..
        if (_currentLabelLength < 1) {
          done = true;
        } else {
          //append the "." to the domain name
          _buffer.append(curr);

          //if it was not a normal dot, then it is url encoded
          //read the next two chars, which are the hex representation
          if (!CharUtils.isDot(curr)) {
            _buffer.append(_reader.read());
            _buffer.append(_reader.read());
          }

          //increment the dots only if it's not part of the zone index and reset the last length.
          if (!_zoneIndex) {
            _dots++;
            _currentLabelLength = 0;
          }

          //if the length of the last section is longer than or equal to 64, it's too long to be a valid domain
          if (_currentLabelLength >= MAX_LABEL_LENGTH) {
            return ReaderNextState.InvalidDomainName;
          }
        }
      } else if (_seenBracket && (CharUtils.isHex(curr) || curr == ':' || curr == '[' || curr == ']' || curr == '%')
          && !_seenCompleteBracketSet) { //if this is an ipv6 address.
        switch (curr) {
          case ':':
            _currentLabelLength = 0;
            break;
          case '[':
            // if we read another '[', we need to restart by re-reading from this bracket instead.
            _reader.goBack();
            return ReaderNextState.InvalidDomainName;
          case ']':
            _seenCompleteBracketSet = true; //means that we already have a complete ipv6 address.
            _zoneIndex = false; //set this back off so that we can keep counting dots after ipv6 is over.
            break;
          case '%': //set flag to subtract subsequent dots because it's part of the zone index
            _zoneIndex = true;
            break;
          default:
            _currentLabelLength++;
            break;
        }
        _numeric = false;
        _buffer.append(curr);
      } else if (CharUtils.isAlphaNumeric(curr) || curr == '-' || curr >= INTERNATIONAL_CHAR_START) {
        //Valid domain name character. Either a-z, A-Z, 0-9, -, or international character
        if (_seenCompleteBracketSet) {
          //covers case of [fe80::]www.google.com
          _reader.goBack();
          done = true;
        } else {
          //if its not numeric, remember that; excluded x/X for hex ip addresses.
          if (curr != 'x' && curr != 'X' && !CharUtils.isNumeric(curr)) {
            _numeric = false;
          }

          //append to the states.
          _buffer.append(curr);
          _currentLabelLength++;
          _topLevelLength = _currentLabelLength;
        }
      } else if (curr == '[' && !_seenBracket) {
        _seenBracket = true;
        _numeric = false;
        _buffer.append(curr);
      } else if (curr == '[' && _seenCompleteBracketSet) { //Case where [::][ ...
        _reader.goBack();
        done = true;
      } else if (curr == '%' && _reader.canReadChars(2) && CharUtils.isHex(_reader.peekChar(0))
          && CharUtils.isHex(_reader.peekChar(1))) {
        //append to the states.
        _buffer.append(curr);
        _buffer.append(_reader.read());
        _buffer.append(_reader.read());
        _currentLabelLength += 3;
        _topLevelLength = _currentLabelLength;
      } else {
        //called to increment the count of matching characters
        _characterHandler.addCharacter(curr);

        //invalid character, we are done.
        done = true;
      }
    }

    //Check the domain name to make sure its ok.
    return checkDomainNameValid(ReaderNextState.ValidDomainName, null);
  }

  /**
   * Checks the current state of this object and returns if the valid state indicates that the
   * object has a valid domain name. If it does, it will return append the last character
   * and return the validState specified.
   * @param validState The state to return if this check indicates that the dns is ok.
   * @param lastChar The last character to add if the domain is ok.
   * @return The validState if the domain is valid, else ReaderNextState.InvalidDomainName
   */
  private ReaderNextState checkDomainNameValid(ReaderNextState validState, Character lastChar) {

    boolean valid = false;

    //Max domain length is 255 which includes the trailing "."
    //most of the time this is not included in the url.
    //If the _currentLabelLength is not 0 then the last "." is not included so add it.
    //Same with number of labels (or dots including the last)
    int lastDotLength =
        _buffer.length() > 3 && _buffer.substring(_buffer.length() - 3).equalsIgnoreCase("%" + HEX_ENCODED_DOT) ? 3 : 1;

    int domainLength = _buffer.length() - _startDomainName + (_currentLabelLength > 0 ? lastDotLength : 0);
    int dotCount = _dots + (_currentLabelLength > 0 ? 1 : 0);
    if (domainLength >= MAX_DOMAIN_LENGTH || (dotCount > MAX_NUMBER_LABELS)) {
      valid = false;
    } else if (_numeric) {
      String testDomain = _buffer.substring(_startDomainName).toLowerCase();
      valid = isValidIpv4(testDomain);
    } else if (_seenBracket) {
      String testDomain = _buffer.substring(_startDomainName).toLowerCase();
      valid = isValidIpv6(testDomain);
    } else if ((_currentLabelLength > 0 && _dots >= 1) || (_dots >= 2 && _currentLabelLength == 0)
        || (_options.hasFlag(UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) && _dots == 0)) {

      int topStart = _buffer.length() - _topLevelLength;
      if (_currentLabelLength == 0) {
        topStart--;
      }
      topStart = Math.max(topStart, 0);

      //get the first 4 characters of the top level domain
      String topLevelStart = _buffer.substring(topStart, topStart + Math.min(4, _buffer.length() - topStart));

      //There is no size restriction if the top level domain is international (starts with "xn--")
      valid =
          ((topLevelStart.equalsIgnoreCase("xn--") || (_topLevelLength >= MIN_TOP_LEVEL_DOMAIN && _topLevelLength <= MAX_TOP_LEVEL_DOMAIN)));
    }

    if (valid) {
      //if it's valid, add the last character (if specified) and return the valid state.
      if (lastChar != null) {
        _buffer.append(lastChar);
      }
      return validState;
    }

    //Roll back one char if its invalid to handle: "00:41.<br />"
    //This gets detected as 41.br otherwise.
    _reader.goBack();

    //return invalid state.
    return ReaderNextState.InvalidDomainName;
  }

  /**
   * Handles Hexadecimal, octal, decimal, dotted decimal, dotted hex, dotted octal.
   * @param testDomain the string we're testing
   * @return Returns true if it's a valid ipv4 address
   */
  private boolean isValidIpv4(String testDomain) {
    boolean valid = false;
    if (testDomain.length() > 0) {
      //handling format without dots. Ex: http://2123123123123/path/a, http://0x8242343/aksdjf
      if (_dots == 0) {
        try {
          long value;
          if (testDomain.length() > 2 && testDomain.charAt(0) == '0' && testDomain.charAt(1) == 'x') { //hex
            value = Long.parseLong(testDomain.substring(2), 16);
          } else if (testDomain.charAt(0) == '0') { //octal
            value = Long.parseLong(testDomain.substring(1), 8);
          } else { //decimal
            value = Long.parseLong(testDomain);
          }
          valid = value <= MAX_NUMERIC_DOMAIN_VALUE && value >= MIN_NUMERIC_DOMAIN_VALUE;
        } catch (NumberFormatException e) {
          valid = false;
        }
      } else if (_dots == 3) {
        //Dotted decimal/hex/octal format
        String[] parts = CharUtils.splitByDot(testDomain);
        valid = true;

        //check each part of the ip and make sure its valid.
        for (int i = 0; i < parts.length && valid; i++) {
          String part = parts[i];
          if (part.length() > 0) {
            String parsedNum;
            int base;
            if (part.length() > 2 && part.charAt(0) == '0' && part.charAt(1) == 'x') { //dotted hex
              parsedNum = part.substring(2);
              base = 16;
            } else if (part.charAt(0) == '0') { //dotted octal
              parsedNum = part.substring(1);
              base = 8;
            } else { //dotted decimal
              parsedNum = part;
              base = 10;
            }

            Integer section;
            if (parsedNum.length() == 0) {
              section = 0;
            } else {
              try {
                section = Integer.parseInt(parsedNum, base);
              } catch (NumberFormatException e) {
                return false;
              }
            }
            if (section < MIN_IP_PART || section > MAX_IP_PART) {
              valid = false;
            }
          } else {
            valid = false;
          }
        }
      }
    }
    return valid;
  }

  /**
   * Sees that there's an open "[", and is now checking for ":"'s and stopping when there is a ']' or invalid character.
   * Handles ipv4 formatted ipv6 addresses, zone indices, truncated notation.
   * @return Returns true if it is a valid ipv6 address
   */
  private boolean isValidIpv6(String testDomain) {
    char[] domainArray = testDomain.toCharArray();

    // Return false if we don't see [....]
    // or if we only have '[]'
    // or if we detect [:8000: ...]; only [::8000: ...] is okay
    if (domainArray.length < 3 || domainArray[domainArray.length - 1] != ']' || domainArray[0] != '['
        || domainArray[1] == ':' && domainArray[2] != ':') {
      return false;
    }

    int numSections = 1;
    int hexDigits = 0;
    char prevChar = 0;

    //used to check ipv4 addresses at the end of ipv6 addresses.
    StringBuilder lastSection = new StringBuilder();
    Boolean hexSection = true;

    // If we see a '%'. Example: http://[::ffff:0xC0.0x00.0x02.0xEB%251]
    boolean zoneIndiceMode = false;

    //If doubleColonFlag is true, that means we've already seen one "::"; we're not allowed to have more than one.
    boolean doubleColonFlag = false;

    int index = 0;
    for (; index < domainArray.length; index++) {
      switch (domainArray[index]) {
        case '[': //found beginning of ipv6 address
          break;
        case '%':
        case ']': //found end of ipv6 address
          if (domainArray[index] == '%') {
            //see if there's a urlencoded dot
            if (domainArray.length - index >= 2 && domainArray[index + 1] == '2' && domainArray[index + 2] == 'e') {
              lastSection.append("%2e");
              index += 2;
              hexSection = false;
              break;
            }
            zoneIndiceMode = true;
          }
          if (!hexSection && (!zoneIndiceMode || domainArray[index] == '%')) {
            if (isValidIpv4(lastSection.toString())) {
              numSections++; //ipv4 takes up 2 sections.
            } else {
              return false;
            }
          }
          break;
        case ':':
          if (prevChar == ':') {
            if (doubleColonFlag) { //only allowed to have one "::" in an ipv6 address.
              return false;
            }
            doubleColonFlag = true;
          }

          //This means that we reached invalid characters in the previous section
          if (!hexSection) {
            return false;
          }

          hexSection = true; //reset hex to true
          hexDigits = 0; //reset count for hex digits
          numSections++;
          lastSection.delete(0, lastSection.length()); //clear last section
          break;
        default:
          if (zoneIndiceMode) {
            if (!CharUtils.isUnreserved(domainArray[index])) {
              return false;
            }
          } else {
            lastSection.append(domainArray[index]); //collect our possible ipv4 address
            if (hexSection && CharUtils.isHex(domainArray[index])) {
              hexDigits++;
            } else {
              hexSection = false; //non hex digit.
            }
          }
          break;
      }
      if (hexDigits > 4 || numSections > 8) {
        return false;
      }
      prevChar = domainArray[index];
    }

    //numSections != 1 checks for things like: [adf]
    //If there are more than 8 sections for the address or there isn't a double colon, then it's invalid.
    return numSections != 1 && (numSections >= 8 || doubleColonFlag);
  }
}
