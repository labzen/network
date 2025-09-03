package cn.labzen.network.onvif.device;

public class UpnpDevice extends Device {

  private final String location;
  private final String server;
  private final String usn;
  private final String st;

  public UpnpDevice(String message, String host, String location, String server, String usn, String st) {
    super(message, host);
    this.location = location;
    this.server = server;
    this.usn = usn;
    this.st = st;
  }

  public String getLocation() {
    return location;
  }

  public String getServer() {
    return server;
  }

  public String getUsn() {
    return usn;
  }

  public String getSt() {
    return st;
  }
}
