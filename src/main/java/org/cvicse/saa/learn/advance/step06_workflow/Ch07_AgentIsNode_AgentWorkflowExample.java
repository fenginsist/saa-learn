package org.cvicse.saa.learn.advance.step06_workflow;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Ch07_AgentIsNode_AgentWorkflowExample {

    public static void main(String[] args) {
        try {
            starter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws Exception {
        OpenAiChatModel chatModel = getOpenAiChatModel();
        Ch07_AgentIsNode_AgentWorkflowExample workflowExample = new Ch07_AgentIsNode_AgentWorkflowExample();
        workflowExample.buildWorkflowWithAgent(chatModel);

    }

    @NotNull
    private static OpenAiChatModel getOpenAiChatModel() {

        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // =========================
        // 1. 创建 OpenAiApi API
        // =========================
        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .baseUrl("https://llm-lp68jcoxmr9qifkd.cn-beijing.maas.aliyuncs.com/compatible-mode")
                .build();

        // =========================
        // 2. 创建 ChatModel
        // =========================
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder().model("qwen3.7-flash-2026-07-15").build())
                .build();
    }


    public void buildWorkflowWithAgent(ChatModel chatModel) throws Exception {
        // 创建专门的数据分析 Agent
        ReactAgent analysisAgent = ReactAgent.builder()
                .name("data_analyzer")
                .model(chatModel)
                .instruction("你是一个数据分析专家，负责分析数据并提供洞察，请分析以下输入数据： {input}")
                .outputKey("analysis_result")
                .build();

        // 创建报告生成 Agent
        ReactAgent reportAgent = ReactAgent.builder()
                .name("report_generator")
                .model(chatModel)
                .instruction("你是一个报告生成专家，负责将分析结果 {analysis_result} 转化为专业报告")
                .outputKey("final_report")
                .build();

        // 定义状态管理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("input", new ReplaceStrategy());
            return strategies;
        };

        // 构建包含 Agent 的工作流
        StateGraph workflow = new StateGraph(keyStrategyFactory);

        // 将 Agent 作为 SubGraph Node 添加
        workflow.addNode(analysisAgent.name(), analysisAgent.asNode(
                true,                     // includeContents: 是否传递父图的消息历史
                false                     // returnReasoningContents: 是否返回推理过程
        ));

        workflow.addNode(reportAgent.name(), reportAgent.asNode(
                true,
                false
        ));

        // 定义流程
        workflow.addEdge(StateGraph.START, analysisAgent.name());
        workflow.addEdge(analysisAgent.name(), reportAgent.name());
        workflow.addEdge(reportAgent.name(), StateGraph.END);

        // 编译并执行工作流
        CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
        NodeOutput lastOutput = compiledGraph.stream(Map.of("input", "2025年全年销量100亿，毛利率 23%，净利率 13%。2024年全年销量80亿，毛利率 20%，净利率 8%。"))
                .doOnNext(output -> {
                    if (output instanceof StreamingOutput<?> streamingOutput) {
                        System.out.println("Output from node " + streamingOutput.node() + ": " + streamingOutput.message().getText());
                    }
                })
                .blockLast();

        System.out.println("最终结果，包含所有节点状态：" + lastOutput);
        if (lastOutput != null) {
            System.out.println("最终结果，包含所有节点状态：" + lastOutput.state().data());
        }
    }
}
