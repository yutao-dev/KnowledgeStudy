# 0710——框架使用：V1 SpringAI 的初步用法

### 前言

SpringAI 是 Spring 生态中的 AI 框架，其提供了高度封装的 AI 调用方法，让开发者可以快速继承 AI 大模型调用。本文将由浅入深，集成 SpringAI 框架并快速搭建出可以使用的聊天助手。

### 快速开始

#### 依赖集成

##### SpringBoot 模板构建

##### 选择 SpringBoot 最新版本 3.5.3

##### 在选择启动器中勾选上 SpringWeb，OpenAI

最终的依赖如下

```xml
_<!-- 在 properties 块中添加 -->_
<properties>
    <java.version>17</java.version>
    <spring-ai.version>1.0.0</spring-ai.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>


_<!-- 在 dependencies 块中添加 -->_
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

</dependencies>
```

#### 配置文件配置

##### 全面版

```yaml
spring:
  ai:
    openai:
#      api-key: ${TEAM_OPEN_API_KEY} # API密钥，用于身份认证，推荐使用环境变量配置
      api-key: sk***b # 当前使用的API密钥
#      base-url: ${TEAM_OPEN_BASE_URL} # 基础URL，用于指定API服务地址，可替换为其他OpenAI兼容接口
      base-url: https://api.deepseek.com # DeepSeek提供的兼容OpenAI API的地址
      chat:
        options:
          model: deepseek-chat # 指定使用的模型名称，deepseek-chat是DeepSeek的基础聊天模型
          temperature: 0.7 # 控制输出随机性，值越高输出越随机（范围0~1）
          max-tokens: 2048 # 最大响应token数，控制回答长度
          top-p: 1.0 # 核采样参数，与temperature类似但更精细（范围0~1）
          frequency-penalty: 0.0 # 对高频词汇进行惩罚，减少重复内容（范围-2~2）
          presence-penalty: 0.0 # 对已出现过的词汇进行惩罚（范围-2~2）
```

##### 基础版

```yaml
spring:
  ai:
    openai:
#      api-key: ${TEAM_OPEN_API_KEY} # API密钥，用于身份认证，推荐使用环境变量配置
      api-key: sk***b # 当前使用的API密钥
#      base-url: ${TEAM_OPEN_BASE_URL} # 基础URL，用于指定API服务地址，可替换为其他OpenAI兼容接口
      base-url: https://api.deepseek.com # DeepSeek提供的兼容OpenAI API的地址
      chat:
        options:
          model: deepseek-chat # 指定使用的模型名称，deepseek-chat是DeepSeek的基础聊天模型
```

#### ChatClient 配置

##### ChatClient 介绍

ChatClient 是 SpringAI 在 ChatModel 基础上提供的更高级别的封装，在 SpringAI 中不会主动提供，而是需要我们手动搭建，其可以通过 Builder 形式进行创建。

##### 和 ChatModel 的区别

ChatModel 则是 ChatClient 的底层级别的实现类接口，ChatClient 的功能底层都是调用的 ChatModel。

##### 手动引入实现类

```java
/**
 * @author by 王玉涛
 * @Classname ChatModelConfig
 * @Description 配置类，用于定义与AI聊天模型交互的客户端Bean。
 *              主要作用是创建并配置ChatClient实例，该实例封装了与OpenAI模型通信的逻辑。
 * @Date 2025/7/10 16:08
 */
@Configuration
public class ChatModelConfig {

    /**
     * 创建一个ChatClient Bean，用于与OpenAI聊天模型进行交互。
     *
     * @param openAiChatModel 注入的OpenAI聊天模型实例，由Spring容器自动装配。
     * @return 返回配置好的ChatClient实例。
     *
     * 此方法使用ChatClient.builder构建器模式创建客户端，
     * 并设置默认系统提示信息为“你是一个心理专家，经常使用简单的话语来安慰别人，通常不超过200字”。
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个心理专家，你经常使用简单的话语来安慰别人，这通常不超过200字")
                .build();
    }
}
```

#### 快速开始

##### Controller 准备

```java
/**
 * @author by 王玉涛
 * @Classname ChatController
 * @Description 聊天功能控制器，提供基础的对话接口
 * @Date 2025/7/10 15:16
 */
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    /**
     * 注入的ChatClient实例，用于与AI模型进行交互
     */
    private final ChatClient chatClient;

    /**
     * GET接口，访问路径为 /api/chats/hi
     * 功能：向AI模型发送问候语并返回响应内容
     */
    @GetMapping("/hi")
    public String hi() {
        // 发送提示信息给AI模型，并获取回复内容
        return chatClient
                .prompt("你好！你是谁呀?")
                .call()
                .content();
    }
}
```

