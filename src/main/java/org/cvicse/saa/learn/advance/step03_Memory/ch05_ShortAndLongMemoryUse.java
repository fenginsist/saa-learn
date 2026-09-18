package org.cvicse.saa.learn.advance.step03_Memory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 短期记忆用于存储对话上下文，长期记忆用于存储持久化数据。下面的示例展示了如何同时使用两种记忆。
 */
public class ch05_ShortAndLongMemoryUse {


    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 创建Agent
        ReactAgent agent = ReactAgent.builder()
                .name("combined_memory_agent")
                .model(chatModel)
                .hooks(new ch05_CombinedMemoryHook())
                .saver(new MemorySaver()) // 短期记忆
                .build();

        // 创建记忆存储
        MemoryStore memoryStore = new MemoryStore();
        // 设置长期记忆
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("name", "李工程师");
        userProfile.put("occupation", "软件工程师");

        StoreItem profileItem = StoreItem.of(List.of("profiles"), "user_002", userProfile);
        memoryStore.putItem(profileItem);

        RunnableConfig config = RunnableConfig.builder()
                .threadId("combined_thread")
                .addMetadata("user_id", "user_002")
                .store(memoryStore)
                .build();

        // 短期记忆：在对话中记住
        Optional<OverAllState> invoke = agent.invoke("我今天在做一个 Spring 项目。", config);
        System.out.println("--------短期记忆：在对话中记住----------");
        System.out.println(invoke);

        // 提出需要同时使用两种记忆的问题
        Optional<OverAllState> invoke1 = agent.invoke("根据我的职业和今天的工作，给我一些建议。", config);
        System.out.println("--------长期记忆----------");
        System.out.println(invoke1);
        // 响应会同时使用长期记忆（职业）和短期记忆（Spring项目）

