# Microservice

## Spring Cloud

### Zuul的工作原理

Zuul的核心是一系列的过滤器，它们在HTTP的请求与响应的路由过程中发挥作用。

Zuul过滤器的核心是：
* Type:
* Execution Order:
* Criteria:
* Action:

Zuul 提供一个动态读取、编译、运行过滤器的框架，过滤器之间不能直接通信，它们是通过`RequestContext`状态共享实现的，每个请求的`RequestContext`是唯一的。

Filters目前是用`Groovy`写的，但是支持任何基于JVM-based的语言。过滤器的源码位于Zuul服务器的指定目录下，Zuul服务器会定期检测变化。修改后，Zuul会从硬盘读取文件并编译，接下来要处理的请求会调用这些过滤器。

**Filter Types**

* PRE Filters: 在路由之前执行，使用例子：请求鉴权，选择服务器，日志信息记录
* ROUTING Filters: 在进行路由时执行，原始http请求在这里被创建，并使用Apache HttpClient或Netflix Ribbon进行发送。
* POST Filters: 在请求路由后执行，使用例子：给响应添加标准的HTTP头，聚合统计数据与metrics，streaming the response from the origin to the client.
* ERROR Filters：上述阶段发生错误时执行

**Netflix实践介绍**

Zuul结合其他的Netflix OSS组件能够提供insight, flexibility, and resilliency.
* Hystrix： wrap calls to origins, to shed and prioritize traffic when issues occur
* Ribbon: client for all outbond requests from Zuul, 提供网络性能、错误的详细信息，也能实现软件负载均衡
* Turbine：聚合实时的细粒度度量，可以快速观察或响应问题
* Archaius：处理配置，提供动态改变属性的能力

**Surgical Routing 外科手术式路由**

可以创建过滤器，将特定的消费者或设备路由到不同的API集群。

**压力测试**

设置一个自动的过程，在Zuul过滤器中使用动态的Archaius的配置，平稳的增加路由到一个小集群的流量，测试性能，调整自动扩容策略。

**Multi-Region Resiliency**

跨区域冗余

* Debug日志信息：默认开启
  * ZUUL_DEBUG: Filters的信息，修改RequestContext
  * REQUEST_DEBUG: 关于HTTP请求与响应的信息
    * REQUEST:
    * ZUUL:
    * ORIGIN_RESPONSE:
    * OUTBOUND:

### 常用组件及功能

* Spring Cloud Config: 外部的中央配置管理，通过git实现，配置资源直接与Spring的`Environment`映射，也可以被non-Spring应用使用

* Spring Netflix
  * Eureka: 服务发现与服务注册
  * Hystrix：断路器
  * Ribbon：客户端负载均衡
  * Archaius：外部配置
  * Zuul：路由与过滤

* Spring Cloud Bus：链接服务与服务实例的分布式消息时间总线，在跨集群状态传递中很有用

* Spring Cloud Cloudfoundry：集成应用于Pivotal Cloud Foundry，提供服务发现，很容易实现SSO和OAuth2

* Spring Cloud Open Service Broker: 提供一个构建实现了Open Service Broker API服务代理的开始点。

* Spring Cloud Cluster: 选举及通用状态模式的抽象与实现，为Zookeeper, Redis， Hazecast， Consul使用。

* Spring Cloud Consul: 服务发现与配置管理，使用Hashicorp Consul

* Spring Cloud Security：提供OAuth2的rest客户端负载均衡的支持及Zuul代理中header中继的认证

* Spring Cloud Sleuth：分布式追踪Spring Cloud应用工具，兼容Zipkin，HTrace和基于日志（ELK）的追踪

* Spring Cloud Data Flow：云原生的服务编排工具，在现代运行时环境上适用于组合式的微服务，易用的DSL，拖拽的GUI，REST-APIs，基于数据流

* Spring Cloud Stream：轻量级的事件驱动微服务框架，能快速开发出连接外部系统的应用，简单声明式的模型，通过Apache Kafka或RabbitMQ在Spring Boot应用之间收发消息

* Spring Cloud Stream App Starters：基于Spring Boot的集成应用，提供与外部应用进行集成。

* Spring Cloud Task：短暂的微服务框架用来快速开发能处理有限数量数据的应用。给Spring Boot应用添加功能或非功能特性只需要简单的声明即可。

* Spring Cloud Zookeeper：通过Apache Zookeeper进行服务发现与配置管理

* Spring Cloud AWS：与托管AWS的服务进行集成

* Spring Cloud Connectors：使得各种平台的Paas应用更容易连接到类似于数据库和消息代理的后端服务

* Spring Cloud Starters：Spring Boot风格的启动应用，简易的Spring Cloud消费应用的依赖管理（Angel.SR2后与其他项目合并，不再是单独的项目）

* Spring Cloud CLI：使用Groovy快速Spring Cloud组件的Spring Boot CLI插件

* Spring Cloud Contract：帮助用户快速实现Consumer Driven Contracts approach的umbrella projec

* Spring Cloud Gateway：基于Project Reactor的只能和可编程的路由

* Spring Cloud OpenFeign：提供通过自动配置和与Spring环境及其他Spring编程模型绑定的集成

* Spring Cloud Pipelines：提供一个 opinionated deployment pipeline，保证应用可以热部署，以及出错时可以快速回滚

* Spring Cloud Function：通过函数提升商业逻辑实现，支持无服务或私有云的统一变成模型