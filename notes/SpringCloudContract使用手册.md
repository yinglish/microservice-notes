# Spring Cloud Contract 使用手册

## 简介

Spring Cloud Contract将测试驱动开发(Test-Driven Development, TDD)提升到了软件架构级别，可以使我们实施消费者驱动(consumer-driven)和服务提供者驱动(producer-driven)契约测试。

### 为什么需要Spring Cloud Contract

假设有一个系统由多个微服务构成，如下图所示：

![microservices-1](images/microservices-1.png)

如果我们要对上图中最左上角的应用进行测试，有两种选择：部署所有的微服务，进行端到端的测试；在单元和集成测试中Mock其他微服务。这两种方法各有优缺点。

#### 部署所有的微服务并进行端到端的测试

优点：

* 模拟了生产环境
* 测试了服务之间的真是情况

缺点：

* 为了测试一个微服务，需要部署所有与之关联的微服务、数据库及其他项目等
* 测试该微服务的环境不能同时进行其他的测试
* 需要较长的时间
* 测试结果的反馈需要一个较长的时间
* 难以调试

#### 在单元和集成测试中Mock其他微服务

优点：

* 提供快速的反馈
* 没有其他基础设施的要求

缺点：

* 服务创建的stubs可能与真实情况没有关系
* 通过了所有测试，但在生产环境可能还是失败

Spring Cloud Contract就是为了解决前面的问题出现的。它的主要思想是在不需要部署所有的微服务的前提下，能够快速给出反馈。使用了它的stubs，测试微服务所需要的只是该服务直接使用的那些应用，如下图所示：

![microservices-2](images/microservices-2.png)

Spring Cloud Contract能保证测试微服务时所使用的stubs是由该服务所调用的服务创建的，并且他们已经通过了服务提供方的测试。

Spring Cloud Contract的主要目标是：

* 保证HTTP和Messaging stubs的表现和服务端的表现是一致的
* 提升ATDD (acceptance test-driven development)方法和微服务架构风格
* 提供一种使契约的变化立刻体现在服务提供与消费方的方式
* 自动生成服务端的测试代码

### 什么是契约

在微服务中，服务的消费方应该清晰的定义出期望得到的结果，而且应该以一种格式化的方式表达出来，这就是契约出现的原因。契约就是服务的提供与消费双方关于API或消息通信方式的一种约定。假设你希望向服务方发出一个带有客户公司ID和希望贷款的数量的请求，通过`/fraudcheck`URL和`PUT`方法，服务方会给出是否为欺诈的结果。服务方需要给出一个这样的契约：

```yaml
request: # (1)
  method: PUT # (2)
  url: /yamlfraudcheck # (3)
    body: # (4)
      "client.id": 1234567890
      loanAmount: 99999
    headers: # (5)
      Content-Type: application/json
    matchers:
      body:
        - path: $.['client.id'] # (6)
          type: by_regex
          value: "[0-9]{10}"
response: # (7)
  status: 200 # (8)
  body: # (9)
    fraudCheckStatus: "FRAUD"
    "rejection.reason": "Amount too high"
  headers: # (10)
    Content-Type: application/json
# 作为消费者，在集成测试中发起一个请求时：
#
# (1) - 如果消费者发起一个请求
# (2) - 使用‘PUT’方法
# (3) - 请求路径是"/yamlfraudcheck"
# (4) - 使用的是JSON格式的body
# * 有一个字段 `client.id`
# * 有一个字段 `loanAmount` 且值为 `99999`
# (5) - 信息头中 `Content-Type` 值为 `application/json`
# (6) - 并且 `client.id` 的值为正则表达式 `[0-9]{10}`
# (7) - 响应的结果是
# (8) - 状态码为 `200`
# (9) - JSON body是
#  { "fraudCheckStatus": "FRAUD", "rejectionReason": "Amount too high" }
# (10) - 头信息 `Content-Type` 等于 `application/json`
#
# 在服务提供方，自动生成测试：
#
# (1) - 请求被发送至消费提供方：
# (2) - 使用的是'PUT'方法
# (3) - URL地址是 "/yamlfraudcheck"
# (4) - JSON格式的body
# * 字段 `client.id` 值为 `1234567890`
# * 字段 `loanAmount` 值为 `99999`
# (5) - 头信息 `Content-Type` 等于 `application/json`
# (7) - 测试会通过，如果响应信息是：
# (8) - 状态码 `200`
# (9) - JSON body是
#  { "fraudCheckStatus": "FRAUD", "rejectionReason": "Amount too high" }
# (10) - 头信息 `Content-Type` 等于 `application/json`
```

## 快速开始

Spring Cloud Contract要设计到至少两个应用：服务提供方与服务消费方。

