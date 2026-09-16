package org.cvicse.saa.learn.base.step07_memory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;

public class ch04_TrimMessages {

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

        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
//                .tools(tools)
                .hooks(new ch04_MessageTrimmingHook())
                .saver(new MemorySaver())
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("1")
                .build();

        agent.call("你好，我叫 bob", config);
        agent.call("写一首关于猫的短诗", config);
        agent.call("现在对狗做同样的事情", config);
        AssistantMessage finalResponse = agent.call("我叫什么名字？", config);

        System.out.println(finalResponse.getText());
        // 输出：你的名字是 Bob。你之前告诉我的。

    }
}
