package org.cvicse.saa.learn.advance.step06_workflow;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.cvicse.saa.learn.advance.step02_HumanInTheLoop.ch03_EntireExampleHumanInTheLoop;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.HashMap;
import java.util.Map;

public class Ch08_MultiAgentWorkflow {

    public record Request(String query) {}

    public static void main(String[] args) {
        try {
            starter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws Exception {
        OpenAiChatModel chatModel = getOpenAiChatModel();
        Ch08_MultiAgentWorkflow workflowExample = new Ch08_MultiAgentWorkflow();

        // 1. 信息搜索工具
        ToolCallback searchTool = FunctionToolCallback.builder(
            "search_information",
            (Request request) -> {
                            System.out.println("正在搜索：" + request.query());

                            return "关于【" + request.query() + "】的搜索结果：" + "这是模拟的搜索结果。";
            })
            .description("根据关键词搜索相关信息")
            .inputType(Request.class)
            .build();

        // 2. 数据分析工具
        ToolCallback analysisTool = FunctionToolCallback.builder(
            "analyze_research_data",
            (Request request) -> {
                    System.out.println("正在分析：" + request.query());
                    return "这是对研究数据进行的模拟分析结果：" + request.query();
            })
            .description("对研究数据进行深入分析")
            .inputType(Request.class)
            .build();


        // 3. 总结工具
        ToolCallback summaryTool = FunctionToolCallback.builder(
            "generate_summary",
            (Request request) -> {
                    System.out.println("正在总结：" + request.query());
                    return "这是最终总结：" + request.query();
            })
            .description("根据研究和分析结果生成最终总结")
            .inputType(Request.class)
            .build();

        workflowExample.buildResearchWorkflow(chatModel, searchTool, analysisTool, summaryTool);

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

    public void buildResearchWorkflow(
            ChatModel chatModel,
            ToolCallback searchTool,
            ToolCallback analysisTool,
            ToolCallback summaryTool) throws Exception {

        // 1. 创建信息收集 Agent
        ReactAgent researchAgent = ReactAgent.builder()
                .name("researcher")
                .model(chatModel)
                .instruction("你是一个研究专家，负责收集和整理相关信息，请研究主题： {input}")
                .tools(searchTool)
                .outputKey("research_data")
                .enableLogging(true)
                .build();

        // 2. 创建数据分析 Agent
        ReactAgent analysisAgent = ReactAgent.builder()
                .name("analyst")
                .model(chatModel)
                .instruction("你是一个分析专家，负责深入分析关于主题 {input} 的研究数据。数据如下： {research_data}")
                .tools(analysisTool)
                .outputKey("analysis_result")
                .enableLogging(true)
                .build();

        // 3. 创建总结 Agent
        ReactAgent summaryAgent = ReactAgent.builder()
                .name("summarizer")
                .model(chatModel)
                .instruction("你是一个总结专家，负责将分析结果提炼为简洁的结论，结果： {analysis_result}")
                .tools(summaryTool)
                .outputKey("final_summary")
                .enableLogging(true)
                .build();

        // 定义状态管理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("input", new ReplaceStrategy());
            return strategies;
        };

        // 4. 构建工作流
        StateGraph workflow = new StateGraph(keyStrategyFactory);

        // 添加 Agent 节点
        workflow.addNode(researchAgent.name(), researchAgent.asNode(
                true, // 包含历史消息
                false // 不返回推理过程
        ));

        workflow.addNode(analysisAgent.name(), analysisAgent.asNode(
                true,
                false
        ));

        workflow.addNode(summaryAgent.name(), summaryAgent.asNode(
                true,
                true // 返回完整推理过程
        ));

        // 定义顺序执行流程
        workflow.addEdge(StateGraph.START, researchAgent.name());
        workflow.addEdge(researchAgent.name(), analysisAgent.name());
        workflow.addEdge(analysisAgent.name(), summaryAgent.name());
        workflow.addEdge(summaryAgent.name(), StateGraph.END);

        // 编译并执行工作流
        CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
        NodeOutput finalOutput = compiledGraph.stream(Map.of("input", "帮我做一份关于AI Agent的研究报告"))
                .doOnNext(output -> {
                    if (output instanceof StreamingOutput<?> streamingOutput) {
                        System.out.println("Output from node " + streamingOutput.node() + ": " + streamingOutput.message().getText());
                    }
                })
                .blockLast();

        System.out.println("多Agent研究工作流构建完成");
        System.out.println("最终输出: " + finalOutput.state().value("final_summary").orElse("无"));
    }
}