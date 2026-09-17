package org.cvicse.saa.learn.base.step08_HooksAndInterceptors.builtInImplementation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.todolist.TodoListInterceptor;
import org.jetbrains.annotations.NotNull;

public class ch06_Planning {

    public static void main(String[] args) {
        starter();
    }


    public static void starter() {

        DashScopeChatModel chatModel = getDashScopeChatModel();


        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("planning_agent")
                .model(chatModel)
//                .tools(myTool)
                .interceptors(TodoListInterceptor.builder().build())
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
