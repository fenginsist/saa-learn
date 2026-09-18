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