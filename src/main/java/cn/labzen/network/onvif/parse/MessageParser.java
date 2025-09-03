package cn.labzen.network.onvif.parse;

import cn.labzen.network.onvif.device.Device;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.util.List;

public abstract class MessageParser {

  protected static final XmlMapper XML_MAPPER;

  protected final String hostname;
  protected final String message;

  static {
    XML_MAPPER = new XmlMapper();
    XML_MAPPER.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
    XML_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public MessageParser(String hostname, String message) {
    this.hostname = hostname;
    this.message = message;
  }

  public String getHostname() {
    return hostname;
  }

  public String getMessage() {
    return message;
  }

  public abstract List<? extends Device> parse();
}
