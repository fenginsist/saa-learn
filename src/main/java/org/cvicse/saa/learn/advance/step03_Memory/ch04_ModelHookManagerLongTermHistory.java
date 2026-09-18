package org.cvicse.saa.learn.advance.step03_Memory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 下面的示例展示了如何使用 ModelHook 在模型调用前后自动加载和保存长期记忆。
 */
public class ch04_ModelHookManagerLongTermHistory {
    // 创建记忆拦截器



    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {

        ModelHook memoryInterceptor = new ModelHook() {
            @Override
            public String getName() {
                return "memory_interceptor";
            }


            @Override
            public HookPosition[] getHookPositions() {
                return new HookPosition[]{HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL};
            }

            @Override
            public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
                // 从配置中获取用户ID
                String userId = (String) config.metadata("user_id").orElse(null);
                if (userId == null) {
                    return CompletableFuture.completedFuture(Map.of());
                }

                Store store = config.store();
                // 从记忆存储中加载用户画像
                Optional<StoreItem> itemOpt = store.getItem(List.of("user_profiles"), userId);
                if (itemOpt.isPresent()) {
                    Map<String, Object> profile = itemOpt.get().getValue();

                    // 将用户上下文注入系统消息
                    String userContext = String.format(
                            "用户信息：姓名=%s, 年龄=%s, 邮箱=%s, 偏好=%s",
                            profile.get("name"),
                            profile.get("age"),
                            profile.get("email"),
                            profile.get("preferences")
                    );

                    // 获取消息列表
                    List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());
                    List<Message> newMessages = new ArrayList<>();

                    // 查找是否已存在 SystemMessage
                    SystemMessage existingSystemMessage = null;
                    int systemMessageIndex = -1;
                    for (int i = 0; i < messages.size(); i++) {
                        Message msg = messages.get(i);
                        if (msg instanceof SystemMessage) {
                            existingSystemMessage = (SystemMessage) msg;
                            systemMessageIndex = i;
                            break;
                        }
                    }

                    // 如果找到 SystemMessage，更新它；否则创建新的
                    SystemMessage enhancedSystemMessage;
                    if (existingSystemMessage != null) {
                        // 更新现有的 SystemMessage
                        enhancedSystemMessage = new SystemMessage(
                                existingSystemMessage.getText() + userContext
                        );
                    } else {
                        // 创建新的 SystemMessage
                        enhancedSystemMessage = new SystemMessage(userContext);
                    }

                    // 构建新的消息列表
                    if (systemMessageIndex >= 0) {
                        // 如果找到了 SystemMessage，替换它
                        for (int i = 0; i < messages.size(); i++) {
                            if (i == systemMessageIndex) {
                                newMessages.add(enhancedSystemMessage);
                            } else {
                                newMessages.add(messages.get(i));
                            }
                        }
                    } else {
                        // 如果没有找到 SystemMessage，在开头添加新的
                        newMessages.add(enhancedSystemMessage);
                        newMessages.addAll(messages);
                    }

                    return CompletableFuture.completedFuture(Map.of("messages", newMessages));
                }

                return CompletableFuture.completedFuture(Map.of());
            }

            @Override
            public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
                // 可以在这里实现对话后的记忆保存逻辑
                return CompletableFuture.completedFuture(Map.of());
            }
        };

        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 4. 创建 Agent
        ReactAgent agent = ReactAgent.builder()
                .name("memory_agent")
                .model(chatModel)
                .hooks(memoryInterceptor)
                .saver(new MemorySaver())
                .build();

        // 创建内存存储
        MemoryStore memoryStore = new MemoryStore();

        // 模拟数据，预先填充用户画像
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("name", "王小明");
        profileData.put("age", 28);
        profileData.put("email", "wang@example.com");
        profileData.put("preferences", List.of("喜欢咖啡", "喜欢阅读"));

        StoreItem profileItem = StoreItem.of(List.of("user_profiles"), "user_001", profileData);
        memoryStore.putItem(profileItem);

        RunnableConfig config = RunnableConfig.builder()
                .threadId("session_001")
                .addMetadata("user_id", "user_001")
                .store(memoryStore)
                .build();

        // Agent会自动加载用户画像信息
        Optional<OverAllState> invoke = agent.invoke("请介绍一下我的信息。", config);

        System.out.println("===== Agent执行完成 =====");

        invoke.ifPresent(state -> {

            System.out.println("\n========== Agent 执行链路 ==========");

            state.value("messages").ifPresent(messages -> {

                List<?> messageList = (List<?>) messages;

                for (Object message : messageList) {

                    System.out.println("\n------------------------------------");

                    // 根据你当前版本的 Message 类型进行判断
                    System.out.println(message);
                }
            });
        });
        /**
         * apiKey = sk-ws-H.EXXEMEX.Y2dm.MEUCIEXNlQepvp3qup3qIhm93T1vAOg-iRZ_jE93U8ZusX8sAiEAir78N9b9dGhpgafs_Vc_0NJAir88hd62l4OIj9KXR6k
         * ===== Agent执行完成 =====
         *
         * ========== Agent 执行链路 ==========
         *
         * ------------------------------------
         * UserMessage{content='请介绍一下我的信息。', metadata={messageType=USER}, messageType=USER}
         *
         * ------------------------------------
         * SystemMessage{textContent='用户信息：姓名=王小明, 年龄=28, 邮箱=wang@example.com, 偏好=[喜欢咖啡, 喜欢阅读]', messageType=SYSTEM, metadata={messageType=SYSTEM}}
         *
         * ------------------------------------
         * UserMessage{content='请介绍一下我的信息。', metadata={messageType=USER}, messageType=USER}
         *
         * ------------------------------------
         * AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=您好，王小明！😊
         * 根据您提供的信息，您的基本情况如下：
         *
         * - **姓名**：王小明
         * - **年龄**：28岁（正值充满活力与成长潜力的黄金阶段）
         */


    }

    @NotNull
    private static DashScopeChatModel getDashScopeChatModel() {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // =========================
        // 1. 创建 DashScope API
        // =========================
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // =========================
        // 2. 创建 ChatModel
        // =========================
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
        return chatModel;
    }
}
