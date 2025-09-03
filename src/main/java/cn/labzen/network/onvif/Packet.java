package cn.labzen.network.onvif;

import cn.labzen.tool.util.Strings;

public record Packet(String uuid, DiscoveryMode mode) {

  private static final String SOAP_DISCOVERY_XML = """
      <?xml version="1.0" encoding="utf-8"?>
      <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xmlns:xsd="http://www.w3.org/2001/XMLSchema"
          xmlns:addr="http://schemas.xmlsoap.org/ws/2004/08/addressing"
          xmlns:disc="http://schemas.xmlsoap.org/ws/2005/04/discovery">
        <soap:Header>
          <addr:MessageID>uuid:{}</addr:MessageID>
          <addr:To soap:mustUnderstand="true">urn:schemas-xmlsoap-org:ws:2005:04:discovery</addr:To>
          <addr:Action soap:mustUnderstand="true">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</addr:Action>
        </soap:Header>
        <soap:Body>
          <disc:Probe>
            <disc:Types>dn:NetworkVideoTransmitter</disc:Types>
          </disc:Probe>
        </soap:Body>
      </soap:Envelope>
      """;
  private static final String UPNP_PACKET_QUERY_DATA = """
      M-SEARCH * HTTP/1.1
      HOST: 239.255.255.250:1900
      MAN: "ssdp:discover"
      MX: 1
      ST: ssdp:all
      """;
  private static final String HIK_VISION_XML = """
      <?xml version="1.0" encoding="UTF-8"?>
      <Probe>
        <Uuid>{}</Uuid>
        <Types>inquiry</Types>
      </Probe>
      """;

  //private String name;
  public String toData() {
    return switch (mode) {
      case ONVIF -> Strings.format(SOAP_DISCOVERY_XML, uuid);
      case UPNP -> UPNP_PACKET_QUERY_DATA;
      case HIK_VISION -> Strings.format(HIK_VISION_XML, uuid);
    };
  }
}
