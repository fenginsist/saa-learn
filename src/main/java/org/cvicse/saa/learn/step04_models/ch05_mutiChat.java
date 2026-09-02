package org.cvicse.saa.learn.step04_models;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

public class ch05_mutiChat {


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

        // 创建对话历史
        List<Message> messages = List.of(
                new SystemMessage("你是一个Java专家"),
                new UserMessage("什么是Spring Boot?, 100字即可"),
                new AssistantMessage("Spring Boot是..., 100字即可"),
                new UserMessage("它有什么优势?, 100字即可")
        );

        Prompt prompt = new Prompt(messages);

        // 使用流式 API
        Flux<ChatResponse> responseStream = chatModel.stream(prompt);

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
