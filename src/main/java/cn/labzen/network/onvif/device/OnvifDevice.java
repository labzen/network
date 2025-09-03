package cn.labzen.network.onvif.device;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class OnvifDevice extends Device {

  private final String address;

  public OnvifDevice(String message, String host, String address) {
    super(message, host);
    this.address = address;
  }

  public String getAddress() {
    return address;
  }

  public static class InternalOnvifEnvelope {

    @JacksonXmlProperty(localName = "Body")
    private InternalOnvifBody body;

    public InternalOnvifEnvelope(InternalOnvifBody body) {
      this.body = body;
    }

    public InternalOnvifBody getBody() {
      return body;
    }

    public void setBody(InternalOnvifBody body) {
      this.body = body;
    }
  }

  public static class InternalOnvifBody {

    @JacksonXmlElementWrapper(localName = "ProbeMatches")
    @JacksonXmlProperty(localName = "ProbeMatch")
    private List<InternalOnvifProbeMatch> probes;

    public InternalOnvifBody(List<InternalOnvifProbeMatch> probes) {
      this.probes = probes;
    }

    public List<InternalOnvifProbeMatch> getProbes() {
      return probes;
    }

    public void setProbes(List<InternalOnvifProbeMatch> probes) {
      this.probes = probes;
    }
  }

  public static class InternalOnvifProbeMatch {

    @JacksonXmlProperty(localName = "Types")
    private String type;
    @JacksonXmlProperty(localName = "XAddrs")
    private String addr;

    public InternalOnvifProbeMatch(String type, String addr) {
      this.type = type;
      this.addr = addr;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getAddr() {
      return addr;
    }

    public void setAddr(String addr) {
      this.addr = addr;
    }
  }
}
