package org.cvicse.saa.learn.advance.step01_contextEngineering;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.springframework.ai.chat.messages.SystemMessage;

import java.util.ArrayList;
import java.util.Collection;

// 从长期记忆加载用户偏好
class ch02_PersonalizedPromptInterceptor extends ModelInterceptor {
    private final UserPreferenceStore store;

    public ch02_PersonalizedPromptInterceptor(UserPreferenceStore store) {
        this.store = store;
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
        // 从运行时上下文获取用户ID
        String userId = getUserIdFromContext(request);

        // 从存储加载用户偏好
        UserPreferences prefs = store.getPreferences(userId);

        // 构建个性化提示
        String personalizedPrompt = buildPersonalizedPrompt(prefs);

        // 更新系统消息（参考 TodoListInterceptor 的实现方式）
        SystemMessage enhancedSystemMessage;
        if (request.getSystemMessage() == null) {
            enhancedSystemMessage = new SystemMessage(personalizedPrompt);
        } else {
            enhancedSystemMessage = new SystemMessage(
                    request.getSystemMessage().getText() + personalizedPrompt
            );
        }

        // 创建增强的请求
        ModelRequest enhancedRequest = ModelRequest.builder(request)
                .systemMessage(enhancedSystemMessage)
                .build();

        // 调用处理器
        return handler.call(enhancedRequest);
    }

    private String getUserIdFromContext(ModelRequest request) {
        // 相当于是从 RunnableConfig 中读取提取用户ID，所以agent调用时要设置 user-id
        return request.getContext().get("user-id").toString(); // 简化示例
    }

    private String buildPersonalizedPrompt(UserPreferences prefs) {
        StringBuilder prompt = new StringBuilder("你是一个有用的助手。");

        if (prefs.getCommunicationStyle() != null) {
            prompt.append("沟通风格：").append(prefs.getCommunicationStyle());
        }

        if (prefs.getLanguage() != null) {
            prompt.append("使用语言：").append(prefs.getLanguage());
        }

        if (!prefs.getInterests().isEmpty()) {
            prompt.append("用户兴趣：").append(String.join(", ", prefs.getInterests().toString()));
        }

        return prompt.toString();
    }

    @Override
    public String getName() {
        return "PersonalizedPromptInterceptor";
    }

    public static class UserPreferenceStore {

        public UserPreferences getPreferences(String userId) {
            return null;
        }
    }

    public static class UserPreferences {

        public Object getCommunicationStyle() {
            return null;
        }

        public Object getLanguage() {
            return null;
        }

        public Collection<Object> getInterests() {
            return new ArrayList<>();
        }
    }
}