##### 访问 localhost:8080/api/users/hi 接口即可获取到回复

##### 流式输出

当前我们使用的是阻塞式输出，也就是说，需要等到模型回复完全生成才可以获取回复。

我们可以切换为流式输出，从而降低等待时间，提升体验。

但是这样会有一个问题，就是页面输出会变成乱码，输出如下

```
鍡▇鎴戞槸浣犵殑蹇冪悊灏忎紮浼淬€傜湅鍒颁綘涓诲姩鎵撴嫑鍛肩湡寮€蹇冨憿锛佹瘡涓汉閮介渶瑕佽鍊惧惉鍜岀悊瑙ｏ紝鑰屾垜寰堟効鎰忓仛閭ｄ釜瀹夐潤鐨勬爲娲炪€傛湁浠€涔堟兂鑱婄殑閮藉彲浠ュ憡璇夋垜鍝︼紝涓嶇敤瑙夊緱鏈夊帇鍔涖€傝浣忥紝浣犳鍒荤殑鎰熷彈閮芥槸鍊煎緱琚噸瑙嗙殑銆�
```

这是因为 Flux 输出是基于事件流的形式输出，当不指定输出编码的时候，就会出现这种异常，只需要在 @GetMapping 中进行指定即可。

```java
/**
 * GET接口，访问路径为 /api/chats/hi-stream
 * 功能：向AI模型发送问候语并返回流式响应内容
 */
@GetMapping(value = "/hi-stream", produces = "text/html;charset=utf-8")
public Flux<String> hiStream() {
    return chatClient
            .prompt("你好！你是谁呀?")
            .stream()
            .content();
}
```

这样即可进行流式输出了~

### 进阶部分

#### 环绕日志引入

SpringAI 提供了日志记录封装，其基于 AOP 进行环绕增强，我们这里使用简单日志记录即可

```java
@Bean
public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
    return ChatClient.builder(openAiChatModel)
            .defaultSystem("你是一个心理专家，你经常使用简单的话语来安慰别人，这通常不超过200字")
            .defaultAdvisors(new SimpleLoggerAdvisor()) // 在这里引入环绕日志增强
            .build();
}
```

但是环绕日志记录级别为 debug，因此我们需要在该日志增强类所在的包下进行日志级别改动，改为 debug 级别即可看到日志输出

```yaml
logging:
  level:
    org.springframework.ai.chat.client.advisor: debug _# 开启聊天控制器的日志_
```

#### 会话记忆存储

##### 前言

当前我们使用到的大模型，并没有对应的上下文记忆，我们需要添加会话记忆方式。

##### 如何实现

SpringAI 提供了抽象接口 ChatMemory 接口，如果我们想要手动实现会话历史记忆，我们可以通过实现这个接口来进行会话记忆

##### 简单方式

SpringAI 提供了一个默认的实现方式，InMemoryChatMemory，其底层基于 Map 方式实现，当应用存储，就会丢失全部记忆。作为简单的聊天机器人，可以这么使用，但是一般情况下，我们依旧推荐手动实现会话记忆存储。

注意：1.0.0 正式版本中有所改动，其通过 MessageWindowChatMemory 作为工厂进行构造，通过注入 InMemoryChatMemoryRepository 实现类进行以上类似的实现。

###### 代码实现

```java
/**
 * 创建一个ChatMemory Bean，用于管理对话历史的存储与读取。
 *
 * @return 返回配置好的ChatMemory实例。
 *
 * 此方法使用MessageWindowChatMemory.builder构建器模式创建聊天记忆组件，
 * 并设置使用InMemoryChatMemoryRepository作为底层存储实现，适用于短期会话场景。
 */
@Bean
public ChatMemory chatMemory() {
    return MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();
}
```

```java
@Bean
public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
    return ChatClient.builder(openAiChatModel)
            .defaultSystem("你是一个心理专家，你经常使用简单的话语来安慰别人，这通常不超过200字")
            .defaultAdvisors(
                    new SimpleLoggerAdvisor(),
                    MessageChatMemoryAdvisor.builder(chatMemory()).build()
            )
            .build();
}
```

