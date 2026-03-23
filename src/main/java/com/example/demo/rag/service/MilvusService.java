package com.example.demo.rag.service;

import com.example.demo.rag.model.Chunk;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Milvus 向量数据库服务实现
 * 提供基础的 Collection 管理、数据插入和向量搜索功能
 */
@Service
public class MilvusService {

    private final String host;
    private final int port;
    private final String collectionName;
    private final int dimension;

    private MilvusServiceClient milvusClient;

    public MilvusService(@Value("${milvus.host:localhost}") String host,
                         @Value("${milvus.port:19530}") int port,
                         @Value("${milvus.collection-name:aibrainbox_knowledge}") String collectionName,
                         @Value("${milvus.dimension:4096}") int dimension) {
        this.host = host;
        this.port = port;
        this.collectionName = collectionName;
        this.dimension = dimension;
    }

    @PostConstruct
    public void init() {
        // 初始化 Milvus 客户端
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();
        this.milvusClient = new MilvusServiceClient(connectParam);

        // 初始化 Collection
        createCollectionIfNotExists();
    }

    @PreDestroy
    public void close() {
        if (milvusClient != null) {
            milvusClient.close();
        }
    }

    /**
     * 如果 Collection 不存在则创建
     */
    private void createCollectionIfNotExists() {
        R<Boolean> hasCollection = milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );

        if (hasCollection.getData()) {
            System.out.println("Milvus collection " + collectionName + " already exists.");
            return;
        }

        // 定义表结构
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();

        FieldType contentField = FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(65535) // 最大文本长度
                .build();

        FieldType sourceField = FieldType.newBuilder()
                .withName("source")
                .withDataType(DataType.VarChar)
                .withMaxLength(512)
                .build();

        FieldType vectorField = FieldType.newBuilder()
                .withName("vector")
                .withDataType(DataType.FloatVector)
                .withDimension(dimension)
                .build();

        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .addFieldType(idField)
                .addFieldType(contentField)
                .addFieldType(sourceField)
                .addFieldType(vectorField)
                .build();

        R<RpcStatus> response = milvusClient.createCollection(createParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            throw new RuntimeException("Failed to create Milvus collection: " + response.getMessage());
        }

        // 创建索引以支持搜索
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("vector")
                .withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.L2)
                .withExtraParam("{\"nlist\":1024}")
                .build();
        
        milvusClient.createIndex(indexParam);
        
        // 加载 Collection 到内存
        milvusClient.loadCollection(LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build());
        
        System.out.println("Milvus collection " + collectionName + " created and loaded.");
    }

    /**
     * 插入知识分片
     */
    public void insertChunks(List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return;

        List<String> contents = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        List<List<Float>> vectors = new ArrayList<>();

        for (Chunk chunk : chunks) {
            contents.add(chunk.getContent());
            sources.add(chunk.getSource());
            vectors.add(chunk.getVector());
        }

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("content", contents));
        fields.add(new InsertParam.Field("source", sources));
        fields.add(new InsertParam.Field("vector", vectors));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        R<MutationResult> response = milvusClient.insert(insertParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.err.println("Failed to insert into Milvus: " + response.getMessage());
        }
    }

    /**
     * 向量搜索最相似的内容
     */
    public List<Chunk> search(List<Float> queryVector, int topK) {
        List<String> outFields = List.of("content", "source");

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withMetricType(MetricType.L2)
                .withOutFields(outFields)
                .withTopK(topK)
                .withVectors(Collections.singletonList(queryVector))
                .withVectorFieldName("vector")
                .withParams("{\"nprobe\":10}")
                .build();

        R<SearchResults> response = milvusClient.search(searchParam);
        if (response.getStatus() != R.Status.Success.getCode()) {
            System.err.println("Milvus search failed: " + response.getMessage());
            return Collections.emptyList();
        }

        SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
        List<Chunk> results = new ArrayList<>();
        
        List<SearchResultsWrapper.IDScore> idScores = wrapper.getIDScore(0);
        for (int i = 0; i < idScores.size(); i++) {
            SearchResultsWrapper.IDScore idScore = idScores.get(i);
            String content = (String) wrapper.getFieldData("content", 0).get(i);
            String source = (String) wrapper.getFieldData("source", 0).get(i);
            
            results.add(Chunk.builder()
                    .content(content)
                    .source(source)
                    .score((double) idScore.getScore())
                    .build());
        }

        return results;
    }
}