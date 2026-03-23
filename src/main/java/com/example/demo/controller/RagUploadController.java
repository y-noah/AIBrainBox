package com.example.demo.controller;

import com.example.demo.rag.service.RAGService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * RAG 文档上传接口
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagUploadController {

    private final RAGService ragService;

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        File tempFile = null;
        try {
            // 为了安全和隔离，创建临时文件来处理上传内容
            Path tempDir = Files.createTempDirectory("rag-uploads");
            tempFile = tempDir.resolve(file.getOriginalFilename()).toFile();
            file.transferTo(tempFile);

            // 调用 RAG 服务进行文件摄入
            ragService.ingestFile(tempFile);

            return ResponseEntity.ok("File ingested successfully: " + file.getOriginalFilename());

        } catch (IOException e) {
            System.err.println("File upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process file: " + e.getMessage());
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
                tempFile.getParentFile().delete(); // 清理临时目录
            }
        }
    }
}