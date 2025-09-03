package cn.labzen.network.onvif;

public enum DiscoveryMode {

  ONVIF(3702), UPNP(1900), HIK_VISION(37020);

  private final int port;

  DiscoveryMode(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }
}
