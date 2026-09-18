package org.cvicse.saa.learn.base.step07_memory;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * 工具中读取短期记忆
 * 使用 ToolContext 参数在工具中访问短期记忆（状态）。
 *
 * toolContext 参数从工具签名中隐藏（因此模型看不到它），但工具可以通过它访问状态。
 *
 *
 * 并且自己定义了工具拦截器，查看工具调用（call、invoke）的链路
 */
public class ch09_ReadUserInfoAndViewToolCallProcess {

    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        System.out.println("apiKey = " + apiKey);

        // 创建 DashScope API 实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        // 创建 ChatModel
        DashScopeChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // 创建工具
        ToolCallback getUserInfoTool = FunctionToolCallback
                .builder("get_user_info", new ch09_ReadUserInfoTool())
                .description("查找用户信息")
                .inputType(ch09_ReadUserInfoTool.Request.class)
                .build();

        // 使用
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .tools(getUserInfoTool)
                .interceptors(new ch09_ToolSchemaInterceptor())
                .saver(new MemorySaver())
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId("1")
                .addMetadata("user_id", "user_123")
                .build();

        AssistantMessage response = agent.call("获取用户信息", config);
        System.out.println(response.getText());

        /**
         * apiKey = sk-ws-H.EXXEMEX.Y2dm.MEUCIEXNlQepvp3qup3qIhm93T1vAOg-iRZ_jE93U8ZusX8sAiEAir78N9b9dGhpgafs_Vc_0NJAir88hd62l4OIj9KXR6k
         *
         * ============================================================
         * ① Agent → LLM
         * ============================================================
         *
         * ModelRequest（调用大模型前携带的数据，包括tool信息如下）::
         * com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest@27cbfddf
         * [get_user_info]
         * null
         * [UserMessage{content='获取用户信息', metadata={messageType=USER}, messageType=USER}]
         * {get_user_info=查找用户信息}
         *
         * ============================================================
         * ② LLM → Agent
         * ============================================================
         *
         * ModelResponse（调用大模型后返回信息，可能包含tool call）::
         * com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse@22bb5646
         * ChatResponse [metadata={ id: 2093093f-4b95-9a82-9f51-6599bd51a2a3, usage: DefaultUsage{promptTokens=175, completionTokens=19, totalTokens=194}, rateLimit: org.springframework.ai.chat.metadata.EmptyRateLimit@1be59f28 }, generations=[Generation[assistantMessage=AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_8b4833fa422f440684648c, type=function, name=get_user_info, arguments={"query": ""}]], textContent=, metadata={finishReason=TOOL_CALLS, search_info=, role=ASSISTANT, id=2093093f-4b95-9a82-9f51-6599bd51a2a3, messageType=ASSISTANT, reasoningContent=}], chatGenerationMetadata=DefaultChatGenerationMetadata[finishReason='TOOL_CALLS', filters=0, metadata=0]]]]
         * AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_8b4833fa422f440684648c, type=function, name=get_user_info, arguments={"query": ""}]], textContent=, metadata={finishReason=TOOL_CALLS, search_info=, role=ASSISTANT, id=2093093f-4b95-9a82-9f51-6599bd51a2a3, messageType=ASSISTANT, reasoningContent=}]
         *
         * 打印解释：模型返回的 AssistantMessage 里面明确带有 toolCalls。Agent 检查这个字段以及结束原因 finishReason=TOOL_CALLS，
         * 从而知道：这次不是最终答案，而是要求调用工具。
         *
         *
         *
         * ============================================================
         * ③ Tool 开始执行
         * ============================================================
         * Tool Name: get_user_info
         *
         * Tool Arguments:
         * {
         *   "query": ""
         * }
         *
         * ToolContext:
         * {_AGENT_CONFIG_=RunnableConfig{ threadId=1, checkPointId=c9818ebc-d72b-4ffd-ad70-ef5a6f5708e3, nextNode=null, streamMode=VALUES }, _AGENT_STATE_FOR_UPDATE_={}, _stream_=false, _AGENT_=my_agent, user_id=user_123, _AGENT_STATE_={"OverAllState":{"data":{"_graph_execution_id_":"b0dbd42f-1ed6-47fc-a186-8f9d657f2653","input":"获取用户信息","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"获取用户信息"},{"messageType":"ASSISTANT","metadata":{"finishReason":"TOOL_CALLS","search_info":"","role":"ASSISTANT","id":"2093093f-4b95-9a82-9f51-6599bd51a2a3","messageType":"ASSISTANT","reasoningContent":""},"toolCalls":[{"id":"call_8b4833fa422f440684648c","type":"function","name":"get_user_info","arguments":"{\"query\": \"\"}"}],"media":[],"text":""}]}}}}
         * user_id = user_123
         *
         * Tool Result:
         * 用户是 John Smith
         *
         * ============================================================
         * ③ Tool 执行结束
         * ============================================================
         *
         * ============================================================
         * ① Agent → LLM
         * ============================================================
         *
         * ModelRequest:
         * com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest@44de94c3
         * [get_user_info]
         * null
         * [UserMessage{content='获取用户信息', metadata={messageType=USER}, messageType=USER}, AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_8b4833fa422f440684648c, type=function, name=get_user_info, arguments={"query": ""}]], textContent=, metadata={finishReason=TOOL_CALLS, search_info=, role=ASSISTANT, id=2093093f-4b95-9a82-9f51-6599bd51a2a3, messageType=ASSISTANT, reasoningContent=}], ToolResponseMessage{responses=[ToolResponse[id=call_8b4833fa422f440684648c, name=get_user_info, responseData="用户是 John Smith"]], messageType=TOOL, metadata={messageType=TOOL}}]
         * {get_user_info=查找用户信息}
         *
         * ============================================================
         * ② LLM → Agent
         * ============================================================
         *
         * ModelResponse:
         * com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse@79d743e6
         * ChatResponse [metadata={ id: 13dfc3b7-b727-9246-8faa-450b869929ea, usage: DefaultUsage{promptTokens=213, completionTokens=5, totalTokens=218}, rateLimit: org.springframework.ai.chat.metadata.EmptyRateLimit@776802b0 }, generations=[Generation[assistantMessage=AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=用户是 John Smith。, metadata={finishReason=STOP, search_info=, role=ASSISTANT, id=13dfc3b7-b727-9246-8faa-450b869929ea, messageType=ASSISTANT, reasoningContent=}], chatGenerationMetadata=DefaultChatGenerationMetadata[finishReason='STOP', filters=0, metadata=0]]]]
         * AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=用户是 John Smith。, metadata={finishReason=STOP, search_info=, role=ASSISTANT, id=13dfc3b7-b727-9246-8faa-450b869929ea, messageType=ASSISTANT, reasoningContent=}]
         * 用户是 John Smith。
         */

    }
}
