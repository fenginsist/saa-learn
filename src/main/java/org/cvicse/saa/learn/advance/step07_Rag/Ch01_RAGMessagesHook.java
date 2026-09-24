package org.cvicse.saa.learn.advance.step07_Rag;

import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// 假设你已经有一个配置好的向量存储
//VectorStore vectorStore = null; // 配置你的向量存储（如Milvus、Pinecone等）

// 创建 RAG Hook：在模型调用前检索文档并添加到消息中
@HookPositions({HookPosition.BEFORE_MODEL})
public class Ch01_RAGMessagesHook extends MessagesModelHook {

    private final VectorStore vectorStore;
    private static final int TOP_K = 5;

    public Ch01_RAGMessagesHook(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public String getName() {
        return "rag_messages_hook";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        // 从消息中提取用户问题
        String userQuestion = extractUserQuestion(previousMessages);
        if (userQuestion == null || userQuestion.isEmpty()) {
            return new AgentCommand(previousMessages);
        }

        // Step 1: 检索相关文档
        List<Document> relevantDocs = vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.builder()
                        .query(userQuestion)
                        .topK(TOP_K)
                        .build()
        );

        // Step 2: 构建上下文
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining(""));
        // Step 3: 构建增强的消息列表
        List<Message> enhancedMessages = new ArrayList<>();

        // 添加系统提示（包含检索到的上下文）
        String systemPrompt = String.format("""
          你是一个有用的助手。基于以下上下文回答问题。
          如果上下文中没有相关信息，请说明你不知道。
          
          上下文：
          %s
          """, context);
        enhancedMessages.add(new SystemMessage(systemPrompt));

        // 保留原有的消息
        enhancedMessages.addAll(previousMessages);

        // 使用 REPLACE 策略替换消息
        return new AgentCommand(enhancedMessages, UpdatePolicy.REPLACE);
    }

    private String extractUserQuestion(List<Message> messages) {
        // 从消息列表中提取最后一个用户消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof UserMessage) {
                return ((UserMessage) msg).getText();
            }
        }
        return null;
    }

}
