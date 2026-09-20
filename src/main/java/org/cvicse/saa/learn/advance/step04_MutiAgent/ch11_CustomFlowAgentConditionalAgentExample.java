package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

public class ch11_CustomFlowAgentConditionalAgentExample {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws Exception {
        DashScopeChatModel chatModel = getDashScopeChatModel();


        // ============================================================
        // 2. 创建 trueAgent
        // ============================================================

        ReactAgent queryAgent = ReactAgent.builder()
                .name("query_agent")
                .description("查询专家Agent")
                .model(chatModel)
                .systemPrompt("""
                        你是一个查询专家。
                        负责处理用户的查询类问题。
                        请直接、简洁地回答用户问题。
                        """)
                .build();


        // ============================================================
        // 3. 创建 falseAgent
        // ============================================================

        ReactAgent normalAgent = ReactAgent.builder()
                .name("normal_agent")
                .description("普通问题处理Agent")
                .model(chatModel)
                .systemPrompt("""
                        你是一个通用助手。
                        负责处理普通问题。
                        请直接、清晰地回答用户问题。
                        """)
                .build();


        // ============================================================
        // 4. 创建 StateGraph
        // ============================================================

        KeyStrategyFactory keyStrategyFactory = () -> {

            HashMap<String, KeyStrategy> strategies = new HashMap<>();

            // 当前路由类型
            strategies.put(
                    "route",
                    new com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy()
            );

            return strategies;
        };


        StateGraph workflow = new StateGraph(keyStrategyFactory);


        // ============================================================
        // 5. 添加条件判断节点
        // ============================================================

        workflow.addNode(
                "condition",
                node_async(state -> {

                    String input = state
                            .value("input")
                            .map(Object::toString)
                            .orElse("");

                    System.out.println();
                    System.out.println("=================================");
                    System.out.println("执行条件判断节点");
                    System.out.println("用户输入：" + input);
                    System.out.println("=================================");


                    /*
                     * 这里就是 Conditional Agent 最核心的地方。
                     *
                     * 为了方便演示：
                     *
                     * 如果用户输入包含：
                     * 查询 / 查一下 / 查询一下
                     *
                     * 就进入 query_agent。
                     *
                     * 否则进入 normal_agent。
                     */

                    boolean isQuery =
                            input.contains("查询")
                                    || input.contains("查一下")
                                    || input.contains("查询一下");


                    String route = isQuery
                            ? "query"
                            : "normal";


                    System.out.println("判断结果：" + route);


                    return Map.of(
                            "route",
                            route
                    );
                })
        );


        // ============================================================
        // 6. 把 ReactAgent 放进 Graph
        // ============================================================

        workflow.addNode(
                queryAgent.name(),
                queryAgent.asNode(
                        true,
                        false
                )
        );

        workflow.addNode(
                normalAgent.name(),
                normalAgent.asNode(
                        true,
                        false
                )
        );


        // ============================================================
        // 7. START → condition
        // ============================================================

        workflow.addEdge(
                START,
                "condition"
        );


        // ============================================================
        // 8. 条件路由
        // ============================================================

        workflow.addConditionalEdges(
                "condition",

                edge_async(state -> {

                    String route = state
                            .value("route")
                            .map(Object::toString)
                            .orElse("normal");

                    return route;
                }),

                Map.of(
                        "query", queryAgent.name(),
                        "normal", normalAgent.name()
                )
        );


        // ============================================================
        // 9. 两个 Agent 最终都进入 END
        // ============================================================

        workflow.addEdge(queryAgent.name(), END);

        workflow.addEdge(normalAgent.name(), END);


        // ============================================================
        // 10. 编译 Graph
        // ============================================================

        CompiledGraph compiledGraph = workflow.compile();


        // ============================================================
        // 11. 测试
        // ============================================================

        test(compiledGraph, "帮我查询一下Spring AI Alibaba是什么");

        test(compiledGraph, "你好，请介绍一下你自己");
    }


    /**
     * 执行测试
     */
    private static void test(CompiledGraph compiledGraph, String input) throws Exception {

        System.out.println();
        System.out.println();
        System.out.println("##################################################");
        System.out.println("开始执行");
        System.out.println("输入：" + input);
        System.out.println("##################################################");


        Map<String, Object> inputState = Map.of(
                "input",
                input
        );


        Optional<OverAllState> result =
                compiledGraph.invoke(inputState);


        System.out.println();
        System.out.println("============== 执行完成 ==============");


        result.ifPresent(state -> {

            System.out.println("最终 State：");
            System.out.println(state);

        });
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
                .build();
        return chatModel;
    }
}
