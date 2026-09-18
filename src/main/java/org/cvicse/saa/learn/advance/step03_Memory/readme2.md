下面整理成一份可以直接沉淀到学习文档里的版本，重点保留**知识点 + 容易混淆的 QA**。

# Spring AI Alibaba：短期记忆与长期记忆

## 一、核心知识点

### 1. 短期记忆：MemorySaver

`MemorySaver` 用于保存和恢复 Agent 的 **State**，主要用于当前 `Thread` 的对话上下文。

```java
ReactAgent agent = ReactAgent.builder()
        .saver(new MemorySaver())
        .build();
```

核心特点：

- 通过 `threadId` 区分不同会话。
- 主要保存 `State` 中的 `messages` 等运行状态。
- Agent 每次 `invoke()` 后，框架会自动更新和保存 State。
- 下一次使用相同 `threadId` 时，框架会自动恢复历史 State。
- 一般不需要手动调用 MemorySaver 进行写入、读取和更新。

可以简单理解为：

> **MemorySaver = Agent 当前会话的短期记忆。**

---

### 2. 长期记忆：MemoryStore

`MemoryStore` 用于保存跨会话、长期有效的业务信息，例如：

- 用户姓名
- 职业
- 用户偏好
- 用户习惯
- 用户长期事实

示例：

```java
MemoryStore memoryStore = new MemoryStore();

Map<String, Object> data = new HashMap<>();
data.put("name", "李工程师");
data.put("occupation", "软件工程师");

StoreItem item =
        StoreItem.of(
                List.of("profiles"),
                "user_001",
                data
        );

memoryStore.putItem(item);
```

读取：

```java
memoryStore.getItem(
        List.of("profiles"),
        "user_001"
);
```

可以简单理解为：

> **MemoryStore = 用户长期信息的记忆存储。**

---

### 3. RunnableConfig 的作用

`RunnableConfig` 是 Agent 本次运行的运行时配置，可以携带：

```java
RunnableConfig config = RunnableConfig.builder()
        .threadId("learning_thread")
        .addMetadata("user_id", "user_004")
        .store(memoryStore)
        .build();
```

其中：

- `threadId`：标识当前对话线程。
- `metadata`：携带用户 ID 等运行时信息。
- `store`：向 Agent/Hook 提供长期记忆 Store。

因此：

> **RunnableConfig 本身不是记忆，而是把 Agent 运行所需要的上下文和资源传递进去。**

---

## 二、短期记忆的读写机制

### 1. 短期记忆写入

通常不需要手动操作 `MemorySaver`。

调用：

```java
agent.invoke("我今天在做 Spring 项目。", config);
```

框架会自动将本次对话加入 State，并在执行后保存。

大致流程：

```text
agent.invoke()
    ↓
恢复 thread 对应的 State
    ↓
加入当前 UserMessage
    ↓
Agent 执行
    ↓
产生 AssistantMessage
    ↓
更新 State
    ↓
MemorySaver 保存
```

---

### 2. 短期记忆读取

下一次使用相同的 `threadId`：

```java
agent.invoke("我刚才在做什么？", config);
```

框架会自动恢复之前的 State。

例如：

```text
threadId = learning_thread

messages：
    USER：我今天在做 Spring 项目。
    ASSISTANT：......
    USER：我刚才在做什么？
```

因此 Agent 可以利用之前的对话上下文。

---

### 3. 短期记忆获取方式

在 Hook 中可以从：

```java
state.value("messages")
```

获取当前 State 中的消息。

例如：

```java
List<Message> messages =
        (List<Message>) state.value("messages")
                .orElse(new ArrayList<>());
```

这里不是 Hook 直接去读取 `MemorySaver`。

实际关系是：

```text
MemorySaver
    ↓
保存/恢复 State
    ↓
OverAllState
    ↓
state.value("messages")
```

---

## 三、长期记忆的读写机制

长期记忆通常需要业务代码主动操作。

### 写入

```java
memoryStore.putItem(item);
```

### 读取

```java
memoryStore.getItem(namespace, key);
```

### 更新

通常是：

```text
读取原来的数据
    ↓
修改数据
    ↓
再次 putItem()
```

因此：

> **短期记忆主要由 Agent 框架自动维护；长期记忆通常由业务代码主动提取、读取、更新和保存。**

---

## 四、短期记忆和长期记忆对比

| 对比项 | 短期记忆 | 长期记忆 |
|---|---|---|
| 实现 | `MemorySaver` | `MemoryStore` |
| 核心数据 | Agent State | 用户长期信息 |
| 典型内容 | 对话消息、运行状态 | 用户画像、偏好、事实 |
| 标识 | `threadId` | namespace + key / userId |
| 写入 | 通常自动 | 通常主动 |
| 读取 | Agent 自动恢复 | 代码主动 `getItem()` |
| 更新 | Agent 执行时自动更新 State | 业务代码主动更新 |
| 生命周期 | 当前 Thread / 会话 | 跨会话 |
| 典型用途 | “刚才聊了什么？” | “这个用户长期喜欢什么？” |

