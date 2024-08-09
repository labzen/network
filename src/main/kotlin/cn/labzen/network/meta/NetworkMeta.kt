package cn.labzen.network.meta

import cn.labzen.meta.component.LabzenComponent

class NetworkMeta : LabzenComponent {

  override fun mark(): String =
    "Labzen.Network"

  override fun packageBased(): String =
    "cn.labzen.network"

  override fun description(): String =
    "网络包，提供轻量级的Http/Tcp服务器或客户端快速开发，以及快捷的网络功能支撑"
}