下面的UML图描述了Spring Cloud Contract中各部分的关系。

![Getting started first application](images/getting-started-three-second.png)

### 服务提供方

在服务提供方的应用中提供以下的依赖：

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-contract-verifier</artifactId>
  <scope>test</scope>
</dependency>
```

还需要在项目的`pom`文件中添加Spring Cloud Contract相关的构建插件：

```xml
<plugin>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-contract-maven-plugin</artifactId>
  <version>${spring-cloud-contract.version}</version>
  <extensions>true</extensions>
</plugin>
```

接下来需要在项目中添加所需要的契约文件，一般该文件位于`$rootDir/src/test/resources/contracts`目录下，文件名不一定要求是`contract.yml`、`contract.yaml`、`contracts.yml`、`contracts.yaml`等。

对于HTTP stubs，一个契约需要定义的内容有请求（包含HTTP方法、URLs、headers、状态码等）与响应。例如：

```yaml
request:
  method: PUT
  url: /fraudcheck
  body:
    "client.id": 1234567890
    loanAmount: 99999
  headers:
    Content-Type: application/json
  matchers:
    body:
      - path: $.['client.id']
        type: by_regex
        value: "[0-9]{10}"
response:
  status: 200
  body:
    fraudCheckStatus: "FRAUD"
    "rejection.reason": "Amount too high"
  headers:
    Content-Type: application/json;charset=UTF-8
```

运行`./mvnw clean install`命令，会自动的生成测试代码来验证应用是否与契约保持一致。默认情况下，生成的测试代码位于`org.springframework.cloud.contract.verifier.tests.`包下。

默认的测试模式是HTTP契约下使用`MockMvc`，生成的代码类似于：

```java
@Test
public void validate_shouldMarkClientAsFraud() throws Exception {
    // given:
    MockMvcRequestSpecification request = given()
        .header("Content-Type", "application/vnd.fraud.v1+json")
        .body("{\"client.id\":\"1234567890\",\"loanAmount\":99999}");
    // when:
    ResponseOptions response = given().spec(request)
        .put("/fraudcheck");
    // then:
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.header("ContentType")).matches("application/vnd.fraud.v1.json.*");
    // and:
    DocumentContext parsedJson = JsonPath.parse(response.getBody().asString());
    assertThatJson(parsedJson).field("['fraudCheckStatus']").matches("[AZ]{5}");
    assertThatJson(parsedJson).field("['rejection.reason']").isEqualTo("Amount too high");
}
```

因为当前应用的功能还没有实现，所以测试无法通过。为了使得测试通过，需要实现能够实例契约中关于HTTP请求的功能，还需要在`pom`文件中配置基础测试类的路径等信息：

```yaml
<build>
  <plugins>
    <plugin>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-contract-maven-plugin</artifactId>
      <version>2.1.2.RELEASE</version>
      <extensions>true</extensions>
      <configuration>
        <baseClassForTests>com.example.contractTest.BaseTestClass</baseClassForTests> # 指定基础测试类
      </configuration>
    </plugin>
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
    </plugin>
  </plugins>
</build>
```

一个最基本的基础测试类是这样的：

```java

package com.example.contractTest;
import org.junit.Before;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
public class BaseTestClass {
    @Before
    public void setup() {
        RestAssuredMockMvc.standaloneSetup(new FraudController());
    }
}

```

### 服务消费方

Spring Cloud Contract的Stub Runner在集成测试中运行WireMock实例模拟真实的服务。

首先需要在消费者应用的`pom`文件中添加Spring Cloud Contract Stub Runner的依赖：

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-contract-stub-runner</artifactId>
  <scope>test</scope>
</dependency>
```

有两种方式从Maven仓库中获得由服务提供方生成的stubs：

* 拉取服务提供方的代码，添加契约，然后运行以下命令：
  ```shell
  cd local-http-server-repo
  ./mvnw clean install -DskipTests
  ```
* 从远程仓库中获取已经存在的服务提供方的服务stubs，这个需要在配置文件中添加：
```yaml
stubrunner:
  ids: 'com.example:http-server-dsl:+:stubs:8080'
  repositoryRoot: https://repo.spring.io/libs-snapshot
```

接下来就可以在测试上写上`@AutoConfigureStubRunner`，并在注解中提供Spring Cloud Contract Stub Runner的`group-id`和` artifact-id`，例如：

```java
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment=WebEnvironment.NONE)
@AutoConfigureStubRunner(ids = {"com.example:http-server-dsl:+:stubs:6565"},
  stubsMode = StubRunnerProperties.StubsMode.LOCAL)
public class LoanApplicationServiceTests {
  ...
}
```

## 功能特性

### Contract DSL

