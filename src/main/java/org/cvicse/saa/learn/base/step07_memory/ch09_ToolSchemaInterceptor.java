package org.cvicse.saa.learn.base.step07_memory;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

/**
 * 区分两个东西：
 * - FunctionToolCallback.builder(...).inputType(...) → 这是工具定义阶段的 Java Schema
 * - 大模型真正收到的 tools 参数 → 是 OpenAI/DashScope Chat API 请求里的 JSON Schema
 * - 大模型决定调用工具后，还会产生一段 tool call JSON 参数，例如 {"query":"用户信息"}
 * 目的是为了查看 tool call JSON 参数
 */
public class ch09_ToolSchemaInterceptor extends ModelInterceptor {

    @Override
    public String getName() {
        return "ToolSchemaInterceptor";
    }

    @Override
    public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {

        System.out.println();
        System.out.println("============================================================");
        System.out.println("① Agent → LLM");
        System.out.println("============================================================");

        System.out.println();
        System.out.println("ModelRequest（调用大模型前携带的数据，包括tool信息如下）:");

        System.out.println(request.toString());
        System.out.println(request.getTools().toString());
        System.out.println(request.getSystemMessage());
        System.out.println(request.getMessages().toString());
        System.out.println(request.getToolDescriptions());

        // ----------------------------------------------------
        // 真正调用大模型
        // ----------------------------------------------------

        ModelResponse response = handler.call(request);

        // ----------------------------------------------------
        // 打印大模型返回
        // ----------------------------------------------------

        System.out.println();
        System.out.println("============================================================");
        System.out.println("② LLM → Agent");
        System.out.println("============================================================");

        System.out.println();
        System.out.println("ModelResponse（调用大模型后返回信息，可能包含tool call）:");

        System.out.println(response);
        System.out.println(response.getChatResponse());
        System.out.println(response.getMessage().toString());

        return response;
    }
}
