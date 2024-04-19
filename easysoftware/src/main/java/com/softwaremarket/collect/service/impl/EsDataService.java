package com.softwaremarket.collect.service.impl;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.alibaba.fastjson.JSON;
import com.softwaremarket.collect.config.EsConfig;
import com.softwaremarket.collect.dto.SoftwareBaseDto;
import com.softwaremarket.collect.service.IEsDataService;
import com.softwaremarket.collect.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EsDataService implements IEsDataService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private BulkProcessor bulkProcessor;


    @Override
    public Boolean createEsIndex(String index, String mappingPath) {
        try {
            if (checkEsIndexExist(index)) {
                return Boolean.TRUE;
            }
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(index);
            String mapping = JsonFileUtil.read(mappingPath);

            createIndexRequest.mapping(mapping, XContentType.JSON);
            createIndexRequest.setTimeout(TimeValue.timeValueMillis(1));
            CreateIndexResponse createIndexResponse = restHighLevelClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            if (createIndexResponse != null && index.equals(createIndexResponse.index()))
                return Boolean.TRUE;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Boolean.FALSE;
    }

    @Override
    public Boolean deleteIndex(String index) {

        boolean exists = checkEsIndexExist(index);
        if (!exists) {
            //不存在就结束
            return Boolean.TRUE;
        }
        //索引存在，就执行删除
        long start = System.currentTimeMillis();

        DeleteIndexRequest request = new DeleteIndexRequest(index);
        request.timeout(TimeValue.timeValueMinutes(2));
        request.timeout("2m");
        try {
            AcknowledgedResponse delete = restHighLevelClient.indices().delete(request, RequestOptions.DEFAULT);
            System.out.println(delete);
            log.info("删除索引成功：" + index);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                restHighLevelClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        long end = System.currentTimeMillis();
        //计算删除耗时
        log.info("删除{}索引成功，耗时：{}", index, end - start);
        return Boolean.TRUE;

    }

    @Override
    public Boolean checkEsIndexExist(String index) {
        GetIndexRequest request = new GetIndexRequest(index);
        request.local(false);
        request.humanReadable(true);
        request.includeDefaults(false);
        try {
            return restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Boolean.FALSE;
    }

    @Override
    public void insertEsData(String index, List<SoftwareBaseDto> datas) {
        List<IndexRequest> indexRequests = new ArrayList<>();
        for (SoftwareBaseDto data : datas) {
            try {
                IndexRequest request = new IndexRequest(index);
                //填充id
                request.id(data.getPkgId() + "#" + SecureRandom.getInstance("SHA1PRNG").nextInt());
                //先不修改id
                request.source(JSON.toJSONString(data), XContentType.JSON);
                request.opType(DocWriteRequest.OpType.CREATE);

                //   IndexRequest indexRequest = new IndexRequest(index).id((String) data.get("path")).source(data);
                IndexResponse indexResponse = restHighLevelClient.index(request, RequestOptions.DEFAULT);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        /*datas.parallelStream().forEach(e -> {
            try {
            IndexRequest request = new IndexRequest(index);
            //填充id
            request.id(e.getPkgId() + "#"+ SecureRandom.getInstance("SHA1PRNG").nextInt());
            //先不修改id
            request.source(JSON.toJSONString(e), XContentType.JSON);
            request.opType(DocWriteRequest.OpType.CREATE);

            //   IndexRequest indexRequest = new IndexRequest(index).id((String) data.get("path")).source(data);
                IndexResponse indexResponse = restHighLevelClient.index(request, RequestOptions.DEFAULT);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            //  indexRequests.add(request);
        });*/
        // indexRequests.forEach(bulkProcessor::add);
    }

    @Override
    public List<Map> getEsData(String index) {
        long st = System.currentTimeMillis();
        int scrollSize = 10;//一次读取的doc数量
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());//读取全量数据
        searchSourceBuilder.size(scrollSize);
        Scroll scroll = new Scroll(TimeValue.timeValueMinutes(10));//设置一次读取的最大连接时长
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.source(searchSourceBuilder);
        searchRequest.scroll(scroll);

        SearchResponse searchResponse = null;
        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println("searchResponse:" + searchResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String scrollId = searchResponse.getScrollId();

        SearchHit[] hits = searchResponse.getHits().getHits();

        for (SearchHit hit : hits) {
            System.out.println(hit);
        }
        return null;
    }
}
