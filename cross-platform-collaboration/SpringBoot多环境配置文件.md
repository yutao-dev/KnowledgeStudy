# 0722——针对运维环境交付：SpringBoot 配置文件

#### 前言

SpringBoot 提供了灵活的 application 配置文件激活策略，简单来说，通过 application-xxx.yml+spring.profiles.active 配置，可以指定对应生效的配置文件。

同时通过 ${}形式，以环境变量 > 命令行参数 > 配置文件的形式，进行更为灵活的配置注入

#### 当前问题分析

首先看一下我们当前配置的 application.yml 文件

```yaml
# 服务器基础配置
server:
  # 服务监听端口号
  port: 8080

# Spring 应用配置
spring:
  # 数据源配置
  datasource:
    # JDBC驱动类全限定名（MySQL 8.x+驱动）
    driver-class-name: com.mysql.cj.jdbc.Driver
    # 数据库连接URL参数说明：
    # - ${datasource.host}: 数据库主机地址（从环境变量获取）
    # - ${datasource.port}: 数据库端口（从环境变量获取）
    # - psy_intervene: 数据库名称
    # - useSSL=false: 禁用SSL连接
    # - serverTimezone=UTC: 设置时区为UTC
    # - characterEncoding=utf8: 使用UTF-8字符编码
    url: jdbc:mysql://mysql/psy_intervene?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true
    # 数据库用户名（从环境变量获取）
    username: root
    # 数据库密码（从环境变量获取）
    password: r*****d # 脱敏

    # HikariCP连接池配置
    hikari:
      # 连接池最大连接数
      maximum-pool-size: 20
      # 连接池最小空闲连接数
      minimum-idle: 5
      # 连接空闲超时时间（毫秒）
      idle-timeout: 30000
      # 连接最大生命周期（毫秒）
      max-lifetime: 1800000
      # 连接获取超时时间（毫秒）
      connection-timeout: 30000

  # RabbitMQ消息队列配置
  rabbitmq:
    # RabbitMQ服务主机地址（从环境变量获取） 
    host: 1xxxxxxxxxx5 # 脱敏
    # RabbitMQ服务端口（从环境变量获取）
    port: 5672
    # 连接用户名（从环境变量获取）
    username: admin
    # 连接密码（从环境变量获取）
    password: d********r # 脱敏
    # 虚拟主机路径（用于业务隔离）
    virtual-host: /
    # 消息监听器配置
    listener:
      simple:
        # 消息消费失败重试策略
        retry:
          # 启用重试机制
          enabled: true
          # 最大重试次数
          max-attempts: 3
          # 首次重试间隔（毫秒）
          initial-interval: 1000

  # Redis缓存配置
  data:
    redis:
      # Redis服务器地址（从环境变量获取）
      host: r***s # 脱敏
      password: re*****d # 脱敏
      # Redis服务端口（从环境变量获取）
      port: 6379
      # 连接超时时间（60秒）
      timeout: 60s
      # Lettuce客户端连接池配置
      lettuce:
        pool:
          # 连接池最大活跃连接数
          max-active: 8
          # 连接池最大空闲连接数
          max-idle: 8
          # 连接池最小空闲连接数
          min-idle: 0
          # 获取连接最大等待时间（5秒）
          max-wait: 5s
  ai:
    openai:
      audio:
        speech:
          options:
            model: ${DOUBAO_REPLY_MODEL}
      base-url: ${DOUBAO_BASE_URL}
      api-key: ${DOUBAO_REPLY_API_KEY}

# MyBatis框架配置
mybatis:
  # MyBatis映射文件路径（classpath下的mapper目录）
  mapper-locations: classpath:mapper/*.xml
  # 实体类所在包路径（用于别名配置）
  type-aliases-package: com.yourpackage.entity
  # MyBatis全局配置
  configuration:
    # 开启数据库字段下划线转驼峰命名
    map-underscore-to-camel-case: true
    # 配置MyBatis日志实现（使用标准输出）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# MyBatis-Plus增强配置
mybatis-plus:
  # MyBatis映射文件路径（与mybatis配置相同）
  mapper-locations: classpath:mapper/*.xml
  # 实体类所在包路径（与mybatis配置相同）
  type-aliases-package: com.yourpackage.entity
  # 全局配置
  global-config:
    # 数据库配置
    db-config:
      # ID生成策略：AUTO-数据库ID自增
      id-type: auto
      # 字段插入更新策略：NOT_NULL-非NULL判断
      insert-strategy: NOT_NULL  # 新增配置
      update-strategy: NOT_NULL  # 新增配置
  # MyBatis原生配置
  configuration:
    # 开启数据库字段下划线转驼峰命名
    map-underscore-to-camel-case: true
    # 配置MyBatis日志实现（使用标准输出）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# JWT(JSON Web Token)配置
jwt:
  # JWT加密密钥（32位字符）
  secret-key: my-32-character-ultra-secure-secret-1234567890
  # Token过期时间（毫秒），604800000=7天
  expire-time: 604800000

sms:
  sign-name: 天津翰海星途科技有限公司
  send-message-template-code: SMS_488235025
  access-key-id: ${ALIBABA_CLOUD_ACCESS_KEY_ID}
  access-key-secret: ${ALIBABA_CLOUD_ACCESS_KEY_SECRET}
  end-point: "dysmsapi.aliyuncs.com"

doubao:
  reply:
    model: ${DOUBAO_REPLY_MODEL}
    api-key: ${DOUBAO_REPLY_API_KEY}
    temperature: 0.7
  history-summary:
    model: ${DOUBAO_HISTORY_SUMMARY_MODEL}
    api-key: ${DOUBAO_HISTORY_SUMMARY_API_KEY}
    temperature: 0.7
  base-url: ${DOUBAO_BASE_URL}
  memory:
    max-messages: 100 # 这里表示SpringAI框架允许给AI的最大消息数目
    max-memory-size: 10 # 这里表示我们自己优化的阈值


azure:
  speech-key: ${FEIJIE_AZURE_SPEECH_KEY}
  service-region: eastasia
  token-url: https://eastasia.api.cognitive.microsoft.com/sts/v1.0/issueToken

aliyun:
  green: # 敏感词检测根属性
    access-key-id: ${ALIBABA_CLOUD_ACCESS_KEY_ID}
    access-key-secret: ${ALIBABA_CLOUD_ACCESS_KEY_SECRET}
    region-id: cn-beijing # 默认区域
    endpoint: green-cip.cn-beijing.aliyuncs.com
    user-service-id: chat_detection_pro_01 # 对用户进行敏感词检测
    ai-service-id: aigc_moderation_byllm_01 # 对AI进行敏感词检测
```

