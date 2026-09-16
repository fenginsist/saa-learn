package org.cvicse.saa.learn.advance.step01_contextEngineering;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.List;

// 创建一个模型拦截器，根据对话长度调整系统提示
public class ch01_StateAwarePromptInterceptor extends ModelInterceptor {
    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        List<Message> messages = request.getMessages();
        int messageCount = messages.size();

        // 基础提示
        String basePrompt = "你是一个有用的助手。";

        // 根据消息数量调整提示
        if (messageCount > 10) {
            basePrompt += "这是一个长对话 - 请尽量保持精准简捷。";
        }

        // 更新系统消息（参考 TodoListInterceptor 的实现方式）
        SystemMessage enhancedSystemMessage;
        if (request.getSystemMessage() == null) {
            enhancedSystemMessage = new SystemMessage(basePrompt);
        } else {
            enhancedSystemMessage = new SystemMessage(
                    request.getSystemMessage().getText() +  basePrompt
            );
        }

        // 创建增强的请求
        ModelRequest enhancedRequest = ModelRequest.builder(request)
                .systemMessage(enhancedSystemMessage)
                .build();

        // 调用处理器
        return handler.call(enhancedRequest);
    }

    @Override
    public String getName() {
        return "StateAwarePromptInterceptor";
    }
}