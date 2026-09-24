package org.cvicse.saa.learn.advance.step05_AgentTool;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 最小示例，其中主Agent通过工具定义访问单个子Agent：
 *
 * 这种模式中：
 *
 * 主Agent在决定任务匹配子Agent的描述时调用工具
 * 子Agent独立运行并返回结果
 * 主Agent接收结果并继续编排
 */
public class Ch01_BaseUseAgentTool {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        OpenAiChatModel chatModel = getOpenAiChatModel();

        // 创建子Agent
        ReactAgent writerAgent = ReactAgent.builder()
                .name("writer_agent")
                .model(chatModel)
                .description("可以写文章")
                .instruction("你是一个知名的作家，擅长写作和创作。请根据用户的提问进行回答。")
                .build();

        // 创建主Agent，将子Agent作为工具
        ReactAgent blogAgent = ReactAgent.builder()
                .name("blog_agent")
                .model(chatModel)
                .instruction("根据用户给定的主题写一篇文章。使用写作工具来完成任务。")
                .tools(AgentTool.getFunctionToolCallback(writerAgent))
                .build();

        // 使用
        Optional<OverAllState> result = blogAgent.invoke("帮我写一个100字左右的散文");
        System.out.println("Optional<OverAllState> result=" + result);
        System.out.println("-------------------方式一----------------------");
        result.ifPresent(overAllState -> System.out.println("overAllState=" + overAllState));

        System.out.println("-------------------方式二----------------------");
        if (result.isPresent()) {
            OverAllState overAllState = result.get();
            System.out.println("overAllState=" + overAllState);

            Map<String, Object> data = overAllState.data();
            System.out.println("overAllState.data()=" + data);

            Object messagesObj = data.get("messages");
            System.out.println("-------------------打印---------------------");
            if (messagesObj instanceof List<?> messages) {
                for (Object messageObj : messages) {
                    System.out.println("message = " + messageObj);
                }
            }

            System.out.println("-------------------分类打印---------------------");
            if (messagesObj instanceof List<?> messages) {
                for (Object obj : messages) {
                    if (obj instanceof UserMessage userMessage) {
                        System.out.println("UserMessage USER:");
                        System.out.println(userMessage.getText());
                    } else if (obj instanceof AssistantMessage assistantMessage) {
                        String text = assistantMessage.getText();
                        System.out.println("AssistantMessage 最终文本：" + text);
                    } else if (obj instanceof ToolResponseMessage toolResponseMessage) {
                        System.out.println("ToolResponseMessage TOOL: " + toolResponseMessage);
                    } else {
                        System.out.println("UNKNOWN: " + obj);
                    }
                }
            }
        }