注意：这里只是传入了一个会话记忆的存储方式，而我们还需要一个会话 id 来进行会话区分，防止 AI 出现记忆串联。

```java
/**
 * GET接口，访问路径为 /api/chats/hi-stream-2
 * 功能：向AI模型发送用户指定的提示语，并根据提供的会话ID进行上下文关联，
 * 返回流式响应内容。
 *
 * @param id      会话唯一标识符，用于维持对话上下文状态
 * @param prompt  用户输入的提示语，作为AI回复的依据
 * @return        返回Flux<String>类型，提供HTML格式的流式响应内容
 */
@GetMapping(value = "/hi-stream-2", produces = "text/html;charset=utf-8")
public Flux<String> hiStream2(@RequestParam("id") Long id,
                              @RequestParam("prompt") String prompt) {
    return chatClient
            .prompt(prompt)
            // 核心：加入环绕增强
            .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, id))
            .stream()
            .content();
}
```

##### 自定义方式

###### 注意事项

- SpringAI 框架中，要实现会话记忆的自定义存储，需要额外注意类型转换
- SpringAI 框架通过使用 ChatMemory 接口，其底层使用的 ChatMemoryRepository 接口来实现会话记忆的存储
- 目前 SpringAI 框架提供了 InMemoryChatMemoryRepository 的实现类，以及 JdbcChatMemoryRepository 实现类，分别对应 Map 存储（内存）以及持久化（Jdbc）的存储
- 但是如果我们需要定制化，需要额外注意一下接口的类型转换，目前 SpringAI 框架内部强制使用其自己的 Message 接口实现类(UserMessage、AssistantMessage、SystemMessage 等)，如果我们需要进行自定义历史会话记忆，需要在实现 ChatMemoryRepository 接口的同时，注意下存储过程的类型手动转换。（踩过坑）

###### 基于 Redis 的会话记忆存储

当前的会话记忆存储，我们需要区分一下：是选择全量存储，还是说进行上下文限制，因为过长的上下文会降低大模型回复的效率，过短的上下文会降低大模型回复的效果

我们目前选择的是基于 Redis 的 String 结构，进行全量存储，后续我们会进行一些优化，让大模型的回复效率保持高效的同时，保证一定的回复效率。

####### 实现思路

通过实现 MemoryRepository 接口，通过 SpringAI 提供的工厂模式注入到 ChatMemory 中，即可实现基于 Redis 内存存储的会话记忆了！

1. 实现 ChatMemoryRepository 接口

```java
package com.project.demo.common.config.ai.common;

import com.project.demo.chat.factory.MessageConvertor;
import com.project.demo.chat.model.bean.MessageInfo;
import com.project.demo.common.provider.CacheProvider;
import com.project.demo.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author by 王玉涛
 * @Classname RedisMemoryRepository
 * @Description 基于Redis实现的聊天记忆存储库，用于管理对话历史记录。
 *              提供了对话ID查询、根据对话ID获取消息记录、保存消息记录和删除对话等功能。
 * @Date 2025/7/12 13:20
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMemoryRepository implements ChatMemoryRepository {

    /**
     * 存储所有对话ID的缓存键
     */
    private static final String AI_MEMORY_CONVERSATION_IDS_KEY = "ai:memory:conversation:ids";

    /**
     * 对话记录的缓存键前缀，后接具体 conversationId 构成完整键名
     */
    private static final String AI_MEMORY_CONVERSATION_KEY_PREFIX = "ai:memory:conversation:";

    /**
     * 缓存提供者，用于与Redis进行交互
     */
    private final CacheProvider cacheProvider;

    /**
     * 查询所有的对话ID列表。
     *
     * @return 返回存储在Redis中的所有对话ID列表
     */
    @Override
    public List<String> findConversationIds() {
        log.info("基于Redis内存的聊天历史记忆：查询所有的id");
        return cacheProvider.getList(AI_MEMORY_CONVERSATION_IDS_KEY, String.class);
    }

    /**
     * 根据指定的 conversationId 查询对应的聊天记录。
     *
     * @param conversationId 对话ID
     * @return 返回对应 conversationId 的聊天消息列表
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        log.info("基于Redis内存的聊天历史记忆：查询id为{}的历史记忆", conversationId);
        String key = AI_MEMORY_CONVERSATION_KEY_PREFIX + conversationId;
        List<String> stringList = cacheProvider.getList(key, String.class);

        if (stringList == null) {
            return List.of();
        }

        // 将字符串列表反序列化为 MessageInfo 对象列表
        List<MessageInfo> messages = stringList.stream()
                .map(string -> StringUtils.jsonToClass(string, MessageInfo.class))
                .toList();

        // 转换为 Spring AI 所需的 Message 类型并返回
        return messages.stream()
                .map(MessageConvertor::myToAiMessage)
                .toList();
    }

    /**
     * 保存指定 conversationId 对应的所有消息记录到Redis中。
     *
     * @param conversationId 对话ID
     * @param messages       需要保存的消息列表
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        log.info("基于Redis内存的聊天历史记忆：保存id为{}的历史记忆", conversationId);
        // 将 Spring AI 的 Message 列表转换为本地定义的 MessageInfo 列表
        List<MessageInfo> messageInfos = messages.stream().map(MessageConvertor::aiMessageToMy).toList();

        // 添加 conversationId 到全局对话ID集合中
        cacheProvider.addList(AI_MEMORY_CONVERSATION_IDS_KEY, conversationId);

        // 序列化消息列表并保存到对应 conversationId 的缓存中
        cacheProvider.addList(AI_MEMORY_CONVERSATION_KEY_PREFIX + conversationId, StringUtils.classToJson(messageInfos));
    }

    /**
     * 删除指定 conversationId 对应的聊天记录。
     *
     * @param conversationId 对话ID
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        log.info("基于Redis内存的聊天历史记忆：删除id为{}的历史记忆", conversationId);
        // 删除该 conversationId 对应的对话记录和全局ID集合中的条目
        cacheProvider.delete(AI_MEMORY_CONVERSATION_IDS_KEY, conversationId);
    }
}
```

