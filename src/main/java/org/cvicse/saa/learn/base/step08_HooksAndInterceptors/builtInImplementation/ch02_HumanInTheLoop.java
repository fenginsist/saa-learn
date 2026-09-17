package org.cvicse.saa.learn.base.step08_HooksAndInterceptors.builtInImplementation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import org.jetbrains.annotations.NotNull;

public class ch02_HumanInTheLoop {

    public static void main(String[] args) {
        starter();
    }


    public static void starter() {

        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 创建 Human-in-the-Loop Hook
        HumanInTheLoopHook humanReviewHook = HumanInTheLoopHook.builder()
                .approvalOn("sendEmailTool", ToolConfig.builder().description("Please confirm sending the email.").build())
                .approvalOn("deleteDataTool",
                        ToolConfig.builder()
                                .description("Please confirm deleting the data.")
                                .build())
                .build();
        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .hooks(humanReviewHook)
                .build();



    }

    @NotNull
    private static DashScopeChatModel getDashScopeChatModel() {
        // =========================
        // 1. 创建 DashScope API
        // =========================
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // =========================
        // 2. 创建 ChatModel
        // =========================
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
        return chatModel;
    }
}
