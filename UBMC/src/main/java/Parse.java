import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class Parse {

    public static final String DOCS = "docs";
    public static final String WEBSITE = "website";
    public static final String BASEPATH = System.getenv("TARGET") + "/";

    public static Map<String, Object> parse(File file) throws Exception {
        String originalPath = file.getPath();
        if (originalPath.contains("/target/website")) {
            return parWebsite(file);
        } else {
            return parDocs(file);
        }
    }

    public static Map<String, Object> parWebsite(File file) throws Exception {
        String originalName = file.getName();
        Map<String, Object> jsonMap = new HashMap<>();
        String path = "";
        if (originalName.contains("community-code-of-conduct")) {
            path = "/organizations";
        } else if (originalName.contains("cookie")) {
            path = "/cookies";
        } else if (originalName.contains("legal-notice")) {
            path = "/legal";
        } else if (originalName.contains("privacy-policy")) {
            path = "/privacy";
        }
        jsonMap.put("path", path);
        jsonMap.put("lang", "zh");
        // 默认版本v1.0
        jsonMap.put("version", "v1.0");
        jsonMap.put("group", path);
        jsonMap.put("type",WEBSITE);
        parseWebsiteContent(jsonMap, FileUtils.readFileToString(file, StandardCharsets.UTF_8), file.getName());
        return jsonMap;
    }

    public static Map<String, Object> parDocs(File file) throws Exception {
        String originalPath = file.getPath();
        String fileName = file.getName();
        String path = originalPath
                .replace("\\", "/")
                .replace(BASEPATH, "")
                .replace("\\\\", "/")
                .replace(".md", "")
                .replace(".html", "");

        String type = path.substring(0, path.indexOf("/"));

        String lang = path.substring(type.length() + 1, path.indexOf("/", type.length() + 1));

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("lang", lang);
        jsonMap.put("type", type);
        jsonMap.put("articleName", fileName);
        jsonMap.put("path", path);
        // 默认版本v0.0
        jsonMap.put("version", "v1.0");
        jsonMap.put("group", path);
        String[] split = path.split("/");
        for (String s : split) {
            // 字母为v开头并且为数字
            if (s.matches("^\\d+(\\.\\d+)+$")) {
                jsonMap.put("version", s);
                jsonMap.put("group", path.replace("/" + s, ""));
            }
        }

        String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        if (type.equals(DOCS)) {
            parseDocsContent(jsonMap, fileContent, fileName);

            int pelen = lang.length() + type.length() + 2;

            String docsType = path.substring(pelen, path.indexOf("/", pelen));
            jsonMap.put("docsType", docsType);
        }

        return jsonMap;
    }


    public static void parseDocsContent(Map<String, Object> jsonMap, String fileContent, String fileName) {

        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        Node document = parser.parse(fileContent);
        Document node = Jsoup.parse(renderer.render(document));
        String info = "";
        if (fileContent.contains("---")) {
            fileContent = fileContent.substring(fileContent.indexOf("---") + 3);
            if (fileContent.contains("---")) {
                info = fileContent.substring(0, fileContent.indexOf("---"));
                fileContent = fileContent.substring(fileContent.indexOf("---") + 3);
            }
        }
        String title = fileName;


        String date = "";
        String[] split = info.split("title:");

        if (split.length > 1) {
            title = split[split.length - 1].trim();
            String dateStr = split[0];
            date = dateStr.replace("date:", "").trim();

        }

        jsonMap.put("title", title);

        jsonMap.put("textContent", fileContent);
        jsonMap.put("date", date);

        Elements h1 = node.getElementsByTag("h1");
        Elements h2 = node.getElementsByTag("h2");
        Elements h3 = node.getElementsByTag("h3");
        Elements h4 = node.getElementsByTag("h4");
        Elements h5 = node.getElementsByTag("h5");
        Elements strong = node.getElementsByTag("strong");

        jsonMap.put("h1", h1.text());
        jsonMap.put("h2", h2.text());
        jsonMap.put("h3", h3.text());
        jsonMap.put("h4", h4.text());
        jsonMap.put("h5", h5.text());
        jsonMap.put("strong", strong.text());

    }


    public static void parseWebsiteContent(Map<String, Object> jsonMap, String fileContent, String fileName) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        Node document = parser.parse(fileContent);
        Document node = Jsoup.parse(renderer.render(document));

        if (node.getElementsByTag("h1").size() > 0) {
            jsonMap.put("title", node.getElementsByTag("h1").first().text());
        } else {
            jsonMap.put("title", fileName);
        }

        jsonMap.put("textContent", node.text());

        Elements h1 = node.getElementsByTag("h1");
        Elements h2 = node.getElementsByTag("h2");
        Elements h3 = node.getElementsByTag("h3");
        Elements h4 = node.getElementsByTag("h4");
        Elements h5 = node.getElementsByTag("h5");
        Elements strong = node.getElementsByTag("strong");

        jsonMap.put("h1", h1.text());
        jsonMap.put("h2", h2.text());
        jsonMap.put("h3", h3.text());
        jsonMap.put("h4", h4.text());
        jsonMap.put("h5", h5.text());
        jsonMap.put("strong", strong.text());

        if (jsonMap.containsKey("date")) {
            jsonMap.put("archives", jsonMap.get("date").toString().substring(0, 7));
        }
    }

}