1. MyMessage 类(贴合业务的类，可以自行拓展，但要包含必要属性：文本内容，发送角色)

```java
/**
 * @author by 王玉涛
 * @Classname MyMessage
 * @Description TODO
 * @Date 2025/7/10 22:17
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MyMessage {
    private Long id;
    private String content;
    private String role;
    private LocalDateTime sendTime;
}
```

1. MessageConvertor 类，负责解耦类型转换逻辑

```java
package com.project.demo.common.config.ai.common;

import com.project.demo.chat.factory.MessageConvertor;
import com.project.demo.chat.model.bean.MessageInfo;
import com.project.demo.common.provider.CacheProvider;
import com.project.demo.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author by 王玉涛
 * @Classname RedisMemoryRepository
 * @Description 基于Redis实现的聊天记忆存储库，用于管理对话历史记录。
 *              提供了对话ID查询、根据对话ID获取消息记录、保存消息记录和删除对话等功能。
 * @Date 2025/7/12 13:20
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMemoryRepository implements ChatMemoryRepository {

    /**
     * 存储所有对话ID的缓存键
     */
    private static final String AI_MEMORY_CONVERSATION_IDS_KEY = "ai:memory:conversation:ids";

    /**
     * 对话记录的缓存键前缀，后接具体 conversationId 构成完整键名
     */
    private static final String AI_MEMORY_CONVERSATION_KEY_PREFIX = "ai:memory:conversation:";

    /**
     * 缓存提供者，用于与Redis进行交互
     */
    private final CacheProvider cacheProvider;

    /**
     * 查询所有的对话ID列表。
     *
     * @return 返回存储在Redis中的所有对话ID列表
     */
    @Override
    public List<String> findConversationIds() {
        log.info("基于Redis内存的聊天历史记忆：查询所有的id");
        return cacheProvider.getList(AI_MEMORY_CONVERSATION_IDS_KEY, String.class);
    }

    /**
     * 根据指定的 conversationId 查询对应的聊天记录。
     *
     * @param conversationId 对话ID
     * @return 返回对应 conversationId 的聊天消息列表
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        log.info("基于Redis内存的聊天历史记忆：查询id为{}的历史记忆", conversationId);
        String key = AI_MEMORY_CONVERSATION_KEY_PREFIX + conversationId;
        List<String> stringList = cacheProvider.getList(key, String.class);

        if (stringList == null) {
            return List.of();
        }

        // 将字符串列表反序列化为 MessageInfo 对象列表
        List<MessageInfo> messages = stringList.stream()
                .map(string -> StringUtils.jsonToClass(string, MessageInfo.class))
                .toList();

        // 转换为 Spring AI 所需的 Message 类型并返回
        return messages.stream()
                .map(MessageConvertor::myToAiMessage)
                .toList();
    }

    /**
     * 保存指定 conversationId 对应的所有消息记录到Redis中。
     *
     * @param conversationId 对话ID
     * @param messages       需要保存的消息列表
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        log.info("基于Redis内存的聊天历史记忆：保存id为{}的历史记忆", conversationId);
        // 将 Spring AI 的 Message 列表转换为本地定义的 MessageInfo 列表
        List<MessageInfo> messageInfos = messages.stream().map(MessageConvertor::aiMessageToMy).toList();

        // 添加 conversationId 到全局对话ID集合中
        cacheProvider.addList(AI_MEMORY_CONVERSATION_IDS_KEY, conversationId);

        // 序列化消息列表并保存到对应 conversationId 的缓存中
        cacheProvider.addList(AI_MEMORY_CONVERSATION_KEY_PREFIX + conversationId, StringUtils.classToJson(messageInfos));
    }

    /**
     * 删除指定 conversationId 对应的聊天记录。
     *
     * @param conversationId 对话ID
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        log.info("基于Redis内存的聊天历史记忆：删除id为{}的历史记忆", conversationId);
        // 删除该 conversationId 对应的对话记录和全局ID集合中的条目
        cacheProvider.delete(AI_MEMORY_CONVERSATION_IDS_KEY, conversationId);
    }
}
```

