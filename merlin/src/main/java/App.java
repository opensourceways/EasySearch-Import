import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

        System.out.println("import end");
        System.exit(0);
    }





    public static void fileDate() {
        File indexFile = new File(TARGET);
        if (!indexFile.exists()) {
            System.out.printf("%s folder does not exist%n", indexFile.getPath());
            return;
        }

        System.out.println("begin to update document");

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

        System.out.println("start delete expired document");
        PublicClient.deleteExpired(idSet, INDEX_PREFIX + "_*");

    }
    
}






