package org.cvicse.saa.learn.base.step07_memory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class ch09_ReadUserInfo {

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

        // 创建工具
        ToolCallback getUserInfoTool = FunctionToolCallback
                .builder("get_user_info", new ch09_ReadUserInfoTool())
                .description("查找用户信息")
                .inputType(ch09_ReadUserInfoTool.Request.class)
                .build();

        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .tools(getUserInfoTool)
                .interceptors(new ch09_ToolSchemaInterceptor())
                .saver(new MemorySaver())
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("1")
                .addMetadata("user_id", "user_123")
                .build();

        AssistantMessage response = agent.call("获取用户信息", config);
        System.out.println(response.getText());

    }
}
