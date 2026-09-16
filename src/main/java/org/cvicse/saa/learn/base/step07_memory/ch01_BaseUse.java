package org.cvicse.saa.learn.base.step07_memory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;

public class ch01_BaseUse {

    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // 创建 DashScope API 实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // 创建 ChatModel
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // 配置 checkpointer
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
//                .tools(getUserInfoTool)
                .saver(new MemorySaver())
                .build();

        // 使用 thread_id 维护对话上下文
        RunnableConfig config = RunnableConfig.builder()
                .threadId("1") // threadId 指定会话 ID
                .build();

        AssistantMessage message = agent.call("你好！我叫 Bob。", config);
        System.out.println(message.getText());


    }
}