---

# 五、MemoryStore 是否应该只有一个实例？

案例中应该尽量使用**同一个 MemoryStore 实例**。

例如：

```java
MemoryStore memoryStore = new MemoryStore();
```

然后：

```java
config.store(memoryStore);
```

Hook 中：

```java
Store memoryStore = config.store();
```

最后验证：

```java
memoryStore.getItem(...);
```

三处实际使用的是同一个 Store。

---

## 六、为什么不能在 Hook 里再次 new MemoryStore？

错误方式：

```java
public class PreferenceHook extends ModelHook {

    MemoryStore memoryStore = new MemoryStore();

}
```

Main 中又：

```java
MemoryStore memoryStore = new MemoryStore();
```

这样实际上是：

```text
MemoryStore A
    ↓
Hook

MemoryStore B
    ↓
RunnableConfig
```

两个对象不是同一个实例。

因此 Hook 读取的是 A，而 `config.store()` 和最后验证使用的是 B，容易造成数据不一致。

---

## 七、推荐的设计方式

Main 中创建一次：

```java
MemoryStore memoryStore = new MemoryStore();
```

放入：

```java
RunnableConfig config = RunnableConfig.builder()
        .store(memoryStore)
        .build();
```

Hook 中不要自己 `new MemoryStore()`，而是：

```java
Store memoryStore = config.store();
```

这样：

```text
Main
 │
 └── 创建 MemoryStore
          │
          ├── RunnableConfig.store()
          │          ↓
          │       Hook
          │          ↓
          │    config.store()
          │
          └── 最后验证
```

整个 Agent 使用同一个 Store。

---

# 八、官方案例为什么可以直接使用外部 MemoryStore？

官方案例通常类似：

```java
MemoryStore memoryStore = new MemoryStore();

ModelHook hook = new ModelHook() {

    @Override
    public ... afterModel(...) {
        memoryStore.getItem(...);
        memoryStore.putItem(...);
    }
};
```

因为匿名内部类可以访问外部作用域中的 `memoryStore`。

所以这里仍然只有一个：

```text
MemoryStore
```

而不是 Hook 自己创建一个新的。

---

# 九、真实项目中的 MemoryStore

案例中的：

```java
new MemoryStore()
```

主要用于学习、演示和测试。

真实项目通常需要考虑持久化，例如将长期记忆保存到数据库或其他持久化存储中。

核心思想是：

```text
Agent
  ↓
Store 接口
  ↓
具体存储实现
  ↓
数据库 / 其他持久化存储
```

Agent 不需要关心底层具体存储在哪里。

---

# 十、短期记忆 → 长期记忆

实际 Agent 中经常存在这样的流程：

```text
用户对话
    ↓
短期记忆 MemorySaver
    ↓
Hook 分析对话
    ↓
发现用户表达了长期有效的信息
    ↓
提取用户偏好/事实
    ↓
MemoryStore
    ↓
保存长期记忆
```

例如：

```text
用户：
我喜欢喝绿茶。

       ↓

短期记忆：
保存本轮对话

       ↓

AFTER_MODEL Hook：
发现“喜欢”属于用户偏好

       ↓

长期记忆：
user_004_preferences
    └── 我喜欢喝绿茶。
```

这就是**从短期记忆中提取信息，并转化为长期记忆**。

---

# 十一、案例中的一个注意事项

如果 `AFTER_MODEL` 每次都遍历：

```java
state.value("messages")
```

而 `messages` 包含整个历史对话，那么以前已经处理过的：

```text
我喜欢喝绿茶。
```

可能在后续每轮执行时再次被发现并写入长期记忆。

因此真实项目需要考虑：

- 只处理本轮新增消息。
- 去重。
- 判断信息是否已经存在。
- 对长期记忆进行更新而不是简单追加。
- 可以使用 LLM/NLP 判断哪些信息值得长期保存。

---

# 十二、最终记忆口诀

```text
MemorySaver
    ↓
短期
    ↓
Thread
    ↓
State / messages
    ↓
框架自动维护


MemoryStore
    ↓
长期
    ↓
User
    ↓
Profile / Preference / Facts
    ↓
业务代码主动管理


RunnableConfig
    ↓
运行时配置
    ↓
threadId + userId + Store
```

### 一句话总结

> **MemorySaver 负责“记住这次聊天”，MemoryStore 负责“记住这个用户”，RunnableConfig 负责“把本次运行需要的 Thread、用户信息和长期记忆 Store 传给 Agent”。**