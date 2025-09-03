package cn.labzen.network.onvif.device;

public abstract class Device {

  private final String message;
  private final String host;

  public Device(String message, String host) {
    this.message = message;
    this.host = host;
  }

  public String getMessage() {
    return message;
  }

  public String getHost() {
    return host;
  }
}
