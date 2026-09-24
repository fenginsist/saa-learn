package org.cvicse.saa.learn.advance.step06_workflow;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.HashMap;
import java.util.Map;

public class Ch10_StreamOutput {

    public static void main(String[] args) {
        try {
            starter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws Exception {
        OpenAiChatModel chatModel = getOpenAiChatModel();
        Ch10_StreamOutput workflowExample = new Ch10_StreamOutput();
        workflowExample.buildWorkflow(chatModel);

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


    private void buildWorkflow(OpenAiChatModel chatModel) throws GraphStateException {

        // 创建专门的数据分析 Agent
        ReactAgent analysisAgent = ReactAgent.builder()
                .name("data_analyzer")
                .model(chatModel)
                .instruction("你是一个数据分析专家，负责分析数据并提供洞察，请分析以下输入数据： {input}")
                .outputKey("analysis_result")
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

        // 定义流程
        workflow.addEdge(StateGraph.START, analysisAgent.name());
        workflow.addEdge(analysisAgent.name(), StateGraph.END);

        // 编译工作流
        CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());

        // 流式执行，实时获取每个节点的输出
        NodeOutput lastOutput = compiledGraph.stream(Map.of("input", "请分析2024年AI行业发展趋势"))
                .doOnNext(output -> {
                    if (output instanceof StreamingOutput<?> streamingOutput) {
                        if (streamingOutput.message() != null) {
                            // streaming output from streaming llm node
                            System.out.println("Streaming output from node " + streamingOutput.node() + ": " + streamingOutput.message().getText());
                        } else {
                            // output from normal node, investigate the state to get the node data
                            System.out.println("Output from node " + streamingOutput.node() + ": " + streamingOutput.state().data());
                        }
                    }
                })
                .blockLast();

        // 获取最终状态
        if (lastOutput != null) {
            System.out.println("最终结果: " + lastOutput.state().data());
        }
    }

}
