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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestHostNormalizer {

  @DataProvider
  private Object[][] getIPAddresses() {
    return new Object[][] {
        {"[fefe::]", "[fefe:0:0:0:0:0:0:0]"},
        {"[::ffff]", "[0:0:0:0:0:0:0:ffff]"},
        {"[::255.255.255.255]", "[0:0:0:0:0:0:ffff:ffff]"},
        {"[::]", "[0:0:0:0:0:0:0:0]"},
        {"[::1]", "[0:0:0:0:0:0:0:1]"},
        {"[aAaA::56.7.7.5]", "[aaaa:0:0:0:0:0:3807:705]"},
        {"[BBBB:ab:f78F:f:DDDD:bab:56.7.7.5]", "[bbbb:ab:f78f:f:dddd:bab:3807:705]"},
        {"[Aaaa::1]", "[aaaa:0:0:0:0:0:0:1]"},
        {"[::192.167.2.2]", "[0:0:0:0:0:0:c0a7:202]"},
        {"[0:ffff::077.0x22.222.11]", "[0:ffff:0:0:0:0:3f22:de0b]"},
        {"[0::ffff:077.0x22.222.11]", "63.34.222.11"},
        {"192.168.1.1", "192.168.1.1"},
        {"0x92.168.1.1", "146.168.1.1"},
        {"3279880203", "195.127.0.11"}
    };
  }

  @Test(dataProvider = "getIPAddresses")
  public void testIpHostNormalizationAndGetBytes(String original, String expectedHost) throws UnknownHostException {
    HostNormalizer hostNormalizer = new HostNormalizer(original);
    Assert.assertEquals(hostNormalizer.getNormalizedHost(), expectedHost);

    InetAddress address = InetAddress.getByName(expectedHost);
    byte[] expectedBytes;
    if (address instanceof Inet4Address) {
      expectedBytes = new byte[16];
      expectedBytes[10] = (byte) 0xff;
      expectedBytes[11] = (byte) 0xff;
      System.arraycopy(address.getAddress(), 0, expectedBytes, 12, 4);
    } else {
      expectedBytes = address.getAddress();
    }
    Assert.assertTrue(Arrays.equals(hostNormalizer.getBytes(), expectedBytes));
  }

  @DataProvider
  private Object[][] getNormalHosts() {
    return new Object[][] {
        {"sALes.com"},
        {"33r.nEt"},
        {"173839.com"},
        {"192.168.-3.1"},
        {"[::-34:50]"},
        {"[-34::192.168.34.-3]"}
    };
  }

  @Test(dataProvider = "getNormalHosts")
  public void testSanityAddresses(String host) {
    HostNormalizer hostNormalizer = new HostNormalizer(host);
    Assert.assertEquals(hostNormalizer.getNormalizedHost(), host.toLowerCase());
    Assert.assertNull(hostNormalizer.getBytes());
  }
}
