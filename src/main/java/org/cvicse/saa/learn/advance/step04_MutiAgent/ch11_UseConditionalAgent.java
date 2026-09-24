package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class ch11_UseConditionalAgent {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        DashScopeChatModel chatModel = getDashScopeChatModel();

        // 创建两个分支Agent
        ReactAgent urgentAgent = ReactAgent.builder()
                .name("urgent_handler")
                .model(chatModel)
                .description("处理紧急请求")
                .instruction("你需要快速响应紧急情况...")
                .outputKey("urgent_result")
                .build();

        ReactAgent normalAgent = ReactAgent.builder()
                .name("normal_handler")
                .model(chatModel)
                .description("处理常规请求")
                .instruction("你可以详细分析和处理常规请求...")
                .outputKey("normal_result")
                .build();

        // 定义条件：检查输入是否包含"紧急"关键字
        Predicate<Map<String, Object>> isUrgent = state -> {

            Object input = state.get("input");

            if (input instanceof String) {
                String text = (String) input;
                System.out.println("========== Condition ==========");
                System.out.println("input = " + text);
                boolean urgent = text.contains("紧急")|| text.toLowerCase().contains("urgent");
                System.out.println("condition = " + urgent);
                return urgent;
            }

            return false;
        };

        // 创建条件路由Agent
        ch11_ConditionalAgent conditionalAgent = ch11_ConditionalAgent.builder()
                        .name("priority_router")
                        .description("根据紧急程度路由请求")
                        .condition(isUrgent)
                        .trueAgent(urgentAgent)
                        .falseAgent(normalAgent)
                        .build();

        // 使用
        Optional<OverAllState> result1 = conditionalAgent.invoke("这是一个紧急问题需要立即处理");
        System.out.println("result1=" + result1);
        // 会路由到 urgentAgent

        Optional<OverAllState> result2 = conditionalAgent.invoke("请帮我分析一下这个问题");
        System.out.println("result2=" + result2);
        // 会路由到 normalAgent

        /**
         *
         *
         * apiKey = sk-ws-H.EXXEMEX.Y2dm.MEUCIEXNlQepvp3qup3qIhm93T1vAOg-iRZ_jE93U8ZusX8sAiEAir78N9b9dGhpgafs_Vc_0NJAir88hd62l4OIj9KXR6k
         * ========== Condition ==========
         * input = 这是一个紧急问题需要立即处理
         * condition = true
         *
         * ========== ConditionalAgent ==========
         * input = 这是一个紧急问题需要立即处理
         * condition = true
         * route = true
         * ======================================
         * result1=Optional[{"OverAllState":{"data":{"_condition_result":"true","_graph_execution_id_":"76619689-1538-4fb9-8644-19c157508311","input":"这是一个紧急问题需要立即处理","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"这是一个紧急问题需要立即处理"},{"messageType":"USER","metadata":{"messageType":"USER"},"rendered":false,"text":"你需要快速响应紧急情况..."},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"e21ebd18-c616-9c20-a48d-e2b404a5bf7b","reasoningContent":""},"toolCalls":[],"media":[],"text":"我理解您可能有紧急需求，但请提供**具体、明确的紧急情况描述**（例如：医疗急救、火灾、事故、安全威胁等），以便我能为您提供**准确、合规且安全的指导**。\n\n⚠️ **重要提醒**：  \n- 若涉及**人身安全或生命危险**，请**立即拨打当地紧急电话**（如中国：110/120/119；国际通用：112）。  \n- 我无法替代专业救援服务，但可提供应急步骤建议、资源指引或心理支持信息。  \n\n请补充细节，我将尽快响应！"}],"urgent_result":{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"e21ebd18-c616-9c20-a48d-e2b404a5bf7b","reasoningContent":""},"toolCalls":[],"media":[],"text":"我理解您可能有紧急需求，但请提供**具体、明确的紧急情况描述**（例如：医疗急救、火灾、事故、安全威胁等），以便我能为您提供**准确、合规且安全的指导**。\n\n⚠️ **重要提醒**：  \n- 若涉及**人身安全或生命危险**，请**立即拨打当地紧急电话**（如中国：110/120/119；国际通用：112）。  \n- 我无法替代专业救援服务，但可提供应急步骤建议、资源指引或心理支持信息。  \n\n请补充细节，我将尽快响应！"}}}}]
         * ========== Condition ==========
         * input = 请帮我分析一下这个问题
         * condition = false
         *
         * ========== ConditionalAgent ==========
         * input = 请帮我分析一下这个问题
         * condition = false
         * route = false
         * ======================================
         * result2=Optional[{"OverAllState":{"data":{"input":"请帮我分析一下这个问题","_graph_execution_id_":"5dadc3a5-ef78-4000-bc86-12f2634b49cc","urgent_result":{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"e21ebd18-c616-9c20-a48d-e2b404a5bf7b","reasoningContent":""},"toolCalls":[],"media":[],"text":"我理解您可能有紧急需求，但请提供**具体、明确的紧急情况描述**（例如：医疗急救、火灾、事故、安全威胁等），以便我能为您提供**准确、合规且安全的指导**。\n\n⚠️ **重要提醒**：  \n- 若涉及**人身安全或生命危险**，请**立即拨打当地紧急电话**（如中国：110/120/119；国际通用：112）。  \n- 我无法替代专业救援服务，但可提供应急步骤建议、资源指引或心理支持信息。  \n\n请补充细节，我将尽快响应！"},"_condition_result":"false","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"这是一个紧急问题需要立即处理"},{"messageType":"USER","metadata":{"messageType":"USER"},"rendered":false,"text":"你需要快速响应紧急情况..."},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"e21ebd18-c616-9c20-a48d-e2b404a5bf7b","reasoningContent":""},"toolCalls":[],"media":[],"text":"我理解您可能有紧急需求，但请提供**具体、明确的紧急情况描述**（例如：医疗急救、火灾、事故、安全威胁等），以便我能为您提供**准确、合规且安全的指导**。\n\n⚠️ **重要提醒**：  \n- 若涉及**人身安全或生命危险**，请**立即拨打当地紧急电话**（如中国：110/120/119；国际通用：112）。  \n- 我无法替代专业救援服务，但可提供应急步骤建议、资源指引或心理支持信息。  \n\n请补充细节，我将尽快响应！"},{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"请帮我分析一下这个问题"},{"messageType":"USER","metadata":{"messageType":"USER"},"rendered":false,"text":"你可以详细分析和处理常规请求..."},{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"f70a31ec-0021-9abe-b198-591d18f327c2","reasoningContent":""},"toolCalls":[],"media":[],"text":"您好！我注意到您似乎**忘记粘贴需要分析的具体问题或文本了**。\n\n目前的对话中只有“你可以详细分析和处理常规请求...”这句话，这看起来像是一个系统提示或占位符，而不是一个具体的待分析问题。\n\n为了给您提供最准确、深入的帮助，请您：\n\n1. **粘贴您需要分析的具体内容**（例如：一段代码、一个商业案例、一篇文章、一个数学问题、一段逻辑推理等）。\n2. **说明您的具体需求**（例如：求错误原因、优化建议、总结摘要、翻译、逻辑验证等）。\n\n✅ 一旦您提供具体内容，我将立即为您进行：\n- **深度解析**：拆解核心要素与潜在逻辑  \n- **结构化输出**：清晰呈现关键点、优缺点或解决方案  \n- **专业建议**：基于领域知识给出可操作的建议  \n\n请补充具体问题，我随时准备为您服务！"}],"normal_result":{"messageType":"ASSISTANT","metadata":{"search_info":"","role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","id":"f70a31ec-0021-9abe-b198-591d18f327c2","reasoningContent":""},"toolCalls":[],"media":[],"text":"您好！我注意到您似乎**忘记粘贴需要分析的具体问题或文本了**。\n\n目前的对话中只有“你可以详细分析和处理常规请求...”这句话，这看起来像是一个系统提示或占位符，而不是一个具体的待分析问题。\n\n为了给您提供最准确、深入的帮助，请您：\n\n1. **粘贴您需要分析的具体内容**（例如：一段代码、一个商业案例、一篇文章、一个数学问题、一段逻辑推理等）。\n2. **说明您的具体需求**（例如：求错误原因、优化建议、总结摘要、翻译、逻辑验证等）。\n\n✅ 一旦您提供具体内容，我将立即为您进行：\n- **深度解析**：拆解核心要素与潜在逻辑  \n- **结构化输出**：清晰呈现关键点、优缺点或解决方案  \n- **专业建议**：基于领域知识给出可操作的建议  \n\n请补充具体问题，我随时准备为您服务！"}}}}]
         */

    }

    @NotNull
    private static DashScopeChatModel getDashScopeChatModel() throws GraphRunnerException {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // =========================
        // 创建 DashScope API
        // =========================
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // =========================
        // 创建 ChatModel
        // =========================
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model("qwen3.7-flash-2026-07-15")
                        .multiModel(true)
                        .build())
                .build();
        return chatModel;
    }
}
