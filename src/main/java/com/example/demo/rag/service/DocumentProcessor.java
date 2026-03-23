package com.example.demo.rag.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档处理器
 * 提供文档加载、解析和切片（Chunking）功能
 */
@Service
public class DocumentProcessor {

    private static final int DEFAULT_CHUNK_SIZE = 500;   // 默认分片大小
    private static final int DEFAULT_OVERLAP = 50;       // 默认重叠大小

    /**
     * 根据文件路径读取文本
     */
    public String readTextFromFile(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".txt")) {
            return Files.readString(file.toPath());
        } else if (fileName.endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        }
        return "";
    }

    /**
     * 将长文本切分为小块（Chunks）
     * 采用固定窗口大小 + 重叠部分的方式，保证上下文连贯性
     */
    public List<String> splitToChunks(String text) {
        return splitToChunks(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public List<String> splitToChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        // 简单的基于字符数的切分
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end).trim());
            
            if (end == text.length()) break;
            start = end - overlap; // 向后移动，保留重叠部分
            
            // 避免无限循环
            if (start < 0) start = 0;
            if (start >= end) start = end;
        }

        return chunks;
    }
}