##### 注意

当前的代码仅限于 demo 阶段，代码质量不是特别高，后续会推出一些可以直接落地以及高质量的代码

#### 多 AgentModel 配置

##### 前言

SpringAI 支持多 Agent 配置，其对单一的简单 Agent 配置做出了便捷性实现，而我们可以通过配置文件注入，进行多个 Agent 的配置。

注意：最好连带 SpringAI 自带的配置文件也进行配置，不然需要排除很多类，且不一定可以正常运行，其他的都可以保持运行

##### 配置文件(从环境变量中获取)

```yaml
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
```

##### Model 配置类，进行多 Model 配置返回

```java
/**
 * @author by 王玉涛
 * @Classname DoubaoAiModelConfig
 * @Description 配置类，用于定义与Doubao AI相关的Bean和配置属性。
 *              提供了两个ChatModel Bean，分别用于回复生成和历史摘要处理。
 * @Date 2025/7/12 12:45
 */
@Data
@Configuration
public class DoubaoAiModelConfig {

    /**
     * 用于回复的模型名称，从配置文件中读取。
     */
    @Value("${doubao.reply.model}")
    private String replyModel;

    /**
     * 用于回复的API密钥，从配置文件中读取。
     */
    @Value("${doubao.reply.api-key}")
    private String replyApiKey;

    /**
     * 用于回复的模型温度值，控制输出的随机性，从配置文件中读取。
     */
    @Value("${doubao.reply.temperature}")
    private String replyTemperature;
    
    /**
     * 用于历史摘要的模型名称，从配置文件中读取。
     */
    @Value("${doubao.history-summary.model}")
    private String historySummaryModel;

    /**
     * 用于历史摘要的API密钥，从配置文件中读取。
     */
    @Value("${doubao.history-summary.api-key}")
    private String historySummaryApiKey;

    /**
     * 用于历史摘要的模型温度值，控制输出的随机性，从配置文件中读取。
     */
    @Value("${doubao.history-summary.temperature}")
    private String historySummaryTemperature;

    /**
     * Doubao API的基础URL，从配置文件中读取。
     */
    @Value("${doubao.base-url}")
    private String baseUrl;

    /**
     * 创建一个ChatModel Bean，用于生成回复。
     * 
     * @return 配置好的ChatModel实例
     */
    @Bean
    public ChatModel doubaoReplyModel() {
        // 获取回复模型的选项
        OpenAiChatOptions openAiChatOptions = getOpenAiChatOptions(replyModel, replyTemperature);
        // 获取回复模型的API实例
        OpenAiApi openAiApi = getOpenAiApi(replyApiKey, baseUrl);

        return OpenAiChatModel.builder()
                .defaultOptions(openAiChatOptions)
                .openAiApi(openAiApi)
                .build();
    }
    
    /**
     * 创建一个ChatModel Bean，用于历史摘要处理。
     * 
     * @return 配置好的ChatModel实例
     */
    @Bean
    public ChatModel doubaoHistorySummaryModel() {
        // 获取历史摘要模型的选项
        OpenAiChatOptions openAiChatOptions = getOpenAiChatOptions(historySummaryModel, historySummaryTemperature);
        // 获取历史摘要模型的API实例
        OpenAiApi openAiApi = getOpenAiApi(historySummaryApiKey, baseUrl);
        
        return OpenAiChatModel.builder()
                .defaultOptions(openAiChatOptions)
                .openAiApi(openAiApi)
                .build();
    }
    
    /**
     * 构建OpenAiApi实例，用于与Doubao API进行交互。
     * 
     * @param apiKey   API密钥
     * @param baseUrl  API的基础URL
     * @return 配置好的OpenAiApi实例
     */
    private OpenAiApi getOpenAiApi(String apiKey, String baseUrl) {
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }
    
    /**
     * 构建OpenAiChatOptions实例，指定模型和温度值。
     * 
     * @param model       模型名称
     * @param temperature 温度值
     * @return 配置好的OpenAiChatOptions实例
     */
    private OpenAiChatOptions getOpenAiChatOptions(String model, String temperature) {
        return OpenAiChatOptions.builder()
                .model(model)
                .temperature(Double.parseDouble(temperature))
                .build();
    }
}
```

