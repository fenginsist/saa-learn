package org.cvicse.saa.learn.step02_trueAgent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Optional;
import java.util.function.BiFunction;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

// 用户位置工具 - 使用上下文
public class UserLocationTool implements BiFunction<UserLocationTool.Request, ToolContext, String> {


    public record Request(String query) {}

    @Override
    public String apply(
            @ToolParam(description = "User query") UserLocationTool.Request query,
            ToolContext toolContext) {
        // 从上下文中获取用户信息
        String userId = "";
        if (toolContext != null && toolContext.getContext() != null) {
            RunnableConfig runnableConfig = (RunnableConfig) toolContext.getContext().get(AGENT_CONFIG_CONTEXT_KEY);
            Optional<Object> userIdObjOptional = runnableConfig.metadata("user_id");
            if (userIdObjOptional.isPresent()) {
                userId = (String) userIdObjOptional.get();
            }
        }
        if (userId == null) {
            userId = "1";
        }
        return "1".equals(userId) ? "Florida" : "San Francisco";
    }
}