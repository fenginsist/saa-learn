# step01_baseAgent 复现说明：`function.arguments` 必须为 JSON 格式

本文记录复现官方 Spring AI Alibaba「快速开始 / 基础 Agent」示例时踩到的坑、完整根因、以及最小修复方案。

---

## 一、问题现象

运行 [step01_firest.java](step01_firest.java) 时，报错如下：

```
org.springframework.ai.retry.NonTransientAiException: 400 - {
  "request_id": "75c8c759-061f-9a3b-89c9-09b410a94246",
  "code": "InvalidParameter",
  "message": "<400> InternalError.Algo.InvalidParameter: The \"function.arguments\" parameter of the code model must be in JSON format."
}
```

核心信息是：**`function.arguments` 参数必须是 JSON 格式**。这是 DashScope（通义千问）服务端在**函数调用（function calling）**阶段对模型返回结果的校验报错，不是网络、鉴权或依赖缺失的问题。

---

## 二、根因定位（已通过反编译依赖确认）

问题出在工具入参的声明方式上：`inputType(String.class)`。整条链路如下。

### 1. 入参 schema 是怎么生成的

`FunctionToolCallback.Builder.build()`（`spring-ai-model` 1.1.2）里，工具入参 schema 由这一行生成：

```java
JsonSchemaGenerator.generateForType(inputType, ...)
```

当 `inputType = String.class` 时，生成的 JSON Schema 是**裸字符串类型**：

```json
{"type":"string"}
```

它没有 `properties`，没有字段名，根类型不是 `object`。

### 2. 这个 schema 是怎么传给千问的

`DashScopeChatModel`（`spring-ai-alibaba-dashscope` 1.1.2.0）把每个工具的 `ToolDefinition.inputSchema()` **原样**塞进 DashScope 函数定义的 `parameters` 字段：

```java
new FunctionTool.Function(description, name, inputSchema)  // parameters = inputSchema
```

于是千问实际拿到的工具定义是：

```json
{
  "name": "get_weather",
  "description": "Get weather for a given city",
  "parameters": { "type": "string" }
}
```

### 3. 千问的协议要求

默认模型是 `qwen-plus`，走 DashScope 的 **OpenAI 兼容协议**（`/compatible-mode/v1`）。该协议要求：模型调用工具时返回的 `function.arguments` 必须是 **JSON 对象**，例如：

```json
{"city": "San Francisco"}
```

而千问拿到一个 `parameters: {"type":"string"}` 的工具定义后，**不知道该往哪个字段名里填值**，于是返回空/非法的 arguments，被服务端校验拦截，抛出 400。

### 一句话总结

```
inputType(String.class)
  → 生成 {"type":"string"}（裸字符串 schema）
  → 千问不知道入参字段名
  → 返回非法 function.arguments
  → 服务端报 400：function.arguments 必须是 JSON 格式
```

---

## 三、为什么「复现官方文档案例」也会报错

「官方文档这么写」与「这么写会报错」并不矛盾，原因是**文档片段落后于千问的 OpenAI 兼容协议**。

文档里 `inputType(String.class)` + `BiFunction<String, ToolContext, String>` 这套写法的本意，是让模型返回一个裸字符串（如 `"San Francisco"`），再由框架反序列化成 `String`。这条路径在早期/旧版千问模型上可能被容忍，但在当前 `qwen-plus` 的兼容协议下已经被堵死：函数调用的 arguments 必须是带字段名的 JSON 对象。

因此这不是复现者写错了，也不是版本/配置问题，而是 **`inputType(String.class)` 这一行本身与函数调用协议不兼容**。

---

## 四、最小修复方案

把工具入参从裸 `String` 改成**带字段名的 record（POJO）**，让 schema 变成正确的 JSON 对象。

### 修改 `WeatherTool.java`

```java
import org.springframework.ai.chat.model.ToolContext;

import java.util.function.BiFunction;

public class WeatherTool implements BiFunction<WeatherTool.Request, ToolContext, String> {

    public record Request(String city) {}

    @Override
    public String apply(Request request, ToolContext toolContext) {
        return "It's always sunny in " + request.city() + "!";
    }
}
```

### 修改 `step01_firest.java`

