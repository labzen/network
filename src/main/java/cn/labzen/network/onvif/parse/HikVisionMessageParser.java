package cn.labzen.network.onvif.parse;

import cn.labzen.network.exception.OnvifException;
import cn.labzen.network.onvif.device.Device;
import cn.labzen.network.onvif.device.HikVisionDevice;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

public class HikVisionMessageParser extends MessageParser {

  public HikVisionMessageParser(String hostname, String message) {
    super(hostname, message);
  }

  @Override
  public List<? extends Device> parse() {
    String xml = message.trim().replaceAll(">(\\s*)<", "><");
    try {
      Map<String, String> values = XML_MAPPER.readValue(xml, new TypeReference<>() {
      });
      HikVisionDevice device = new HikVisionDevice(message,
          hostname,
          values.getOrDefault("DeviceType", ""),
          values.getOrDefault("DeviceDescription", ""),
          values.getOrDefault("DeviceSN", ""),
          values.getOrDefault("IPv4Address", ""),
          values.getOrDefault("IPv4Gateway", ""),
          parsePort(values.getOrDefault("CommandPort", "")),
          values.getOrDefault("MAC", ""),
          values.getOrDefault("SoftwareVersion", ""),
          values.getOrDefault("BootTime", ""),
          values.getOrDefault("SafeCode", ""));
      return List.of(device);
    } catch (JsonProcessingException e) {
      throw new OnvifException(e, "读取信息失败：{}", message);
    }
  }

  private static int parsePort(String portStr) {
    if (portStr == null || portStr.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(portStr.trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
