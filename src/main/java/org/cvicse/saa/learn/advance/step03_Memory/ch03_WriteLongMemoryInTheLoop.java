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

public class ch03_WriteLongMemoryInTheLoop {

    // 定义请求记录
    public record SaveMemoryRequest(List<String> namespace, String key, Map<String, Object> value) {}
    public record MemoryResponse(String message, Map<String, Object> value) {}

    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();


        // 1. 创建长期记忆存储
        MemoryStore store = new MemoryStore();

        // 2. 创建保存用户信息的工具
        BiFunction<SaveMemoryRequest, ToolContext, MemoryResponse> saveUserInfoFunction =
                (request, context) -> {
                    RunnableConfig runnableConfig = (RunnableConfig) context.getContext().get("_AGENT_CONFIG_");
                    Store store2 = runnableConfig.store();
                    StoreItem item = StoreItem.of(request.namespace(), request.key(), request.value());
                    store2.putItem(item);
                    return new MemoryResponse("成功保存用户信息", request.value());
                };

        // 3. 创建写入长期记忆的 Tool
        ToolCallback saveUserInfoTool = FunctionToolCallback.builder("saveUserInfo", saveUserInfoFunction)
                .description("保存用户信息")
                .inputType(SaveMemoryRequest.class)
                .build();

        // 4. 创建 Agent
        ReactAgent agent = ReactAgent.builder()
                .name("save_memory_agent")
                .model(chatModel)
                .tools(saveUserInfoTool)
                .saver(new MemorySaver())
                .build();

        // 5. 创建运行配置
        RunnableConfig config = RunnableConfig.builder()
                .threadId("session_001")
                .addMetadata("user_id", "user_123")
                .store(store)
                .build();

        // 6. 运行 Agent
        Optional<OverAllState> invoke = agent.invoke("我叫张三，请保存我的信息。使用 saveUserInfo 工具，namespace=['users'], key='user_123', value={'name': '张三'}", config);
        System.out.println("===== Agent执行完成 =====");
        // 7. 可以直接访问存储获取值
        Optional<StoreItem> savedItem = store.getItem(List.of("users"), "user_123");
        if (savedItem.isPresent()) {
            Map<String, Object> savedValue = savedItem.get().getValue();
            System.out.println("直接访问存储获取值: " + savedValue);
        }

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
