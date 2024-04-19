package com.softwaremarket.collect.test;


import com.alibaba.fastjson.JSONObject;
import com.softwaremarket.collect.Application;
import com.softwaremarket.collect.config.SoftwareConfig;
import com.softwaremarket.collect.enums.SoftwareTypeEnum;
import com.softwaremarket.collect.handler.SoftwareImportHandler;
import com.softwaremarket.collect.service.IEsDataService;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
@ContextConfiguration
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Slf4j
public class ImportApplicationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext ap;
    @Autowired
    private RestHighLevelClient restHighLevelClient;


    @Autowired
    SoftwareImportHandler softwareImportHandler;

    @Autowired
    IEsDataService esDataService;
    @Autowired
    SoftwareConfig softwareConfig;

    @Before
    public void setupMockMvc() throws Exception {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
    }


    @Test
    public void test() throws Exception {
       /* SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();


        boolQueryBuilder.filter(QueryBuilders.termQuery("dataType.keyword", "all"));
        sourceBuilder.query(boolQueryBuilder);
        SearchRequest request = new SearchRequest(softwareConfig.getImportEsIndex());
        request.source(sourceBuilder);

        System.out.println(request);
        SearchResponse searchResponse = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        System.out.println(searchResponse);*/
       /* String scrollId = searchResponse.getScrollId();

        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {

            DeleteRequest deleteRequest = new DeleteRequest(hit.getIndex(), hit.getId());
            System.out.println(hit.getId());
            DeleteResponse deleteResponse = restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);

        }*/


        //  softwareImportHandler.dbDataImportToEs(SoftwareTypeEnum.APPLICATION);
        //List<Map> esData = esDataService.getEsData(softwareConfig.getImportEsIndex());
        //System.out.println(JSONObject.toJSONString(esData));
        // esDataService.getEsData(softwareConfig.getImportEsIndex());
        //softwareImportHandler.applicationSoftwareHandle();
        //  softwareImportHandler.dbDataImportToEs(SoftwareTypeEnum.EKPG);
        //System.out.println("删除索引" + esDataService.deleteIndex(softwareConfig.getImportEsIndex()));
        // System.out.println("索引是否存在" + esDataService.checkEsIndexExist(softwareConfig.getImportEsIndex()));
        //  System.out.println("删除索引" + esDataService.deleteIndex(softwareConfig.getImportEsIndex()));
        // System.out.println("创建索引测试是否成功：" + esDataService.createEsIndex(softwareConfig.getImportEsIndex(), softwareConfig.getMappingPath()));


        //  System.out.println("索引是否存在" + esDataService.checkEsIndexExist(softwareConfig.getImportEsIndex()));


    }


}
