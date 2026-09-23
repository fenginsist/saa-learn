# DashScopeChatModel 默认参数手册：默认值是什么 + 怎么查

> 适用版本：`com.alibaba.cloud.ai:spring-ai-alibaba-dashscope:1.1.2.0`（本项目 `pom.xml` 声明，本地 jar 在 `D:\maven_repo\com\alibaba\cloud\ai\spring-ai-alibaba-dashscope\1.1.2.0\`）
> 本文所有结论都经过 **源码 jar** + **javap 反编译** + **运行期探针（打印真实请求体 JSON）** 三重验证，不是推测。

---

## 一、先给结论（TL;DR）

你贴的这段代码：

```java
DashScopeChatModel chatModel = DashScopeChatModel.builder()
        .dashScopeApi(dashScopeApi)
        .build();
```

**完全等价于**（把 Builder 的隐含默认值显式写出来）：

```java
DashScopeChatModel chatModel = DashScopeChatModel.builder()
        .dashScopeApi(dashScopeApi)
        .defaultOptions(DashScopeChatOptions.builder()
                .model(DashScopeChatModel.DEFAULT_MODEL_NAME)   // "qwen-plus"
                .build())
        .retryTemplate(RetryUtils.DEFAULT_RETRY_TEMPLATE)
        .toolCallingManager(ToolCallingManager.builder().build())
        .observationRegistry(ObservationRegistry.NOOP)
        .toolExecutionEligibilityPredicate(new DefaultToolExecutionEligibilityPredicate())
        .build();
```

由此可得两条最重要的结论：

1. **只有 `model` 被显式设为 `qwen-plus`**（`DEFAULT_MODEL_NAME` = `DashScopeApi.DEFAULT_CHAT_MODEL` = `DashScopeModel.ChatModel.QWEN_PLUS.getValue()`），其余业务常见的模型参数（`temperature` / `top_p` / `max_tokens` / `seed` / `top_k` / `stop` …）**全部是 `null`**。
2. 请求体记录类带 `@JsonInclude(Include.NON_NULL)`，所以 **`null` 参数根本不会出现在 HTTP 请求 JSON 里**，最终由阿里云百炼**服务端**按模型自身的默认值处理（SDK javadoc 记载：temperature 默认 0.85、top_p 默认 0.8、repetition_penalty 默认 1.1 …）。

### 实测：不配置任何参数时，真正发出去的请求体

（反射调用 `DashScopeChatModel#createRequest` 打印，未发生任何网络请求；`stream=false` 即 `call()` 非流式路径）

```json
{
  "model" : "qwen-plus",
  "input" : {
    "messages" : [ {
      "content" : "你好",
      "role" : "user"
    } ]
  },
  "parameters" : {
    "result_format" : "message",
    "enable_search" : false,
    "incremental_output" : false,
    "enable_thinking" : false,
    "vl_enable_image_hw_output" : false
  },
  "stream" : false
}
```

可见 `temperature`、`top_p`、`max_tokens`、`seed`、`top_k`、`repetition_penalty`、`stop`、`response_format` 全部**不在请求里**。

---

## 二、默认参数清单

### 2.1 ChatModel.Builder（框架层）隐含默认值

| Builder 字段 | 默认值 | 源码位置（1.1.2.0 sources jar） |
| --- | --- | --- |
| `defaultOptions` | `DashScopeChatOptions.builder().model(DEFAULT_MODEL_NAME).build()` | `DashScopeChatModel.java:747-749` |
| `retryTemplate` | `RetryUtils.DEFAULT_RETRY_TEMPLATE`（Spring AI 默认重试模板） | `DashScopeChatModel.java:751` |
| `toolCallingManager` | `null` → `build()` 时替换为 `DEFAULT_TOOL_CALLING_MANAGER = ToolCallingManager.builder().build()` | `DashScopeChatModel.java:103, 753, 790-799` |
| `observationRegistry` | `ObservationRegistry.NOOP`（默认不上报可观测数据） | `DashScopeChatModel.java:757` |
| `toolExecutionEligibilityPredicate` | `new DefaultToolExecutionEligibilityPredicate()` | `DashScopeChatModel.java:755` |
| `observationConvention` | `DashScopeChatModelObservationConvention` | `DashScopeChatModel.java:101` |