```java
ToolCallback weatherTool = FunctionToolCallback.builder("get_weather", new WeatherTool())
        .description("Get weather for a given city")
        .inputType(WeatherTool.Request.class)   // 原来是 inputType(String.class)
        .build();
```

改完后，工具 schema 会生成为：

```json
{
  "type": "object",
  "properties": {
    "city": { "type": "string" }
  },
  "required": ["city"]
}
```

千问就能正确返回 `{"city":"San Francisco"}`，`JsonParser.fromJson(..., Request.class)` 也能干净地反序列化成 `Request("San Francisco")`，报错消失。

---

## 五、为什么其他「看似能保留 String」的改法走不通

### 1. 换版本

`String.class → {"type":"string"}` 这条 schema 生成路径由 `JsonSchemaGenerator` 决定，是 Spring AI 核心的固定行为，与 spring-ai-alibaba 版本无关。降级/升级救不了。

### 2. 手写 `.inputSchema(...)` 保留 `BiFunction<String, ToolContext, String>`

即便手写一个正确的对象 schema 让千问返回 `{"city":"San Francisco"}`，执行阶段仍会出错：

`FunctionToolCallback.call()` 内部用

```java
JsonParser.fromJson(arguments, String.class)
```

反序列化入参。对 `String.class` 而言，Jackson 会把 `{"city":"San Francisco"}` 整段 JSON 文本当成字符串返回，导致 `apply(city)` 拿到的是 `{"city":"San Francisco"}` 而不是 `San Francisco`，工具输出依然错误。

所以 **record 是唯一既忠实于文档意图、又能正确运行的最小改法**，也是 Spring AI 官方当前对工具入参的推荐写法。

---

## 六、涉及文件

- [step01_firest.java](step01_firest.java) — Agent 入口，工具回调的 `inputType` 在此处修正
- [WeatherTool.java](WeatherTool.java) — 天气工具，入参从 `String` 改为 `Request` record

---

## 七、补充提醒

1. **step02 存在同样的问题**：`step02_trueAgent/TrueAgent.java` 里 `getWeatherForLocation`、`getUserLocation` 两个工具也都用了 `inputType(String.class)`，一旦真正运行会报一模一样的错，需要一并改成 record 入参（其中 `UserLocationTool` 因为还要从 `ToolContext` 取 userId，改法需额外注意）。

2. **API Key 硬编码**：示例代码里 `AI_DASHSCOPE_API_KEY` 直接写在源码中，且已进入 git 提交历史。建议去阿里云控制台吊销该 key，改用环境变量/配置文件注入，并清理历史提交中的 key。

---

## 八、附录：改用普通实体类（非 record）的写法

如果你不熟悉 record，也可以使用普通实体类（POJO），两者在工具链路上完全等价——`JsonSchemaGenerator` 生成 schema 和 `JsonParser.fromJson` 反序列化靠的都是 Jackson，而 Jackson 对 record 和普通 POJO 都支持。

### 关键前提：必须有无参构造器 + getter/setter

执行工具时，框架用 `JsonParser.fromJson(arguments, 实体类.class)` 把 `{"city":"San Francisco"}` 反序列化成对象。普通类要能被反序列化，必须满足：

- **无参构造器**（`public WeatherRequest() {}`）
- **getter / setter**（或 public 字段）

如果写一个「只有全参构造器、没有无参构造器」的类（很多人习惯这样写实体类），会直接报反序列化错误。record 之所以省心，是因为它天生带全参构造器，Jackson 2.12+ 原生就能反序列化，无需额外处理。

### 修改 `WeatherTool.java`（实体类版本）

```java
import java.util.function.Function;

public class WeatherTool implements Function<WeatherTool.WeatherRequest, String> {

    // 注意：必须有无参构造器 + getter/setter
    public static class WeatherRequest {
        private String city;

        public WeatherRequest() {}          // ← 无参构造器必须有

        public String getCity() { return city; }

        public void setCity(String city) { this.city = city; }
    }

    @Override
    public String apply(WeatherRequest request) {
        return "It's always sunny in " + request.getCity() + "!";
    }
}
```

### 修改 `step01_firest.java`（实体类版本）

```java
ToolCallback weatherTool = FunctionToolCallback.builder("get_weather", new WeatherTool())
        .description("Get weather for a given city")
        .inputType(WeatherTool.WeatherRequest.class)   // 原来是 inputType(String.class)
        .build();
```

