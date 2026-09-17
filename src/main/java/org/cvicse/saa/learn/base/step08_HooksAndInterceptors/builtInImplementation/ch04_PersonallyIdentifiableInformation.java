package org.cvicse.saa.learn.base.step08_HooksAndInterceptors.builtInImplementation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIType;
import com.alibaba.cloud.ai.graph.agent.hook.pii.RedactionStrategy;
import org.jetbrains.annotations.NotNull;

public class ch04_PersonallyIdentifiableInformation {

    public static void main(String[] args) {
        starter();
    }


    public static void starter() {

        DashScopeChatModel chatModel = getDashScopeChatModel();
        /**
         * 检测和处理对话中的个人身份信息。
         *
         * 适用场景：
         *
         * 具有合规要求的医疗保健和金融应用；
         * 需要清理日志的客户服务 Agent；
         * 任何处理敏感用户数据的应用程序。
         */
        PIIDetectionHook pii = PIIDetectionHook.builder()
                .piiType(PIIType.EMAIL)
                .strategy(RedactionStrategy.REDACT)
                .applyToInput(true)
                .build();

        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("secure_agent")
                .model(chatModel)
                .hooks(pii)
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
