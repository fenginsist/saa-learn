package org.cvicse.saa.learn.step05_message;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

public class ch06_ReactAgentUseMessage {


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
                .systemPrompt("你是一个有帮助的助手")
                .build();

        // 使用字符串
        AssistantMessage response1 = agent.call("你好");

        // 使用 UserMessage
        UserMessage userMsg = new UserMessage("帮我写一首诗");
        AssistantMessage response2 = agent.call(userMsg);

        // 使用消息列表
        List<Message> messages = List.of(
                new UserMessage("我喜欢春天"),
                new UserMessage("写一首关于春天的诗")
        );
        AssistantMessage response3 = agent.call(messages);
    }
}