### 2.2 DashScopeChatOptions（模型参数本体）默认值

`DashScopeChatOptions.builder()` 只是 `new DashScopeChatOptions()`（`DashScopeChatOptions.java:659-669`），默认值全部来自字段初始化语句，**没有额外的 builder 默认值**。“服务端默认值”列来自同文件字段的 javadoc（即 Spring AI Alibaba 抄录的百炼官方说明）。

| 参数（JSON） | Java 字段 | SDK 默认值 | 是否出现在请求中 | 服务端默认值 | 说明 / 源码行 |
| --- | --- | --- | --- | --- | --- |
| `model` | `model` | `null`（ChatModel 中已被设为 `qwen-plus`） | ✅ 总是 | — | `:50-51` |
| `stream` | `stream` | `null` | ✅ 由调用方式决定：`call()`→`false`，`stream()`→`true` | `false` | `@JsonIgnore`，写入请求体顶层 `stream` |
| `temperature` | `temperature` | `null` | ❌ 不发送 | `0.85`，范围 `[0,2)`，不建议设 0 | javadoc `:67` |
| `top_p` | `topP` | `null` | ❌ 不发送 | `0.8`，范围 `(0,1.0)`，不可 ≥1.0 | javadoc `:82-84` |
| `top_k` | `topK` | `null` | ❌ 不发送 | 关闭（`null` 或 `>100` 时 top-k 不生效，只用 top-p） | javadoc `:92-93` |
| `seed` | `seed` | `null` | ❌ 不发送 | 无（不保证完全可复现） | javadoc `:72-76` |
| `max_tokens` | `maxTokens` | `null` | ❌ 不发送 | 无（受模型上下文长度限制） | javadoc `:132-134` |
| `stop` | `stop` | `null` | ❌ 不发送 | 无 | javadoc `:97-110` |
| `repetition_penalty` | `repetitionPenalty` | `null` | ❌ 不发送 | `1.1`（1.0 表示不惩罚） | javadoc `:147-148` |
| `enable_search` | `enableSearch` | **`false`** | ✅ 总是 | `false` | `:123` |
| `incremental_output` | `incrementalOutput` | **`true`** | ✅ 发送，值被 `stream` 覆盖：非流式 `false`、流式 `true` | `true`（仅流式有意义） | `:144`、`createRequest` |
| `response_format` | `responseFormat` | `null` | ❌ 不发送 | `{"type":"text"}` | javadoc `:126-129` |
| `tools` | `tools` | `null`（运行时由 `ToolCallingManager.resolveToolDefinitions` 从 `toolCallbacks`/`toolNames` 解析后回填） | 有工具时发送 | — | `:152-157` |
| `tool_choice` | `toolChoice` | `null` | ❌ 不发送 | `tools` 为空 → `"none"`；非空 → `"auto"` | javadoc `:176-183` |
| `parallel_tool_calls` | `parallelToolCalls` | `null` | ❌ 不发送 | — | `:167` |
| `enable_thinking` | `enableThinking` | **`false`** | ✅ 总是 | `false` | `:196` |
| `thinking_budget` | `thinkingBudget` | `null` | ❌ 不发送 | 仅 Qwen3 全系统模型生效 | `:202` |
| `vl_high_resolution_images` | `vlHighResolutionImages` | `null` | ❌ 不发送 | 仅 VL 模型（上限 16384 token） | `:191` |
| `vl_enable_image_hw_output` | `vlEnableImageHwOutput` | **`false`** | ✅ 总是 | `false` | `:230` |
| `multi_model` | `multiModel` | **`false`** | ❌ 不发送（`@JsonIgnore`） | — | `:225` |
| `max_input_tokens` | `maxInputTokens` | `null` | ❌ 不发送 | — | `:251` |
| `logprobs` / `top_logprobs` | `logprobs` / `topLogProbs` | `null` | ❌ 不发送 | `false` / 取值 `[0,5]` | `:271-278` |
| `modalities` | `modalities` | `null` | ❌ 不发送 | `["text"]` | javadoc `:256-260` |
| `audio`、`stream_options`、`asr_options`、`ocr_options`、`translation_options`、`output_format`、`extra_body` | 同名字段 | `null` | ❌ 不发送 | 各专有模型专用 | `:236-246, 265, 283, 300+` |
| `result_format` | **无对应字段**，在 `createRequest` 中硬编码 | `"message"` | ✅ 总是 | `"message"` | `DashScopeChatModel#toDashScopeRequestParameter` |
| `http_headers` | `httpHeaders` | `{}` | ❌ 不进 body（`@JsonIgnore`，改为附加到 HTTP Header） | — | `:172-173` |
| `tool_callbacks` / `tool_names` / `tool_context` / `internal_tool_execution_enabled` | 同名字段 | `[]` / `[]` / `{}` / `null` | ❌ 不进 body（`@JsonIgnore`，框架内部使用） | — | `:207-220` |
| `frequency_penalty` / `presence_penalty` | `getFrequencyPenalty()` / `getPresencePenalty()` | **恒返回 `null`**（DashScope 不支持，仅为满足 `ChatOptions` 接口） | ❌ | — | `:410-413, 424-427` |

