import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final String TARGET = System.getenv("TARGET");

    private static final String APPLICATION_PATH = System.getenv("APPLICATION_PATH");

    private static final String MAPPING_PATH = System.getenv("MAPPING_PATH");

    private static final String INDEX_PREFIX = "merlin_articles";

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


    public static void fileDate() {
        File indexFile = new File(TARGET);
        if (!indexFile.exists()) {
            logger.error("folder does not exist: ", indexFile.getPath());
            return;
        }

        logger.info("begin to update document");

        Set<String> idSet = new HashSet<>();

        Collection<File> listFiles = FileUtils.listFiles(indexFile, new String[]{"md", "html"}, true);
        ArrayList<Map<String, Object>> dateLists = new ArrayList<>();

        for (File paresFile : listFiles) {
            if (!paresFile.getName().startsWith("_")) {
                try {
                    Map<String, Object> escape = Parse.parse(paresFile);
                    if (null != escape) {
                        dateLists.add(escape);
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

        Map<Object, List<Map<String, Object>>> group = dateLists.stream().collect(Collectors.groupingBy(a -> a.get("group")));
        for (Map.Entry<Object, List<Map<String, Object>>> objectListEntry : group.entrySet()) {
            List<Map<String, Object>> value = objectListEntry.getValue();
            if (value.size() == 1) {
                Map<String, Object> map = value.get(0);
                map.put("versionTag","maximumVersion");
            } else {
                value.sort((u1, u2) -> compareVersions(String.valueOf(u2.get("version")), String.valueOf(u1.get("version"))));
                Map<String, Object> map = value.get(0);
                map.put("versionTag","maximumVersion");
            }
        }
        for (Map<String, Object> escape : dateLists) {
            try {
                if(!escape.containsKey("versionTag")){
                    escape.put("versionTag","common");
                }
                PublicClient.insert(escape, INDEX_PREFIX + "_" + escape.get("lang"));
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        logger.info("start delete expired document");
        PublicClient.deleteExpired(idSet, INDEX_PREFIX + "_*");

    }

    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.substring(1).split("\\.");
        String[] parts2 = v2.substring(1).split("\\.");

        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return num1 - num2;
            }
        }
        return 0;
    }

}






