import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import etherpad.EPLiteClient;

public class App {
    private static final String TARGET = System.getenv("TARGET");

    private static final String TARGET_SIG = System.getenv("TARGET") + "/sig";

    private static final String APPLICATION_PATH = System.getenv("APPLICATION_PATH");

    private static final String MAPPING_PATH = System.getenv("MAPPING_PATH");

    private static final String SIG_PATH = System.getenv("SIG_PATH");

    private static final String ETHERPAD_PATH = System.getenv("ETHERPAD_PATH");

    private static final String INDEX_PREFIX = "openeuler_articles";

    private static final String ETHERPAD_URL = System.getenv("ETHERPAD_URL");

    private static final String ETHERPAD_KEY = System.getenv("ETHERPAD_KEY");

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        try {
            PublicClient.CreateClientFormConfig(APPLICATION_PATH);
            PublicClient.makeIndex(INDEX_PREFIX + "_zh", MAPPING_PATH);
            PublicClient.makeIndex(INDEX_PREFIX + "_en", MAPPING_PATH);
            sigData();
            etherpadData();
            fileDate();
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error(e.toString());
        }

        logger.info("import end");
    }

    public static void sigData() throws Exception {
        File indexFile = new File(TARGET_SIG);
        if (!indexFile.exists()) {
            logger.info("%s folder does not exist%n", indexFile.getPath());
            return;
        }

        logger.info("begin to update sig data");
        Collection<File> listFiles = new ArrayList<>();
        File[] subdirectories = indexFile.listFiles((dir, name) -> new File(dir, name).isDirectory());
        if (subdirectories != null) {
            for (File subdir : subdirectories) {
                Collection<File> filesInSubdirectory = FileUtils.listFiles(subdir, new String[]{"yaml"}, false);
                listFiles.addAll(filesInSubdirectory);
            }
        } else {
            logger.info("sig data is null");
            return;
        }

        for (File paresFile : listFiles) {
                try {
                    // sig information has two language
                    Map<String, Object> escape = Parse.parseSigYaml(paresFile, "zh", SIG_PATH);
                    if (null != escape) {
                        PublicClient.insert(escape, INDEX_PREFIX + "_" + escape.get("lang"));
                    } else {
                        logger.info("parse null : " + paresFile.getPath());
                    }
                } catch (Exception e) {
                    logger.error(paresFile.getPath());
                    logger.error("sig data imported error {}", e.getMessage());
                }
            }
        logger.info("sig data imported end");
    }

    public static void etherpadData(){
        EPLiteClient client = new EPLiteClient(ETHERPAD_URL, ETHERPAD_KEY);

        Map result = client.listAllPads();
        List padIds = (List) result.get("padIDs");

        logger.info("begin to update etherpad data");
        for(Object padId : padIds) {
            Map<String, Object> resMap = client.getText((String) padId);
            if(resMap.containsKey("text")) {
                try {
                    Map<String, Object> escape = Parse.parseEtherPad(resMap.get("text"), padId.toString(), ETHERPAD_PATH);
                    if (null != escape) {
                        PublicClient.insert(escape, INDEX_PREFIX + "_" + escape.get("lang"));
                    }
                } catch (Exception e) {
                    logger.error("etherpad data imported error {}", e.getMessage());
                }
            }
        }
        logger.info("etherpad data imported end");
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
                try {
                    PublicClient.insert(lm, INDEX_PREFIX + "_" + lm.get("lang"));
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }


}