### 2.3 DashScopeApi（连接层）默认值

| 配置项 | 默认值 | 源码位置 |
| --- | --- | --- |
| `DEFAULT_CHAT_MODEL` | `"qwen-plus"` | `DashScopeApi.java:110` |
| `DEFAULT_EMBEDDING_MODEL` | `"text-embedding-v2"` | `DashScopeApi.java:112` |
| `baseUrl` | `https://dashscope.aliyuncs.com` | `DashScopeApiConstants.java:47` |
| `completionsPath` | `/api/v1/services/aigc/text-generation/generation` | `DashScopeApiConstants.java:51` |
| `embeddingsPath` | `/api/v1/services/embeddings/text-embedding/text-embedding` | `DashScopeApiConstants.java:53` |
| `headers` | 空 `LinkedMultiValueMap`；请求时补 `Authorization: Bearer <apiKey>`、`Content-Type: application/json` | `DashScopeApi.java:665` |
| `responseErrorHandler` | `RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER` | `DashScopeApi.java:675` |

### 2.4 Spring Boot 自动配置层默认值（本项目未用到，仅作对照）

自动配置类 `DashScopeChatAutoConfiguration` 会把属性对象直接当作 `defaultOptions`：

```java
DashScopeChatModel.builder()
        .dashScopeApi(dashscopeApi)
        .defaultOptions(chatProperties.getOptions())   // ← 即 spring.ai.dashscope.chat.options.*
        .build();
```

| 配置项 | 默认值 | 源码位置 |
| --- | --- | --- |
| 配置前缀 | `spring.ai.dashscope.chat` | `DashScopeChatProperties.java:3` |
| `spring.ai.dashscope.chat.enabled` | `true` | 构造器 `:15-17` |
| `DEFAULT_DEPLOYMENT_NAME` | `"qwen-plus"` | `:5` |
| `options` | `DashScopeChatOptions.builder().model("qwen-plus").build()`（其余仍为 `null`） | `:19-23` |

> ⚠️ 本项目是「纯 `main` + `System.getenv("AI_DASHSCOPE_API_KEY")` + 手写 Builder」的写法，**没有启动 Spring 容器**，所以 `src/main/resources/application.properties` 里的 `spring.ai.dashscope.chat.options.model=qwen3-max` 对接手写的 `DashScopeApi` / `DashScopeChatModel` **不生效**。那几行只在引入 Spring Boot 自动配置（`spring-ai-alibaba-starter-dashscope` + 启动 `@SpringBootApplication`）时才起作用。

---

## 三、参数合并优先级：谁能覆盖谁