Spring Cloud Contract支持用多种语言编写，支持的语言有：Groovy、YAML、Java、Kotlin。但我们推荐的方式是使用YAML，所以本手册的例子契约均是使用YAML编写。

Spring Cloud Contract也支持在同一个文件中定义多个契约，一个典型的契约定义如下：

```yaml
description: Some description
name: some name
priority: 8
ignored: true
request:
  url: /foo
  queryParameters:
    a: b
    b: c
  method: PUT
  headers:
    foo: bar
    fooReq: baz
  body:
    foo: bar
  matchers:
    body:
      - path: $.foo
      type: by_regex
      value: bar
    headers:
      - key: foo
      regex: bar
response:
  status: 200
  headers:
    foo2: bar
    foo3: foo33
    fooRes: baz
  body:
    foo2: bar
    foo3: baz
    nullValue: null
  matchers:
    body:
      - path: $.foo2
        type: by_regex
        value: bar
      - path: $.foo3
        type: by_command
        value: executeMe($it)
      - path: $.nullValue
        type: by_null
        value: null
    headers:
      - key: foo2
        regex: bar
      - key: foo3
        command: andMeToo($it)
```

在契约中，常见的顶层元素有：

* Description：在契约中可以添加一段文本作为该契约的描述。
* Name：契约的名称，如果契约的名称是`should register a user`，自动生成的测试代码为`validate_should_register_a_user`，在WireMock stub里面生成的stub的名字为`should_register_a_user.json`。
* Ignoring Contracts：如果想要忽略某个契约，可以在插件配置中设置或者在契约中设置`ignored`属性值为`true`。
* Contracts in Progress：`inProgress`值为`true`时，不会生成服务提供方的测试代码，只会生成对应的stubs。
* Passing Values from Files：可以通过文件向契约中传值，例如，在契约文件的同一文件夹下有`request.json`和`response.json`，`yml`文件的内容可以是
```yaml
request:
  method: GET
  url: /foo
  bodyFromFile: request.json
response:
  status: 200
  bodyFromFile: response.json
```
其中JSON文件的内容是：

```json
// request.json
{
  "status": "REQUEST"
}
```

```json
// response.json
{
  "status": "RESPONSE"
}
```

如果需要以二进制的形式向契约中传值，可以在YAML中使用`bodyFromFileAsBytes`字段。

### Contracts for HTTP

Spring Cloud Contract用来验证通过REST或HTTP通信的应用，使得符合契约`request`部分的请求，服务提供方的响应与`response`保持一致。通过契约能够生成WireMock stubs，对于符合契约的请求，能够从服务端得到合适的响应。

#### HTTP顶级元素

在契约的定义中，顶级的元素有：

* request: 强制
* response: 强制
* priority：可选（数组越小，优先级越高）

#### HTTP 请求

在HTTP协议中，只要求请求中有请求方法与URL，在契约中的`request`部分关于这些信息是强制有的，如：

```yaml
method: PUT
url: /foo
```

`url`既可以是相对路径也可以是绝对路径，推荐使用`urlPath`字段，如：

```yaml
request:
  method: PUT
  urlPath: /foo
```

`request`还可以包含查询参数，例如：

```yaml
request:
...
queryParameters:
  a: b
  b: c
```

`request`还可以包含额外的请求头信息，如：

```yaml
request:
...
headers:
  foo: bar
  fooReq: baz
```

`request`还可以包含额外的请求cookies信息，如：

```yaml
request:
...
cookies:
  foo: bar
  fooReq: baz
```

`request`还可以包含额外的请求体信息，如：

```yaml
request:
...
  body:
  foo: ba
```

`request`还可以包含multipart元素，如：

```yaml
request:
  method: PUT
  url: /multipart
  headers:
    Content-Type: multipart/form-data;boundary=AaB03x
  multipart:
    params:
      # key (parameter name), value (parameter value) pair
      formParameter: '"formParameterValue"'
      someBooleanParameter: true
    named:
      - paramName: file
        fileName: filename.csv
        fileContent: file content
  matchers:
    multipart:
      params:
        - key: formParameter
          regex: ".+"
        - key: someBooleanParameter
          predefined: any_boolean
      named:
        - paramName: file
          fileName:
            predefined: non_empty
          fileContent:
            predefined: non_empty
response:
  status: 200
```

#### HTTP 响应

响应必须包含状态码，还可以包含其他的一些信息，如：

```yaml
response:
...
status: 200
```

除了状态码，响应还可以包含的信息有头信息、cookies、体信息，这些信息的格式与请求中的格式是一致的。

#### 动态属性

契约还包含了一些动态属性，如时间戳、ID等。也可以在契约中使用正则表达式。

### Spring Cloud Contract Stub Runner

