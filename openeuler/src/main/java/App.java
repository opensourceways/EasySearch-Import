import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class App {
    private static final String TARGET = System.getenv("TARGET");

    private static final String APPLICATION_PATH = System.getenv("APPLICATION_PATH");

    private static final String MAPPING_PATH = System.getenv("MAPPING_PATH");

    private static final String INDEX_PREFIX = "openeuler_articles";

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            PublicClient.CreateClientFormConfig(APPLICATION_PATH);
            PublicClient.makeIndex(INDEX_PREFIX + "_zh", MAPPING_PATH);
            PublicClient.makeIndex(INDEX_PREFIX + "_en", MAPPING_PATH);
            fileDate();
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error(e.toString());
        }

        logger.info("import end");
        System.exit(0);
    }

    public static void fileDate() throws Exception {
        File indexFile = new File(TARGET);
        if (!indexFile.exists()) {
            logger.info("%s folder does not exist%n", indexFile.getPath());
            return;
        }

        logger.info("begin to update document");

        Set<String> idSet = new HashSet<>();

        Collection<File> listFiles = FileUtils.listFiles(indexFile, new String[]{"md", "html"}, true);
        List<Map<String, Object>> inserDataList = new ArrayList<>();

        for (File paresFile : listFiles) {
            if (!paresFile.getName().startsWith("_")) {
                try {
                    Map<String, Object> escape = Parse.parse(paresFile);
                    if (null != escape) {
                        inserDataList.add(escape);
                        idSet.add((String) escape.get("path"));
                    } else {
                        logger.info("parse null : " + paresFile.getPath());
                    }
                } catch (Exception e) {
                    logger.error(paresFile.getPath());
                    logger.error(e.getMessage());
                }
            }
        }

        List<Map<String, Object>> customizeEscape = Parse.customizeData();
        if (null == customizeEscape) {
            return;
        }
        inserDataList.addAll(customizeEscape);

        Map<String, List<Map<String, Object>>> typeMap = inserDataList.stream().collect(Collectors.groupingBy(a -> a.get("type") + "#" + a.get("lang")
        ));
        for (Map.Entry<String, List<Map<String, Object>>> stringListEntry : typeMap.entrySet()) {
            String key = stringListEntry.getKey();
            String[] split = key.split("#");
            String type = split[0];
            String lang = split[1];
            PublicClient.deleteByType(INDEX_PREFIX + "_" + lang, type);
            for (Map<String, Object> lm : stringListEntry.getValue()) {
                PublicClient.insert(lm, INDEX_PREFIX + "_" + lm.get("lang"));
            }
        }
    }


}
