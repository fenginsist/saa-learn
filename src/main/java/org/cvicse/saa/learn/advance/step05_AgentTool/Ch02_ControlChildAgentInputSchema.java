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

public class Ch02_ControlChildAgentInputSchema {
    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        OpenAiChatModel chatModel = getOpenAiChatModel();

        // 定义子Agent的输入Schema（标准 JSON Schema 格式）
        String writerInputSchema = """
  {
      "type": "object",
      "properties": {
          "topic": {
              "type": "string"
          },
          "wordCount": {
              "type": "integer"
          },
          "style": {
              "type": "string"
          }
      },
      "required": ["topic", "wordCount", "style"]
  }
  """;

        ReactAgent writerAgent = ReactAgent.builder()
                .name("structured_writer_agent")
                .model(chatModel)
                .description("根据结构化输入写文章")
                .instruction("你是一个专业作家。请严格按照输入的主题、字数和风格要求创作文章。")
                .inputSchema(writerInputSchema)
                .build();

        ReactAgent coordinatorAgent = ReactAgent.builder()
                .name("coordinator_agent")
                .model(chatModel)
                .instruction("你需要调用写作工具来完成用户的写作请求。请根据用户需求，使用结构化的参数调用写作工具。")
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
         * overAllState={"OverAllState":{"data":{"_graph_execution_id_":"16f6c907-9340-4584-83a2-92ae22a37dfe","input":"请写一篇关于春天的散文，大约100字","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"请写一篇关于春天的散文，大约100字"},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","refusal":"","finishReason":"TOOL_CALLS","annotations":[{}],"index":0,"id":"chatcmpl-a1b05e8d-bb6c-9521-9456-b1b9e5bd9ba7"},"toolCalls":[{"id":"call_81b403f872b94719a5cabc59","type":"function","name":"structured_writer_agent","arguments":"{\"input\": {\"topic\": \"春天\", \"wordCount\": 100, \"style\": \"散文\"}}"}],"media":[],"text":""},{"messageType":"TOOL","metadata":{"messageType":"TOOL"},"responses":[{"id":"call_81b403f872b94719a5cabc59","name":"structured_writer_agent","responseData":"春风化雨，悄染千山新绿；暖阳破冰，轻漾一溪碎金。柳眼初睁，桃腮乍露，莺啼婉转间，唤醒了蛰伏的梦。落花无言，流水有韵，天地间自有不言的生机。且停行步，任衣袂沾满草香与花气，将温柔妥帖安放，便是不负韶光。"}],"text":""},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","refusal":"","finishReason":"STOP","annotations":[{}],"index":0,"id":"chatcmpl-30dd31bf-bd8d-99a4-9f82-185b66485562"},"toolCalls":[],"media":[],"text":"春风化雨，悄染千山新绿；暖阳破冰，轻漾一溪碎金。柳眼初睁，桃腮乍露，莺啼婉转间，唤醒了蛰伏的梦。落花无言，流水有韵，天地间自有不言的生机。且停行步，任衣袂沾满草香与花气，将温柔妥帖安放，便是不负韶光。"}]}}}
         * overAllState.data()={_graph_execution_id_=16f6c907-9340-4584-83a2-92ae22a37dfe, input=请写一篇关于春天的散文，大约100字, messages=[UserMessage{content='请写一篇关于春天的散文，大约100字', metadata={messageType=USER}, messageType=USER}, AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_81b403f872b94719a5cabc59, type=function, name=structured_writer_agent, arguments={"input": {"topic": "春天", "wordCount": 100, "style": "散文"}}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, annotations=[{}], index=0, id=chatcmpl-a1b05e8d-bb6c-9521-9456-b1b9e5bd9ba7}], ToolResponseMessage{responses=[ToolResponse[id=call_81b403f872b94719a5cabc59, name=structured_writer_agent, responseData=春风化雨，悄染千山新绿；暖阳破冰，轻漾一溪碎金。柳眼初睁，桃腮乍露，莺啼婉转间，唤醒了蛰伏的梦。落花无言，流水有韵，天地间自有不言的生机。且停行步，任衣袂沾满草香与花气，将温柔妥帖安放，便是不负韶光。]], messageType=TOOL, metadata={messageType=TOOL}}, AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=春风化雨，悄染千山新绿；暖阳破冰，轻漾一溪碎金。柳眼初睁，桃腮乍露，莺啼婉转间，唤醒了蛰伏的梦。落花无言，流水有韵，天地间自有不言的生机。且停行步，任衣袂沾满草香与花气，将温柔妥帖安放，便是不负韶光。, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, annotations=[{}], index=0, id=chatcmpl-30dd31bf-bd8d-99a4-9f82-185b66485562}]]}
         * -------------------打印---------------------
         * message = UserMessage{content='请写一篇关于春天的散文，大约100字', metadata={messageType=USER}, messageType=USER}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_81b403f872b94719a5cabc59, type=function, name=structured_writer_agent, arguments={"input": {"topic": "春天", "wordCount": 100, "style": "散文"}}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, annotations=[{}], index=0, id=chatcmpl-a1b05e8d-bb6c-9521-9456-b1b9e5bd9ba7}]
         * message = ToolResponseMessage{responses=[ToolResponse[id=call_81b403f872b94719a5cabc59, name=structured_writer_agent, responseData=春风化雨，悄染千山新绿；暖阳破冰，轻漾一溪碎金。柳眼初睁，桃腮乍露，莺啼婉转间，唤醒了蛰伏的梦。落花无言，流水有韵，天地间自有不言的生机。且停行步，任衣袂沾满草香与花气，将温柔妥帖安放，便是不负韶光。]], messageType=TOOL, metadata={messageType=TOOL}}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=春风化雨，悄染千山新绿；暖阳破冰，轻漾一溪碎金。柳眼初睁，桃腮乍露，莺啼婉转间，唤醒了蛰伏的梦。落花无言，流水有韵，天地间自有不言的生机。且停行步，任衣袂沾满草香与花气，将温柔妥帖安放，便是不负韶光。, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, annotations=[{}], index=0, id=chatcmpl-30dd31bf-bd8d-99a4-9f82-185b66485562}]
         * -------------------分类打印---------------------
         * UserMessage USER:
         * 请写一篇关于春天的散文，大约100字
         * AssistantMessage 最终文本：
         * ToolResponseMessage TOOL: ToolResponseMessage{responses=[ToolResponse[id=call_81b403f872b94719a5cabc59, name=structured_writer_agent, responseData=春风化雨，悄染千山新绿；暖阳破冰，轻漾一溪碎金。柳眼初睁，桃腮乍露，莺啼婉转间，唤醒了蛰伏的梦。落花无言，流水有韵，天地间自有不言的生机。且停行步，任衣袂沾满草香与花气，将温柔妥帖安放，便是不负韶光。]], messageType=TOOL, metadata={messageType=TOOL}}
         * AssistantMessage 最终文本：春风化雨，悄染千山新绿；暖阳破冰，轻漾一溪碎金。柳眼初睁，桃腮乍露，莺啼婉转间，唤醒了蛰伏的梦。落花无言，流水有韵，天地间自有不言的生机。且停行步，任衣袂沾满草香与花气，将温柔妥帖安放，便是不负韶光。
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
