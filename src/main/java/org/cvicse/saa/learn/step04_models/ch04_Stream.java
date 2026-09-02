package org.cvicse.saa.learn.step04_models;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

public class ch04_Stream {


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

        // 使用流式 API
        Flux<ChatResponse> responseStream = chatModel.stream(new Prompt("使用100字，解释Spring Boot的自动配置原理"));

        // 订阅并处理流式响应
        responseStream.subscribe(
                chatResponse -> {
                    String content = chatResponse.getResult()
                            .getOutput()
                            .getText();
                    System.out.println(content);
                    System.out.println(chatResponse);
                },
                error -> System.err.println("错误: " + error.getMessage()),
                    () -> System.out.println("流式响应完成")
                );

        // 等待异步流返回
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
