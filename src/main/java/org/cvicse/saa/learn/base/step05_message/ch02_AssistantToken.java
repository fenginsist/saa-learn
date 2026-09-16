package org.cvicse.saa.learn.base.step05_message;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

public class ch02_AssistantToken {

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

        ChatResponse response = chatModel.call(new Prompt("旧金山的天气怎么样"));
        System.out.println(response);
        ChatResponseMetadata metadata = response.getMetadata();

        // 访问使用信息
        if (metadata != null && metadata.getUsage() != null) {
            System.out.println("Input tokens: " + metadata.getUsage().getPromptTokens());
            System.out.println("Output tokens: " + metadata.getUsage().getCompletionTokens());
            System.out.println("Total tokens: " + metadata.getUsage().getTotalTokens());
        }

    }
}
