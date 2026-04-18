package cn.labzen.network.onvif.parse;

import cn.labzen.network.onvif.device.Device;
import cn.labzen.network.onvif.device.UpnpDevice;

import java.util.List;

public class UpnpMessageParser extends MessageParser {

  public UpnpMessageParser(String hostname, String message) {
    super(hostname, message);
  }

  @Override
  public List<? extends Device> parse() {
    UpnpDevice device = new UpnpDevice(message,
        hostname,
        search("LOCATION: "),
        search("SERVER: "),
        search("USN: "),
        search("ST: "));
    return List.of(device);
  }

  private String search(String target) {
    int index = message.indexOf(target);
    if (index < 0) {
      return "";
    }

    int start = index + target.length();
    int end = message.indexOf("\r\n", start);
    if (end < 0) {
      end = message.length();
    }
    return message.substring(start, end).trim();
  }
}
