package cn.labzen.network.onvif.device;

public class HikVisionDevice extends Device{

  private final String type;
  private final String description;
  private final String sn;
  private final String ipv4;
  private final String gateway;
  private final int port;
  private final String mac;
  private final String version;
  private final String bootTime;
  private final String safeCode;

  public HikVisionDevice(String message,
                         String host,
                         String type,
                         String description,
                         String sn,
                         String ipv4,
                         String gateway,
                         int port,
                         String mac,
                         String version,
                         String bootTime,
                         String safeCode) {
    super(message, host);
    this.type = type;
    this.description = description;
    this.sn = sn;
    this.ipv4 = ipv4;
    this.gateway = gateway;
    this.port = port;
    this.mac = mac;
    this.version = version;
    this.bootTime = bootTime;
    this.safeCode = safeCode;
  }

  public String getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public String getSn() {
    return sn;
  }

  public String getIpv4() {
    return ipv4;
  }

  public String getGateway() {
    return gateway;
  }

  public int getPort() {
    return port;
  }

  public String getMac() {
    return mac;
  }

  public String getVersion() {
    return version;
  }

  public String getBootTime() {
    return bootTime;
  }

  public String getSafeCode() {
    return safeCode;
  }
}