### 若使用 Lombok

用 `@Data` 或 `@Getter @Setter @NoArgsConstructor` 也能省掉手写，但务必确保 `@NoArgsConstructor` 在（`@Data` 本身不带无参构造器，需要显式加 `@NoArgsConstructor`）。

### record 与普通实体类对比

| 维度 | record | 普通实体类 |
|---|---|---|
| 代码量 | 一行搞定 | 需手写无参构造、getter/setter，较冗长 |
| 不可变性 | 天然不可变，更安全 | 可变，字段可能被外部修改 |
| equals/hashCode/toString | 自动生成 | 需手写或依赖 Lombok |
| 反序列化要求 | 无额外要求 | **必须有无参构造器**（否则报错） |
| 灵活性 | 不能加校验逻辑、不能继承 | 可加字段校验、默认值、继承等 |

**结论**：工具入参这种「只用来传数据、不存状态」的场景，record 更省事、更安全，是官方和社区的主流推荐；但普通实体类完全可用，只要记得无参构造器这个坑即可。

---

## 九、概念详解：`function.arguments`、JSON Schema 与完整调用流程

这一节把前面反复提到的几个概念拆开讲清楚，方便理解整条链路。

### 9.1 `function.arguments` 到底是什么

先说结论：**`function.arguments` 是大模型「想调用某个工具时」，告诉框架的调用参数；它的值是一段 JSON 字符串，且内容必须是一个 JSON 对象（`{...}`）。**

关键点是：**大模型自己不会去执行你的 Java 工具**。它只是「表达」一个调用意图。这个过程遵循 OpenAI 兼容协议（DashScope 的 `/compatible-mode/v1` 就是这一套）。

当模型决定调用工具时，它返回的不是普通回答，而是一条带 `tool_calls` 的特殊消息，大致长这样：

```json
{
  "role": "assistant",
  "content": null,
  "tool_calls": [
    {
      "id": "call_1",
      "type": "function",
      "function": {
        "name": "get_weather",
        "arguments": "{\"city\": \"San Francisco\"}"
      }
    }
  ]
}
```

这里：

- `function.name` —— 要调哪个工具（对应你 `FunctionToolCallback.builder("get_weather", ...)` 里的名字）。
- `function.arguments` —— 调用参数。**注意它本身是字符串**，但这个字符串的内容必须是一段合法 JSON（这里是 `{"city":"San Francisco"}`）。

然后**由框架**把这段 JSON 字符串解析出来，反序列化成你的 Java 对象（`Request`），再去调用你的 `apply(...)` 方法。所以：

> 你的 `apply()` 方法根本不认识 `function.arguments` 这个东西——它拿到的已经是框架解析好的 `Request` 对象。

回到你的问题「千问调用工具时传递的是 JSON 是吗」——是的。协议的硬性要求就是：`function.arguments` 的内容必须是一个 **JSON 对象**（带字段名），形如 `{"city":"San Francisco"}`，而不是一个裸字符串 `"San Francisco"`。

至于「之前是不是可以传字符串」——准确说不是「以前允许字符串、现在不允许」这么简单。OpenAI 兼容协议从一开始就要求 arguments 是 JSON 对象。`inputType(String.class)` 这个写法的原始意图是「让模型返回一个裸字符串参数」，这本身就与「arguments 必须是 JSON 对象」的协议要求冲突。文档里还保留这个写法，只能说明文档没跟上协议（或沿袭自某个更宽松的早期演示）；至于某个具体旧版本是否真能跑通，无法从当前代码确证，但可以确定的是：**在 1.1.2.0 + qwen-plus 的兼容协议下，`String.class` 这条路径是跑不通的**。

### 9.2 为什么 `String.class` 会生成 `{"type":"string"}`（裸字符串 schema）

这里需要先理解 **JSON Schema** 是什么。

框架要把你的工具「介绍」给大模型，就得告诉它：这个工具接收的参数长什么样。它用一个 **JSON Schema** 来描述这个「形状」。JSON Schema 描述的是结构类型：

- 「是一个对象，里面有字段 `city`，类型是字符串」→ 对应 record/POJO
- 「就是一个裸字符串」→ 对应 `String.class`
- 「就是一个数字」→ 对应 `Integer.class`

