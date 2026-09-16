package org.cvicse.saa.learn.base.step04_models;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

public class ch02_ModelBaseUsePrompt {


    public static void main(String[] args) {
        starter();
    }

    public static void starter() {

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

        // 创建 Prompt
        Prompt prompt = new Prompt(new UserMessage("用100字，解释什么是微服务架构"));

        // 调用并获取响应
        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        System.out.println(answer);
    }
}