`DashScopeChatModel#buildRequestPrompt`（`:419-470`）的核心逻辑：

```java
DashScopeChatOptions runtimeOptions = ModelOptionsUtils.copyToTarget(prompt.getOptions(), ..., DashScopeChatOptions.class);
DashScopeChatOptions requestOptions = ModelOptionsUtils.merge(runtimeOptions, this.defaultOptions, DashScopeChatOptions.class);
return new Prompt(prompt.getInstructions(), requestOptions);
```

即 **优先级：单次调用传入的 `Prompt` options > `ChatModel.defaultOptions` > 不发该字段（服务端默认）**。

实测验证（`defaultOptions`: model=`qwen-max`、temperature=`0.5`、max_tokens=`1024`；`Prompt` options: temperature=`0.9`）：

```json
{
  "model" : "qwen-max",
  "input" : { "messages" : [ { "content" : "你好", "role" : "user" } ] },
  "parameters" : {
    "result_format" : "message",
    "temperature" : 0.9,
    "enable_search" : false,
    "incremental_output" : false,
    "enable_thinking" : false,
    "vl_enable_image_hw_output" : false,
    "max_tokens" : 1024
  },
  "stream" : false
}
```

结论：`temperature` 被 Prompt 级的 `0.9` 覆盖，`max_tokens` 与 `model` 沿用 `defaultOptions` 的 `1024` / `qwen-max`。

---

## 四、怎么查这些默认值（4 条可复现路径）

### 路径 1（最推荐）：看源码 jar，一切默认值都在源码里

```bash
# 方式 A：只下载 dashscope 模块的源码 jar
mvn dependency:get -Dartifact=com.alibaba.cloud.ai:spring-ai-alibaba-dashscope:1.1.2.0:jar:sources

# 方式 B：把项目所有依赖的源码 jar 一次拉齐
mvn dependency:sources
```

本项目本地已经存在（无需联网）：

```
D:\maven_repo\com\alibaba\cloud\ai\spring-ai-alibaba-dashscope\1.1.2.0\spring-ai-alibaba-dashscope-1.1.2.0-sources.jar
D:\maven_repo\com\alibaba\cloud\ai\spring-ai-alibaba-autoconfigure-dashscope\1.1.2.0\spring-ai-alibaba-autoconfigure-dashscope-1.1.2.0-sources.jar
```

用 IDEA 时更简单：`External Libraries` → 找到 `spring-ai-alibaba-dashscope-1.1.2.0.jar` → 右键 `Download Sources` / `Choose Sources`，即可直接跳进 `DashScopeChatOptions` 看每个字段的默认值与官方注释。

**要看的 5 个关键文件 / 行号：**

| 文件 | 关键内容 |
| --- | --- |
| `chat/DashScopeChatModel.java:747-749` | Builder 里 `defaultOptions = DashScopeChatOptions.builder().model(DEFAULT_MODEL_NAME).build()` ← **默认参数的源头** |
| `chat/DashScopeChatModel.java:99,103,751-757` | `DEFAULT_MODEL_NAME` / `DEFAULT_TOOL_CALLING_MANAGER` / `retryTemplate` / `observationRegistry` |
| `chat/DashScopeChatOptions.java:44-320` | 所有模型参数字段 + 默认值 + 官方注释（含各参数服务端默认值） |
| `api/DashScopeApi.java:110-114, 659-675` | `DEFAULT_CHAT_MODEL`、`baseUrl`、`completionsPath` 等 |
| `spec/DashScopeApiSpec.java:527-534, 597+` | `ChatCompletionRequest` / `ChatCompletionRequestParameter` 的 `@JsonInclude(NON_NULL)` 与 JSON 字段名 |

### 路径 2：没有源码 jar 时，javap 反编译 class