当你写 `inputType(WeatherTool.Request.class)`（`Request` 是 `record Request(String city)`）时，框架生成的 schema 是：

```json
{
  "type": "object",
  "properties": {
    "city": { "type": "string" }
  },
  "required": ["city"]
}
```

这是「对象 + 带名字的字段」。

而当你写 `inputType(String.class)` 时，`JsonSchemaGenerator` 对 Java 的 `String` 类型生成的就是：

```json
{ "type": "string" }
```

也就是你说的「裸字符串类型」——它表达的意思是「参数就是一个字符串，没有对象外壳、没有字段名」。

**它本身没有「错误」**，从「描述一个 String」的角度它完全正确。问题在于它跟协议要求对不上：协议要求 arguments 是 JSON **对象**，而 `{"type":"string"}` 告诉模型「参数是裸字符串」。于是模型照着这个（错误的）描述去组织参数时，产出的东西就不符合「必须是 JSON 对象」的校验。

所以你的理解需要修正一点：**不是「强行指定 String 导致无法调用工具」，而是「String 生成的 schema 描述错了参数形状，模型据此产出的 arguments 不符合协议，被服务端拦截」**。模型依然会尝试调用，只是调用的参数格式被判为非法。

### 9.3 一次正常、完整的工具调用长什么样（ReAct 全流程）

`ReactAgent`（ReAct = Reasoning + Acting）就是反复执行下面这套循环。以「查询旧金山天气」为例：

**第 1 步：注册工具（framework → 模型）**

框架把你的工具定义放进请求发给模型：

```json
{
  "model": "qwen-plus",
  "messages": [{"role": "user", "content": "what is the weather in San Francisco"}],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "Get weather for a given city",
        "parameters": {
          "type": "object",
          "properties": { "city": { "type": "string" } },
          "required": ["city"]
        }
      }
    }
  ]
}
```

`parameters` 就是你工具入参的 JSON Schema（9.2 里那个 `{"type":"object",...}`）。

**第 2 步：模型决定调用工具（模型 → framework）**

模型判断「我需要先查天气」，于是返回 `tool_calls`（不是最终答案）：

```json
{
  "role": "assistant",
  "content": null,
  "tool_calls": [
    {
      "id": "call_1",
      "type": "function",
      "function": {
        "name": "get_weather",
        "arguments": "{\"city\": \"San Francisco\"}"
      }
    }
  ]
}
```

因为第 1 步的 `parameters` 正确描述了「对象 + `city` 字段」，模型就能准确地填出 `{"city":"San Francisco"}`。

**第 3 步：框架解析并执行工具（framework 内部）**

框架读 `name` 找到你的工具，读 `arguments` 反序列化成 Java 对象，再调你的方法：

```java
// arguments = "{\"city\":\"San Francisco\"}"
JsonParser.fromJson(arguments, WeatherTool.Request.class)  // → Request("San Francisco")
weatherTool.apply(request, toolContext)                     // → "It's always sunny in San Francisco!"
```

**第 4 步：把结果回填给模型（framework → 模型）**

```json
{"role": "tool", "tool_call_id": "call_1", "content": "It's always sunny in San Francisco!"}
```

**第 5 步：模型生成最终回答（模型 → framework）**

模型拿到工具结果后，组织出自然语言答案，例如 `"The weather in San Francisco is sunny!"`。此时不再带 `tool_calls`，循环结束，答案返回给你。

### 9.4 回到本案例：错在哪一步

把上面的流程套进去，本案例崩在**第 1 步 → 第 2 步**之间：

- 因为 `inputType(String.class)`，第 1 步发出去的 `parameters` 变成了 `{"type":"string"}`（裸字符串，没有 `city` 字段）。
- 模型不知道要往哪个字段名填值，于是第 2 步返回的 `arguments` 是空/非法的（不是合法的 JSON 对象）。
- 服务端在收到模型这条 `tool_calls` 时校验 `function.arguments`，发现不是合法 JSON 对象，直接返回 400：

```
The "function.arguments" parameter of the code model must be in JSON format.
```

改成 `Request` record 后，第 1 步的 `parameters` 变成正确的对象 schema，第 2 步模型就能产出 `{"city":"San Francisco"}`，整条链路就通了。
