package cn.labzen.network.ntp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NtpTest {

  @Test
  void request() {
    Double time = NTPClient.request("ntp.aliyun.com", 123);
    System.out.println(time);
    assertNotNull(time, "NTP request should return a valid offset");
    assertTrue(Math.abs(time) < 86400, "NTP time offset should be within a day");
  }
}
