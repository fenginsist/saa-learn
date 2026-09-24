package org.cvicse.saa.learn.advance.step06_workflow;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

public class Ch09_AgentNodeAndNormalNode_HybridWorkflow {

    public static void main(String[] args) {
        try {
            starter();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws Exception {
        OpenAiChatModel chatModel = getOpenAiChatModel();
        Ch09_AgentNodeAndNormalNode_HybridWorkflow workflowExample = new Ch09_AgentNodeAndNormalNode_HybridWorkflow();
        workflowExample.buildHybridWorkflow(chatModel);

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



    public void buildHybridWorkflow(ChatModel chatModel) throws Exception {
        // 创建 Agent
        ReactAgent qaAgent = ReactAgent.builder()
                .name("qa_agent")
                .model(chatModel)
                .instruction("你是一个问答专家，负责回答用户的问题：{cleaned_input}")
                .outputKey("qa_result")
                .enableLogging(true)
                .build();

        // 创建自定义 Node
        class PreprocessorNode implements NodeAction {
            @Override
            public Map<String, Object> apply(OverAllState state) throws Exception {
                String input = state.value("input", "").toString();
                String cleaned = input.trim().toLowerCase();
                return Map.of("cleaned_input", cleaned);
            }
        }

        class ValidatorNode implements NodeAction {
            @Override
            public Map<String, Object> apply(OverAllState state) throws Exception {
                Message message = (Message) state.value("qa_result").get();
                boolean isValid = message.getText().length() > 50; // 简单验证
                return Map.of("is_valid", isValid);
            }
        }

        // 定义状态管理策略
        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("input", new ReplaceStrategy());
            strategies.put("cleaned_input", new ReplaceStrategy());
            strategies.put("qa_result", new ReplaceStrategy());
            strategies.put("is_valid", new ReplaceStrategy());
            return strategies;
        };

        // 构建混合工作流
        StateGraph workflow = new StateGraph(keyStrategyFactory);

        // 添加普通 Node
        workflow.addNode("preprocess", node_async(new PreprocessorNode()));
        workflow.addNode("validate", node_async(new ValidatorNode()));

        // 添加 Agent Node
        workflow.addNode(qaAgent.name(), qaAgent.asNode(
                true,
                false
        ));

        // 定义流程：预处理 -> Agent处理 -> 验证
        workflow.addEdge(StateGraph.START, "preprocess");
        workflow.addEdge("preprocess", qaAgent.name());
        workflow.addEdge(qaAgent.name(), "validate");

        // 条件边：验证通过则结束，否则重新处理
        workflow.addConditionalEdges(
                "validate",
                edge_async(state -> (Boolean) state.value("is_valid", false) ? "end" : qaAgent.name()),
                Map.of("end", StateGraph.END, qaAgent.name(), qaAgent.name())
        );

        // 编译并执行工作流
        CompiledGraph compiledGraph = workflow.compile(CompileConfig.builder().build());
        NodeOutput lastOutput = compiledGraph.stream(Map.of("input", "请解释量子计算的基本原理"))
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

        System.out.println("最终结果，包含所有节点状态：" + lastOutput.state().data());
    }
}
