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

public class Ch03_ControlChildAgentInputType {

    // 定义输入类型
    public record ArticleRequest(
            String topic,      // 文章主题
            int wordCount,     // 字数要求
            String style       // 文章风格
    ) {}


    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        OpenAiChatModel chatModel = getOpenAiChatModel();

        ReactAgent writerAgent = ReactAgent.builder()
                .name("typed_writer_agent")
                .model(chatModel)
                .description("根据类型化输入写文章")
                .instruction("你是一个专业作家。请严格按照输入的 topic（主题）、wordCount（字数）和 style（风格）要求创作文章。")
                .inputType(ArticleRequest.class)
                .build();

        ReactAgent coordinatorAgent = ReactAgent.builder()
                .name("coordinator_with_type_agent")
                .model(chatModel)
                .instruction("你需要调用写作工具来完成用户的写作请求。工具接收 JSON 格式的参数。")
                .tools(AgentTool.getFunctionToolCallback(writerAgent))
                .build();


        Optional<OverAllState> result = coordinatorAgent.invoke("请写一篇关于春天的散文，大约100字");

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
         * overAllState={"OverAllState":{"data":{"_graph_execution_id_":"70619664-647b-4333-8a9a-ab2404abdf7c","input":"请写一篇关于春天的散文，大约100字","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"请写一篇关于春天的散文，大约100字"},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","refusal":"","finishReason":"TOOL_CALLS","annotations":[{}],"index":0,"id":"chatcmpl-3c16f234-697b-96b3-9031-8fcba669412a"},"toolCalls":[{"id":"call_96ef5753789c4b15bf5fe501","type":"function","name":"typed_writer_agent","arguments":"{\"input\": {\"style\": \"散文\", \"topic\": \"春天\", \"wordCount\": 100}}"}],"media":[],"text":""},{"messageType":"TOOL","metadata":{"messageType":"TOOL"},"responses":[{"id":"call_96ef5753789c4b15bf5fe501","name":"typed_writer_agent","responseData":"风软了冰河，柳眼初睁。泥土吐息，草尖探出怯生生的绿。雀儿衔泥剪影，点染旧墙。春非客，乃长梦初醒。寒霜化作溪语，滴落掌心。万物静默，却以拔节之声应答光阴。一树杏花风起，便燃亮了半座江南。岁华流转，心亦随此间清嘉，悄然回温。"}],"text":""},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","refusal":"","finishReason":"STOP","annotations":[{}],"index":0,"id":"chatcmpl-072bd340-f6f9-94a1-b85c-1da70f824836"},"toolCalls":[],"media":[],"text":"风软了冰河，柳眼初睁。泥土吐息，草尖探出怯生生的绿。雀儿衔泥剪影，点染旧墙。春非客，乃长梦初醒。寒霜化作溪语，滴落掌心。万物静默，却以拔节之声应答光阴。一树杏花风起，便燃亮了半座江南。岁华流转，心亦随此间清嘉，悄然回温。"}]}}}
         * overAllState.data()={_graph_execution_id_=70619664-647b-4333-8a9a-ab2404abdf7c, input=请写一篇关于春天的散文，大约100字, messages=[UserMessage{content='请写一篇关于春天的散文，大约100字', metadata={messageType=USER}, messageType=USER}, AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_96ef5753789c4b15bf5fe501, type=function, name=typed_writer_agent, arguments={"input": {"style": "散文", "topic": "春天", "wordCount": 100}}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, annotations=[{}], index=0, id=chatcmpl-3c16f234-697b-96b3-9031-8fcba669412a}], ToolResponseMessage{responses=[ToolResponse[id=call_96ef5753789c4b15bf5fe501, name=typed_writer_agent, responseData=风软了冰河，柳眼初睁。泥土吐息，草尖探出怯生生的绿。雀儿衔泥剪影，点染旧墙。春非客，乃长梦初醒。寒霜化作溪语，滴落掌心。万物静默，却以拔节之声应答光阴。一树杏花风起，便燃亮了半座江南。岁华流转，心亦随此间清嘉，悄然回温。]], messageType=TOOL, metadata={messageType=TOOL}}, AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=风软了冰河，柳眼初睁。泥土吐息，草尖探出怯生生的绿。雀儿衔泥剪影，点染旧墙。春非客，乃长梦初醒。寒霜化作溪语，滴落掌心。万物静默，却以拔节之声应答光阴。一树杏花风起，便燃亮了半座江南。岁华流转，心亦随此间清嘉，悄然回温。, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, annotations=[{}], index=0, id=chatcmpl-072bd340-f6f9-94a1-b85c-1da70f824836}]]}
         * -------------------打印---------------------
         * message = UserMessage{content='请写一篇关于春天的散文，大约100字', metadata={messageType=USER}, messageType=USER}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_96ef5753789c4b15bf5fe501, type=function, name=typed_writer_agent, arguments={"input": {"style": "散文", "topic": "春天", "wordCount": 100}}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, annotations=[{}], index=0, id=chatcmpl-3c16f234-697b-96b3-9031-8fcba669412a}]
         * message = ToolResponseMessage{responses=[ToolResponse[id=call_96ef5753789c4b15bf5fe501, name=typed_writer_agent, responseData=风软了冰河，柳眼初睁。泥土吐息，草尖探出怯生生的绿。雀儿衔泥剪影，点染旧墙。春非客，乃长梦初醒。寒霜化作溪语，滴落掌心。万物静默，却以拔节之声应答光阴。一树杏花风起，便燃亮了半座江南。岁华流转，心亦随此间清嘉，悄然回温。]], messageType=TOOL, metadata={messageType=TOOL}}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=风软了冰河，柳眼初睁。泥土吐息，草尖探出怯生生的绿。雀儿衔泥剪影，点染旧墙。春非客，乃长梦初醒。寒霜化作溪语，滴落掌心。万物静默，却以拔节之声应答光阴。一树杏花风起，便燃亮了半座江南。岁华流转，心亦随此间清嘉，悄然回温。, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, annotations=[{}], index=0, id=chatcmpl-072bd340-f6f9-94a1-b85c-1da70f824836}]
         * -------------------分类打印---------------------
         * UserMessage USER:
         * 请写一篇关于春天的散文，大约100字
         * AssistantMessage 最终文本：
         * ToolResponseMessage TOOL: ToolResponseMessage{responses=[ToolResponse[id=call_96ef5753789c4b15bf5fe501, name=typed_writer_agent, responseData=风软了冰河，柳眼初睁。泥土吐息，草尖探出怯生生的绿。雀儿衔泥剪影，点染旧墙。春非客，乃长梦初醒。寒霜化作溪语，滴落掌心。万物静默，却以拔节之声应答光阴。一树杏花风起，便燃亮了半座江南。岁华流转，心亦随此间清嘉，悄然回温。]], messageType=TOOL, metadata={messageType=TOOL}}
         * AssistantMessage 最终文本：风软了冰河，柳眼初睁。泥土吐息，草尖探出怯生生的绿。雀儿衔泥剪影，点染旧墙。春非客，乃长梦初醒。寒霜化作溪语，滴落掌心。万物静默，却以拔节之声应答光阴。一树杏花风起，便燃亮了半座江南。岁华流转，心亦随此间清嘉，悄然回温。
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
                .defaultOptions(OpenAiChatOptions.builder().model("qwen3.7-flash").build())
                .build();
    }
}
