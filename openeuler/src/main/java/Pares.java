import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pares {
    public static final String BASEPATH = System.getenv("TARGET") + "/";
    public static final String BLOG = "blog";
    public static final String BLOGS = "blogs";
    public static final String DOCS = "docs";
    public static final String NEWS = "news";
    public static final String OTHER = "other";
    public static final String MIGRATION = "migration";
    public static final String SHOWCASE = "showcase";
    public static final String EVENTS = "events";
    public static final String GITEE = "gitee";
    public static final String USERPRACTICE = "userPractice";
    private static final String GITEE_REPOS_URL = System.getenv("GITEE_REPOS_URL");
    private static final String GITEE_README_URL = System.getenv("GITEE_README_URL");
    private static final String GITEE_PROJS = System.getenv("GITEE_PROJS");
    private static final String FORUM_DOMAIN = System.getenv("FORUM_DOMAIN");
    private static final String SERVICE_URL = System.getenv("SERVICE_URL");
    private static final String WHITEPAPER_URLS = System.getenv("WHITEPAPER_URLS");
    private static final String PACKAGES_DB_PRIORIT = System.getenv("PACKAGES_DB_PRIORIT");
    private static final String PACKAGES_SRC = System.getenv("PACKAGES_SRC");
    private static final String PACKAGES_SRC_DETAIL = System.getenv("PACKAGES_SRC_DETAIL");
    private static final String PACKAGES_SRC_DOC = System.getenv("PACKAGES_SRC_DOC");
    private static final Logger logger = LoggerFactory.getLogger(Pares.class);

    public static Map<String, Object> parse(File file) throws Exception {
        String originalPath = file.getPath();
        String fileName = file.getName();
        String path = originalPath
                .replace("\\", "/")
                .replace(BASEPATH, "")
                .replace("\\\\", "/")
                .replace(".md", "")
                .replace(".html", "");

        String lang = path.substring(0, path.indexOf("/"));

        String type = path.substring(lang.length() + 1, path.indexOf("/", lang.length() + 1));
        if (!DOCS.equals(type)
                && !BLOG.equals(type)
                && !BLOGS.equals(type)
                && !NEWS.equals(type)
                && !SHOWCASE.equals(type)
                && !MIGRATION.equals(type)
                && !EVENTS.equals(type)
                && !USERPRACTICE.equals(type)) {
            type = OTHER;
            if (!fileName.equals("index.html")) {
                return null;
            }

        }
        if (type.equals(OTHER) || type.equals(SHOWCASE) || type.equals(MIGRATION)) {
            path = path.substring(0, path.length() - 5);
        }
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("lang", lang);
        jsonMap.put("type", type);
        jsonMap.put("articleName", fileName);
        jsonMap.put("path", path);

        String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        if (fileName.endsWith(".html")) {
            parseHtml(jsonMap, fileContent);
        } else {
            if (DOCS.equals(type)) {
                parseDocsType(jsonMap, fileContent, fileName, path, type);
            } else {
                parseUnDocsType(jsonMap, fileContent);
            }
        }

        if (jsonMap.get("title") == "" && jsonMap.get("textContent") == "") {
            return null;
        }
        return jsonMap;
    }

    public static void parseHtml(Map<String, Object> jsonMap, String fileContent) {
        Document node = Jsoup.parse(fileContent);
        Elements titles = node.getElementsByTag("title");
        if (titles.size() > 0) {
            jsonMap.put("title", titles.first().text());
        }

        Elements elements = node.getElementsByTag("main");
        if (elements.size() > 0) {
            Element mainNode = elements.first();
            jsonMap.put("textContent", mainNode.text());
        }
    }

    public static void parseDocsType(Map<String, Object> jsonMap, String fileContent, String fileName, String path,
                                     String type) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        Node document = parser.parse(fileContent);
        Document node = Jsoup.parse(renderer.render(document));

        if (node.getElementsByTag("h1").size() > 0) {
            jsonMap.put("title", node.getElementsByTag("h1").first().text());
        } else {
            jsonMap.put("title", fileName);
        }

        if (node.getElementsByTag("a").size() > 0 && node.getElementsByTag("ul").size() > 0) {
            Element a = node.getElementsByTag("a").first();
            if (a.attr("href").startsWith("#")) {
                node.getElementsByTag("ul").first().remove();
            }
        }
        jsonMap.put("textContent", node.text());

        String version = path.replaceFirst(jsonMap.get("lang") + "/" + type + "/", "");
        version = version.substring(0, version.indexOf("/"));

        jsonMap.put("version", version);
    }

    public static void parseUnDocsType(Map<String, Object> jsonMap, String fileContent) {
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String r = "";
        if (fileContent.contains("---")) {
            fileContent = fileContent.substring(fileContent.indexOf("---") + 3);
            if (fileContent.contains("---")) {
                r = fileContent.substring(0, fileContent.indexOf("---"));
                fileContent = fileContent.substring(fileContent.indexOf("---") + 3);
            }
        }

        Node document = parser.parse(fileContent);
        Document node = Jsoup.parse(renderer.render(document));
        jsonMap.put("textContent", node.text());

        Yaml yaml = new Yaml();
        Map<String, Object> ret = yaml.load(r);
        String key = "";
        Object value = "";
        for (Map.Entry<String, Object> entry : ret.entrySet()) {
            key = entry.getKey().toLowerCase(Locale.ROOT);
            value = entry.getValue();
            if (key.equals("date")) {
                // 需要处理日期不标准导致的存入ES失败的问题。
                String dateString = "";
                if (value.getClass().getSimpleName().equals("Date")) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    dateString = format.format(value);
                } else {
                    dateString = value.toString();
                }
                Pattern pattern = Pattern.compile("\\D"); // 匹配所有非数字
                Matcher matcher = pattern.matcher(dateString);
                dateString = matcher.replaceAll("-");
                if (dateString.length() < 10) {
                    StringBuilder stringBuilder = new StringBuilder(dateString);
                    if (stringBuilder.charAt(7) != '-') {
                        stringBuilder.insert(5, "0");
                    }
                    if (stringBuilder.length() < 10) {
                        stringBuilder.insert(8, "0");
                    }
                    dateString = stringBuilder.toString();
                }
                value = dateString;
            }
            if (key.equals("author") && value instanceof String) {
                value = new String[]{value.toString()};
            }
            if (key.equals("head")) {
                continue;
            }
            jsonMap.put(key, value);
        }
        if (jsonMap.containsKey("date")) {
            jsonMap.put("archives", jsonMap.get("date").toString().substring(0, 7));
        }
    }

    public static List<Map<String, Object>> customizeData() {
        logger.info("begin update customizeData");
        List<Map<String, Object>> r = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(4);
        Thread formThread = new Thread(() -> {
            if (!setForum(r)) {
                logger.error("Failed to add forum data");
            }
            logger.info("setForum success");
            countDownLatch.countDown();
        });
        Thread serviceThread = new Thread(() -> {
            if (!setService(r)) {
                logger.error("Failed to add service data");
            }
            logger.info("setService success");
            countDownLatch.countDown();
        });
        Thread whitepaperThread = new Thread(() -> {
            if (!setWhitepaperData(r)) {
                logger.error("Failed to add whitepaperData data");
            }
            logger.info("setWhitepaperData success");
            countDownLatch.countDown();
        });
        Thread giteeDataThread = new Thread(() -> {
            if (!setGiteeData(r)) {
                logger.error("Failed to add setGitee data");
            }
            logger.info("setGiteeData success");
            countDownLatch.countDown();
        });
        formThread.start();
        serviceThread.start();
        whitepaperThread.start();
        giteeDataThread.start();
        try {
            countDownLatch.wait();
        } catch (InterruptedException e) {
            logger.error(e.toString());
        }
        logger.info("geData success size:"+r.size());
        return r;
    }

    public static Boolean setPackageManagementData(List<Map<String, Object>> r) {
        logger.info("PackagemanagementData: " + r.size());
        logger.info("PACKAGES_DB_PRIORIT:" + PACKAGES_DB_PRIORIT);
        if (PACKAGES_DB_PRIORIT != null) {
            String httpResponse = getHttpResponse(PACKAGES_DB_PRIORIT, "GET", null, null);
            JSONObject dbPriorityObj = getPackagesSuccessRequestObj(httpResponse);
            if (dbPriorityObj != null) {
                JSONArray resp = dbPriorityObj.getJSONArray("resp");
                CountDownLatch countDownLatch = new CountDownLatch(resp.size());
                resp.parallelStream().forEach(m -> {
                    String srcUrl = String.valueOf(PACKAGES_SRC).replace("{database_name}", String.valueOf(m));
                    handPackagesData(srcUrl, r, String.valueOf(m));
                    countDownLatch.countDown();
                });
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    logger.error(e.toString());
                }
            }
        }
        logger.info("packages导入完成,size:" + r.size());
        return true;
    }

    public static void handPackagesData(String srcUrl, List<Map<String, Object>> handleList, String databaseName) {
        try {
            JSONArray resultArray = new JSONArray();
            Integer pageNum = 0;
            do {
                pageNum++;
                StringBuilder urlBuilder = new StringBuilder(srcUrl).append(URLEncoder.encode(String.valueOf(pageNum), "utf-8"));
                String httpResponse = getHttpResponse(urlBuilder.toString(), "GET", null, null);
                JSONObject srcObj = getPackagesSuccessRequestObj(httpResponse);
                if (srcObj != null) {
                    resultArray = srcObj.getJSONArray("resp");
                    resultArray.stream().forEach(r -> {
                        JSONObject eachResp = (JSONObject) r;
                        Map<String, Object> result = new HashMap<>();
                        String pkgName = eachResp.getString("pkg_name");
                        result.put("title", pkgName);
                        result.put("lang", "zh");
                        result.put("version", databaseName);
                        String[] split = databaseName.split("openeuler-");
                        if (split != null && split.length > 1)
                            result.put("version", split[1].toUpperCase(Locale.ROOT).replace("-", "_"));
                        result.put("path", String.valueOf(PACKAGES_SRC_DETAIL).replace("{pkg_name}", pkgName).replace("{database_name}", databaseName));
                        result.put("type", "packages");
                        setPackagesDescription(databaseName, pkgName, result);
                        handleList.add(result);
                    });
                }
            } while (resultArray.size() == 100);

        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    public static void setPackagesDescription(String databaseName, String pkgName, Map<String, Object> result) {
        try {
            String srcDocUrl = String.valueOf(PACKAGES_SRC_DOC).replace("{database_name}", databaseName).replace("{pkg_name}", pkgName);
            String httpResponse = getHttpResponse(srcDocUrl, "GET", null, null);
            JSONObject srcObj = getPackagesSuccessRequestObj(httpResponse);
            if (srcObj != null) {
                JSONObject resp = srcObj.getJSONObject("resp");
                JSONArray detailArray = resp.getJSONArray(databaseName);
                if (detailArray != null && detailArray.size() > 0) {
                    JSONObject detail = detailArray.getJSONObject(0);
                    result.put("textContent", detail.getString("description"));
                    result.put("secondaryTitle", detail.getString("summary"));
                }
            }
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    public static JSONObject getPackagesSuccessRequestObj(String httpResponse) {
        if (httpResponse != null) {
            JSONObject srcObj = JSONObject.parseObject(httpResponse);
            if (srcObj != null && srcObj.containsKey("code") && "200".equals(srcObj.getString("code")) && srcObj.containsKey("resp")) {
                return srcObj;
            }
        }
        return null;
    }

    public static Boolean setWhitepaperData(List<Map<String, Object>> r) {
        logger.info("開始導入白皮書,原始size:" + r.size());
        logger.info("WHITEPAPER_URLS:" + WHITEPAPER_URLS);
        if (WHITEPAPER_URLS != null) {
            String[] urls = WHITEPAPER_URLS.split(",");
            for (int i = 0; i < urls.length; i++) {
                getImgList(urls[i], r);
            }
        }
        logger.info("導入白皮書完成,size:" + r.size());
        return true;
    }

    private static void getImgList(String pdfPath, List<Map<String, Object>> r) {
        try {
            PDDocument pdfDoc = PDDocument.load(downloadFileByURL(pdfPath));
            int numPages = pdfDoc.getNumberOfPages();
            for (int i = 0; i < numPages; i++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);// 排序
                stripper.setStartPage(i + 1);//要解析的首页
                stripper.setEndPage(i + 2);//要解析的结束页数
                stripper.setWordSeparator("##");//单元格内容的分隔符号
                stripper.setLineSeparator("\n");//行与行之间的分隔符号
                String text = stripper.getText(pdfDoc);
                HashMap<String, Object> result = new HashMap<>();
                result.put("type", "whitepaper");
                result.put("lang", "zh");
                result.put("path", new StringBuilder(pdfPath).append("#page=").append(i + 1).toString());
                result.put("textContent", text);
                r.add(result);
            }
            pdfDoc.close();
        } catch (Exception e) {
            logger.error(e.toString());
        }

    }

    public static byte[] downloadFileByURL(String fileURL) throws IOException {
        URL url = new URL(fileURL);
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        byte[] bytes = in.readAllBytes();
        in.close();
        return bytes;

    }

    private static boolean setForum(List<Map<String, Object>> r) {
        String path = FORUM_DOMAIN + "/latest.json?no_definitions=true&page=";

        String req = "";
        HttpURLConnection connection = null;
        String result;  // 返回结果字符串

        for (int i = 0; ; i++) {
            req = path + i;
            try {
                connection = sendHTTP(req, "GET", null, null);
                TimeUnit.SECONDS.sleep(30);
                if (connection.getResponseCode() == 200) {
                    result = ReadInput(connection.getInputStream());
                    if (!setData(result, r)) {
                        break;
                    }
                } else {
                    logger.info(req + " - " + connection.getResponseCode());
                    return false;
                }
            } catch (IOException | InterruptedException e) {
                logger.error("Connection failed, error is: " + e.getMessage());
                return false;
            } finally {
                if (null != connection) {
                    connection.disconnect();
                }
            }
        }
        return true;
    }

    private static boolean setData(String data, List<Map<String, Object>> r) {
        JSONObject post = JSON.parseObject(data);
        JSONObject topicList = post.getJSONObject("topic_list");
        JSONArray jsonArray = topicList.getJSONArray("topics");
        if (jsonArray.size() <= 0) {
            return false;
        }
        String path = "";
        HttpURLConnection connection = null;
        String result; // 返回结果字符串
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject topic = jsonArray.getJSONObject(i);
            String id = topic.getString("id");
            String slug = topic.getString("slug");
            path = String.format("%s/t/%s/%s.json?track_visit=true&forceLoad=true", FORUM_DOMAIN, slug, id);
            try {
                connection = sendHTTP(path, "GET", null, null);
                if (connection.getResponseCode() == 200) {
                    result = ReadInput(connection.getInputStream());
                    JSONObject st = JSON.parseObject(result);
                    JSONObject postStream = st.getJSONObject("post_stream");
                    JSONArray posts = postStream.getJSONArray("posts");
                    JSONObject pt = posts.getJSONObject(0);

                    String cooked = pt.getString("cooked");
                    Parser parser = Parser.builder().build();
                    HtmlRenderer renderer = HtmlRenderer.builder().build();
                    Node document = parser.parse(cooked);
                    Document node = Jsoup.parse(renderer.render(document));
                    Map<String, Object> jsonMap = new HashMap<>();
                    jsonMap.put("textContent", node.text());
                    jsonMap.put("title", st.getString("title"));
                    jsonMap.put("eulerForumId", pt.get("id"));
                    jsonMap.put("type", "forum");
                    jsonMap.put("lang", "zh");
                    jsonMap.put("path", "/t/" + slug + "/" + id);

                    r.add(jsonMap);
                } else {
                    logger.info(path + " - " + connection.getResponseCode());
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            } finally {
                if (null != connection) {
                    connection.disconnect();
                }
            }
        }
        return true;
    }

    public static boolean setService(List<Map<String, Object>> r) {
        HttpURLConnection connection = null;
        try {
            connection = sendHTTP(SERVICE_URL, "GET", null, null);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }
            Yaml yaml = new Yaml();
            List<Map<String, Object>> data = yaml.load(connection.getInputStream());
            for (Map<String, Object> datum : data) {
                Map<String, Object> jsonMap = new HashMap<>();
                jsonMap.put("title", datum.get("name"));
                jsonMap.put("secondaryTitle", datum.get("introduction"));
                jsonMap.put("lang", datum.get("lang"));
                jsonMap.put("path", datum.get("path"));
                jsonMap.put("type", "service");

                StringBuilder textContentBuilder = new StringBuilder();
                if (datum.get("introduction") != null) {
                    textContentBuilder.append(datum.get("introduction")).append(" ");
                }
                if (datum.get("description") != null) {
                    textContentBuilder.append(datum.get("description"));
                }
                String textContent = textContentBuilder.toString();

                jsonMap.put("textContent", textContent);
                r.add(jsonMap);
            }

        } catch (IOException e) {
            logger.error("load yaml failed, error is: " + e.getMessage());
            return false;
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
        return true;
    }

    public static Boolean setGiteeData(List<Map<String, Object>> r) {
        logger.info("开始更新gitee数据，初始size：" + r.size());
        if (GITEE_PROJS != null && !GITEE_PROJS.isEmpty()) {
            List<String> projectsList = Arrays.asList(new String(GITEE_PROJS).split(","));
            CountDownLatch countDownLatch = new CountDownLatch(projectsList.size());
            projectsList.parallelStream().forEach(p -> {
                String orgsUrl = String.valueOf(GITEE_REPOS_URL).replace("{org}", p);
                String readmeUrl = String.valueOf(GITEE_README_URL).replace("{org}", p);
                handGiteeData(orgsUrl, r, readmeUrl);
                logger.info(p + "的gitee数据更新完成");
                countDownLatch.countDown();
            });
            try {
                countDownLatch.wait();
            } catch (InterruptedException e) {
                logger.error(e.toString());
            }
            logger.info("gitee数据更新完成，size：" + r.size());
            return true;
        }
        return false;
    }


    public static void handGiteeData(String orgsUrl, List<Map<String, Object>> handleList, String readmeUrl) {
        try {
            JSONArray resultArray = new JSONArray();
            Integer page = 0;
            do {
                page++;
                StringBuilder urlBuilder = new StringBuilder(orgsUrl).append(URLEncoder.encode(String.valueOf(page), "utf-8"));
                String httpResponse = getHttpResponse(urlBuilder.toString(), "GET", null, null);
                if (httpResponse != null) {
                    resultArray = JSONArray.parseArray(httpResponse);
                    if (resultArray != null && resultArray.size() > 0) {
                        getEsDataFromGitee(resultArray, handleList, readmeUrl);
                    }
                }
            } while (resultArray.size() == 20);

        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    private static void getEsDataFromGitee(JSONArray resultArray, List handleList, String readmeUrl) {
        resultArray.stream().forEach(a -> {
            try {
                JSONObject each = (JSONObject) a;
                Map<String, Object> date = new HashMap<>();
                String name = each.getString("name");
                date.put("title", name);
                date.put("path", each.getString("html_url"));
                date.put("type", GITEE);
                date.put("lang", "zh");
                handleList.add(date);
                String description = each.getString("description");
                StringBuilder textContentBuilder = new StringBuilder();
                if (description != null && !description.isEmpty() && !"null".equals(description)) {
                    textContentBuilder.append(description);
                }
                String readmeResponse = getHttpResponse(String.valueOf(readmeUrl).replace("{repo}", each.getString("path")), "GET", null, null);
                if (readmeResponse != null) {
                    JSONObject readmeJson = JSONObject.parseObject(readmeResponse);
                    textContentBuilder.append("   ").append(decodeBase64(readmeJson.getString("content")));
                }
                date.put("textContent", textContentBuilder.toString());
            } catch (Exception e) {
                logger.error("gitee数据处理错误：" + e);
            }
        });
    }

    public static String getHttpResponse(String url, String method, String param, Map<String, String> header) {
        String response = null;
        HttpURLConnection connection = null;
        try {
            connection = sendHTTP(url, method, param, header);
            if (connection == null || (connection.getResponseCode() != HttpURLConnection.HTTP_OK && connection.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND)) {
                logger.error("http请求失败：" + connection);
                return null;
            }
            response = ReadInput(connection.getInputStream());
        } catch (Exception e) {
            logger.error("http请求失败：" + e.toString());
            logger.error("http请求失败：" + e.getMessage());
            logger.error("http请求失败參數：" + connection);
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
        return response;
    }

    public static String decodeBase64(String base64Str) {
        String decodeStr = "";
        byte[] base64Data = Base64.getDecoder().decode(base64Str);
        decodeStr = new String(base64Data, StandardCharsets.UTF_8);
        return decodeStr;
    }

    private static HttpURLConnection sendHTTP(String path, String method, String param, Map<String, String> header) throws IOException {
        URL url = new URL(path);
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);
        if (header != null && !header.isEmpty()) {
            for (Map.Entry<String, String> eachEntry : header.entrySet()) {
                connection.setRequestProperty(eachEntry.getKey(), eachEntry.getValue());
            }
            connection.setDoOutput(true);
        }
        connection.connect();
        if (param != null) {
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = param.getBytes("utf-8");
                os.write(input);
            }
        }
        return connection;
    }

    private static String ReadInput(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuffer sbf = new StringBuffer();
        String temp = null;
        while ((temp = br.readLine()) != null) {
            sbf.append(temp);
        }
        try {
            br.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return sbf.toString();
    }
}