```powershell
$dst = "D:\tmp\saai_inspect"
# 解包 jar
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory(
  "$env:USERPROFILE\.m2\repository\com\alibaba\cloud\ai\spring-ai-alibaba-dashscope\1.1.2.0\spring-ai-alibaba-dashscope-1.1.2.0.jar", $dst)

$javap = "C:\Program Files\Java\jdk-17\bin\javap.exe"

# 1) 看默认常量与字段
& $javap -constants -p -classpath $dst com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel
& $javap -constants -p -classpath $dst com.alibaba.cloud.ai.dashscope.api.DashScopeApi

# 2) 看 Builder 构造函数字节码里的默认值（关键：getstatic DEFAULT_MODEL_NAME + model(...)）
& $javap -c -p -classpath $dst 'com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel$Builder'

# 3) 看 DashScopeChatOptions 无参构造器里字段的赋值（默认值）
& $javap -c -p -classpath $dst com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions

# 4) 看请求参数类是否 @JsonInclude(NON_NULL)（决定 null 会不会被发出去）
& $javap -v -p -classpath $dst 'com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec$ChatCompletionRequest' | Select-String JsonInclude
```

要点：`javap -constants` 只能显示编译期常量；像 `DEFAULT_MODEL_NAME = DashScopeApi.DEFAULT_CHAT_MODEL` 是运行期赋值的，必须看 `static {}` 静态块（`javap -c` 最后一段），或用路径 3 直接跑出来。

### 路径 3（最直观）：写个探针程序，直接打印默认值与真实请求体

把下面文件放到任意目录（本仓库验证时放在 `target/tmpprobe/OptionsProbe.java`，构建产物目录不污染源码），它**不发起任何网络请求**，只反射调用包级私有的 `buildRequestPrompt` / `createRequest` 打印最终 payload：

```java
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.prompt.Prompt;

/** 打印 DashScopeChatModel 的默认参数与真实请求体（不发起网络请求）。 */
public class OptionsProbe {

    public static void main(String[] args) throws Exception {
        System.out.println("DEFAULT_MODEL_NAME  = " + DashScopeChatModel.DEFAULT_MODEL_NAME);
        System.out.println("DEFAULT_CHAT_MODEL  = " + DashScopeApi.DEFAULT_CHAT_MODEL);
        System.out.println("builder().build()   = " + DashScopeChatOptions.builder().build());

        DashScopeChatModel model = DashScopeChatModel.builder()
                .dashScopeApi(DashScopeApi.builder().apiKey("dummy-key").build())
                .build();
        System.out.println("getDefaultOptions() = " + model.getDefaultOptions());
        System.out.println("retryTemplate       = " + model.retryTemplate.getClass().getName());

        // 不配置任何参数（非流式 / 流式）
        printRequest(model, new Prompt("你好", DashScopeChatOptions.builder().build()), false);
        printRequest(model, new Prompt("你好", DashScopeChatOptions.builder().build()), true);

        // 指定部分参数
        printRequest(model, new Prompt("你好", DashScopeChatOptions.builder()
                .temperature(0.7).topP(0.9).maxToken(2000).build()), false);
    }

    /** 反射调用包级私有方法，得到真正会发给百炼的请求体 JSON */
    private static void printRequest(DashScopeChatModel model, Prompt prompt, boolean stream) throws Exception {
        var build = DashScopeChatModel.class.getDeclaredMethod("buildRequestPrompt", Prompt.class);
        build.setAccessible(true);
        Prompt merged = (Prompt) build.invoke(model, prompt);

        var create = DashScopeChatModel.class.getDeclaredMethod("createRequest", Prompt.class, boolean.class);
        create.setAccessible(true);
        Object request = create.invoke(model, merged, stream);

        System.out.println("stream=" + stream + " -> " + new ObjectMapper()
                .writerWithDefaultPrettyPrinter().writeValueAsString(request));
    }
}
```

编译运行（本项目 `target/cp.txt` 已经是 `mvn dependency:build-classpath` 的产物）：

```powershell
cd D:\ffl\code\saa-learned
mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt   # 已有可跳过
$cp = (Get-Content target\cp.txt -Raw).Trim()
& "C:\Program Files\Java\jdk-17\bin\javac.exe" -encoding UTF-8 -cp $cp -d target\tmpprobe target\tmpprobe\OptionsProbe.java
& "C:\Program Files\Java\jdk-17\bin\java.exe" -cp "$cp;target\tmpprobe" OptionsProbe
```

