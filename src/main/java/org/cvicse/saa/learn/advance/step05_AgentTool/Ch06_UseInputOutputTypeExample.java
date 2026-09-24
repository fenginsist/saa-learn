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

public class Ch06_UseInputOutputTypeExample {
    // 定义输入和输出类型
    public record ArticleRequest(String topic, int wordCount, String style) {}


    public static void main(String[] args) {
        try {
            starter();
        } catch (GraphRunnerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void starter() throws GraphRunnerException {
        OpenAiChatModel chatModel = getOpenAiChatModel();

        // 创建完整类型化的Agent
        ReactAgent writerAgent = ReactAgent.builder()
                .name("full_typed_writer")
                .model(chatModel)
                .description("完整类型化的写作工具")
                .instruction("根据结构化输入（topic、wordCount、style）创作文章，并返回结构化输出（title、content、characterCount）。")
                .inputType(ArticleRequest.class)
                .outputType(ArticleOutput.class)
                .build();

        ReactAgent reviewerAgent = ReactAgent.builder()
                .name("typed_reviewer")
                .model(chatModel)
                .description("完整类型化的评审工具")
                .instruction("对文章进行评审，返回评审意见（comment、approved、suggestions）。")
                .outputType(ReviewOutput.class)
                .build();

        ReactAgent orchestratorAgent = ReactAgent.builder()
                .name("orchestrator")
                .model(chatModel)
                .instruction("协调写作和评审流程。先调用写作工具创作文章，然后调用评审工具进行评审。")
                .tools(
                        AgentTool.getFunctionToolCallback(writerAgent),
                        AgentTool.getFunctionToolCallback(reviewerAgent)
                )
                .build();

        Optional<OverAllState> result = orchestratorAgent.invoke("请写一篇关于友谊的散文，约200字，需要评审");

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
         * overAllState={"OverAllState":{"data":{"_graph_execution_id_":"a74dd3e5-06c2-455e-8a48-1c5a9d369113","input":"请写一篇关于友谊的散文，约200字，需要评审","messages":[{"messageType":"USER","metadata":{"messageType":"USER"},"media":[],"text":"请写一篇关于友谊的散文，约200字，需要评审"},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","refusal":"","finishReason":"TOOL_CALLS","annotations":[{}],"index":0,"id":"chatcmpl-dd74d15e-b9da-9884-b8d8-4d0cd0fdc5a0"},"toolCalls":[{"id":"call_12573a162aea4db59cdbaa89","type":"function","name":"full_typed_writer","arguments":"{\"input\": {\"style\": \"散文\", \"topic\": \"友谊\", \"wordCount\": 200}}"}],"media":[],"text":""},{"messageType":"TOOL","metadata":{"messageType":"TOOL"},"responses":[{"id":"call_12573a162aea4db59cdbaa89","name":"full_typed_writer","responseData":"```json\n{\n  \"title\": \"岁月长河中的同行者\",\n  \"content\": \"友谊如春风拂过心湖，泛起层层涟漪。年少时共摘野花的指尖温度，失意时默默递来的温热茶杯，都是时光镌刻的印记。不必刻意维系，像两棵相邻的树，根系在暗处悄然相握。岁月流转中，有人渐行渐远，而真正的知己始终站在记忆的站台，以目光为你点亮归途。那些共享的沉默比喧嚣更懂人心，分歧时的退让比迎合更见真诚。当白发覆上额角，仍能笑着说起当年月光下的誓言，原来最好的陪伴从不需要永恒证明，只需在每个平凡日子里，做彼此生命里安静的见证人。\",\n  \"characterCount\": 218\n}\n```"}],"text":""},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","refusal":"","finishReason":"TOOL_CALLS","annotations":[{}],"index":0,"id":"chatcmpl-ec372eb2-fcc6-99d9-8a4c-90050a4e8c18"},"toolCalls":[{"id":"call_5c2690b64c0a4c50bba9f7db","type":"function","name":"typed_reviewer","arguments":"{\"input\": \"岁月长河中的同行者\\n友谊如春风拂过心湖，泛起层层涟漪。年少时共摘野花的指尖温度，失意时默默递来的温热茶杯，都是时光镌刻的印记。不必刻意维系，像两棵相邻的树，根系在暗处悄然相握。岁月流转中，有人渐行渐远，而真正的知己始终站在记忆的站台，以目光为你点亮归途。那些共享的沉默比喧嚣更懂人心，分歧时的退让比迎合更见真诚。当白发覆上额角，仍能笑着说起当年月光下的誓言，原来最好的陪伴从不需要永恒证明，只需在每个平凡日子里，做彼此生命里安静的见证人。\"}"}],"media":[],"text":""},{"messageType":"TOOL","metadata":{"messageType":"TOOL"},"responses":[{"id":"call_5c2690b64c0a4c50bba9f7db","name":"typed_reviewer","responseData":"{\n  \"approved\": true,\n  \"comment\": \"文章语言凝练优美，情感真挚细腻，以诗意化的笔触勾勒出友谊在时光流转中的恒久力量。隐喻运用恰当，如“根系在暗处悄然相握”精准传递了知己间无需刻意维系的默契；结尾升华自然，点明“安静的见证人”这一核心立意，余韵悠长。整体具备成熟的散文质感与较强的感染力。\",\n  \"suggestions\": [\n    \"可在开篇或中段嵌入一个具体的生活切片或微小事件作为叙事支点，使抒情更有所依，增强文本的画面感与共鸣深度。\",\n    \"部分常见意象（如春风、心湖、月光誓言）虽意境妥帖，但若替换为更具个人生命经验的独特细节，将显著提升作品的辨识度与新鲜感。\",\n    \"目前为单段呈现，若用于正式发布建议按情感递进逻辑分段（如初识相伴、岁月沉淀、回首无言），以优化阅读节奏与呼吸感。\"\n  ]\n}"}],"text":""},{"messageType":"ASSISTANT","metadata":{"role":"ASSISTANT","messageType":"ASSISTANT","refusal":"","finishReason":"STOP","annotations":[{}],"index":0,"id":"chatcmpl-8e028fba-2bd2-904e-897f-46a653f5e11f"},"toolCalls":[],"media":[],"text":"为您完成了关于“友谊”的散文创作及专业评审，结果如下：\n\n### 📝 创作作品：《岁月长河中的同行者》\n\n> 友谊如春风拂过心湖，泛起层层涟漪。年少时共摘野花的指尖温度，失意时默默递来的温热茶杯，都是时光镌刻的印记。不必刻意维系，像两棵相邻的树，根系在暗处悄然相握。岁月流转中，有人渐行渐远，而真正的知己始终站在记忆的站台，以目光为你点亮归途。那些共享的沉默比喧嚣更懂人心，分歧时的退让比迎合更见真诚。当白发覆上额角，仍能笑着说起当年月光下的誓言，原来最好的陪伴从不需要永恒证明，只需在每个平凡日子里，做彼此生命里安静的见证人。\n\n---\n\n### ⚖️ 评审报告\n\n**评审结果：通过 ✅**\n\n**点评摘要：**\n文章语言凝练优美，情感真挚细腻，以诗意化的笔触勾勒出友谊在时光流转中的恒久力量。隐喻运用恰当，如“根系在暗处悄然相握”精准传递了知己间无需刻意维系的默契；结尾升华自然，点明“安静的见证人”这一核心立意，余韵悠长。整体具备成熟的散文质感与较强的感染力。\n\n**改进建议：**\n1. **叙事支点：** 可在开篇或中段嵌入一个具体的生活切片或微小事件作为叙事支点，使抒情更有所依，增强文本的画面感与共鸣深度。\n2. **意象独特性：** 部分常见意象（如春风、心湖、月光誓言）虽意境妥帖，但若替换为更具个人生命经验的独特细节，将显著提升作品的辨识度与新鲜感。\n3. **排版节奏：** 目前为单段呈现，若用于正式发布建议按情感递进逻辑分段（如初识相伴、岁月沉淀、回首无言），以优化阅读节奏与呼吸感。"}]}}}
         * overAllState.data()={_graph_execution_id_=a74dd3e5-06c2-455e-8a48-1c5a9d369113, input=请写一篇关于友谊的散文，约200字，需要评审, messages=[UserMessage{content='请写一篇关于友谊的散文，约200字，需要评审', metadata={messageType=USER}, messageType=USER}, AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_12573a162aea4db59cdbaa89, type=function, name=full_typed_writer, arguments={"input": {"style": "散文", "topic": "友谊", "wordCount": 200}}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, annotations=[{}], index=0, id=chatcmpl-dd74d15e-b9da-9884-b8d8-4d0cd0fdc5a0}], ToolResponseMessage{responses=[ToolResponse[id=call_12573a162aea4db59cdbaa89, name=full_typed_writer, responseData=```json
         * {
         *   "title": "岁月长河中的同行者",
         *   "content": "友谊如春风拂过心湖，泛起层层涟漪。年少时共摘野花的指尖温度，失意时默默递来的温热茶杯，都是时光镌刻的印记。不必刻意维系，像两棵相邻的树，根系在暗处悄然相握。岁月流转中，有人渐行渐远，而真正的知己始终站在记忆的站台，以目光为你点亮归途。那些共享的沉默比喧嚣更懂人心，分歧时的退让比迎合更见真诚。当白发覆上额角，仍能笑着说起当年月光下的誓言，原来最好的陪伴从不需要永恒证明，只需在每个平凡日子里，做彼此生命里安静的见证人。",
         *   "characterCount": 218
         * }
         * ```]], messageType=TOOL, metadata={messageType=TOOL}}, AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_5c2690b64c0a4c50bba9f7db, type=function, name=typed_reviewer, arguments={"input": "岁月长河中的同行者\n友谊如春风拂过心湖，泛起层层涟漪。年少时共摘野花的指尖温度，失意时默默递来的温热茶杯，都是时光镌刻的印记。不必刻意维系，像两棵相邻的树，根系在暗处悄然相握。岁月流转中，有人渐行渐远，而真正的知己始终站在记忆的站台，以目光为你点亮归途。那些共享的沉默比喧嚣更懂人心，分歧时的退让比迎合更见真诚。当白发覆上额角，仍能笑着说起当年月光下的誓言，原来最好的陪伴从不需要永恒证明，只需在每个平凡日子里，做彼此生命里安静的见证人。"}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, annotations=[{}], index=0, id=chatcmpl-ec372eb2-fcc6-99d9-8a4c-90050a4e8c18}], ToolResponseMessage{responses=[ToolResponse[id=call_5c2690b64c0a4c50bba9f7db, name=typed_reviewer, responseData={
         *   "approved": true,
         *   "comment": "文章语言凝练优美，情感真挚细腻，以诗意化的笔触勾勒出友谊在时光流转中的恒久力量。隐喻运用恰当，如“根系在暗处悄然相握”精准传递了知己间无需刻意维系的默契；结尾升华自然，点明“安静的见证人”这一核心立意，余韵悠长。整体具备成熟的散文质感与较强的感染力。",
         *   "suggestions": [
         *     "可在开篇或中段嵌入一个具体的生活切片或微小事件作为叙事支点，使抒情更有所依，增强文本的画面感与共鸣深度。",
         *     "部分常见意象（如春风、心湖、月光誓言）虽意境妥帖，但若替换为更具个人生命经验的独特细节，将显著提升作品的辨识度与新鲜感。",
         *     "目前为单段呈现，若用于正式发布建议按情感递进逻辑分段（如初识相伴、岁月沉淀、回首无言），以优化阅读节奏与呼吸感。"
         *   ]
         * }]], messageType=TOOL, metadata={messageType=TOOL}}, AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=为您完成了关于“友谊”的散文创作及专业评审，结果如下：
         *
         * ### 📝 创作作品：《岁月长河中的同行者》
         *
         * > 友谊如春风拂过心湖，泛起层层涟漪。年少时共摘野花的指尖温度，失意时默默递来的温热茶杯，都是时光镌刻的印记。不必刻意维系，像两棵相邻的树，根系在暗处悄然相握。岁月流转中，有人渐行渐远，而真正的知己始终站在记忆的站台，以目光为你点亮归途。那些共享的沉默比喧嚣更懂人心，分歧时的退让比迎合更见真诚。当白发覆上额角，仍能笑着说起当年月光下的誓言，原来最好的陪伴从不需要永恒证明，只需在每个平凡日子里，做彼此生命里安静的见证人。
         *
         * ---
         *
         * ### ⚖️ 评审报告
         *
         * **评审结果：通过 ✅**
         *
         * **点评摘要：**
         * 文章语言凝练优美，情感真挚细腻，以诗意化的笔触勾勒出友谊在时光流转中的恒久力量。隐喻运用恰当，如“根系在暗处悄然相握”精准传递了知己间无需刻意维系的默契；结尾升华自然，点明“安静的见证人”这一核心立意，余韵悠长。整体具备成熟的散文质感与较强的感染力。
         *
         * **改进建议：**
         * 1. **叙事支点：** 可在开篇或中段嵌入一个具体的生活切片或微小事件作为叙事支点，使抒情更有所依，增强文本的画面感与共鸣深度。
         * 2. **意象独特性：** 部分常见意象（如春风、心湖、月光誓言）虽意境妥帖，但若替换为更具个人生命经验的独特细节，将显著提升作品的辨识度与新鲜感。
         * 3. **排版节奏：** 目前为单段呈现，若用于正式发布建议按情感递进逻辑分段（如初识相伴、岁月沉淀、回首无言），以优化阅读节奏与呼吸感。, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, annotations=[{}], index=0, id=chatcmpl-8e028fba-2bd2-904e-897f-46a653f5e11f}]]}
         * -------------------打印---------------------
         * message = UserMessage{content='请写一篇关于友谊的散文，约200字，需要评审', metadata={messageType=USER}, messageType=USER}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_12573a162aea4db59cdbaa89, type=function, name=full_typed_writer, arguments={"input": {"style": "散文", "topic": "友谊", "wordCount": 200}}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, annotations=[{}], index=0, id=chatcmpl-dd74d15e-b9da-9884-b8d8-4d0cd0fdc5a0}]
         * message = ToolResponseMessage{responses=[ToolResponse[id=call_12573a162aea4db59cdbaa89, name=full_typed_writer, responseData=```json
         * {
         *   "title": "岁月长河中的同行者",
         *   "content": "友谊如春风拂过心湖，泛起层层涟漪。年少时共摘野花的指尖温度，失意时默默递来的温热茶杯，都是时光镌刻的印记。不必刻意维系，像两棵相邻的树，根系在暗处悄然相握。岁月流转中，有人渐行渐远，而真正的知己始终站在记忆的站台，以目光为你点亮归途。那些共享的沉默比喧嚣更懂人心，分歧时的退让比迎合更见真诚。当白发覆上额角，仍能笑着说起当年月光下的誓言，原来最好的陪伴从不需要永恒证明，只需在每个平凡日子里，做彼此生命里安静的见证人。",
         *   "characterCount": 218
         * }
         * ```]], messageType=TOOL, metadata={messageType=TOOL}}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[ToolCall[id=call_5c2690b64c0a4c50bba9f7db, type=function, name=typed_reviewer, arguments={"input": "岁月长河中的同行者\n友谊如春风拂过心湖，泛起层层涟漪。年少时共摘野花的指尖温度，失意时默默递来的温热茶杯，都是时光镌刻的印记。不必刻意维系，像两棵相邻的树，根系在暗处悄然相握。岁月流转中，有人渐行渐远，而真正的知己始终站在记忆的站台，以目光为你点亮归途。那些共享的沉默比喧嚣更懂人心，分歧时的退让比迎合更见真诚。当白发覆上额角，仍能笑着说起当年月光下的誓言，原来最好的陪伴从不需要永恒证明，只需在每个平凡日子里，做彼此生命里安静的见证人。"}]], textContent=, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=TOOL_CALLS, annotations=[{}], index=0, id=chatcmpl-ec372eb2-fcc6-99d9-8a4c-90050a4e8c18}]
         * message = ToolResponseMessage{responses=[ToolResponse[id=call_5c2690b64c0a4c50bba9f7db, name=typed_reviewer, responseData={
         *   "approved": true,
         *   "comment": "文章语言凝练优美，情感真挚细腻，以诗意化的笔触勾勒出友谊在时光流转中的恒久力量。隐喻运用恰当，如“根系在暗处悄然相握”精准传递了知己间无需刻意维系的默契；结尾升华自然，点明“安静的见证人”这一核心立意，余韵悠长。整体具备成熟的散文质感与较强的感染力。",
         *   "suggestions": [
         *     "可在开篇或中段嵌入一个具体的生活切片或微小事件作为叙事支点，使抒情更有所依，增强文本的画面感与共鸣深度。",
         *     "部分常见意象（如春风、心湖、月光誓言）虽意境妥帖，但若替换为更具个人生命经验的独特细节，将显著提升作品的辨识度与新鲜感。",
         *     "目前为单段呈现，若用于正式发布建议按情感递进逻辑分段（如初识相伴、岁月沉淀、回首无言），以优化阅读节奏与呼吸感。"
         *   ]
         * }]], messageType=TOOL, metadata={messageType=TOOL}}
         * message = AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=为您完成了关于“友谊”的散文创作及专业评审，结果如下：
         *
         * ### 📝 创作作品：《岁月长河中的同行者》
         *
         * > 友谊如春风拂过心湖，泛起层层涟漪。年少时共摘野花的指尖温度，失意时默默递来的温热茶杯，都是时光镌刻的印记。不必刻意维系，像两棵相邻的树，根系在暗处悄然相握。岁月流转中，有人渐行渐远，而真正的知己始终站在记忆的站台，以目光为你点亮归途。那些共享的沉默比喧嚣更懂人心，分歧时的退让比迎合更见真诚。当白发覆上额角，仍能笑着说起当年月光下的誓言，原来最好的陪伴从不需要永恒证明，只需在每个平凡日子里，做彼此生命里安静的见证人。
         *
         * ---
         *
         * ### ⚖️ 评审报告
         *
         * **评审结果：通过 ✅**
         *
         * **点评摘要：**
         * 文章语言凝练优美，情感真挚细腻，以诗意化的笔触勾勒出友谊在时光流转中的恒久力量。隐喻运用恰当，如“根系在暗处悄然相握”精准传递了知己间无需刻意维系的默契；结尾升华自然，点明“安静的见证人”这一核心立意，余韵悠长。整体具备成熟的散文质感与较强的感染力。
         *
         * **改进建议：**
         * 1. **叙事支点：** 可在开篇或中段嵌入一个具体的生活切片或微小事件作为叙事支点，使抒情更有所依，增强文本的画面感与共鸣深度。
         * 2. **意象独特性：** 部分常见意象（如春风、心湖、月光誓言）虽意境妥帖，但若替换为更具个人生命经验的独特细节，将显著提升作品的辨识度与新鲜感。
         * 3. **排版节奏：** 目前为单段呈现，若用于正式发布建议按情感递进逻辑分段（如初识相伴、岁月沉淀、回首无言），以优化阅读节奏与呼吸感。, metadata={role=ASSISTANT, messageType=ASSISTANT, refusal=, finishReason=STOP, annotations=[{}], index=0, id=chatcmpl-8e028fba-2bd2-904e-897f-46a653f5e11f}]
         * -------------------分类打印---------------------
         * UserMessage USER:
         * 请写一篇关于友谊的散文，约200字，需要评审
         * AssistantMessage 最终文本：
         * ToolResponseMessage TOOL: ToolResponseMessage{responses=[ToolResponse[id=call_12573a162aea4db59cdbaa89, name=full_typed_writer, responseData=```json
         * {
         *   "title": "岁月长河中的同行者",
         *   "content": "友谊如春风拂过心湖，泛起层层涟漪。年少时共摘野花的指尖温度，失意时默默递来的温热茶杯，都是时光镌刻的印记。不必刻意维系，像两棵相邻的树，根系在暗处悄然相握。岁月流转中，有人渐行渐远，而真正的知己始终站在记忆的站台，以目光为你点亮归途。那些共享的沉默比喧嚣更懂人心，分歧时的退让比迎合更见真诚。当白发覆上额角，仍能笑着说起当年月光下的誓言，原来最好的陪伴从不需要永恒证明，只需在每个平凡日子里，做彼此生命里安静的见证人。",
         *   "characterCount": 218
         * }
         * ```]], messageType=TOOL, metadata={messageType=TOOL}}
         * AssistantMessage 最终文本：
         * ToolResponseMessage TOOL: ToolResponseMessage{responses=[ToolResponse[id=call_5c2690b64c0a4c50bba9f7db, name=typed_reviewer, responseData={
         *   "approved": true,
         *   "comment": "文章语言凝练优美，情感真挚细腻，以诗意化的笔触勾勒出友谊在时光流转中的恒久力量。隐喻运用恰当，如“根系在暗处悄然相握”精准传递了知己间无需刻意维系的默契；结尾升华自然，点明“安静的见证人”这一核心立意，余韵悠长。整体具备成熟的散文质感与较强的感染力。",
         *   "suggestions": [
         *     "可在开篇或中段嵌入一个具体的生活切片或微小事件作为叙事支点，使抒情更有所依，增强文本的画面感与共鸣深度。",
         *     "部分常见意象（如春风、心湖、月光誓言）虽意境妥帖，但若替换为更具个人生命经验的独特细节，将显著提升作品的辨识度与新鲜感。",
         *     "目前为单段呈现，若用于正式发布建议按情感递进逻辑分段（如初识相伴、岁月沉淀、回首无言），以优化阅读节奏与呼吸感。"
         *   ]
         * }]], messageType=TOOL, metadata={messageType=TOOL}}
         * AssistantMessage 最终文本：为您完成了关于“友谊”的散文创作及专业评审，结果如下：
         *
         * ### 📝 创作作品：《岁月长河中的同行者》
         *
         * > 友谊如春风拂过心湖，泛起层层涟漪。年少时共摘野花的指尖温度，失意时默默递来的温热茶杯，都是时光镌刻的印记。不必刻意维系，像两棵相邻的树，根系在暗处悄然相握。岁月流转中，有人渐行渐远，而真正的知己始终站在记忆的站台，以目光为你点亮归途。那些共享的沉默比喧嚣更懂人心，分歧时的退让比迎合更见真诚。当白发覆上额角，仍能笑着说起当年月光下的誓言，原来最好的陪伴从不需要永恒证明，只需在每个平凡日子里，做彼此生命里安静的见证人。
         *
         * ---
         *
         * ### ⚖️ 评审报告
         *
         * **评审结果：通过 ✅**
         *
         * **点评摘要：**
         * 文章语言凝练优美，情感真挚细腻，以诗意化的笔触勾勒出友谊在时光流转中的恒久力量。隐喻运用恰当，如“根系在暗处悄然相握”精准传递了知己间无需刻意维系的默契；结尾升华自然，点明“安静的见证人”这一核心立意，余韵悠长。整体具备成熟的散文质感与较强的感染力。
         *
         * **改进建议：**
         * 1. **叙事支点：** 可在开篇或中段嵌入一个具体的生活切片或微小事件作为叙事支点，使抒情更有所依，增强文本的画面感与共鸣深度。
         * 2. **意象独特性：** 部分常见意象（如春风、心湖、月光誓言）虽意境妥帖，但若替换为更具个人生命经验的独特细节，将显著提升作品的辨识度与新鲜感。
         * 3. **排版节奏：** 目前为单段呈现，若用于正式发布建议按情感递进逻辑分段（如初识相伴、岁月沉淀、回首无言），以优化阅读节奏与呼吸感。
         *
         */
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

    public static class ReviewOutput {
        private String comment;
        private boolean approved;
        private List<String> suggestions;
        // getters and setters


        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }
    }

}
