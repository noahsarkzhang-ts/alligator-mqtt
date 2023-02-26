# alligator-mqtt
`MQTT` 协议是物联网中用于设备接入的一个重要协议，该序列文章将介绍 `MQTT` 协议相关的知识点，并在 `Moquette` 开源项目基础上实现一个集群版的 `MQTT Broker` 服务器，以供学习的目的使用。

<!-- more -->

这个序列包含如下内容：

**1. 整体架构**
内容包括 `MQTT Broker` 包含的模块、参与的角色、整体流程及相关的数据等等。

**2. QoS**
`MQTT` 协议提供了 3 种消息服务质量等级（Quality of Service），保证了在不同的网络环境下消息传递的可靠性。

**3. Clean Session**
`MQTT` 可以设置是否持久化 `Session` 数据，这样可以保证终端断线之后重新上线可以恢复之前的 `Session` 数据，这个功能可以通过 `Clean Session` 设置。

**4. Retain Message**
`MQTT` 可以设置 `Topic Retain` 消息，每当有新的订阅关系匹配时，所属的新终端便会收到该 `Retain` 消息。如果有需要在新建订阅时，推送一些初始化信息，可以使用该功能。

**5. Will**
`MQTT` 提供了遗嘱 `Will` 功能，可以在终端异常下线时，向特定的 `Will Topic` 发送指定的 `Will Message`, 从而让第三方感知终端的异常下线。

**6. Subscription & CTrie**
在 `Moquette` 中使用类似 `Trie` (单词查找树) 的数据结构来实现 `Topic` 的匹配，通过该数据结构可以实现消息的定位及路由。

**7. 集群间通信**
每一个 `MQTT` 节点只负载了部分客户端，要实现消息在集群中不同客户端间的传递，必须要实现集群间的消息共享，在项目中能，通过搭建一个 `RPC` 的通信网络实现消息的内部传递。

**8. 持久化**
讲述在 `MQTT Broker` 中数据如何进行持久化操作，包括什么数据需要进行持久化 `(WHO)` 以及怎么进行持久化操作 `(HOW)`。

**9. 服务加载**
借助 `Java` 内置的服务加载功能，实现不同集群模式、不同数据库/缓存组件的按需加载。

**10. Metric 指标**
定义 `MQTT Broker` 不同维度的 `Metric` 指标，可以实时检查其内部的运行情况，方便服务的运维监控。

**12. 部署方式**
提供多种部署方式，包括1）单 `Jar` 包部署模式；2）`Docker` 单实例模式；3）`Docker compose` 单机集群模式。


## 文章列表

[1. Mqtt 序列：整体架构][1]

[2. Mqtt 序列：QoS][2]

[3. Mqtt 序列：Clean Session][3]

[4. Mqtt 序列：Retain Message][4]

[5. Mqtt 序列：Will][5]

[6. Mqtt 序列：Subscription & CTrie][6]

[7. Mqtt 序列：集群间通信][7]

[8. Mqtt 序列：持久化][8]

[9. Mqtt 序列：服务加载][9]

[10. Mqtt 序列：SSL][10]

[11. Mqtt 序列：Docker 部署][11]

[12. Mqtt 序列：工程结构][12]

[13. Mqtt 序列：Metric 指标(未完成)][13]


[1]:https://zhangxt.top/2022/12/31/mqtt-architecture-overview/
[2]:https://zhangxt.top/2022/12/31/mqtt-qos/
[3]:https://zhangxt.top/2023/01/01/mqtt-clean-session/
[4]:https://zhangxt.top/2023/01/01/mqtt-retain-message/
[5]:https://zhangxt.top/2023/01/01/mqtt-will/
[6]:https://zhangxt.top/2023/01/14/mqtt-subscription/
[7]:https://zhangxt.top/2023/01/14/mqtt-cluster/
[8]:https://zhangxt.top/2023/02/05/mqtt-persistent/
[9]:https://zhangxt.top/2023/02/11/mqtt-service-loader/
[10]:https://zhangxt.top/2023/02/18/mqtt-ssl/
[11]:https://zhangxt.top/2023/02/18/mqtt-docker/
[12]:https://zhangxt.top/2023/02/26/mqtt-project/
[13]:https://zhangxt.top/2022/12/18/mqtt-series/
