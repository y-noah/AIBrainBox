package com.example.demo.rag.service;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MilvusInitializer {

    @Autowired
    private MilvusServiceClient milvusClient;

    @Autowired
    private MilvusService milvusService;

    @Value("${milvus.collection-name}")
    private String collectionName;

    @Value("${milvus.dimension}")
    private int dimension;

    @PostConstruct
    public void init() {
        HasCollectionParam hasParam = HasCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .build();
        Boolean exists = milvusClient.hasCollection(hasParam).getData();

        if (!exists) {
            FieldType id = FieldType.newBuilder()
                    .withName("id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(true)
                    .build();

            FieldType content = FieldType.newBuilder()
                    .withName("content")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(2000)
                    .build();

            FieldType vector = FieldType.newBuilder()
                    .withName("embedding")
                    .withDataType(DataType.FloatVector)
                    .withDimension(dimension)
                    .build();

            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .addFieldType(id)
                    .addFieldType(content)
                    .addFieldType(vector)
                    .build();

            milvusClient.createCollection(createParam);
            System.out.println("Collection创建成功");
        }

        // 建索引
        IndexType indexType = IndexType.IVF_FLAT;
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(collectionName)
                .withFieldName("embedding")
                .withIndexType(indexType)
                .withMetricType(MetricType.L2)
                .withExtraParam("{\"nlist\":128}")
                .build();
        milvusClient.createIndex(indexParam);
        System.out.println("索引创建成功");

        milvusService.loadCollection();
        System.out.println("初始化完成");
    }
}
