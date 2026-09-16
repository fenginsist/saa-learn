package org.cvicse.saa.learn.base.step07_memory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;

public class ch07_DeleteOldMessages {

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

        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .systemPrompt("请简洁明了。")
                .hooks(new ch07_DeleteOldMessagesHook())
                .saver(new MemorySaver())
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("1")
                .build();

        // 第一次调用
        AssistantMessage message1 = agent.call("你好！我是 bob", config);
        System.out.println("message1 = " + message1.getText());
        // 输出：[('human', "你好！我是 bob"), ('assistant', '你好 Bob！很高兴见到你...')]

        // 第二次调用
        AssistantMessage message2 = agent.call("我叫什么名字？", config);
        System.out.println("message2 = " + message2.getText());
        // 输出：[('human', "我叫什么名字？"), ('assistant', '你的名字是 Bob...')]

    }
}
