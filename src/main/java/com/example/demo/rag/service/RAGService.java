package com.example.demo.rag.service;

import com.example.demo.llm.api.LLMClient;
import com.example.demo.llm.api.LLMRequest;
import com.example.demo.llm.api.LLMResult;
import com.example.demo.rag.model.Chunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG 服务核心逻辑
 * 编排整个流程：文档摄入（Ingest）、检索（Retrieve）、生成（Generate）
 */
@Service
@RequiredArgsConstructor
public class RAGService {

    private final LLMClient llmClient;
    private final MilvusService milvusService;
    private final DocumentProcessor documentProcessor;

    /**
     * 将文件摄入向量库
     */
    public void ingestFile(File file) throws IOException {
        System.out.println("Ingesting file: " + file.getName());
        
        // 1. 读取文本
        String fullText = documentProcessor.readTextFromFile(file);
        
        // 2. 切片
        List<String> textChunks = documentProcessor.splitToChunks(fullText);
        
        // 3. 生成向量并构建 Chunk 对象
        List<Chunk> chunksToInsert = new ArrayList<>();
        for (String text : textChunks) {
            List<Float> vector = llmClient.embed(text);
            if (!vector.isEmpty()) {
                chunksToInsert.add(Chunk.builder()
                        .content(text)
                        .source(file.getName())
                        .vector(vector)
                        .build());
            }
        }
        
        // 4. 存入 Milvus
        milvusService.insertChunks(chunksToInsert);
        System.out.println("Successfully ingested " + chunksToInsert.size() + " chunks.");
    }

    /**
     * 根据查询获取最相关的上下文
     */
    public String retrieveContext(String query, int topK) {
        System.out.println("\n[RAG] 开始检索上下文，问题: " + query);
        
        // 1. 查询文本向量化
        List<Float> queryVector = llmClient.embed(query);
        if (queryVector.isEmpty()) {
            System.out.println("[RAG] 向量化失败，未找到上下文");
            return "";
        }
        
        // 2. 从向量库搜索
        List<Chunk> relevantChunks = milvusService.search(queryVector, topK);
        
        if (relevantChunks.isEmpty()) {
            System.out.println("[RAG] Milvus 检索结果为空");
            return "";
        }

        System.out.println("[RAG] 检索到 " + relevantChunks.size() + " 条相关片段:");
        for (int i = 0; i < relevantChunks.size(); i++) {
            Chunk chunk = relevantChunks.get(i);
            System.out.printf("   [%d] 分数: %.4f | 来源: %s | 内容: %s...\n", 
                i + 1, chunk.getScore(), chunk.getSource(), 
                chunk.getContent().substring(0, Math.min(50, chunk.getContent().length())).replace("\n", " "));
        }
        
        // 3. 拼接待用上下文
        return relevantChunks.stream()
                .map(chunk -> "[Source: " + chunk.getSource() + "]\n" + chunk.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    /**
     * 使用 RAG 回答问题
     */
    public String askWithRAG(String query) {
        // 1. 检索上下文
        String context = retrieveContext(query, 3);
        
        // 2. 构建增强提示词 (Prompt Augmentation)
        String systemPrompt = """
            你是一个知识渊博的助手。请基于提供的【参考信息】来回答用户的问题。
            如果参考信息中没有相关内容，请如实告知。
            
            【参考信息】：
            %s
            """.formatted(context.isEmpty() ? "暂无相关参考信息" : context);

        System.out.println("\n[RAG] 增强后的 System Prompt:\n" + systemPrompt);
        System.out.println("[RAG] 发送给 LLM 的用户问题: " + query);

        // 3. 调用 LLM 生成回答
        LLMRequest request = new LLMRequest(query, systemPrompt);
        LLMResult result = llmClient.chat(request);
        
        if (result.isSuccess()) {
            System.out.println("[RAG] LLM 生成回答成功");
            return result.rawText();
        } else {
            System.out.println("[RAG] LLM 生成回答失败: " + result.error());
            return "Error in LLM generation: " + result.error();
        }
    }
}