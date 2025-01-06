import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import etherpad.EPLiteClient;

public final class App {

    /**
     *Target file route.
     */
    private static final String TARGET = System.getenv("TARGET");

    /**
     *Target sig file route.
     */
    private static final String TARGET_SIG = System.getenv("TARGET") + "/sig";

    /**
     *Target release file route.
     */
    private static final String TARGET_RELEASE = System.getenv("TARGET") + "/release";

    /**
     *Target aggregate file route.
     */
    private static final String TARGET_AGGREGATE = System.getenv("TARGET") + "/aggregate";

    /**
     *Application path.
     */
    private static final String APPLICATION_PATH = System.getenv("APPLICATION_PATH");

    /**
     *Mapping path of es.
     */
    private static final String MAPPING_PATH = System.getenv("MAPPING_PATH");

    /**
     *Etherpad data path prefix.
     */
    private static final String ETHERPAD_PATH = System.getenv("ETHERPAD_PATH");

    /**
     *Etherpad data path prefix.
     */
    private static final String INDEX_PREFIX = "openeuler_articles";

    /**
     *Etherpad api url.
     */
    private static final String ETHERPAD_URL = System.getenv("ETHERPAD_URL");

    /**
     *Etherpad api key.
     */
    private static final String ETHERPAD_KEY = System.getenv("ETHERPAD_KEY");

    /**
     *Release data path prefix.
     */
    private static final String RELEASE_PATH = System.getenv("RELEASE_PATH");

    /**
     *Release data path prefix.
     */
    private static final String REPO_PATH = System.getenv("REPO_PATH");

    /**
     * Logger for logging messages in App class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private App() {
    }

    /**
     * The entry point of the application.
     *
     * @param args an array of command-line arguments
     */
    public static void main(String[] args) {
        try {
            PublicClient.CreateClientFormConfig(APPLICATION_PATH);
            PublicClient.makeIndex(INDEX_PREFIX + "_zh", MAPPING_PATH);
            PublicClient.makeIndex(INDEX_PREFIX + "_en", MAPPING_PATH);
            fileDate();
            sigData();
            etherpadData();
            releaseData();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            LOGGER.error(e.toString());
        }

        LOGGER.info("import end");
        System.exit(0);
    }

    /**
     * Imports and processes sig data.
     *
     * @throws Exception if an error occurs during the import the sign data.
     */
    public static void sigData() throws Exception {
        File indexFile = new File(TARGET_SIG);
        if (!indexFile.exists()) {
            LOGGER.info("%s folder does not exist%n", indexFile.getPath());
            return;
        }

        LOGGER.info("begin to update sig data");
        Collection<File> listFiles = new ArrayList<>();
        File[] subdirectories = indexFile.listFiles((dir, name) -> new File(dir, name).isDirectory());
        if (subdirectories != null) {
            for (File subdir : subdirectories) {
                Collection<File> filesInSubdirectory = FileUtils.listFiles(subdir, new String[]{"yaml"}, false);
                listFiles.addAll(filesInSubdirectory);
            }
        } else {
            LOGGER.info("sig data is null");
            return;
        }

        for (File paresFile : listFiles) {
            try {
                // sig information has two language
                List<Map<String, Object>> escapes = new ArrayList<>();
                escapes.add(Parse.parseSigYaml(paresFile, "zh"));
                escapes.add(Parse.parseSigYaml(paresFile, "en"));
                if (null != escapes) {
                    for (Map<String, Object> escape : escapes) {
                        PublicClient.insert(escape, INDEX_PREFIX + "_" + escape.get("lang"));
                    }
                } else {
                    LOGGER.info("parse null : " + paresFile.getPath());
                }
            } catch (Exception e) {
                LOGGER.error(paresFile.getPath());
                LOGGER.error("sig data imported error {}", e.getMessage());
            }
        }
        LOGGER.info("sig data imported end");
    }

