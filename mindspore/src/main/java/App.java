import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;

public class App {
    private static final String TARGET = System.getenv("TARGET");

    private static final String TARGET_PAPERS = System.getenv("TARGET") + "/papers";

    private static final String TARGET_CASES = System.getenv("TARGET") + "/cases";

    private static final String TARGET_COURSES = System.getenv("TARGET") + "/courses";

    private static final String APPLICATION_PATH = System.getenv("APPLICATION_PATH");

    private static final String MAPPING_PATH = System.getenv("MAPPING_PATH");

    private static final String INDEX_PREFIX = "mindspore_articles";


    public static void main(String[] args) {
        try {
            PublicClient.CreateClientFormConfig(APPLICATION_PATH);
            PublicClient.makeIndex(INDEX_PREFIX + "_zh", MAPPING_PATH);
            PublicClient.makeIndex(INDEX_PREFIX + "_en", MAPPING_PATH);
            portalData();
            fileDate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        System.out.println("import end");
        System.exit(0);
    }

    public static void portalData() throws Exception {
        File indexPaperFile = new File(TARGET_PAPERS);
        File indexCaseFile = new File(TARGET_CASES);
        File indexCourseFile = new File(TARGET_COURSES);
        if (!indexPaperFile.exists()) {
            System.out.printf("%s folder does not exist%n", indexPaperFile.getPath());
            return;
        }
        if (!indexCaseFile.exists()) {
            System.out.printf("%s folder does not exist%n", indexCaseFile.getPath());
            return;
        }
        if (!indexCourseFile.exists()) {
            System.out.printf("%s folder does not exist%n", indexCourseFile.getPath());
        }

        System.out.println("begin to update portal data,开始更新");
        Map<String, File> hashMap = Map.of("paper", indexPaperFile, "case", indexCaseFile, "course", indexCourseFile);
        hashMap.entrySet().forEach(entry -> {
            try {
                updatePortal(entry.getKey(), entry.getValue());
                System.out.println(entry.getKey() + " data  import success");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void updatePortal(String category, File indexFile) throws Exception {
        Collection<File> listFiles = FileUtils.listFiles(indexFile, new String[]{"json"}, true);

        for(File paresFile : listFiles) {
            try {
                List<Map<String, Object>> escapeList = null;
                switch (category) {
                    case "paper":
                        escapeList = Parse.parsePaperJson(paresFile);
                        break;
                    case "case":
                        escapeList = Parse.parseCaseJson(paresFile);
                        break;
                    case "course":
                        escapeList = Parse.parseCourseJson(paresFile);
                        break;
                    default:
                        break;
                }
                if (escapeList != null && !escapeList.isEmpty() ) {
                    for (Map<String, Object> escape : escapeList) {
                        PublicClient.insert(escape, INDEX_PREFIX + "_" + escape.get("lang"));
                    }
                } else {
                    System.out.println("parse null : " + paresFile.getPath());
                }
            } catch (Exception e) {
                System.out.println(paresFile.getPath());
                System.out.println(e.getMessage());
            }
        }
    }

    public static void fileDate() throws Exception {
        File indexFile = new File(TARGET);
        if (!indexFile.exists()) {
            System.out.printf("%s folder does not exist%n", indexFile.getPath());
            return;
        }

        System.out.println("begin to update document,开始更新");

        Set<String> idSet = new HashSet<>();

        Collection<File> listFiles = FileUtils.listFiles(indexFile, new String[]{"md", "html"}, true);

        for (File paresFile : listFiles) {
            if (!paresFile.getName().startsWith("_")) {
                try {
                    Map<String, Object> escape = Parse.parse(paresFile);
                    if (null != escape) {
                        PublicClient.insert(escape, INDEX_PREFIX + "_" + escape.get("lang"));
                        idSet.add((String) escape.get("path"));
                    } else {
                        System.out.println("parse null : " + paresFile.getPath());
                    }
                } catch (Exception e) {
                    System.out.println(paresFile.getPath());
                    System.out.println(e.getMessage());
                }
            }
        }

        List<Map<String, Object>> customizeEscape = Parse.customizeData();
        if (null == customizeEscape) {
            return;
        }

        for (Map<String, Object> lm : customizeEscape) {
            PublicClient.insert(lm, INDEX_PREFIX + "_" + lm.get("lang"));
            idSet.add((String) lm.get("path"));
        }

        System.out.println("start delete expired document");
        PublicClient.deleteExpired(idSet, INDEX_PREFIX + "_*");
    }


}