- 我们配置文件部分使用了 ${}，通过环境变量注入
- 但是对应的 spring.data.redis.host/password, 以及 mysql 相关配置，虽然注释上写着“通过环境变量注入”，但是依旧使用的是硬编码方式注入。
- 同时 application.yml 文件的硬编码配置，意味着我们本地开发需要通过修改 idea 的命令行参数进行不同的配置文件激活，对于新人，如果没人说，会觉得这个问题比较绕。
- 原来的本地开发进行适配是这样的

  - 选择对应服务
  - 编辑配置
  - 在 `有效配置文件处`，添加生效配置文件的后缀，例如让 application-local.yaml 文件生效，只需要写"local"即可，该配置是通过命令行参数启动的，可以直接覆盖配置文件配置。

![](static/EryybPr8BoeFnYxnW1Lc2Do9nZf.png)

- 通过添加命令行参数，指定 local 配置文件，使得 application-local.yml 文件生效，同时在使用.gitignore 文件忽略 application-local.yml 配置文件，但是需要将 application.yml 文件配置全部复制粘贴过来再输入自己的配置，很麻烦，而且如果 application.yml 文件有更新，很可能会导致本地开发也出现问题

#### 适配方案

1. 我们需要修改 application.yml 文件，通过 ${}，如果通过环境变量，就不需要写在对应的 profiles 激活的文件上
2. 同时在 application.yml 文件中写入 spring.profiles.active，指定 local 文件
3. 在 docker-compose 文件中，继续指定环境变量 SPRING_PROFILES_ACTIVE 为 prod，指定 application-prod.yml 文件为部署时候的生效文件
4. 因为环境变量 > 命令行参数 > application.yml 文件，因此在 docker-compose 文件进行部署时候，会优先扫描 application-prod.yml 文件，作为参数，注入 application.yml 文件中 ${}部分，而对于从环境变量中注入的参数，我们可以在 docker-compose.yml 文件中进行环境变量声明
5. 同时对于本地，如果嫌麻烦，可以不用设置本地的系统用户变量，而是通过 application-local.yml 文件，进行参数注入
6. 同时可以利用 ${DATASOURCE_HOST:127.0.0.1}，注入默认值，当读取不到的时候，直接通过默认值传入，进一步提升灵活性

##### 修改后的 application.yml 文件如下：

