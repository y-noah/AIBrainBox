package com.example.demo.rag.service;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.MetricType;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.highlevel.dml.SearchSimpleParam;
import io.milvus.response.SearchResultsWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MilvusService {

    @Autowired
    private MilvusServiceClient milvusClient;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Value("${milvus.collection-name}")
    private String collectionName;

    // 存入一批文本块
    public void insert(List<String> chunks) {
        List<List<Float>> vectors = chunks.stream()
                .map(embeddingClient::embed)
                .collect(Collectors.toList());

        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("content", chunks));
        fields.add(new InsertParam.Field("embedding", vectors));

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)
                .withFields(fields)
                .build();

        milvusClient.insert(insertParam);
        System.out.println("插入成功，共" + chunks.size() + "条");
    }

    // 建完collection之后调用
    public void loadCollection() {
        LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
        milvusClient.loadCollection(loadParam);
        System.out.println("Collection已加载");
    }


    public List<String> search(String query, int topK) {
        List<Float> queryVector = embeddingClient.embed(query);

        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectorFieldName("embedding")
                .withVectors(Collections.singletonList(queryVector))
                .withTopK(topK)
                .withMetricType(MetricType.L2)
                .withOutFields(Collections.singletonList("content"))
                .build();

        SearchResultsWrapper wrapper = new SearchResultsWrapper(
                milvusClient.search(searchParam).getData().getResults()
        );

        List<String> results = new ArrayList<>();
        List<?> contentList = wrapper.getFieldWrapper("content").getFieldData();
        for (Object item : contentList) {
            results.add(item.toString());
        }
        return results;
    }

}
