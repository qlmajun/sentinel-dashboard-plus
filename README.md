## sentinel 使用文档

### sentinel dashboard 升级

​        sentinel 提供一个轻量级的开源控制台，是流量控制、熔断降级规则统一配置和管理的入口，它为用户提供了机器自发现、簇点链路自发现、监控、规则配置和推送功能。原始的sentinel控制台配置的规则只存储在内存中，客户端服务配置好规则在应用重启后会出现丢失的问题，sentinel控制台的实时监控数据，默认只存储5分钟以内的数据。生产环境 sentinel dashboard的使用需改造几个特性：规则管理及推送持久化到Nacos、实时监控数据存储在ES、集成saas平台用户鉴权。

​        创建一个maven工程（sentinel-dashboard-plus），引入GitHub上开源的最新release发行版本的sentinel-dashboard.jar到项目工程中，进行sentinel控制台项目的升级改造。

现引入maven依赖：

```xml
 <dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-dashboard</artifactId>
    <version>1.7.2</version>
 </dependency>
```



#### 规则持久化到Nacos

​        sentinel自身就支持了多种不同的数据源来持久化规则配置，目前包括：文件配置、Nacos配置、Zookeeper配置、Apollo配置，现使用Nacos配置进行规则持久化改造。

项目pom.xml文件中引入Nacos存储扩展：

```xml
 <dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
    <version>1.7.2</version>
 </dependency>
```

编写配置规则Nacos存储配置规则推送及控制台界面操作Controller接口代码更改，详细更改查看项目代码。



#### 实时监控数据存储ES

项目pom.xml文件中引入ES依赖：

```xml
<dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
   <version>2.2.0.RELEASE</version>
</dependency>
<dependency>
   <groupId>io.searchbox</groupId>
   <artifactId>jest</artifactId>
   <version>6.3.1</version>
</dependency>
<dependency>
   <groupId>net.java.dev.jna</groupId>
   <artifactId>jna</artifactId>
   <version>5.5.0</version>
</dependency>
```

编写监控数据相关存储操作及控制台请求实时监控数据Controller接口代码更改，详细更改查看项目代码。



#### 集成saas平台用户鉴权

暂未实现 ......



### 微服务应用接入sentinel



* 在微服务应用pom.xml中引入maven依赖：

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
            <version>0.9.0.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-datasource-nacos</artifactId>
        </dependency>
```



* 在微服务应用配置文件中添加配置规则：

  ```properties
  # sentinel dashboard
  spring.cloud.sentinel.transport.dashboard=192.168.32.128:9090
  
  # sentinel datasource nacos
  spring.cloud.sentinel.datasource.ds.nacos.server-addr=192.168.32.128:8848
  spring.cloud.sentinel.datasource.ds.nacos.data-id=${spring.application.name}-flow-rules
  spring.cloud.sentinel.datasource.ds.nacos.group-id=SENTINEL_GROUP
  #限流规则
  spring.cloud.sentinel.datasource.ds.nacos.rule-type=flow
  
  # sentinel datasource nacos
  spring.cloud.sentinel.datasource.ds2.nacos.server-addr=192.168.32.128:8848
  spring.cloud.sentinel.datasource.ds2.nacos.data-id=${spring.application.name}-degrade-rules
  spring.cloud.sentinel.datasource.ds2.nacos.group-id=SENTINEL_GROUP
  #熔断降级规则
  spring.cloud.sentinel.datasource.ds2.nacos.rule-type=degrade
  
  # sentinel datasource nacos
  spring.cloud.sentinel.datasource.ds3.nacos.server-addr=192.168.32.128:8848
  spring.cloud.sentinel.datasource.ds3.nacos.data-id=${spring.application.name}-param-flow-rules
  spring.cloud.sentinel.datasource.ds3.nacos.group-id=SENTINEL_GROUP
  #热点参数限流规则
  spring.cloud.sentinel.datasource.ds3.nacos.rule-type=param_flow
  ```

  

* 登入sentinel控制台，在簇点链路中对需要配置的url资源进行相关规则配置即可。



### 控制规则描述

#### 流控规则

​        流量控制用于监控应用流量的QPS或并发线程数等指标，当达到指定的阈值时对流量进行控制以避免瞬时的流量高峰冲垮从而保障应用的高可用性，同一个资源可以创建多条限流规则，FlowSlot会对该资源的所有规则进行遍历直到有规则触发或所有规则处理完成。

* **QPS流量控制**

  当QPS超过配置的阈值时则采取措施进行流量控制，流量控制效果：快速失败、Warm Up、 排队等待。

  ***快速失败：*** 默认的流量控制方式，当QPS超过配置的阈值后，新的请求就会立即被拒绝，拒绝方式抛出FlowException。

  ***Warm Up：***预热/冷启动方式，当系统长期处于低水位的情况下，当流量突然增加时，直接把系统拉升到高水位可能把系统瞬间压垮。通过”冷启动“让通过的流量缓慢增加，在一定的时间内增加到阈值的上限，给冷系统一个预热的时间，避免冷系统被压垮。

  ***排队等待：***会严格的控制请求通过的时间，即让请求以均匀的速度通过，对应的是漏桶算法。

  

* **并发线程数流量控制**

​        并发线程数限流用于保护业务线程数不被耗尽。例如，当应用所依赖的下游应用由于某种原因导致服务不稳定、响应延迟增加，对于调用者来说，意味着吞吐量下降和更多的线程数占用，极端情况下甚至导致线程池耗尽。Sentinel 并发线程数限流不负责创建和管理线程池，而是简单统计当前请求上下文的线程数目，如果超出阈值，新的请求会被立即拒绝，效果类似于信号量隔离。

#### 降级规则

​        由于调用关系的复杂性，如果调用链路中的某个资源不稳定，最终会导致请求发生堆积。Sentinel熔断降级会在调用链路中的某个资源出现不稳定状态时（例如：调用超时或异常比例升高），对这个资源的请求进行限制让请求快速失败，避免影响到其他的资源而导致级联的错误。当资源被降级后，在接下来的降级时间窗口之内，对该资源的调用都自动熔断（默认行为是抛出DegradeException）。

**降级策略**

* ***平均响应时间(RT)：***

  当1s内持续进入5 个请求，对应的平均响应时间（秒级）均超过阈值（以ms为单位），那在接下来的时间窗口（以s为单位）之内，对这个方法的调用都会自动的熔断（抛出DegradeException）。

* ***异常比例：***

  当资源每秒异常总数占通过量的比值超过阈值之后，资源进入降级状态，即在接下的时间窗口（以s为单位）之内，对这个方法的调用都会自动地返回。异常比率的阈值范围是 `[0.0, 1.0]`，代表 0% - 100%。

* ***异常数：***

  当资源近一分钟的异常数目超过阈值之后会进行熔断，由于统计时间窗口是分钟级别的，若 `timeWindow` 小于 60s，则结束熔断状态后仍可能再进入熔断状态。*异常降级仅针对业务异常，对 Sentinel限流降级本身的异常（BlockException）不生效。*



#### 热点规则

​        热点参数限流会统计传入参数中的热点参数，并根据配置中的限流阈值与模式，对包含热点参数的资源调用进行限流，热点参数限流可以看做一种特殊的流量控制，仅对包含热点参数的资源调用生效。



*详细可产看 Sentinel 官方文档：https://github.com/alibaba/Sentinel/wiki*