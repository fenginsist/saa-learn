package org.cvicse.saa.learn.advance.step03_Memory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.*;
import java.util.function.BiFunction;

public class ch02_ReadLongMemoryIntheTool {

    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    // 定义请求和响应记录
    public record GetMemoryRequest(List<String> namespace, String key) {}
    public record MemoryResponse(String message, Map<String, Object> value) {}

    public static void starter() throws GraphRunnerException {

        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 1. 创建长期记忆存储
        MemoryStore store = new MemoryStore();

        // 2. 写入长期记忆，向存储中写入示例数据
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "张三");
        userData.put("language", "中文");

        StoreItem userItem = StoreItem.of(List.of("users"), "user_123", userData);
        store.putItem(userItem);

        /**
         * // 3. 创建查询长期记忆的 Tool，创建获取用户信息的工具
         */
        BiFunction<GetMemoryRequest, ToolContext, MemoryResponse> getUserInfoFunction =
                (request, context) -> {
                    System.out.println("context: " + context.getContext());
                    RunnableConfig runnableConfig = (RunnableConfig) context.getContext().get("_AGENT_CONFIG_");
                    System.out.println("runnableConfig: " + runnableConfig.toString());
                    Store memoryStore = runnableConfig.store();
                    Optional<StoreItem> itemOpt = memoryStore.getItem(request.namespace(), request.key());
                    System.out.println("itemOpt: " + itemOpt.toString());
                    if (itemOpt.isPresent()) {
                        Map<String, Object> value = itemOpt.get().getValue();
                        return new MemoryResponse("找到用户信息", value);
                    }
                    return new MemoryResponse("未找到用户", Map.of());
                };

        // 4. 创建 Tool
        ToolCallback getUserInfoTool = FunctionToolCallback.builder("getUserInfo", getUserInfoFunction)
                .description("查询用户信息")
                .inputType(GetMemoryRequest.class)
                .build();

        // 5. 创建 Agent
        ReactAgent agent = ReactAgent.builder()
                .name("memory_agent")
                .model(chatModel)
                .tools(getUserInfoTool)
                .saver(new MemorySaver())
                .build();

        // 6. 创建运行配置
        RunnableConfig config = RunnableConfig.builder()
                .threadId("session_001")
                .addMetadata("user_id", "user_123")
                .store(store)
                .build();

        // 7. 运行 Agent
        Optional<OverAllState> invoke = agent.invoke("查询用户信息，namespace=['users'], key='user_123'", config);
        System.out.println("===== Agent执行完成 =====");
        System.out.println("invoke：" + invoke.toString());
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