### 路径 4：官方文档与配置元数据（查“服务端默认值”用这个）

- 依赖 jar 内 `META-INF/spring-configuration-metadata.json`（`spring-ai-alibaba-autoconfigure-dashscope`）列出了全部可配置键名，例如 `spring.ai.dashscope.chat.options.temperature`、`...max-tokens`、`...top-p`，配合路径 1 的 `DashScopeChatOptions` 字段即可确定“键名 ↔ 默认值”的对应关系。
- 阿里云百炼官方文档（**服务端默认值**的唯一权威出处）：
  - 《文本生成模型 API 参考》 → 各模型的参数表；
  - 《OpenAI 兼容-Chat》 / 《输入参数配置》→ `temperature`、`top_p`、`top_k`、`max_tokens`、`repetition_penalty`、`incremental_output`、`enable_search` 等参数的取值范围与默认值。
- ⚠️ 注意区分两类“默认值”：**SDK 里的 null**（= 不发送）与**服务端默认值**（= 你没发时服务端实际用的值）。源码 javadoc 里已经把常用的服务端默认值抄录进来了（temperature 0.85 / top_p 0.8 / repetition_penalty 1.1 …），日常查这些就够。

---

## 五、怎么改这些默认参数

### 5.1 全局默认：`builder().defaultOptions(...)`（本项目现有写法）

```java
DashScopeApi dashScopeApi = DashScopeApi.builder()
        .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
        .build();

DashScopeChatModel chatModel = DashScopeChatModel.builder()
        .dashScopeApi(dashScopeApi)
        .defaultOptions(DashScopeChatOptions.builder()
                .model(DashScopeChatModel.DEFAULT_MODEL_NAME)  // qwen-plus
                .temperature(0.7)      // 控制随机性；不设则服务端按 0.85 处理
                .topP(0.9)             // 核采样；不设则 0.8
                .maxToken(2000)        // 最大输出 token；不设则受上下文长度限制
                .build())
        .build();
```

> **坑（本仓库 `TrueAgent.java` / `TrueAgent2.java` 里专门写了注释）**：一旦你调用 `.defaultOptions(DashScopeChatOptions.builder()...build())` 覆盖默认值，**必须显式设置 `model`**。因为 Builder 的默认值 `builder().model(DEFAULT_MODEL_NAME).build()` 会被你传入的对象整体替换，`model` 不写就是 `null`，请求体里没有 `model` 字段，服务端报参数错误。
>
> 另外：`withModel/withTemperature/withTopP/withMaxToken/...` 这一批 `withXxx` 方法在 1.1.2.0 已标记 `@Deprecated`，只是 `model()/temperature()/topP()/maxToken()` 的别名（`DashScopeChatOptions.java:671-800`），新代码建议直接用不带 `with` 的版本。

### 5.2 单次调用覆盖：`Prompt` 级 options（优先级最高，不污染全局）

```java
ChatResponse response = chatModel.call(new Prompt("你好",
        DashScopeChatOptions.builder()
                .temperature(0.2)     // 只影响这一次调用
                .maxToken(512)
                .build()));
```

### 5.3 配置文件方式（仅当使用 Spring Boot 自动配置时有效）

```properties
spring.ai.dashscope.api-key=${AI_DASHSCOPE_API_KEY}
spring.ai.dashscope.chat.options.model=qwen-plus
spring.ai.dashscope.chat.options.temperature=0.7
spring.ai.dashscope.chat.options.top-p=0.9
spring.ai.dashscope.chat.options.max-tokens=2000
```

本仓库当前是手写 Builder 的用法，这些键**不会生效**（见 2.4 的说明）。

---

## 六、附录：本次验证环境与探针原始输出

### 6.1 验证环境