        /**
         *
         * apiKey = sk-ws-H.EXXEMEX.Y2dm.MEUCIEXNlQepvp3qup3qIhm93T1vAOg-iRZ_jE93U8ZusX8sAiEAir78N9b9dGhpgafs_Vc_0NJAir88hd62l4OIj9KXR6k
         * Optional<OverAllState> result=Optional[{"OverAllState":{"data":{"_graph_execution_id_":"11b34e7f-d88e-456d-8d0f-8157fd1f41d2","input":"帮我写一个100字左右的散文","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"帮我写一个100字左右的散文"},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"TOOL_CALLS","refusal":"","index":0,"annotations":[{}],"id":"chatcmpl-7161762c-1d76-92e0-b875-c3b14ebd7a93"},"toolCalls":[{"id":"call_c282404c70064fedb1993656","type":"function","name":"writer_agent","arguments":"{\"input\": \"请以“微雨初霁”为主题，写一篇100字左右的散文。要求语言清新自然，意境悠远，描绘雨后初晴时分的宁静与生机。\"}"}],"media":[],"text":""},{"messageType":"TOOL","metadata":{"messageType":"TOOL"},"responses":[{"id":"call_c282404c70064fedb1993656","name":"writer_agent","responseData":"微雨初霁，云隙漏下淡金晨光。青石板湿痕斑驳，叶尖悬露欲滴，风过处，簌簌点破浅洼。远林忽落一声鸟鸣，轻轻荡开满巷寂静。空气沁着泥土与青草的微芳，天地似刚从长梦中醒来。万物敛息，却自有生生不息的脉动。"}],"text":""},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","refusal":"","index":0,"annotations":[{}],"id":"chatcmpl-769003bc-8f2d-9f3f-9ff0-840c08394bb0"},"toolCalls":[],"media":[],"text":"微雨初霁，云隙漏下淡金晨光。青石板湿痕斑驳，叶尖悬露欲滴，风过处，簌簌点破浅洼。远林忽落一声鸟鸣，轻轻荡开满巷寂静。空气沁着泥土与青草的微芳，天地似刚从长梦中醒来。万物敛息，却自有生生不息的脉动。"}]}}}]
         * -------------------方式一----------------------
         * overAllState={"OverAllState":{"data":{"_graph_execution_id_":"11b34e7f-d88e-456d-8d0f-8157fd1f41d2","input":"帮我写一个100字左右的散文","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"帮我写一个100字左右的散文"},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"TOOL_CALLS","refusal":"","index":0,"annotations":[{}],"id":"chatcmpl-7161762c-1d76-92e0-b875-c3b14ebd7a93"},"toolCalls":[{"id":"call_c282404c70064fedb1993656","type":"function","name":"writer_agent","arguments":"{\"input\": \"请以“微雨初霁”为主题，写一篇100字左右的散文。要求语言清新自然，意境悠远，描绘雨后初晴时分的宁静与生机。\"}"}],"media":[],"text":""},{"messageType":"TOOL","metadata":{"messageType":"TOOL"},"responses":[{"id":"call_c282404c70064fedb1993656","name":"writer_agent","responseData":"微雨初霁，云隙漏下淡金晨光。青石板湿痕斑驳，叶尖悬露欲滴，风过处，簌簌点破浅洼。远林忽落一声鸟鸣，轻轻荡开满巷寂静。空气沁着泥土与青草的微芳，天地似刚从长梦中醒来。万物敛息，却自有生生不息的脉动。"}],"text":""},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","refusal":"","index":0,"annotations":[{}],"id":"chatcmpl-769003bc-8f2d-9f3f-9ff0-840c08394bb0"},"toolCalls":[],"media":[],"text":"微雨初霁，云隙漏下淡金晨光。青石板湿痕斑驳，叶尖悬露欲滴，风过处，簌簌点破浅洼。远林忽落一声鸟鸣，轻轻荡开满巷寂静。空气沁着泥土与青草的微芳，天地似刚从长梦中醒来。万物敛息，却自有生生不息的脉动。"}]}}}
         * -------------------方式二----------------------
         * overAllState={"OverAllState":{"data":{"_graph_execution_id_":"11b34e7f-d88e-456d-8d0f-8157fd1f41d2","input":"帮我写一个100字左右的散文","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"帮我写一个100字左右的散文"},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"TOOL_CALLS","refusal":"","index":0,"annotations":[{}],"id":"chatcmpl-7161762c-1d76-92e0-b875-c3b14ebd7a93"},"toolCalls":[{"id":"call_c282404c70064fedb1993656","type":"function","name":"writer_agent","arguments":"{\"input\": \"请以“微雨初霁”为主题，写一篇100字左右的散文。要求语言清新自然，意境悠远，描绘雨后初晴时分的宁静与生机。\"}"}],"media":[],"text":""},{"messageType":"TOOL","metadata":{"messageType":"TOOL"},"responses":[{"id":"call_c282404c70064fedb1993656","name":"writer_agent","responseData":"微雨初霁，云隙漏下淡金晨光。青石板湿痕斑驳，叶尖悬露欲滴，风过处，簌簌点破浅洼。远林忽落一声鸟鸣，轻轻荡开满巷寂静。空气沁着泥土与青草的微芳，天地似刚从长梦中醒来。万物敛息，却自有生生不息的脉动。"}],"text":""},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","finishReason":"STOP","refusal":"","index":0,"annotations":[{}],"id":"chatcmpl-769003bc-8f2d-9f3f-9ff0-840c08394bb0"},"toolCalls":[],"media":[],"text":"微雨初霁，云隙漏下淡金晨光。青石板湿痕斑驳，叶尖悬露欲滴，风过处，簌簌点破浅洼。远林忽落一声鸟鸣，轻轻荡开满巷寂静。空气沁着泥土与青草的微芳，天地似刚从长梦中醒来。万物敛息，却自有生生不息的脉动。"}]}}}
         * overAllState.data()={_graph_execution_id_=11b34e7f-d88e-456d-8d0f-8157fd1f41d2, input=帮我写一个100字左右的散文, messages=[UserMessage{content='帮我写一个100字左右的散文', metadata={messageType=USER}, messageType=USER}, AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_c282404c70064fedb1993656, type=function, name=writer_agent, arguments={"input": "请以“微雨初霁”为主题，写一篇100字左右的散文。要求语言清新自然，意境悠远，描绘雨后初晴时分的宁静与生机。"}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, finishReason=TOOL_CALLS, refusal=, index=0, annotations=[{}], id=chatcmpl-7161762c-1d76-92e0-b875-c3b14ebd7a93}], ToolResponseMessage{responses=[ToolResponse[id=call_c282404c70064fedb1993656, name=writer_agent, responseData=微雨初霁，云隙漏下淡金晨光。青石板湿痕斑驳，叶尖悬露欲滴，风过处，簌簌点破浅洼。远林忽落一声鸟鸣，轻轻荡开满巷寂静。空气沁着泥土与青草的微芳，天地似刚从长梦中醒来。万物敛息，却自有生生不息的脉动。]], messageType=TOOL, metadata={messageType=TOOL}}, AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=微雨初霁，云隙漏下淡金晨光。青石板湿痕斑驳，叶尖悬露欲滴，风过处，簌簌点破浅洼。远林忽落一声鸟鸣，轻轻荡开满巷寂静。空气沁着泥土与青草的微芳，天地似刚从长梦中醒来。万物敛息，却自有生生不息的脉动。, metadata={role=ASSISTANT, messageType=ASSISTANT, finishReason=STOP, refusal=, index=0, annotations=[{}], id=chatcmpl-769003bc-8f2d-9f3f-9ff0-840c08394bb0}]]}
         * message = UserMessage{content='帮我写一个100字左右的散文', metadata={messageType=USER}, messageType=USER}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_c282404c70064fedb1993656, type=function, name=writer_agent, arguments={"input": "请以“微雨初霁”为主题，写一篇100字左右的散文。要求语言清新自然，意境悠远，描绘雨后初晴时分的宁静与生机。"}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, finishReason=TOOL_CALLS, refusal=, index=0, annotations=[{}], id=chatcmpl-7161762c-1d76-92e0-b875-c3b14ebd7a93}]
         * message = ToolResponseMessage{responses=[ToolResponse[id=call_c282404c70064fedb1993656, name=writer_agent, responseData=微雨初霁，云隙漏下淡金晨光。青石板湿痕斑驳，叶尖悬露欲滴，风过处，簌簌点破浅洼。远林忽落一声鸟鸣，轻轻荡开满巷寂静。空气沁着泥土与青草的微芳，天地似刚从长梦中醒来。万物敛息，却自有生生不息的脉动。]], messageType=TOOL, metadata={messageType=TOOL}}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=微雨初霁，云隙漏下淡金晨光。青石板湿痕斑驳，叶尖悬露欲滴，风过处，簌簌点破浅洼。远林忽落一声鸟鸣，轻轻荡开满巷寂静。空气沁着泥土与青草的微芳，天地似刚从长梦中醒来。万物敛息，却自有生生不息的脉动。, metadata={role=ASSISTANT, messageType=ASSISTANT, finishReason=STOP, refusal=, index=0, annotations=[{}], id=chatcmpl-769003bc-8f2d-9f3f-9ff0-840c08394bb0}]
         *
         * */
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
}