说明：当前的 Model 配置，进行了两个模型配置：一个是会话历史总结模型，一个是回复生成模型。并且其 beanName 分别为其对应的方法名字，因此我们注入的时候，需要注意优先使用 beanName 进行注入，因为当前相同的类型（ChatModel）有多个实现类，如果不按照 beanName 严格注入，就会导致依赖注入出现异常。

#### 多 AgentClient 配置

##### 前言

当前我们通过多 AgentModel 配置，进行了多的底层 model 实现，我们可以通过不同的 Model，再次封装不同的 Client，从而让我们可以直接通过 Client 进行调用

##### Client 配置类，进行多 Client 配置返回

```java
/**
 * @author by 王玉涛
 * @Classname DoubaoAiClientConfig
 * @Description 配置用于创建与豆包AI交互的ChatClient实例，提供对话回复和历史摘要功能。
 * @Date 2025/7/12 13:09
 */
@Configuration
public class DoubaoAiClientConfig {
    
    /**
     * 创建用于生成AI回复的ChatClient Bean。
     * 使用注入的doubaoReplyModel模型构建ChatClient实例，适用于实时对话场景。
     *
     * @param doubaoReplyModel 提供AI对话能力的模型Bean
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient doubaoReplyClient(ChatModel doubaoReplyModel, ChatMemory doubaoAiMemoryRepository) {
        return ChatClient
                .builder(doubaoReplyModel)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(doubaoAiMemoryRepository).build()
                )
                .build();
    }
    
    /**
     * 创建用于处理历史记录摘要的ChatClient Bean。
     * 使用注入的doubaoHistorySummaryModel模型构建ChatClient实例，适用于需要总结或分析对话历史的场景。
     *
     * @param doubaoHistorySummaryModel 提供AI对话能力的模型Bean，专用于历史信息处理
     * @return ChatClient 实例
     */
    @Bean
    public ChatClient doubaoHistorySummaryClient(ChatModel doubaoHistorySummaryModel, ChatMemory doubaoAiMemoryRepository) {
        return ChatClient
                .builder(doubaoHistorySummaryModel)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(doubaoAiMemoryRepository).build()
                )
                .build();
    }
}
```

#### 通过一个类，统一管理 AI 调用

```java
/**
 * @author 王玉涛
 * @Classname DoubaoAiProvider
 * @Description 集成豆包大模型AI能力的提供者类，用于配置和管理与豆包AI相关的客户端资源。
 *              当前托管两个核心功能的客户端：
 *              - doubaoReplyClient: 豆包回复生成客户端，用于对话场景中的实时回复生成。
 *              - doubaoHistorySummaryClient: 豆包历史对话总结客户端，用于对长对话历史进行摘要处理。
 * @Date 2025/7/12 13:11
 */
@Component
public class DoubaoAiProvider {
    
    /**
     * 豆包回复生成客户端，注入名为 "doubaoReplyClient" 的Bean，
     * 用于在对话交互中生成自然语言回复。
     */
    @Qualifier("doubaoReplyClient")
    private ChatClient replyClient;

    /**
     * 豆包历史对话摘要客户端，注入名为 "doubaoHistorySummaryClient" 的Bean，
     * 用于对较长的对话历史进行内容压缩和关键信息提取。
     */
    @Qualifier("doubaoHistorySummaryClient")
    private ChatClient historySummaryClient;
}
```
