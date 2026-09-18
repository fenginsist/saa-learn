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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 跨会话记忆
 * 同一用户在不同会话中应该能够访问相同的长期记忆。
 */
public class ch06_CrossSessionMemory {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 定义请求和响应记录
        record SaveMemoryRequest(List<String> namespace, String key, Map<String, Object> value) {}
        record GetMemoryRequest(List<String> namespace, String key) {}
        record MemoryResponse(String message, Map<String, Object> value) {}

        // 创建记忆存储和工具
        MemoryStore memoryStore = new MemoryStore();

        ToolCallback saveMemoryTool = FunctionToolCallback.builder("saveMemory",
                        (BiFunction<SaveMemoryRequest, ToolContext, MemoryResponse>) (request, context) -> {
                            StoreItem item = StoreItem.of(request.namespace(), request.key(), request.value());
                            System.out.println("context:" + context.getContext());
                            RunnableConfig runnableConfig = (RunnableConfig) context.getContext().get("_AGENT_CONFIG_");
                            Store store = runnableConfig.store();
                            store.putItem(item);
                            return new MemoryResponse("已保存", request.value());
                        })
                .description("保存到长期记忆")
                .inputType(SaveMemoryRequest.class)
                .build();

        ToolCallback getMemoryTool = FunctionToolCallback.builder("getMemory",
                        (BiFunction<GetMemoryRequest, ToolContext, MemoryResponse>) (request, context) -> {
                            RunnableConfig runnableConfig = (RunnableConfig) context.getContext().get("_AGENT_CONFIG_");
                            Store store = runnableConfig.store();
                            Optional<StoreItem> itemOpt = store.getItem(request.namespace(), request.key());
                            return new MemoryResponse(
                                    itemOpt.isPresent() ? "找到" : "未找到",
                                    itemOpt.map(StoreItem::getValue).orElse(Map.of())
                            );
                        })
                .description("从长期记忆获取")
                .inputType(GetMemoryRequest.class)
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("session_agent")
                .model(chatModel)
                .tools(saveMemoryTool, getMemoryTool)
                .saver(new MemorySaver())
                .build();

        // 会话1：保存信息
        RunnableConfig session1 = RunnableConfig.builder()
                .threadId("session_morning")
                .addMetadata("user_id", "user_003")
                .store(memoryStore)
                .build();

        Optional<OverAllState> invoke = agent.invoke(
                "记住我的密码是 secret123。用 saveMemory 保存，namespace=['credentials'], key='user_003_password', value={'password': 'secret123'}。",
                session1
        );
        System.out.println("invoke:" + invoke);

        // 会话2：检索信息（不同的线程，同一用户）
        RunnableConfig session2 = RunnableConfig.builder()
                .threadId("session_afternoon")
                .addMetadata("user_id", "user_003")
                .store(memoryStore)
                .build();

        Optional<OverAllState> invoke1 = agent.invoke(
                "我的密码是什么？用 getMemory 获取，namespace=['credentials'], key='user_003_password'。",
                session2
        );
        System.out.println("invoke:" + invoke1);
        // 长期记忆在不同会话间持久化


        /**
         * apiKey = sk-ws-H.EXXEMEX.Y2dm.MEUCIEXNlQepvp3qup3qIhm93T1vAOg-iRZ_jE93U8ZusX8sAiEAir78N9b9dGhpgafs_Vc_0NJAir88hd62l4OIj9KXR6k
         * context:{_AGENT_CONFIG_=RunnableConfig{ threadId=session_morning, checkPointId=a8b5505c-2f09-4d44-8d19-d885d0b56f38, nextNode=null, streamMode=VALUES }, _AGENT_STATE_FOR_UPDATE_={}, _stream_=false, _AGENT_=session_agent, user_id=user_003, _AGENT_STATE_={"OverAllState":{"data":{"_graph_execution_id_":"0f73fcdb-1fa6-49b4-81e5-bda09dc1e1ec","input":"记住我的密码是 secret123。用 saveMemory 保存，namespace=['credentials'], key='user_003_password', value={'password': 'secret123'}。","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"记住我的密码是 secret123。用 saveMemory 保存，namespace=['credentials'], key='user_003_password', value={'password': 'secret123'}。"},{"messageType":"ASSISTANT","metadata":{"finishReason":"TOOL_CALLS","search_info":"","role":"ASSISTANT","id":"d0759383-9544-9819-9944-8a12f8a5964b","messageType":"ASSISTANT","reasoningContent":""},"toolCalls":[{"id":"call_a0642bbce5e94a19a6d9f9","type":"function","name":"saveMemory","arguments":"{\"key\": \"user_003_password\", \"namespace\": [\"credentials\"], \"value\": {\"password\": \"secret123\"}}"}],"media":[],"text":""}]}}}}
         * invoke:Optional[{"OverAllState":{"data":{"_graph_execution_id_":"0f73fcdb-1fa6-49b4-81e5-bda09dc1e1ec","input":"记住我的密码是 secret123。用 saveMemory 保存，namespace=['credentials'], key='user_003_password', value={'password': 'secret123'}。","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"记住我的密码是 secret123。用 saveMemory 保存，namespace=['credentials'], key='user_003_password', value={'password': 'secret123'}。"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"TOOL_CALLS","id":"d0759383-9544-9819-9944-8a12f8a5964b","reasoningContent":""},"toolCalls":[{"id":"call_a0642bbce5e94a19a6d9f9","type":"function","name":"saveMemory","arguments":"{\"key\": \"user_003_password\", \"namespace\": [\"credentials\"], \"value\": {\"password\": \"secret123\"}}"}],"media":[],"text":""},{"messageType":"TOOL","metadata":{"messageType":"TOOL"},"responses":[{"id":"call_a0642bbce5e94a19a6d9f9","name":"saveMemory","responseData":"{\"message\":\"已保存\",\"value\":{\"password\":\"secret123\"}}"}],"text":""},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"5d4d2f1a-7086-9a92-8643-b27dab35686e","reasoningContent":""},"toolCalls":[],"media":[],"text":"已成功将您的密码 `secret123` 保存到长期记忆中，键为 `user_003_password`，命名空间为 `['credentials']`。如有需要，可随时调用 `getMemory` 获取。"}]}}}]
         * invoke:Optional[{"OverAllState":{"data":{"_graph_execution_id_":"7d435f53-6347-4dcf-a8cc-9ca2fc695d69","input":"我的密码是什么？用 getMemory 获取，namespace=['credentials'], key='user_003_password'。","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"我的密码是什么？用 getMemory 获取，namespace=['credentials'], key='user_003_password'。"},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"TOOL_CALLS","id":"8c9f76be-521e-9072-9d6f-ea2dd0a2014a","reasoningContent":""},"toolCalls":[{"id":"call_63c43464795146d78c5758","type":"function","name":"getMemory","arguments":"{\"key\": \"user_003_password\", \"namespace\": [\"credentials\"]}"}],"media":[],"text":""},{"messageType":"TOOL","metadata":{"messageType":"TOOL"},"responses":[{"id":"call_63c43464795146d78c5758","name":"getMemory","responseData":"{\"message\":\"找到\",\"value\":{\"password\":\"secret123\"}}"}],"text":""},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"b93a924f-55de-9d49-902e-b8d18cc43ce3","reasoningContent":""},"toolCalls":[],"media":[],"text":"您的密码是：secret123。"}]}}}]
         *
         *
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
