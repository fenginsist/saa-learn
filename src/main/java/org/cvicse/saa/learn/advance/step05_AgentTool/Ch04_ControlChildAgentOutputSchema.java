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

public class Ch04_ControlChildAgentOutputSchema {

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

        // 使用 BeanOutputConverter 生成 outputSchema
        BeanOutputConverter<ArticleOutput> outputConverter = new BeanOutputConverter<>(ArticleOutput.class);
        String format = outputConverter.getFormat();

        ReactAgent writerAgent = ReactAgent.builder()
                .name("writer_with_output_schema")
                .model(chatModel)
                .description("写文章并返回结构化输出")
                .instruction("你是一个专业作家。请创作文章并严格按照指定的JSON格式返回结果。")
                .outputSchema(format)
                .build();

        ReactAgent coordinatorAgent = ReactAgent.builder()
                .name("coordinator_output_schema")
                .model(chatModel)
                .instruction("调用写作工具完成用户请求，工具会返回结构化的文章数据。")
                .tools(AgentTool.getFunctionToolCallback(writerAgent))
                .build();

        Optional<OverAllState> result = coordinatorAgent.invoke("写一篇100字关于冬天的短文");

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
         * overAllState={"OverAllState":{"data":{"_graph_execution_id_":"972bfb11-b22f-4103-bbfa-7b3bfbd1e86a","input":"写一篇100字关于冬天的短文","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"写一篇100字关于冬天的短文"},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","refusal":"","finishReason":"TOOL_CALLS","annotations":[{}],"index":0,"id":"chatcmpl-0c196b55-f201-9c51-b374-766ac6ecd1d9"},"toolCalls":[{"id":"call_ef62414d7b634cd1abd18a48","type":"function","name":"writer_with_output_schema","arguments":"{\"input\": \"写一篇100字关于冬天的短文\"}"}],"media":[],"text":""},{"messageType":"TOOL","metadata":{"messageType":"TOOL"},"responses":[{"id":"call_ef62414d7b634cd1abd18a48","name":"writer_with_output_schema","responseData":"```json\n{\n  \"characterCount\": 42,\n  \"content\": \"冬日悄临，寒霜覆盖大地。雪花轻舞，枝头缀满晶莹。围炉煮茶，暖意融融；踏雪寻梅，童趣盎然。岁末清欢，静候春回。\",\n  \"title\": \"冬韵\"\n}\n```"}],"text":""},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","refusal":"","finishReason":"STOP","annotations":[{}],"index":0,"id":"chatcmpl-acbd753e-0248-9695-86c4-6d3f0d6cf9ad"},"toolCalls":[],"media":[],"text":"# 《冬韵》\n\n冬日悄临，寒霜覆盖大地。雪花轻舞，枝头缀满晶莹。围炉煮茶，暖意融融；踏雪寻梅，童趣盎然。岁末清欢，静候春回。"}]}}}
         * overAllState.data()={_graph_execution_id_=972bfb11-b22f-4103-bbfa-7b3bfbd1e86a, input=写一篇100字关于冬天的短文, messages=[UserMessage{content='写一篇100字关于冬天的短文', metadata={messageType=USER}, messageType=USER}, AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_ef62414d7b634cd1abd18a48, type=function, name=writer_with_output_schema, arguments={"input": "写一篇100字关于冬天的短文"}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, annotations=[{}], index=0, id=chatcmpl-0c196b55-f201-9c51-b374-766ac6ecd1d9}], ToolResponseMessage{responses=[ToolResponse[id=call_ef62414d7b634cd1abd18a48, name=writer_with_output_schema, responseData=```json
         * {
         *   "characterCount": 42,
         *   "content": "冬日悄临，寒霜覆盖大地。雪花轻舞，枝头缀满晶莹。围炉煮茶，暖意融融；踏雪寻梅，童趣盎然。岁末清欢，静候春回。",
         *   "title": "冬韵"
         * }
         * ```]], messageType=TOOL, metadata={messageType=TOOL}}, AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=# 《冬韵》
         *
         * 冬日悄临，寒霜覆盖大地。雪花轻舞，枝头缀满晶莹。围炉煮茶，暖意融融；踏雪寻梅，童趣盎然。岁末清欢，静候春回。, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, annotations=[{}], index=0, id=chatcmpl-acbd753e-0248-9695-86c4-6d3f0d6cf9ad}]]}
         * -------------------打印---------------------
         * message = UserMessage{content='写一篇100字关于冬天的短文', metadata={messageType=USER}, messageType=USER}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_ef62414d7b634cd1abd18a48, type=function, name=writer_with_output_schema, arguments={"input": "写一篇100字关于冬天的短文"}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, annotations=[{}], index=0, id=chatcmpl-0c196b55-f201-9c51-b374-766ac6ecd1d9}]
         * message = ToolResponseMessage{responses=[ToolResponse[id=call_ef62414d7b634cd1abd18a48, name=writer_with_output_schema, responseData=```json
         * {
         *   "characterCount": 42,
         *   "content": "冬日悄临，寒霜覆盖大地。雪花轻舞，枝头缀满晶莹。围炉煮茶，暖意融融；踏雪寻梅，童趣盎然。岁末清欢，静候春回。",
         *   "title": "冬韵"
         * }
         * ```]], messageType=TOOL, metadata={messageType=TOOL}}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=# 《冬韵》
         *
         * 冬日悄临，寒霜覆盖大地。雪花轻舞，枝头缀满晶莹。围炉煮茶，暖意融融；踏雪寻梅，童趣盎然。岁末清欢，静候春回。, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, annotations=[{}], index=0, id=chatcmpl-acbd753e-0248-9695-86c4-6d3f0d6cf9ad}]
         * -------------------分类打印---------------------
         * UserMessage USER:
         * 写一篇100字关于冬天的短文
         * AssistantMessage 最终文本：
         * ToolResponseMessage TOOL: ToolResponseMessage{responses=[ToolResponse[id=call_ef62414d7b634cd1abd18a48, name=writer_with_output_schema, responseData=```json
         * {
         *   "characterCount": 42,
         *   "content": "冬日悄临，寒霜覆盖大地。雪花轻舞，枝头缀满晶莹。围炉煮茶，暖意融融；踏雪寻梅，童趣盎然。岁末清欢，静候春回。",
         *   "title": "冬韵"
         * }
         * ```]], messageType=TOOL, metadata={messageType=TOOL}}
         * AssistantMessage 最终文本：# 《冬韵》
         *
         * 冬日悄临，寒霜覆盖大地。雪花轻舞，枝头缀满晶莹。围炉煮茶，暖意融融；踏雪寻梅，童趣盎然。岁末清欢，静候春回。
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

    // 定义输出类型
    public static class ArticleOutput {
        private String title;
        private String content;
        private int characterCount;

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getCharacterCount() { return characterCount; }
        public void setCharacterCount(int characterCount) { this.characterCount = characterCount; }
    }
}
