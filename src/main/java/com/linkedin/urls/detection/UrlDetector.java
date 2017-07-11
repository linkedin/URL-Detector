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

import com.linkedin.urls.Url;
import com.linkedin.urls.UrlMarker;
import com.linkedin.urls.UrlPart;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class UrlDetector {
  /**
   * Contains the string to check for and remove if the scheme is this.
   */
  private static final String HTML_MAILTO = "mailto:";

  /**
   * Valid protocol schemes.
   */
  private static final Set<String> VALID_SCHEMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
      "http://", "https://", "ftp://", "ftps://", "http%3a//", "https%3a//", "ftp%3a//", "ftps%3a//")));

  /**
   * The response of character matching.
   */
  private enum CharacterMatch {
    /**
     * The character was not matched.
     */
    CharacterNotMatched,
    /**
     * A character was matched with requires a stop.
     */
    CharacterMatchStop,
    /**
     * The character was matched which is a start of parentheses.
     */
    CharacterMatchStart
  }

  /**
   * Stores options for detection.
   */
  private final UrlDetectorOptions _options;

  /**
   * The input stream to read.
   */
  private final InputTextReader _reader;

  /**
   * Buffer to store temporary urls inside of.
   */
  private StringBuilder _buffer = new StringBuilder();

  /**
   * Has the scheme been found in this iteration?
   */
  private boolean _hasScheme = false;

  /**
   * If the first character in the url is a quote, then look for matching quote at the end.
   */
  private boolean _quoteStart = false;

  /**
   * If the first character in the url is a single quote, then look for matching quote at the end.
   */
  private boolean _singleQuoteStart = false;

  /**
   * If we see a '[', didn't find an ipv6 address, and the bracket option is on, then look for urls inside the brackets.
   */
  private boolean _dontMatchIpv6 = false;

  /**
   * Stores the found urls.
   */
  private ArrayList<Url> _urlList = new ArrayList<Url>();

  /**
   * Keeps the count of special characters used to match quotes and different types of brackets.
   */
  private HashMap<Character, Integer> _characterMatch = new HashMap<Character, Integer>();

  /**
   * Keeps track of certain indices to create a Url object.
   */
  private UrlMarker _currentUrlMarker = new UrlMarker();

  /**
   * The states to use to continue writing or not.
   */
  public enum ReadEndState {
    /**
     * The current url is valid.
     */
    ValidUrl,
    /**
     * The current url is invalid.
     */
    InvalidUrl
  }

  /**
   * Creates a new UrlDetector object used to find urls inside of text.
   * @param content The content to search inside of.
   * @param options The UrlDetectorOptions to use when detecting the content.
   */
  public UrlDetector(String content, UrlDetectorOptions options) {
    _reader = new InputTextReader(content);
    _options = options;
  }

  /**
   * Gets the number of characters that were backtracked while reading the input. This is useful for performance
   * measurement.
   * @return The count of characters that were backtracked while reading.
   */
  public int getBacktracked() {
    return _reader.getBacktrackedCount();
  }

  /**
   * Detects the urls and returns a list of detected url strings.
   * @return A list with detected urls.
   */
  public List<Url> detect() {
    readDefault();
    return _urlList;
  }

  /**
   * The default input reader which looks for specific flags to start detecting the url.
   */
  private void readDefault() {
    //Keeps track of the number of characters read to be able to later cut out the domain name.
    int length = 0;

    //until end of string read the contents
    while (!_reader.eof()) {
      //read the next char to process.
      char curr = _reader.read();

      switch (curr) {
        case ' ':
          //space was found, check if it's a valid single level domain.
          if (_options.hasFlag(UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) && _buffer.length() > 0 && _hasScheme) {
            _reader.goBack();
            readDomainName(_buffer.substring(length));
          }
          _buffer.append(curr);
          readEnd(ReadEndState.InvalidUrl);
          length = 0;
          break;
        case '%':
          if (_reader.canReadChars(2)) {
            if (_reader.peek(2).equalsIgnoreCase("3a")) {
              _buffer.append(curr);
              _buffer.append(_reader.read());
              _buffer.append(_reader.read());
              length = processColon(length);
            } else if (CharUtils.isHex(_reader.peekChar(0)) && CharUtils.isHex(_reader.peekChar(1))) {
              _buffer.append(curr);
              _buffer.append(_reader.read());
              _buffer.append(_reader.read());

              readDomainName(_buffer.substring(length));
              length = 0;
            }
          }
          break;
        case '\u3002': //non-standard dots
        case '\uFF0E':
        case '\uFF61':
        case '.': //"." was found, read the domain name using the start from length.
          _buffer.append(curr);
          readDomainName(_buffer.substring(length));
          length = 0;
          break;
        case '@': //Check the domain name after a username
          if (_buffer.length() > 0) {
            _currentUrlMarker.setIndex(UrlPart.USERNAME_PASSWORD, length);
            _buffer.append(curr);
            readDomainName(null);
            length = 0;
          }
          break;
        case '[':
          if (_dontMatchIpv6) {
            //Check if we need to match characters. If we match characters and this is a start or stop of range,
            //either way reset the world and start processing again.
            if (checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
              readEnd(ReadEndState.InvalidUrl);
              length = 0;
            }
          }
          int beginning = _reader.getPosition();

          //if it doesn't have a scheme, clear the buffer.
          if (!_hasScheme) {
            _buffer.delete(0, _buffer.length());
          }
          _buffer.append(curr);

          if (!readDomainName(_buffer.substring(length))) {
            //if we didn't find an ipv6 address, then check inside the brackets for urls
            _reader.seek(beginning);
            _dontMatchIpv6 = true;
          }
          length = 0;
          break;
        case '/':
          // "/" was found, then we either read a scheme, or if we already read a scheme, then
          // we are reading a url in the format http://123123123/asdf

          if (_hasScheme || (_options.hasFlag(UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) && _buffer.length() > 1)) {
            //we already have the scheme, so then we already read:
            //http://something/ <- if something is all numeric then its a valid url.
            //OR we are searching for single level domains. We have buffer length > 1 condition
            //to weed out infinite backtrack in cases of html5 roots

            //unread this "/" and continue to check the domain name starting from the beginning of the domain
            _reader.goBack();
            readDomainName(_buffer.substring(length));
            length = 0;
          } else {

            //we don't have a scheme already, then clear state, then check for html5 root such as: "//google.com/"
            // remember the state of the quote when clearing state just in case its "//google.com" so its not cleared.
            readEnd(ReadEndState.InvalidUrl);
            _buffer.append(curr);
            _hasScheme = readHtml5Root();
            length = _buffer.length();
          }
          break;
        case ':':
          //add the ":" to the url and check for scheme/username
          _buffer.append(curr);
          length = processColon(length);
          break;
        default:
          //Check if we need to match characters. If we match characters and this is a start or stop of range,
          //either way reset the world and start processing again.
          if (checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
            readEnd(ReadEndState.InvalidUrl);
            length = 0;
          } else {
            _buffer.append(curr);
          }
          break;
      }
    }
    if (_options.hasFlag(UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN) && _buffer.length() > 0 && _hasScheme) {
      readDomainName(_buffer.substring(length));
    }
  }

  /**
   * We found a ":" and is now trying to read either scheme, username/password
   * @param length first index of the previous part (could be beginning of the buffer, beginning of the username/password, or beginning
   * @return new index of where the domain starts
   */
  private int processColon(int length) {
    if (_hasScheme) {
      //read it as username/password if it has scheme
      if (!readUserPass(length) && _buffer.length() > 0) {
        //unread the ":" so that the domain reader can process it
        _reader.goBack();
        _buffer.delete(_buffer.length() - 1, _buffer.length());

        int backtrackOnFail = _reader.getPosition() - _buffer.length() + length;
        if (!readDomainName(_buffer.substring(length))) {
          //go back to length location and restart search
          _reader.seek(backtrackOnFail);
          readEnd(ReadEndState.InvalidUrl);
        }
        length = 0;
      }
    } else if (readScheme() && _buffer.length() > 0) {
      _hasScheme = true;
      length = _buffer.length(); //set length to be right after the scheme
    } else if (_buffer.length() > 0 && _options.hasFlag(UrlDetectorOptions.ALLOW_SINGLE_LEVEL_DOMAIN)
        && _reader.canReadChars(1)) { //takes care of case like hi:
      _reader.goBack(); //unread the ":" so readDomainName can take care of the port
      _buffer.delete(_buffer.length() - 1, _buffer.length());
      readDomainName(_buffer.toString());
    } else {
      readEnd(ReadEndState.InvalidUrl);
      length = 0;
    }

    return length;
  }

  /**
   * Gets the number of times the current character was seen in the document. Only special characters are tracked.
   * @param curr The character to look for.
   * @return The number of times that character was seen
   */
  private int getCharacterCount(char curr) {
    Integer count = _characterMatch.get(curr);
    return count == null ? 0 : count;
  }

  /**
   * Increments the counter for the characters seen and return if this character matches a special character
   * that might require stopping reading the url.
   * @param curr The character to check.
   * @return The state that this character requires.
   */
  private CharacterMatch checkMatchingCharacter(char curr) {

    //This is a quote and we are matching quotes.
    if ((curr == '\"' && _options.hasFlag(UrlDetectorOptions.QUOTE_MATCH))
        || (curr == '\'' && _options.hasFlag(UrlDetectorOptions.SINGLE_QUOTE_MATCH))) {
      boolean quoteStart;
      if (curr == '\"') {
        quoteStart = _quoteStart;

        //remember that a double quote was found.
        _quoteStart = true;
      } else {
        quoteStart = _singleQuoteStart;

        //remember that a single quote was found.
        _singleQuoteStart = true;
      }

      //increment the number of quotes found.
      Integer currVal = getCharacterCount(curr) + 1;
      _characterMatch.put(curr, currVal);

      //if there was already a quote found, or the number of quotes is even, return that we have to stop, else its a start.
      return quoteStart || currVal % 2 == 0 ? CharacterMatch.CharacterMatchStop : CharacterMatch.CharacterMatchStart;
    } else if (_options.hasFlag(UrlDetectorOptions.BRACKET_MATCH) && (curr == '[' || curr == '{' || curr == '(')) {
      //Look for start of bracket
      _characterMatch.put(curr, getCharacterCount(curr) + 1);
      return CharacterMatch.CharacterMatchStart;
    } else if (_options.hasFlag(UrlDetectorOptions.XML) && (curr == '<')) {
      //If its html, look for "<"
      _characterMatch.put(curr, getCharacterCount(curr) + 1);
      return CharacterMatch.CharacterMatchStart;
    } else if ((_options.hasFlag(UrlDetectorOptions.BRACKET_MATCH) && (curr == ']' || curr == '}' || curr == ')'))
        || (_options.hasFlag(UrlDetectorOptions.XML) && (curr == '>'))) {

      //If we catch a end bracket increment its count and get rid of not ipv6 flag
      Integer currVal = getCharacterCount(curr) + 1;
      _characterMatch.put(curr, currVal);

      //now figure out what the start bracket was associated with the closed bracket.
      char match = '\0';
      switch (curr) {
        case ']':
          match = '[';
          break;
        case '}':
          match = '{';
          break;
        case ')':
          match = '(';
          break;
        case '>':
          match = '<';
          break;
        default:
          break;
      }

      //If the number of open is greater then the number of closed, return a stop.
      return getCharacterCount(match) > currVal ? CharacterMatch.CharacterMatchStop
          : CharacterMatch.CharacterMatchStart;
    }

    //Nothing else was found.
    return CharacterMatch.CharacterNotMatched;
  }

  /**
   * Checks if the url is in the format:
   * //google.com/static/js.js
   * @return True if the url is in this format and was matched correctly.
   */
  private boolean readHtml5Root() {
    //end of input then go away.
    if (_reader.eof()) {
      return false;
    }

    //read the next character. If its // then return true.
    char curr = _reader.read();
    if (curr == '/') {
      _buffer.append(curr);
      return true;
    } else {
      //if its not //, then go back and reset by 1 character.
      _reader.goBack();
      readEnd(ReadEndState.InvalidUrl);
    }
    return false;
  }

  /**
   * Reads the scheme and allows returns true if the scheme is http(s?):// or ftp(s?)://
   * @return True if the scheme was found, else false.
   */
  private boolean readScheme() {
    //Check if we are checking html and the length is longer than mailto:
    if (_options.hasFlag(UrlDetectorOptions.HTML) && _buffer.length() >= HTML_MAILTO.length()) {
      //Check if the string is actually mailto: then just return nothing.
      if (HTML_MAILTO.equalsIgnoreCase(_buffer.substring(_buffer.length() - HTML_MAILTO.length()))) {
        return readEnd(ReadEndState.InvalidUrl);
      }
    }

    int originalLength = _buffer.length();
    int numSlashes = 0;

    while (!_reader.eof()) {
      char curr = _reader.read();

      //if we match a slash, look for a second one.
      if (curr == '/') {
        _buffer.append(curr);
        if (numSlashes == 1) {
          //return only if its an approved protocol. This can be expanded to allow others
          if (VALID_SCHEMES.contains(_buffer.toString().toLowerCase())) {
            _currentUrlMarker.setIndex(UrlPart.SCHEME, 0);
            return true;
          }
          return false;
        }
        numSlashes++;
      } else if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
        //if we find a space or end of input, then nothing found.
        _buffer.append(curr);
        return false;
      } else if (curr == '[') { //if we're starting to see an ipv6 address
        _reader.goBack(); //unread the '[', so that we can start looking for ipv6
        return false;
      } else if (originalLength > 0 || numSlashes > 0 || !CharUtils.isAlpha(curr)) {
        // if it's not a character a-z or A-Z then assume we aren't matching scheme, but instead
        // matching username and password.
        _reader.goBack();
        return readUserPass(0);
      }
    }

    return false;
  }

  /**
   * Reads the input and looks for a username and password.
   * Handles:
   * http://username:password@...
   * @param beginningOfUsername Index of the buffer of where the username began
   * @return True if a valid username and password was found.
   */
  private boolean readUserPass(int beginningOfUsername) {

    //The start of where we are.
    int start = _buffer.length();

    //keep looping until "done"
    boolean done = false;

    //if we had a dot in the input, then it might be a domain name and not a username and password.
    boolean rollback = false;
    while (!done && !_reader.eof()) {
      char curr = _reader.read();

      // if we hit this, then everything is ok and we are matching a domain name.
      if (curr == '@') {
        _buffer.append(curr);
        _currentUrlMarker.setIndex(UrlPart.USERNAME_PASSWORD, beginningOfUsername);
        return readDomainName("");
      } else if (CharUtils.isDot(curr) || curr == '[') {
        //everything is still ok, just remember that we found a dot or '[' in case we might need to backtrack
        _buffer.append(curr);
        rollback = true;
      } else if (curr == '#' || curr == ' ' || curr == '/'
          || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
        //one of these characters indicates we are invalid state and should just return.
        rollback = true;
        done = true;
      } else {
        //all else, just append character assuming its ok so far.
        _buffer.append(curr);
      }
    }

    if (rollback) {
      //got to here, so there is no username and password. (We didn't find a @)
      int distance = _buffer.length() - start;
      _buffer.delete(start, _buffer.length());

      int currIndex = Math.max(_reader.getPosition() - distance - (done ? 1 : 0), 0);
      _reader.seek(currIndex);

      return false;
    } else {
      return readEnd(ReadEndState.InvalidUrl);
    }
  }

  /**
   * Try to read the current string as a domain name
   * @param current The current string used.
   * @return Whether the domain is valid or not.
   */
  private boolean readDomainName(String current) {
    int hostIndex = current == null ? _buffer.length() : _buffer.length() - current.length();
    _currentUrlMarker.setIndex(UrlPart.HOST, hostIndex);
    //create the domain name reader and specify the handler that will be called when a quote character
    //or something is found.
    DomainNameReader reader =
        new DomainNameReader(_reader, _buffer, current, _options, new DomainNameReader.CharacterHandler() {
          @Override
          public void addCharacter(char character) {
            checkMatchingCharacter(character);
          }
        });

    //Try to read the dns and act on the response.
    DomainNameReader.ReaderNextState state = reader.readDomainName();
    switch (state) {
      case ValidDomainName:
        return readEnd(ReadEndState.ValidUrl);
      case ReadFragment:
        return readFragment();
      case ReadPath:
        return readPath();
      case ReadPort:
        return readPort();
      case ReadQueryString:
        return readQueryString();
      default:
        return readEnd(ReadEndState.InvalidUrl);
    }
  }

  /**
   * Reads the fragments which is the part of the url starting with #
   * @return If a valid fragment was read true, else false.
   */
  private boolean readFragment() {
    _currentUrlMarker.setIndex(UrlPart.FRAGMENT, _buffer.length() - 1);

    while (!_reader.eof()) {
      char curr = _reader.read();

      //if it's the end or space, then a valid url was read.
      if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
        return readEnd(ReadEndState.ValidUrl);
      } else {
        //otherwise keep appending.
        _buffer.append(curr);
      }
    }

    //if we are here, anything read is valid.
    return readEnd(ReadEndState.ValidUrl);
  }

  /**
   * Try to read the query string.
   * @return True if the query string was valid.
   */
  private boolean readQueryString() {
    _currentUrlMarker.setIndex(UrlPart.QUERY, _buffer.length() - 1);

    while (!_reader.eof()) {
      char curr = _reader.read();

      if (curr == '#') { //fragment
        _buffer.append(curr);
        return readFragment();
      } else if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
        //end of query string
        return readEnd(ReadEndState.ValidUrl);
      } else { //all else add to buffer.
        _buffer.append(curr);
      }
    }
    //a valid url was read.
    return readEnd(ReadEndState.ValidUrl);
  }

  /**
   * Try to read the port of the url.
   * @return True if a valid port was read.
   */
  private boolean readPort() {
    _currentUrlMarker.setIndex(UrlPart.PORT, _buffer.length());
    //The length of the port read.
    int portLen = 0;
    while (!_reader.eof()) {
      //read the next one and remember the length
      char curr = _reader.read();
      portLen++;

      if (curr == '/') {
        //continue to read path
        _buffer.append(curr);
        return readPath();
      } else if (curr == '?') {
        //continue to read query string
        _buffer.append(curr);
        return readQueryString();
      } else if (curr == '#') {
        //continue to read fragment.
        _buffer.append(curr);
        return readFragment();
      } else if (checkMatchingCharacter(curr) == CharacterMatch.CharacterMatchStop || !CharUtils.isNumeric(curr)) {
        //if we got here, then what we got so far is a valid url. don't append the current character.
        _reader.goBack();

        //no port found; it was something like google.com:hello.world
        if (portLen == 1) {
          //remove the ":" from the end.
          _buffer.delete(_buffer.length() - 1, _buffer.length());
        }
        _currentUrlMarker.unsetIndex(UrlPart.PORT);
        return readEnd(ReadEndState.ValidUrl);
      } else {
        //this is a valid character in the port string.
        _buffer.append(curr);
      }
    }

    //found a correct url
    return readEnd(ReadEndState.ValidUrl);
  }

  /**
   * Tries to read the path
   * @return True if the path is valid.
   */
  private boolean readPath() {
    _currentUrlMarker.setIndex(UrlPart.PATH, _buffer.length() - 1);
    while (!_reader.eof()) {
      //read the next char
      char curr = _reader.read();

      if (curr == ' ' || checkMatchingCharacter(curr) != CharacterMatch.CharacterNotMatched) {
        //if end of state and we got here, then the url is valid.
        return readEnd(ReadEndState.ValidUrl);
      }

      //append the char
      _buffer.append(curr);

      //now see if we move to another state.
      if (curr == '?') {
        //if ? read query string
        return readQueryString();
      } else if (curr == '#') {
        //if # read the fragment
        return readFragment();
      }
    }

    //end of input then this url is good.
    return readEnd(ReadEndState.ValidUrl);
  }

  /**
   * The url has been read to here. Remember the url if its valid, and reset state.
   * @param state The state indicating if this url is valid. If its valid it will be added to the list of urls.
   * @return True if the url was valid.
   */
  private boolean readEnd(ReadEndState state) {
    //if the url is valid and greater then 0
    if (state == ReadEndState.ValidUrl && _buffer.length() > 0) {
      //get the last character. if its a quote, cut it off.
      int len = _buffer.length();
      if (_quoteStart && _buffer.charAt(len - 1) == '\"') {
        _buffer.delete(len - 1, len);
      }

      //Add the url to the list of good urls.
      if (_buffer.length() > 0) {
        _currentUrlMarker.setOriginalUrl(_buffer.toString());
        _urlList.add(_currentUrlMarker.createUrl());
      }
    }

    //clear out the buffer.
    _buffer.delete(0, _buffer.length());

    //reset the state of internal objects.
    _quoteStart = false;
    _hasScheme = false;
    _dontMatchIpv6 = false;
    _currentUrlMarker = new UrlMarker();

    //return true if valid.
    return state == ReadEndState.ValidUrl;
  }
}
