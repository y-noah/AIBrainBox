package com.example.demo.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识分片模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Chunk {
    private Long id;          // 自动生成的 ID
    private String content;   // 文本内容
    private String source;    // 来源文件名或 URL
    private List<Float> vector; // 向量
    private Double score;     // 检索时的相似度分数
}