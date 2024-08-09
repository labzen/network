package cn.labzen.network.ntp;

import org.junit.jupiter.api.Test;

public class NtpTest {

  @Test
  void request() {
    double time = NtpClient.request("ntp.aliyun.com", 123);
    System.out.println(time);
  }
}