```yaml
# 服务器基础配置
server:
  # 服务监听端口号
  port: ${SERVER_PORT:8080} # 如果环境变量读取不到，直接使用默认值8080

# Spring 应用配置
spring:
  # 配置文件指定, 在本地配置为local，在docker-compose.yml配置中则指定环境变量为 dev
  profiles:
    active: local

  # 数据源配置
  datasource:
    # JDBC驱动类全限定名（MySQL 8.x+驱动）
    driver-class-name: com.mysql.cj.jdbc.Driver
    # 数据库连接URL参数说明：
    # - ${datasource.host}: 数据库主机地址（从环境变量获取）
    # - ${datasource.port}: 数据库端口（从环境变量获取）
    # - psy_intervene: 数据库名称
    # - useSSL=false: 禁用SSL连接
    # - serverTimezone=UTC: 设置时区为UTC
    # - characterEncoding=utf8: 使用UTF-8字符编码
    url: jdbc:mysql://${DATASOURCE_HOST:127.0.0.1}:${DATASOURCE_PORT:3306}/psy_intervene?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowPublicKeyRetrieval=true
    # 数据库用户名（从环境变量获取）
    username: ${DATASOURCE_USERNAME} # 敏感信息，只能通过环境变量读取
    # 数据库密码（从环境变量获取）
    password: ${DATASOURCE_PASSWORD} # 敏感信息，只能通过环境变量读取

    # HikariCP连接池配置
    hikari:
      # 连接池最大连接数
      maximum-pool-size: 20
      # 连接池最小空闲连接数
      minimum-idle: 5
      # 连接空闲超时时间（毫秒）
      idle-timeout: 30000
      # 连接最大生命周期（毫秒）
      max-lifetime: 1800000
      # 连接获取超时时间（毫秒）
      connection-timeout: 30000

  # RabbitMQ消息队列配置
  rabbitmq:
    # RabbitMQ服务主机地址（从环境变量获取） 
    host: ${RABBITMQ_HOST:127.0.0.1}
    # RabbitMQ服务端口（从环境变量获取）
    port: ${RABBITMQ_PORT:5672}
    # 连接用户名（从环境变量获取）
    username: ${RABBITMQ_USERNAME}
    # 连接密码（从环境变量获取）
    password: ${RABBITMQ_PASSWORD}
    # 虚拟主机路径（用于业务隔离）
    virtual-host: ${RABBITMQ_VIRTUAL_HOST:/}
    # 消息监听器配置
    listener:
      simple:
        # 消息消费失败重试策略
        retry:
          # 启用重试机制
          enabled: true
          # 最大重试次数
          max-attempts: 3
          # 首次重试间隔（毫秒）
          initial-interval: 1000

  # Redis缓存配置
  data:
    redis:
      # Redis服务器地址（从环境变量获取）
      host: ${REDIS_HOST:127.0.0.1}
      password: ${REDIS_PASSWORD:""}
      # Redis服务端口（从环境变量获取）
      port: 6379
      # 连接超时时间（60秒）
      timeout: 60s
      # Lettuce客户端连接池配置
      lettuce:
        pool:
          # 连接池最大活跃连接数
          max-active: 8
          # 连接池最大空闲连接数
          max-idle: 8
          # 连接池最小空闲连接数
          min-idle: 0
          # 获取连接最大等待时间（5秒）
          max-wait: 5s
  ai:
    openai:
      audio:
        speech:
          options:
            model: ${DOUBAO_REPLY_MODEL}
      base-url: ${DOUBAO_BASE_URL}
      api-key: ${DOUBAO_REPLY_API_KEY}

# MyBatis框架配置
mybatis:
  # MyBatis映射文件路径（classpath下的mapper目录）
  mapper-locations: classpath:mapper/*.xml
  # 实体类所在包路径（用于别名配置）
  type-aliases-package: com.yourpackage.entity
  # MyBatis全局配置
  configuration:
    # 开启数据库字段下划线转驼峰命名
    map-underscore-to-camel-case: true
    # 配置MyBatis日志实现（使用标准输出）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# MyBatis-Plus增强配置
mybatis-plus:
  # MyBatis映射文件路径（与mybatis配置相同）
  mapper-locations: classpath:mapper/*.xml
  # 实体类所在包路径（与mybatis配置相同）
  type-aliases-package: com.yourpackage.entity
  # 全局配置
  global-config:
    # 数据库配置
    db-config:
      # ID生成策略：AUTO-数据库ID自增
      id-type: auto
      # 字段插入更新策略：NOT_NULL-非NULL判断
      insert-strategy: NOT_NULL  # 新增配置
      update-strategy: NOT_NULL  # 新增配置
  # MyBatis原生配置
  configuration:
    # 开启数据库字段下划线转驼峰命名
    map-underscore-to-camel-case: true
    # 配置MyBatis日志实现（使用标准输出）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# JWT(JSON Web Token)配置
jwt:
  # JWT加密密钥（32位字符）
  secret-key: ${jwt.secret-key}
  # Token过期时间（毫秒），604800000=7天
  expire-time: ${jwt.expire-time}

sms:
  sign-name: 天津翰海星途科技有限公司
  send-message-template-code: SMS_488235025
  access-key-id: ${ALIBABA_CLOUD_ACCESS_KEY_ID}
  access-key-secret: ${ALIBABA_CLOUD_ACCESS_KEY_SECRET}
  end-point: "dysmsapi.aliyuncs.com"

doubao:
  reply:
    model: ${DOUBAO_REPLY_MODEL}
    api-key: ${DOUBAO_REPLY_API_KEY}
    temperature: 0.7
  history-summary:
    model: ${DOUBAO_HISTORY_SUMMARY_MODEL}
    api-key: ${DOUBAO_HISTORY_SUMMARY_API_KEY}
    temperature: 0.7
  base-url: ${DOUBAO_BASE_URL}
  memory:
    max-messages: 100 # 这里表示SpringAI框架允许给AI的最大消息数目
    max-memory-size: 10 # 这里表示我们自己优化的阈值


azure:
  speech-key: ${FEIJIE_AZURE_SPEECH_KEY}
  service-region: eastasia
  token-url: https://eastasia.api.cognitive.microsoft.com/sts/v1.0/issueToken

aliyun:
  green: # 敏感词检测根属性
    access-key-id: ${ALIBABA_CLOUD_ACCESS_KEY_ID}
    access-key-secret: ${ALIBABA_CLOUD_ACCESS_KEY_SECRET}
    region-id: cn-beijing # 默认区域
    endpoint: green-cip.cn-beijing.aliyuncs.com
    user-service-id: chat_detection_pro_01 # 对用户进行敏感词检测
    ai-service-id: aigc_moderation_byllm_01 # 对AI进行敏感词检测
```