    /**
     * Imports and processes etherpad data.
     *
     * @throws Exception if an error occurs during the import the etherpad data.
     */
    public static void etherpadData() {
        EPLiteClient client = new EPLiteClient(ETHERPAD_URL, ETHERPAD_KEY);

        Map result = client.listAllPads();
        List padIds = (List) result.get("padIDs");

        LOGGER.info("begin to update etherpad data");
        for (Object padId : padIds) {
            Map<String, Object> resMap = client.getText((String) padId);
            if (resMap.containsKey("text")) {
                try {
                    Map<String, Object> escape = Parse.parseEtherPad(resMap.get("text"),
                    padId.toString(), ETHERPAD_PATH);
                    if (null != escape) {
                        PublicClient.insert(escape, INDEX_PREFIX + "_" + escape.get("lang"));
                    }
                } catch (Exception e) {
                    LOGGER.error("etherpad data imported error {}", e.getMessage());
                }
            }
        }
        LOGGER.info("etherpad data imported end");
    }

    /**
     * Imports and processes release data.
     *
     * @throws Exception if an error occurs during the import the release data.
     */
    public static void releaseData() {
        File indexFile = new File(TARGET_RELEASE);
        if (!indexFile.exists()) {
            LOGGER.info("%s folder does not exist%n", indexFile.getPath());
            return;
        }

        LOGGER.info("begin to update release data");
        Collection<File> listFiles = FileUtils.listFiles(indexFile, new String[]{"ts"}, true);
        for (File paresFile : listFiles) {
            try {
                List<Map<String, Object>> escapes = Parse.parseReleaseDataOnGitee(paresFile);
                if (null != escapes) {
                    for (Map<String, Object> escape : escapes) {
                        PublicClient.insert(escape, INDEX_PREFIX + "_" + escape.get("lang"));
                    }
                } else {
                    LOGGER.info("parse null : " + paresFile.getPath());
                }
            } catch (Exception e) {
                LOGGER.error(paresFile.getPath());
                LOGGER.error("release data imported error on gitee: {}", e.getMessage());
            }
        }
        try {
            List<Map<String, Object>> escapes = Parse.parseReleaseDataOnMirror(RELEASE_PATH, REPO_PATH);
            if (null != escapes) {
                for (Map<String, Object> escape : escapes) {
                    PublicClient.insert(escape, INDEX_PREFIX + "_" + escape.get("lang"));
                }
            } else {
                LOGGER.info("parse null : " + RELEASE_PATH);
            }
        } catch (Exception e) {
            LOGGER.error("release data imported error on port: {}", e.getMessage());
        }

        File aggreFile = new File(TARGET_AGGREGATE);
        if (!indexFile.exists()) {
            LOGGER.info("%s folder does not exist%n", indexFile.getPath());
            return;
        }

        Collection<File> files = FileUtils.listFiles(aggreFile, new String[]{"ts"}, true);
        try {
            Map<String, Object> escape = Parse.parseAggreReleaseData(files);
            if (null != escape) {
                PublicClient.insert(escape, INDEX_PREFIX + "_" + escape.get("lang"));
            } else {
                LOGGER.info("parse null :" + aggreFile.getPath());
            }
        } catch (Exception e) {
            LOGGER.error("aggregate release data imported error: {}", e.getMessage());
        }
        LOGGER.info("all release data imported end");
    }

    /**
     * Imports and file release data.
     *
     * @throws Exception if an error occurs during the import the file data.
     */
    public static void fileDate() throws Exception {
        File indexFile = new File(TARGET);
        if (!indexFile.exists()) {
            LOGGER.info("%s folder does not exist%n", indexFile.getPath());
            return;
        }

        LOGGER.info("begin to update document");

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
                        LOGGER.info("parse null : " + paresFile.getPath());
                    }
                } catch (Exception e) {
                    LOGGER.error(paresFile.getPath());
                    LOGGER.error(e.getMessage());
                }
            }
        }

        List<Map<String, Object>> customizeEscape = Parse.customizeData();
        if (null == customizeEscape) {
            return;
        }
        inserDataList.addAll(customizeEscape);

        Map<String, List<Map<String, Object>>> typeMap = inserDataList.stream().collect(
            Collectors.groupingBy(a -> a.get("type") + "#" + a.get("lang")
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
                    LOGGER.error(e.getMessage());
                }
            }
        }
    }
}
