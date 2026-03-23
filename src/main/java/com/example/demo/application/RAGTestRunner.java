package com.example.demo.application;

import com.example.demo.rag.service.RAGService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * RAG 功能演示与验证
 * 启动后会自动摄入 `resources/knowledge_base` 目录下的所有文档
 */
@Component
@RequiredArgsConstructor
public class RAGTestRunner implements CommandLineRunner {

    private final RAGService ragService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("         RAG 功能验证 - 自动文档摄入");
        System.out.println("=".repeat(60));

        try {
            // 1. 扫描 `resources/knowledge_base` 目录下的所有文件
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:knowledge_base/*");

            if (resources.length == 0) {
                System.out.println("No knowledge files found in 'resources/knowledge_base'. Skipping ingestion.");
                return;
            }

            System.out.println("Found " + resources.length + " knowledge files to ingest.");

            // 2. 遍历并摄入每个文件
            for (Resource resource : resources) {
                File file = resource.getFile();
                ragService.ingestFile(file);
            }

            // 3. 测试检索 (可选)
            System.out.println("\nStep 2: Testing RAG retrieval and generation...");
            String query = "订单 ORD20240115001 的客户是谁？什么时候能到？";
            System.out.println("Query: " + query);

            String answer = ragService.askWithRAG(query);
            System.out.println("\nAI Answer:\n" + answer);

        } catch (IOException e) {
            System.err.println("RAG Test failed during file scanning or ingestion: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("RAG Test failed: " + e.getMessage());
            // 如果 Milvus 没启动，这里会报错，属于预期内
        }

        System.out.println("=".repeat(60) + "\n");
    }
}