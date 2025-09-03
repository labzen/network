package cn.labzen.network.meta;

import cn.labzen.meta.component.DeclaredComponent;

public class NetworkMeta implements DeclaredComponent {

  @Override
  public String mark() {
    return "Labzen.Network";
  }

  @Override
  public String packageBased() {
    return "cn.labzen.network";
  }

  @Override
  public String description() {
    return "网络包，提供轻量级的Http/Tcp服务器或客户端快速开发，以及快捷的网络功能支撑";
  }
}
