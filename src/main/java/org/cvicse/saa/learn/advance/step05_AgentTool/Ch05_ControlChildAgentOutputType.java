package org.cvicse.saa.learn.advance.step05_AgentTool;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Ch05_ControlChildAgentOutputType {


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
                .name("writer_with_output_type")
                .model(chatModel)
                .description("写文章并返回类型化输出")
                .instruction("你是一个专业作家。请创作文章并返回包含 title、content 和 characterCount 的结构化结果。")
                .outputType(ArticleOutput.class)
                .build();

        ReactAgent coordinatorAgent = ReactAgent.builder()
                .name("coordinator_output_type")
                .model(chatModel)
                .instruction("调用写作工具完成用户请求。")
                .tools(AgentTool.getFunctionToolCallback(writerAgent))
                .build();

        Optional<OverAllState> result = coordinatorAgent.invoke("写一篇关于夏天的小诗");

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
         * overAllState={"OverAllState":{"data":{"_graph_execution_id_":"0fd9617c-f0cf-47ff-8fdf-e6594eb1ebcf","input":"写一篇关于夏天的小诗","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"写一篇关于夏天的小诗"},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","refusal":"","finishReason":"TOOL_CALLS","index":0,"annotations":[{}],"id":"chatcmpl-57eb8bc1-b6f9-9f53-8e6e-1ddb915a8d67"},"toolCalls":[{"id":"call_4fa757bbbaf545baba96ee5e","type":"function","name":"writer_with_output_type","arguments":"{\"input\": \"写一篇关于夏天的小诗\"}"}],"media":[],"text":""},{"messageType":"TOOL","metadata":{"messageType":"TOOL"},"responses":[{"id":"call_4fa757bbbaf545baba96ee5e","name":"writer_with_output_type","responseData":"```json\n{\n  \"title\": \"盛夏手札\",\n  \"content\": \"蝉鸣撕开翡翠帘幕\\n冰棍滴落琥珀时光\\n裙摆扬起蒲公英的航船\\n晚风偷走晒黑的秘密\\n海浪在贝壳里种满月\\n星空跌进萤火虫的灯盏\\n所有热烈都正在发生\\n此刻即是永恒的诗行\",\n  \"characterCount\": 78\n}\n```"}],"text":""},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","refusal":"","finishReason":"STOP","index":0,"annotations":[{}],"id":"chatcmpl-556dc0f6-dae3-9062-8069-d832126296fa"},"toolCalls":[],"media":[],"text":"《盛夏手札》  \n蝉鸣撕开翡翠帘幕  \n冰棍滴落琥珀时光  \n裙摆扬起蒲公英的航船  \n晚风偷走晒黑的秘密  \n海浪在贝壳里种满月  \n星空跌进萤火虫的灯盏  \n所有热烈都正在发生  \n此刻即是永恒的诗行"}]}}}
         * overAllState.data()={_graph_execution_id_=0fd9617c-f0cf-47ff-8fdf-e6594eb1ebcf, input=写一篇关于夏天的小诗, messages=[UserMessage{content='写一篇关于夏天的小诗', metadata={messageType=USER}, messageType=USER}, AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_4fa757bbbaf545baba96ee5e, type=function, name=writer_with_output_type, arguments={"input": "写一篇关于夏天的小诗"}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, index=0, annotations=[{}], id=chatcmpl-57eb8bc1-b6f9-9f53-8e6e-1ddb915a8d67}], ToolResponseMessage{responses=[ToolResponse[id=call_4fa757bbbaf545baba96ee5e, name=writer_with_output_type, responseData=```json
         * {
         *   "title": "盛夏手札",
         *   "content": "蝉鸣撕开翡翠帘幕\n冰棍滴落琥珀时光\n裙摆扬起蒲公英的航船\n晚风偷走晒黑的秘密\n海浪在贝壳里种满月\n星空跌进萤火虫的灯盏\n所有热烈都正在发生\n此刻即是永恒的诗行",
         *   "characterCount": 78
         * }
         * ```]], messageType=TOOL, metadata={messageType=TOOL}}, AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=《盛夏手札》
         * 蝉鸣撕开翡翠帘幕
         * 冰棍滴落琥珀时光
         * 裙摆扬起蒲公英的航船
         * 晚风偷走晒黑的秘密
         * 海浪在贝壳里种满月
         * 星空跌进萤火虫的灯盏
         * 所有热烈都正在发生
         * 此刻即是永恒的诗行, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, index=0, annotations=[{}], id=chatcmpl-556dc0f6-dae3-9062-8069-d832126296fa}]]}
         * -------------------打印---------------------
         * message = UserMessage{content='写一篇关于夏天的小诗', metadata={messageType=USER}, messageType=USER}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_4fa757bbbaf545baba96ee5e, type=function, name=writer_with_output_type, arguments={"input": "写一篇关于夏天的小诗"}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, index=0, annotations=[{}], id=chatcmpl-57eb8bc1-b6f9-9f53-8e6e-1ddb915a8d67}]
         * message = ToolResponseMessage{responses=[ToolResponse[id=call_4fa757bbbaf545baba96ee5e, name=writer_with_output_type, responseData=```json
         * {
         *   "title": "盛夏手札",
         *   "content": "蝉鸣撕开翡翠帘幕\n冰棍滴落琥珀时光\n裙摆扬起蒲公英的航船\n晚风偷走晒黑的秘密\n海浪在贝壳里种满月\n星空跌进萤火虫的灯盏\n所有热烈都正在发生\n此刻即是永恒的诗行",
         *   "characterCount": 78
         * }
         * ```]], messageType=TOOL, metadata={messageType=TOOL}}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=《盛夏手札》
         * 蝉鸣撕开翡翠帘幕
         * 冰棍滴落琥珀时光
         * 裙摆扬起蒲公英的航船
         * 晚风偷走晒黑的秘密
         * 海浪在贝壳里种满月
         * 星空跌进萤火虫的灯盏
         * 所有热烈都正在发生
         * 此刻即是永恒的诗行, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, index=0, annotations=[{}], id=chatcmpl-556dc0f6-dae3-9062-8069-d832126296fa}]
         * -------------------分类打印---------------------
         * UserMessage USER:
         * 写一篇关于夏天的小诗
         * AssistantMessage 最终文本：
         * ToolResponseMessage TOOL: ToolResponseMessage{responses=[ToolResponse[id=call_4fa757bbbaf545baba96ee5e, name=writer_with_output_type, responseData=```json
         * {
         *   "title": "盛夏手札",
         *   "content": "蝉鸣撕开翡翠帘幕\n冰棍滴落琥珀时光\n裙摆扬起蒲公英的航船\n晚风偷走晒黑的秘密\n海浪在贝壳里种满月\n星空跌进萤火虫的灯盏\n所有热烈都正在发生\n此刻即是永恒的诗行",
         *   "characterCount": 78
         * }
         * ```]], messageType=TOOL, metadata={messageType=TOOL}}
         * AssistantMessage 最终文本：《盛夏手札》
         * 蝉鸣撕开翡翠帘幕
         * 冰棍滴落琥珀时光
         * 裙摆扬起蒲公英的航船
         * 晚风偷走晒黑的秘密
         * 海浪在贝壳里种满月
         * 星空跌进萤火虫的灯盏
         * 所有热烈都正在发生
         * 此刻即是永恒的诗行
         *
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

    public static class ArticleOutput {
        private String title;
        private String content;
        private int characterCount;

        // getters and setters


        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public int getCharacterCount() {
            return characterCount;
        }

        public void setCharacterCount(int characterCount) {
            this.characterCount = characterCount;
        }
    }


}
