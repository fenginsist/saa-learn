package org.cvicse.saa.learn.advance.step03_Memory;


import com.alibaba.cloud.ai.graph.store.StoreSearchRequest;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.store.StoreItem;

import java.util.*;

public class ch01_MemoryStore {
    public static void main(String[] args) {
        starter();
    }

    public static void starter() {


        // MemoryStore 将数据保存到内存字典中。在生产环境中请使用基于数据库的存储实现
        MemoryStore store = new MemoryStore();

        String userId = "my-user";
        String applicationContext = "chitchat";
        List<String> namespace = List.of(userId, applicationContext);

        // 保存记忆
        Map<String, Object> memoryData = new HashMap<>();
        memoryData.put("rules", List.of(
                "用户喜欢简短直接的语言",
                "用户只说中文和Java"
        ));
        memoryData.put("my-key", "my-value");

        StoreItem item = StoreItem.of(namespace, "a-memory", memoryData);
        store.putItem(item);

        // 通过ID获取记忆
        Optional<StoreItem> retrievedItem = store.getItem(namespace, "a-memory");

        // 在此命名空间内搜索记忆，通过内容等价性过滤，按向量相似度排序
        // List<StoreItem> items = store.searchItems(namespace, Map.of("my-key", "my-value"));
        List<StoreItem> items = store.searchItems(StoreSearchRequest.builder().build()).getItems();
    }
}
