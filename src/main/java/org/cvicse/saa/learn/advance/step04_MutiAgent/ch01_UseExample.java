package org.cvicse.saa.learn.advance.step04_MutiAgent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;

/**
 * 使用示例
 *
 * 占位符工作原理
 * 自动替换：系统会在执行 Agent 的 instruction 时，自动将占位符替换为对应的实际值
 * 状态查找：占位符会从当前状态（OverAllState）中查找对应的值
 * 类型安全：占位符的值会被转换为字符串并插入到 instruction 中
 */
public class ch01_UseExample {
    public static void main(String[] args) {
        starter();
    }

    public static void starter() {
        // 第一个Agent：使用 {input} 获取用户输入
        ReactAgent writerAgent = ReactAgent.builder()
                .name("writer_agent")
                .instruction("你是一个知名的作家。请根据用户的提问进行回答：{input}。")
                .outputKey("article")
                .build();

        // 第二个Agent：使用 {article} 引用第一个Agent的输出
        ReactAgent reviewerAgent = ReactAgent.builder()
                .name("reviewer_agent")
                .instruction("请对文章进行评审修正：\n{article}，最终返回评审修正后的文章内容")
                .outputKey("reviewed_article")
                .build();
    }
}
