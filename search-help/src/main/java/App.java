import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {
    private static final String APPLICATION_PATH = System.getenv("APPLICATION_PATH");

    private static final String NDCG_ES_INDEX = System.getenv("NDCG_ES_INDEX");

    private static final String SEARCH_WORD_INDEX = System.getenv("SEARCH_WORD_INDEX");

    public static RestHighLevelClient restHighLevelClientNDCG;

    public static RestHighLevelClient restHighLevelClient;

    public static YamlConfig yamlConfig;

      private static final Logger logger = LoggerFactory.getLogger(App.class);


    public static void main(String[] args) {
        String configPath = APPLICATION_PATH;
        try {
            Yaml yaml = new Yaml(new Constructor(YamlConfig.class));
            InputStream inputStream = new FileInputStream(configPath);
            yamlConfig = yaml.load(inputStream);
            File configFile = new File(configPath);
            if (configFile.exists()) {
                if (configFile.delete()) {
                    System.out.println("File deleted successfully");
                } else {
                    System.out.println("Failed to delete the file");
                }
            }

            if (yamlConfig.isUseCer()) {
                restHighLevelClientNDCG = EsClientCer.create(
                        yamlConfig.getHostNDCG(),
                        yamlConfig.getPortNDCG(),
                        yamlConfig.getProtocolNDCG(),
                        5 * 1000,
                        5 * 1000,
                        30 * 1000,
                        yamlConfig.getUsernameNDCG(),
                        yamlConfig.getPasswordNDCG(),
                        yamlConfig.getCerFilePathNDCG(),
                        yamlConfig.getCerPasswordNDCG()
                );
            } else {
                restHighLevelClientNDCG = EsClient.create(
                        yamlConfig.getHostNDCG(),
                        yamlConfig.getPortNDCG(),
                        yamlConfig.getProtocolNDCG(),
                        5 * 1000,
                        5 * 1000,
                        30 * 1000,
                        yamlConfig.getUsernameNDCG(),
                        yamlConfig.getPasswordNDCG()
                );
            }

            if (yamlConfig.isUseCer()) {
                restHighLevelClient = EsClientCer.create(
                        yamlConfig.getHost(),
                        yamlConfig.getPort(),
                        yamlConfig.getProtocol(),
                        5 * 1000,
                        5 * 1000,
                        30 * 1000,
                        yamlConfig.getUsername(),
                        yamlConfig.getPassword(),
                        yamlConfig.getCerFilePath(),
                        yamlConfig.getCerPassword()
                );
            } else {
                restHighLevelClient = EsClient.create(
                        yamlConfig.getHost(),
                        yamlConfig.getPort(),
                        yamlConfig.getProtocol(),
                        5 * 1000,
                        5 * 1000,
                        30 * 1000,
                        yamlConfig.getUsername(),
                        yamlConfig.getPassword()
                );
            }

            GetIndexRequest request = new GetIndexRequest(SEARCH_WORD_INDEX);
            request.local(false);
            request.humanReadable(true);
            request.includeDefaults(false);
            boolean exists = restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
            if (exists) {
                return;
            }
            CreateIndexRequest request1 = new CreateIndexRequest(SEARCH_WORD_INDEX);
            restHighLevelClient.indices().create(request1, RequestOptions.DEFAULT);

            importSearchKey();

            logger.info("import end");
            System.exit(0);
        } catch (Exception e) {
            logger.error("import error !", e.getMessage());

            System.exit(0);
        }
    }

    public static void importSearchKey() throws Exception {

        SearchRequest request = new SearchRequest(NDCG_ES_INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.aggregation(AggregationBuilders.terms("data").field("search_key.keyword").size(100000));
        request.source(sourceBuilder);


        SearchResponse response = restHighLevelClientNDCG.search(request, RequestOptions.DEFAULT);

        ParsedTerms aggregation = response.getAggregations().get("data");

        List<? extends Terms.Bucket> buckets = aggregation.getBuckets();

        String validCharsRegex = "\\A[\\p{IsHan}\\p{Alnum}\\p{Punct}\\s]*\\z";


        for (Terms.Bucket bucket : buckets) {
            if (bucket.getKeyAsString().matches(validCharsRegex)) {
                String searchWord = bucket.getKeyAsString();
                long searchCount = bucket.getDocCount();

                Map<String, Object> data = new HashMap<>();
                data.put("path", searchWord);
                data.put("searchWord", searchWord);
                data.put("searchCount", searchCount);
                try {
                    IndexRequest indexRequest = new IndexRequest(SEARCH_WORD_INDEX).id((String) data.get("path")).source(data);
                    restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
                } catch (Exception e) {
                    System.out.println(searchWord + " -- " + searchCount + "not insert");
                }
            }
        }
        System.out.println("over");
    }




}
