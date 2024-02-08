import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

public class App {
    private static final String APPLICATION_PATH = System.getenv("APPLICATION_PATH");

    private static final String ES_INDEX = System.getenv("ES_INDEX");

    public static RestHighLevelClient restHighLevelClient;

    public static YamlConfig yamlConfig;

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

            importSearchKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void importSearchKey() throws Exception {

        SearchRequest request = new SearchRequest(ES_INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.aggregation(AggregationBuilders.terms("data").field("search_key.keyword").size(100000));
        request.source(sourceBuilder);


        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);

        ParsedTerms aggregation = response.getAggregations().get("data");

        List<? extends Terms.Bucket> buckets = aggregation.getBuckets();

        String validCharsRegex = "\\A[\\p{IsHan}\\p{Alnum}\\p{Punct}\\s]*\\z";


        String url = yamlConfig.getMysqlUrl();
        String username = yamlConfig.getUsername();
        String password = yamlConfig.getPassword();
        Connection connection = DriverManager.getConnection(url, username, password);

        int d = 0;
        for (Terms.Bucket bucket : buckets) {
            if (bucket.getKeyAsString().matches(validCharsRegex)) {
                String searchWord = bucket.getKeyAsString();
                long searchCount = bucket.getDocCount();
                String checkSql = "SELECT id FROM search_key_count WHERE search_word = ?";
                try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                    checkStmt.setString(1, searchWord);
                    ResultSet resultSet = checkStmt.executeQuery();

                    if (resultSet.next()) {
                        String updateSql = "UPDATE search_key_count SET search_count = ? WHERE search_word = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                            d++;
                            System.out.println(d);
                            System.out.println(searchWord + " - " + searchCount);
                            updateStmt.setLong(1, searchCount);
                            updateStmt.setString(2, searchWord);
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // 如果不存在，插入新记录
                        String insertSql = "INSERT INTO search_key_count (id, search_word, search_count) VALUES (?, ?, ?)";
                        try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                            insertStmt.setString(1, UUID.randomUUID().toString().replace("-", ""));
                            insertStmt.setString(2, searchWord);
                            insertStmt.setLong(3, searchCount);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        }
        System.out.println("over");
    }
}
