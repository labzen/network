package cn.labzen.network.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static cn.labzen.network.util.Addresses.isIp;
import static cn.labzen.network.util.Addresses.isIpv4;

public class AddressesTest {

  @Test
  void testIsIp() {
    Assertions.assertTrue(isIpv4("192.168.1.255"));
    Assertions.assertFalse(isIpv4("192.168.1.300"));
    Assertions.assertFalse(isIpv4("ABCD:EF01:2345:6789:ABCD:EF01:2345:6789"));
    Assertions.assertFalse(isIpv4("ABCD:EF01:2345:6789:ABCD:EF01:"));

    Assertions.assertTrue(isIp("127.0.0.1"));
  }
}