对应的 docker-compose 文件就不做展示了，通过环境变量指定参数即可

注意：必须指定 SPRING_PROFILES_ACTIVE=dev，保证除开环境变量外，还可以从 application-dev.yml 配置文件读取

##### 修改后的 application-dev.yml 如下:

```yaml
# 部署到线上环境的配置项，这里本地开发就需要填写
jwt:
  secret-key: my-32-character-ultra-secure-secret-1234567890
  expire-time: 604800000
# 因为其他的参数都可以从环境变量中获取，因此不需要在这里指定参数
```

##### 修改后的 application-local.yml 文件如下：

```yaml
# JWT 配置
jwt:
  secret-key: my-32-character-ultra-secure-secret-1234567890  # JWT 签名使用的密钥，请确保其安全性
  expire-time: 604800000  # JWT token 的过期时间，单位为毫秒（例如：7天）

# 数据库连接主机地址
DATASOURCE_HOST: 127.0.0.1  # 数据库服务的IP地址
# 数据库连接端口
DATASOURCE_PORT: 3306  # 数据库服务的端口号
# 数据库用户名
DATASOURCE_USERNAME: root  # 用于连接数据库的用户名
# 数据库密码
DATASOURCE_PASSWORD: abc123321  # 用于连接数据库的用户密码

# Redis 缓存服务主机地址
REDIS_HOST: 127.0.0.1  # Redis 服务的IP地址
# Redis 缓存服务连接密码
REDIS_PASSWORD: ""  # 这里直接注入空字符串，如果没有密码也可以直接连接上

# RabbitMQ 消息队列服务配置
RABBITMQ_HOST: 127.0.0.1  # RabbitMQ 服务的IP地址
RABBITMQ_PORT: 5672  # RabbitMQ 服务的端口号
RABBITMQ_USERNAME: admin  # RabbitMQ 登录用户名
RABBITMQ_PASSWORD: admin  # RabbitMQ 登录密码
RABBITMQ_VIRTUAL_HOST: psy-intervention  # RabbitMQ 虚拟主机名称

# 服务监听端口
SERVER_PORT: 8080  # 应用启动时监听的端口号
```

这样修改后，将会有以下优点：

1. 配置文件在部署环境中，真正做到读取环境变量，而不是硬编码。更加安全。
2. 本地配置文件则是优先读取 application-local.yml 文件，与部署环境互不冲突
3. 同时只用修改通过 ${}注入的变量，不再需要手动配置系统环境变量
