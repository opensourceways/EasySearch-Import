import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.yaml.snakeyaml.Yaml;

public class Parse {

    public static final String DOCS = "docs";

    public static final String BASEPATH = System.getenv("TARGET") + "/";

     public static Map<String, Object> parse(File file) throws Exception {
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
        jsonMap.put("version", "v0.0.0");
        jsonMap.put("group", path);
        String[] split = path.split("/");
        for (String s : split) {
            // 字母为v开头并且为数字
            if (s.matches("^\\d+(\\.\\d+)+$")) {
                jsonMap.put("version", s);
                jsonMap.put("group", path.replace("/"+s,""));
            }
        }

        String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        if (type.equals(DOCS)) {
            parseDocsType(jsonMap, fileContent, fileName);

            int pelen = lang.length() + type.length() + 2;

            String docsType = path.substring(pelen, path.indexOf("/", pelen));
            jsonMap.put("docsType", docsType);
        }

        return jsonMap;
     }



    public static void parseDocsType(Map<String, Object> jsonMap, String fileContent, String fileName) {

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
