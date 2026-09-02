package org.cvicse.saa.learn.step04_models;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

public class ch03_ModelBaseUseRuntimeOptions {


    public static void main(String[] args) {
        starter();
    }

    public static void starter() {

        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel("qwen-plus")           // 模型名称
                .withTemperature(0.7)              // Temperature 参数
                .withMaxToken(2000)                // 最大令牌数
                .withTopP(0.9)                     // Top-P 采样
                .build();

        // 创建带有特定选项的 Prompt
        DashScopeChatOptions runtimeOptions = DashScopeChatOptions.builder()
                .withTemperature(0.3)  // 更低的温度，更确定的输出
                .withMaxToken(500)
                .build();

        Prompt prompt = new Prompt(
                new UserMessage("用一句话总结Java的特点"),
                runtimeOptions
        );

        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
//                .defaultOptions(options)
                .build();

        ChatResponse response = chatModel.call(prompt);
        System.out.println(response);
    }
}