        /**
         *apiKey = sk-ws-H.EXXEMEX.Y2dm.MEUCIEXNlQepvp3qup3qIhm93T1vAOg-iRZ_jE93U8ZusX8sAiEAir78N9b9dGhpgafs_Vc_0NJAir88hd62l4OIj9KXR6k
         * beforeModel:模型调用之前的操作
         * --------短期记忆：在对话中记住----------
         * Optional[{"OverAllState":{"data":{"_graph_execution_id_":"76bb9862-402b-4212-a63e-890732eedb6c","input":"我今天在做一个 Spring 项目。","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"我今天在做一个 Spring 项目。"},{"messageType":"SYSTEM","metadata":{"messageType":"SYSTEM"},"text":"长期记忆：用户 李工程师, 职业: 软件工程师"},{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"我今天在做一个 Spring 项目。"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"982f9688-56e5-9a44-bacf-eef4d624e9bf","reasoningContent":""},"toolCalls":[],"media":[],"text":"你好，李工程师！很高兴听到你在做 Spring 项目 😊  \n不知道你目前是在搭建基础结构、集成某个模块（比如 Spring Boot + MyBatis / JPA / Redis），还是遇到了具体问题？比如：\n\n- 启动失败（如 `ApplicationContext` 初始化异常、Bean 注入失败）  \n- REST 接口返回 404 / 500 / CORS 问题  \n- 数据库连接或事务管理配置疑惑  \n- Spring Security 权限控制逻辑设计  \n- 或是想优化启动速度、打包部署（如 Docker 镜像构建）？\n\n欢迎随时分享代码片段、报错日志或你的目标（比如“想实现一个带 JWT 登录的微服务接口”），我可以帮你分析、调试或提供最佳实践建议。期待为你助力！🚀"}]}}}]
         * beforeModel:模型调用之前的操作
         * --------长期记忆----------
         * Optional[{"OverAllState":{"data":{"input":"根据我的职业和今天的工作，给我一些建议。","_graph_execution_id_":"59a018f1-04f1-4b95-ad8d-ff03f4d0bf95","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"我今天在做一个 Spring 项目。"},{"messageType":"SYSTEM","metadata":{"messageType":"SYSTEM"},"text":"长期记忆：用户 李工程师, 职业: 软件工程师"},{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"我今天在做一个 Spring 项目。"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"982f9688-56e5-9a44-bacf-eef4d624e9bf","reasoningContent":""},"toolCalls":[],"media":[],"text":"你好，李工程师！很高兴听到你在做 Spring 项目 😊  \n不知道你目前是在搭建基础结构、集成某个模块（比如 Spring Boot + MyBatis / JPA / Redis），还是遇到了具体问题？比如：\n\n- 启动失败（如 `ApplicationContext` 初始化异常、Bean 注入失败）  \n- REST 接口返回 404 / 500 / CORS 问题  \n- 数据库连接或事务管理配置疑惑  \n- Spring Security 权限控制逻辑设计  \n- 或是想优化启动速度、打包部署（如 Docker 镜像构建）？\n\n欢迎随时分享代码片段、报错日志或你的目标（比如“想实现一个带 JWT 登录的微服务接口”），我可以帮你分析、调试或提供最佳实践建议。期待为你助力！🚀"},{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"根据我的职业和今天的工作，给我一些建议。"},{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"我今天在做一个 Spring 项目。"},{"messageType":"SYSTEM","metadata":{"messageType":"SYSTEM"},"text":"长期记忆：用户 李工程师, 职业: 软件工程师长期记忆：用户 李工程师, 职业: 软件工程师"},{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"我今天在做一个 Spring 项目。"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"982f9688-56e5-9a44-bacf-eef4d624e9bf","reasoningContent":""},"toolCalls":[],"media":[],"text":"你好，李工程师！很高兴听到你在做 Spring 项目 😊  \n不知道你目前是在搭建基础结构、集成某个模块（比如 Spring Boot + MyBatis / JPA / Redis），还是遇到了具体问题？比如：\n\n- 启动失败（如 `ApplicationContext` 初始化异常、Bean 注入失败）  \n- REST 接口返回 404 / 500 / CORS 问题  \n- 数据库连接或事务管理配置疑惑  \n- Spring Security 权限控制逻辑设计  \n- 或是想优化启动速度、打包部署（如 Docker 镜像构建）？\n\n欢迎随时分享代码片段、报错日志或你的目标（比如“想实现一个带 JWT 登录的微服务接口”），我可以帮你分析、调试或提供最佳实践建议。期待为你助力！🚀"},{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"根据我的职业和今天的工作，给我一些建议。"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"e133bd3b-aa8e-9c48-8201-fd000ef62fc7","reasoningContent":""},"toolCalls":[],"media":[],"text":"李工程师，作为同为一线开发者的同行，结合你**软件工程师**的职业背景和今天正在推进 **Spring 项目** 的实际场景，我为你梳理了几条**务实、可立即落地的建议**——既兼顾工程效率，也着眼长期可维护性与技术成长：\n\n---\n\n✅ **1. 优先保障「可调试性」和「可观测性」**  \nSpring 项目初期容易陷入“能跑就行”，但稍复杂后排查问题成本陡增。建议今天就做三件事：  \n- ✅ 在 `application.yml` 中启用 `spring-boot-starter-actuator` + 开放 `health`, `beans`, `env`, `loggers` 端点（生产环境按需关闭）；  \n- ✅ 配置 `logging.level.com.yourpackage=DEBUG`，并在关键 Service/Controller 加上结构化日志（如用 `@Slf4j` + `log.debug(\"user {} invoked orderService, orderId={}\", userId, orderId)`）；  \n- ✅ 若涉及异步（`@Async`）或事务（`@Transactional`），务必验证其生效范围——加个断点或日志确认代理是否被正确织入。\n\n💡 *为什么重要？* —— 软件工程师的核心竞争力之一，是「快速定位根因」。日志+actuator 是你最轻量却最高效的“诊断听诊器”。\n\n---\n\n✅ **2. 主动约束分层边界（尤其对新手易踩的坑）**  \nSpring 项目常见腐化路径：Controller 直接调 DAO、Service 塞满业务逻辑、DTO 和 Entity 混用……建议今天花 10 分钟明确：  \n- ✅ `@Controller` 只做协议适配（接收请求、校验参数、返回响应），不碰业务；  \n- ✅ `@Service` 承载核心业务逻辑，**拒绝数据库操作**（交给 `@Repository`）；  \n- ✅ 严格区分 `DTO`（面向 API）、`VO`（面向前端）、`Entity`（面向数据库），用 `BeanUtils.copyProperties()` 或 MapStruct 显式转换——避免隐式耦合。\n\n💡 *为什么重要？* —— 这不是“过度设计”，而是为你后续写单元测试（Mock Repository）、做接口演进（DTO 版本兼容）、甚至迁移微服务（拆分模块）埋下干净接口。\n\n---\n\n✅ **3. 把「自动化验证」变成今日收工前的仪式感**  \n哪怕只写一个最简单的测试，也比零测试强：  \n- ✅ 用 `@SpringBootTest` + `@AutoConfigureTestDatabase` 启动最小上下文，测试一个典型 HTTP 接口（如 `GET /api/users`）；  \n- ✅ 或用 `@WebMvcTest` 测试 Controller 层（Mock Service），验证参数校验（`@Valid`）是否生效；  \n- ✅ 提交前 `mvn clean test` 运行一次——让 CI/CD 习惯从本地开始。\n\n💡 *为什么重要？* —— 对工程师而言，**可重复验证 = 可靠交付**。今天的 1 个测试，可能帮你下周省掉 2 小时回归排查。\n\n---\n\n🎯 **Bonus：一个小挑战（可选）**  \n如果你今天有 15 分钟余量，试试：  \n👉 在 `application.yml` 中添加：  \n```yaml\nspring:\n  main:\n    banner-mode: off\n    web-application-type: servlet\n  profiles:\n    active: dev\n```\n然后创建 `application-dev.yml`，把数据库密码等敏感配置移入其中，并用 `@Value(\"${db.url}\")` 注入——这是走向**配置治理**的第一小步 🌱\n\n---\n\n需要我帮你：  \n🔹 审阅某段 Spring 配置/代码是否符合最佳实践？  \n🔹 生成一个符合上述建议的「标准模块脚手架」（含分层结构 + 日志 + Actuator + 测试模板）？  \n🔹 或针对你当前卡点（比如“事务不回滚”、“循环依赖”、“Feign 调用超时”）给精准解法？  \n\n随时告诉我，李工！我们继续高效推进 👨‍💻✨"}]}}}]
         */

    }

    @NotNull
    private static DashScopeChatModel getDashScopeChatModel() {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // =========================
        // 创建 DashScope API
        // =========================
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // =========================
        // 创建 ChatModel
        // =========================
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
        return chatModel;
    }
}