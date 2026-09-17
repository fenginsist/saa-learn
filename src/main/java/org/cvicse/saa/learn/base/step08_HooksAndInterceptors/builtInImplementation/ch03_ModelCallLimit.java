package org.cvicse.saa.learn.base.step08_HooksAndInterceptors.builtInImplementation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import org.jetbrains.annotations.NotNull;

public class ch03_ModelCallLimit {

    public static void main(String[] args) {
        starter();
    }


    public static void starter() {

        DashScopeChatModel chatModel = getDashScopeChatModel();
        /**
         * 限制模型调用次数以防止无限循环或过度成本。
         *
         * 适用场景：
         *
         * 防止失控的 Agent 进行太多 API 调用；
         * 在生产部署中强制执行成本控制；
         * 在特定调用预算内测试 Agent 行为。
         */
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .hooks(ModelCallLimitHook.builder().runLimit(5).build())
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