在使用Spring Cloud Contract Verifier的时候需要解决的一个问题是将服务端生成的WireMock JSON stubs传递给客户端。为了避免通过手动复制的方式解决这个问题，需要使用Spring Cloud Contract Stub Runner。

#### 将Stubs以JARs包的方式进行发布

最简单的方式就是将stubs以jar包的方式发布到仓库中，例如maven仓库。在maven中添加一下配置：

```xml
<!-- First disable the default jar setup in the properties section -->
<!-- we don't want the verifier to do a jar for us -->
<spring.cloud.contract.verifier.skip>true</spring.cloud.contract.verifier.skip><!-- Next add the assembly plugin to your build -->
<!-- we want the assembly plugin to generate the JAR -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <executions>
        <execution>
        <id>stub</id>
        <phase>prepare-package</phase>
        <goals>
            <goal>single</goal>
        </goals>
        <inherited>false</inherited>
        <configuration>
            <attach>true</attach>
            <descriptors>
                ${basedir}/src/assembly/stub.xml
            </descriptors>
        </configuration>
        </execution>
    </executions>
    </plugin>
<!-- Finally setup your assembly. Below you can find the contents of
src/main/assembly/stub.xml -->
<assembly
xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.3"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/plugins/maven-assemblyplugin/assembly/1.1.3 https://maven.apache.org/xsd/assembly-1.1.3.xsd">
    <id>stubs</id>
    <formats>
        <format>jar</format>
    </formats>
    <includeBaseDirectory>false</includeBaseDirectory>
    <fileSets>
        <fileSet>
            <directory>src/main/java</directory>
            <outputDirectory>/</outputDirectory>
            <includes>
            <include>**com/example/model/*.*</include>
            </includes>
        </fileSet>
        <fileSet>
            <directory>${project.build.directory}/classes</directory>
            <outputDirectory>/</outputDirectory>
            <includes>
                <include>**com/example/model/*.*</include>
            </includes>
        </fileSet>
        <fileSet>
            <directory>${project.build.directory}/snippets/stubs</directory>
            <outputDirectory>METAINF/${project.groupId}/${project.artifactId}/${project.version}/        mappings</    outputDi
            rectory>
            <includes>
                <include>**/*</include>
            </includes>
        </fileSet>
        <fileSet>
            <directory>${basedir}/src/test/resources/contracts</directory>
            <outputDirectory>METAINF/${project.groupId}/${project.artifactId}/${project.version}/            contracts</outputD
            irectory>
            <includes>
                <include>**/*.groovy</include>
            </includes>
        </fileSet>
    </fileSets>
</assembly>
```

##### Stub Runner Core

Stub Runner Core运行服务提供方的stubs，可以将stubs理解为服务方的使用契约，即stub-runner是消费驱动契约的一个实现。Stub Runner自动的下载依赖的stubs，并依据stub启动一个WireMock服务。

使用时可以在项目的`pom`文件中添加：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>beer-api-producer-restdocs</artifactId>
    <classifier>stubs</classifier>
    <version>0.0.1-SNAPSHOT</version>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>*</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.example.thing1</groupId>
    <artifactId>thing2</artifactId>
    <classifier>superstubs</classifier>
    <version>1.0.0</version>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>*</groupId>
            <artifactId>*</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

##### 配置HTTP Server Stubs

```java
@CompileStatic
static class HttpsForFraudDetection extends WireMockHttpServerStubConfigurer {
    private static final Log log = LogFactory.getLog(HttpsForFraudDetection)
    @Override
    WireMockConfiguration configure(WireMockConfiguration httpStubConfiguration,
        HttpServerStubConfiguration httpServerStubConfiguration) {
        if (httpServerStubConfiguration.stubConfiguration.artifactId ==
            "fraudDetectionServer") {
            int httpsPort = SocketUtils.findAvailableTcpPort()
            log.info("Will set HTTPs port [" + httpsPort + "] for fraud detection
            server")
            return httpStubConfiguration
            .httpsPort(httpsPort)
        }
        return httpStubConfiguration
    }
}
```

##### 运行Stubs

stubs是以JSON文档的形式定义的，它的语法可以参考WireMock文档，例如：

```json
{
    "request": {
        "method": "GET",
        "url": "/ping"
    },
    "response": {
        "status": 200,
        "body": "pong",
        "headers": {
            "Content-Type": "text/plain"
        }
    }
}
```

##### 查看注册的映射

所有的调用服务的stub在`__/admin/`端点暴露了一个映射的列表，可以使用`mappingsOutputFolder`属性将这些信息写入到文件中。

#### 在Spring Cloud中使用Stub Runner

Spring Runner可以与Spring Cloud集成。

## 常见问题

### 最佳实践

### FAQs
