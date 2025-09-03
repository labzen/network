package cn.labzen.network.onvif.parse;

import cn.labzen.network.exception.OnvifException;
import cn.labzen.network.onvif.device.Device;
import cn.labzen.network.onvif.device.OnvifDevice;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Collections;
import java.util.List;

public class OnvifMessageParser extends MessageParser {

  public OnvifMessageParser(String hostname, String message) {
    super(hostname, message);
  }

  @Override
  public List<? extends Device> parse() throws OnvifException {
    String xml = message.trim().replaceAll(">(\\s*)<", "><");
    try {
      OnvifDevice.InternalOnvifEnvelope onvifEnvelope = XML_MAPPER.readValue(xml,
          OnvifDevice.InternalOnvifEnvelope.class);

      if (onvifEnvelope != null && onvifEnvelope.getBody() != null && onvifEnvelope.getBody().getProbes() != null) {
        return onvifEnvelope.getBody()
                            .getProbes()
                            .stream()
                            .filter(prob -> prob.getType() != null &&
                                            prob.getType().contains("NetworkVideoTransmitter"))
                            .map(prob -> new OnvifDevice(message, hostname, prob.getAddr()))
                            .toList();
      }
      return Collections.emptyList();
    } catch (JsonProcessingException e) {
      throw new OnvifException(e, "读取信息失败：{}", message);
    }
  }
}
