package org.cvicse.saa.learn.base.step05_message;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

public class ch03_AssistantStreamAndBlock {


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

        Flux<ChatResponse> responseStream = chatModel.stream(new Prompt("你好"));

        StringBuilder fullResponse = new StringBuilder();
        responseStream.subscribe(
                chunk -> {
                    String content = chunk.getResult().getOutput().getText();
                    fullResponse.append(content);
                    System.out.println(content);
                }
        );

        // 等待异步流返回
        try {
            Thread.sleep(5000);
            System.out.println("fullResponse = " + fullResponse.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
