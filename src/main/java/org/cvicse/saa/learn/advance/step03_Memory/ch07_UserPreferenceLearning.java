package org.cvicse.saa.learn.advance.step03_Memory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class ch07_UserPreferenceLearning {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        ReactAgent agent = ReactAgent.builder()
                .name("learning_agent")
                .model(chatModel)
                .hooks(new ch07_PreferenceLearningHook())
                .saver(new MemorySaver())
                .build();

        MemoryStore memoryStore = new MemoryStore();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("learning_thread")
                .addMetadata("user_id", "user_004")
                .store(memoryStore)
                .build();

        // 用户表达偏好
        Optional<OverAllState> invoke = agent.invoke("我喜欢喝绿茶。", config);
        System.out.println("第一个问题，invoke " + invoke.isPresent());

        Optional<OverAllState> invoke1 = agent.invoke("我偏好早上运动。", config);
        System.out.println("第二个问题，invoke1 " + invoke1.isPresent());

        // 验证偏好已被存储
        Optional<StoreItem> savedPrefs = memoryStore.getItem(List.of("user_data"), "user_004_preferences");
        if (savedPrefs.isPresent()) {
            // 偏好应该被保存到长期记忆中
            System.out.println(savedPrefs.toString());
        }

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
