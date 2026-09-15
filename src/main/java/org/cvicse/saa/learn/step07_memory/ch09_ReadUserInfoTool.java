package org.cvicse.saa.learn.step07_memory;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;
import java.util.function.BiFunction;

public class ch09_ReadUserInfoTool implements BiFunction<ch09_ReadUserInfoTool.Request, ToolContext, String> {

    public record Request(String query) {}

    @Override
    public String apply(ch09_ReadUserInfoTool.Request query, ToolContext toolContext) {
        // 从上下文中获取用户信息
        Map<String, Object> context = toolContext.getContext();
        String userId = context.get("user_id").toString();

        if ("user_123".equals(userId)) {
            return "用户是 John Smith";
        } else {
            return "未知用户";
        }
    }
}