| 项 | 值 |
| --- | --- |
| JDK | Oracle JDK 17（`C:\Program Files\Java\jdk-17`） |
| 依赖版本 | `spring-ai-alibaba-dashscope:1.1.2.0`（`pom.xml` 声明） |
| 本地仓库 | `D:\maven_repo`（`mvn dependency:build-classpath` 落盘于 `target/cp.txt`） |
| 被验证 jar 的 SHA-256 | `FD484FF4F462C8BF0AF8E711CA991EA49C50A941852568609AD8356357FFCAAE`（`spring-ai-alibaba-dashscope-1.1.2.0.jar`，与 `%USERPROFILE%\.m2` 内副本一致） |
| 验证手段 | 源码 jar 阅读 + `javap -c -p` 反编译 + 运行期探针打印请求体 |

### 6.2 探针输出（逐字段版 `FullOptionsProbe` 的原文）

> 本仓库保留了两份探针：`target/tmpprobe/OptionsProbe.java`（路径 3 的精简版，输出见 `target/tmpprobe/probe_simple.out.txt`）与 `target/tmpprobe/FullOptionsProbe.java`（逐字段打印版，输出见 `target/tmpprobe/probe.out.txt`）。两份结论完全一致。

```
=== 1. 模型名称默认常量 ===
DashScopeChatModel.DEFAULT_MODEL_NAME = qwen-plus
DashScopeApi.DEFAULT_CHAT_MODEL      = qwen-plus
DashScopeApi.DEFAULT_EMBEDDING_MODEL = text-embedding-v2

=== 2. new DashScopeChatOptions() 默认值 ===
model                 = null
stream                = null
temperature           = null
topP                  = null
topK                  = null
seed                  = null
maxTokens             = null
stop                  = null
repetitionPenalty     = null
enableSearch          = false
incrementalOutput     = true
enableThinking        = false
multiModel            = false
vlEnableImageHwOutput = false
httpHeaders           = {}
toolCallbacks         = []
toolNames             = []
toolContext           = {}

=== 3. builder().build() 默认值（与 new 等价）===
DashScopeChatOptions: {"vlEnableImageHwOutput":false,"enable_search":false,"incremental_output":true,"enable_thinking":false,"multi_model":false}

=== 4. ChatModel.builder() 未显式指定 defaultOptions 时的默认值 ===
getDefaultOptions()             = DashScopeChatOptions: {"vlEnableImageHwOutput":false,"model":"qwen-plus","enable_search":false,"incremental_output":true,"enable_thinking":false,"multi_model":false}
getDashScopeChatOptions()       = （同上）
retryTemplate                   = org.springframework.retry.support.RetryTemplate
defaultOptions.getModel()       = qwen-plus
defaultOptions.getTemperature() = null
```

> 第 3、4 段是 `DashScopeChatOptions#toString()` 的结果，带 `@JsonInclude(NON_NULL)` 语义：**只打印非 null 字段**。因此 `temperature/topP/maxTokens` 等字段干脆没出现在字符串里，也正是它们在 HTTP 请求里被省略的原因。
> 第 5、6 段（真实请求体 JSON）已分别内联在本文**第一节**与**第三节**。
> （上表“默认值”列的逐字段来源：`target/tmpprobe/FullOptionsProbe.java` 运行输出，含 `getDashScopeChatOptions()`、`retryTemplate` 等打印项。）

### 6.3 一句话总结

- **查默认值**：看 `spring-ai-alibaba-dashscope-*-sources.jar` 里的 `DashScopeChatOptions` / `DashScopeChatModel$Builder`（路径 1），或用探针打印 + 反射看真实请求体（路径 3）。
- **默认值是什么**：SDK 侧只有 `model=qwen-plus` 与 4 个 `false`/`true` 布尔量（`enable_search=false`、`incremental_output`、`enable_thinking=false`、`vl_enable_image_hw_output=false`）；`temperature/top_p/max_tokens/seed/top_k/repetition_penalty/stop` 全部为 `null`，**不发送**，由百炼服务端按模型默认值（0.85 / 0.8 / 1.1 …）处理。

