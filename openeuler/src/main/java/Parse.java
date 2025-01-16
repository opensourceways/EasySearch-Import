
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
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

public final class Parse {

    /**
     *Target file route is named as bath path.
     */
    public static final String BASEPATH = System.getenv("TARGET") + "/";

    /**
     * The path segment for the blog.
     */
    public static final String BLOG = "blog";

    /**
     * The path segment for the blogs.
     */
    public static final String BLOGS = "blogs";

    /**
     * The path segment for the docs.
     */
    public static final String DOCS = "docs";

    /**
     * The path segment for the news.
     */
    public static final String NEWS = "news";

    /**
     * The path segment for the other.
     */
    public static final String OTHER = "other";

    /**
     * The path segment for the other.
     */
    public static final String MIGRATION = "migration";

    /**
     * The path segment for the showcase.
     */
    public static final String SHOWCASE = "showcase";

    /**
     * The path segment for the events.
     */
    public static final String EVENTS = "events";

    /**
     * The path segment for the gitee.
     */
    public static final String GITEE = "gitee";

    /**
     * The path segment for the userPractice.
     */
    public static final String USERPRACTICE = "userPractice";

    /**
     * The URL of the gitee repositories.
     */
    private static final String GITEE_REPOS_URL = System.getenv("GITEE_REPOS_URL");

    /**
     * The URL of the gitee readme.
     */
    private static final String GITEE_README_URL = System.getenv("GITEE_README_URL");

    /**
     * The String of the gitee projects.
     */
    private static final String GITEE_PROJS = System.getenv("GITEE_PROJS");

    /**
     * The String of the forum domain.
     */
    private static final String FORUM_DOMAIN = System.getenv("FORUM_DOMAIN");

    /**
     * The url of the service.
     */
    private static final String SERVICE_URL = System.getenv("SERVICE_URL");

    /**
     * The String of the white paper urls.
     */
    private static final String WHITEPAPER_URLS = System.getenv("WHITEPAPER_URLS");

    /**
     * The String of the packages db priorit.
     */
    private static final String PACKAGES_DB_PRIORIT = System.getenv("PACKAGES_DB_PRIORIT");

    /**
     * The String of the packages src.
     */
    private static final String PACKAGES_SRC = System.getenv("PACKAGES_SRC");

    /**
     * The detail of the packages src.
     */
    private static final String PACKAGES_SRC_DETAIL = System.getenv("PACKAGES_SRC_DETAIL");

    /**
     * The String of the packages src doc.
     */
    private static final String PACKAGES_SRC_DOC = System.getenv("PACKAGES_SRC_DOC");

    /**
     * Logger for logging messages in Parse class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Parse.class);

    /**
     * HashSet to store sig name.
     */
    private static HashSet<Object> sigSet = new HashSet<>();

    private Parse() {
    }

    /**
     * Parses the specified file and returns a map of parsed data.
     *
     * @param file the file to be parsed. Must not be null.
     * @return a map containing the parsed data.
     */
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
        if (type.equals(OTHER) || type.equals(MIGRATION)) {
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

    /**
     * Parses HTML content using the provided JSON map for context or configuration.
     *
     * @param jsonMap a JSON map that provides context for the HTML parsing process.
     * @param fileContent a string containing the HTML content to be parsed.
     */
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

    /**
     * Parses the given file content as HTML and stores it in a JSON map.
     *
     * @param jsonMap     a JSON map to store the extracted information.
     * @param fileContent a string containing the HTML content to be parsed.
     * @param fileName    the name of the file containing the HTML content.
     * @param path        a string containing the path to the file.
     * @param type        a string indicating the type of document.
     */
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

    /**
     * Parses un-docs-type using the provided JSON map for context or configuration.
     *
     * @param jsonMap a JSON map that provides context for the HTML parsing process.
     * @param fileContent a string containing the HTML content to be parsed.
     */
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

    /**
     * Customizes and returns a list of maps containing data.
     *
     * @return A list of maps containing customized data.
     */
    public static List<Map<String, Object>> customizeData() {
        LOGGER.info("begin update customizeData");
        List<Map<String, Object>> r = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(5);
        Thread formThread = new Thread(() -> {
            if (!setForum(r)) {
                LOGGER.error("Failed to add forum data");
            }
            LOGGER.info("setForum success");
            countDownLatch.countDown();
        });
        formThread.setUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.error("Uncaught exception in thread: " + thread.getName(), throwable);
            countDownLatch.countDown();
        });
        Thread serviceThread = new Thread(() -> {
            if (!setService(r)) {
                LOGGER.error("Failed to add service data");
            }
            LOGGER.info("setService success");
            countDownLatch.countDown();
        });
        serviceThread.setUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.error("Uncaught exception in thread: " + thread.getName(), throwable);
            countDownLatch.countDown();
        });
        Thread whitepaperThread = new Thread(() -> {
            if (!setWhitepaperData(r)) {
                LOGGER.error("Failed to add whitepaperData data");
            }
            LOGGER.info("setWhitepaperData success");
            countDownLatch.countDown();
        });
        whitepaperThread.setUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.error("Uncaught exception in thread: " + thread.getName(), throwable);
            countDownLatch.countDown();
        });
        Thread giteeDataThread = new Thread(() -> {
            if (!setGiteeData(r)) {
                LOGGER.error("Failed to add setGitee data");
            }
            LOGGER.info("setGiteeData success");
            countDownLatch.countDown();
        });
        giteeDataThread.setUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.error("Uncaught exception in thread: " + thread.getName(), throwable);
            countDownLatch.countDown();
        });
        Thread packageThread = new Thread(() -> {
            if (!setPackageManagementData(r)) {
                LOGGER.error("Failed to add setPackageManagementData data");
            }
            LOGGER.info("setPackageManagementData success");
            countDownLatch.countDown();
        });
        packageThread.setUncaughtExceptionHandler((thread, throwable) -> {
            LOGGER.error("Uncaught exception in thread: " + thread.getName(), throwable);
            countDownLatch.countDown();
        });
        formThread.start();
        serviceThread.start();
        whitepaperThread.start();
        giteeDataThread.start();
        packageThread.start();
        try {
            countDownLatch.await();
        } catch (Exception e) {
            LOGGER.error(e.toString());
        }
        LOGGER.info("geData success size:" + r.size());
        return r;
    }

    /**
     * Sets package management data.
     *
     * @param r A list of maps containing package management data.
     * @return {@code true} if the data is set successfully; {@code false} otherwise.
     */
    public static Boolean setPackageManagementData(List<Map<String, Object>> r) {
        LOGGER.info("PackagemanagementData: " + r.size());
        LOGGER.info("PACKAGES_DB_PRIORIT:" + PACKAGES_DB_PRIORIT);
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
                    LOGGER.error(e.toString());
                }
            }
        }
        LOGGER.info("packages导入完成,size:" + r.size());
        return true;
    }

    /**
     * Handles package data from a specified source URL.
     *
     * @param srcUrl       The URL from which to retrieve or process package data.
     * @param handleList   A list of maps containing handling instructions or data points.
     * @param databaseName The name of the database where the processed data should be stored.
     */
    public static void handPackagesData(String srcUrl, List<Map<String, Object>> handleList, String databaseName) {
        try {
            JSONArray resultArray = new JSONArray();
            Integer pageNum = 0;
            do {
                pageNum++;
                StringBuilder urlBuilder = new StringBuilder(srcUrl).append(
                    URLEncoder.encode(String.valueOf(pageNum), "utf-8"));
                Map<String, String> randomIpHeader = getRandomIpHeader();
                String httpResponse = getHttpResponse(urlBuilder.toString(), "GET", null, randomIpHeader);
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
                        if (split != null && split.length > 1) {
                            result.put("version", split[1].toUpperCase(
                                Locale.ROOT).replace("-", "_"));
                        }
                        result.put("path", String.valueOf(PACKAGES_SRC_DETAIL).replace(
                            "{pkg_name}", pkgName).replace("{database_name}", databaseName));
                        result.put("type", "packages");
                        setPackagesDescription(databaseName, pkgName, result);
                        handleList.add(result);
                    });
                }
            } while (resultArray.size() == 100);

        } catch (Exception e) {
            LOGGER.error(e.toString());
        }
    }

    /**
     * Sets the description information for a given package name in a specified
     * database.
     *
     * @param databaseName The name of the database to operate on.
     * @param pkgName      The package name for which to set the description.
     * @param result       A Map containing the description information.
     */
    public static void setPackagesDescription(String databaseName, String pkgName, Map<String, Object> result) {
        try {
            String srcDocUrl = String.valueOf(PACKAGES_SRC_DOC).replace(
                "{database_name}", databaseName).replace("{pkg_name}", pkgName);
            Map<String, String> randomIpHeader = getRandomIpHeader();
            String httpResponse = getHttpResponse(srcDocUrl, "GET", null, randomIpHeader);
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
            LOGGER.error(e.toString());
        }
    }

    /**
     * Parses an HTTP response string and returns the JSON object.
     *
     * @param httpResponse The string of an HTTP response to beparsed.
     * @return The parsed JSON object if it meets the success criteria.
     */
    public static JSONObject getPackagesSuccessRequestObj(String httpResponse) {
        if (httpResponse != null) {
            JSONObject srcObj = JSONObject.parseObject(httpResponse);
            if (srcObj != null && srcObj.containsKey("code") && "200".equals(
                srcObj.getString("code")) && srcObj.containsKey("resp")) {
                return srcObj;
            }
        }
        return null;
    }

    /**
     * Imports whitepaper data by fetching image lists from a set of URLs.
     *
     * @param r The list to which fetched whitepaper data will be appended.
     * @return Always returns {@code true} to indicate that the import process was attempted.
     */
    public static Boolean setWhitepaperData(List<Map<String, Object>> r) {
        LOGGER.info("開始導入白皮書,原始size:" + r.size());
        LOGGER.info("WHITEPAPER_URLS:" + WHITEPAPER_URLS);
        if (WHITEPAPER_URLS != null) {
            String[] urls = WHITEPAPER_URLS.split(",");
            for (int i = 0; i < urls.length; i++) {
                getImgList(urls[i], r);
            }
        }
        LOGGER.info("導入白皮書完成,size:" + r.size());
        return true;
    }

    /**
     * Extracts image list from a PDF file at the specified path.
     *
     * @param pdfPath The path to the PDF file from which images should be extracted.
     * @param r The list to which extracted image data should be appended.
     */
    private static void getImgList(String pdfPath, List<Map<String, Object>> r) {
        try {
            String title = URLDecoder.decode(pdfPath.split("whitepaper/")[1], "UTF-8");
            PDDocument pdfDoc = PDDocument.load(downloadFileByURL(pdfPath));
            int numPages = pdfDoc.getNumberOfPages();
            for (int i = 0; i < numPages; i++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true); // 排序
                stripper.setStartPage(i + 1); //要解析的首页
                stripper.setEndPage(i + 1); //要解析的结束页数
                stripper.setWordSeparator(" "); //单元格内容的分隔符号
                stripper.setLineSeparator("\n"); //行与行之间的分隔符号
                String text = stripper.getText(pdfDoc);
                HashMap<String, Object> result = new HashMap<>();
                result.put("type", "whitepaper");
                result.put("lang", "zh");
                result.put("path", new StringBuilder(pdfPath).append(
                    "#page=").append(i + 1).toString());
                result.put("textContent", text);
                result.put("secondaryTitle", title);
                r.add(result);
            }
            HashMap<String, Object> pdfResult = new HashMap<>();
            pdfResult.put("type", "whitepaper");
            pdfResult.put("lang", "zh");
            pdfResult.put("path", pdfPath);
            pdfResult.put("title", title);
            r.add(pdfResult);
            pdfDoc.close();
        } catch (Exception e) {
            LOGGER.error(e.toString());
        }

    }

    /**
     * Downloads the contents of a file from the specified URL.
     *
     * @param fileURL The URL of the file to download.
     * @return A byte array containing the contents of the downloaded file.
     */
    public static byte[] downloadFileByURL(String fileURL) throws IOException {
        URL url = new URL(fileURL);
        URLConnection conn = url.openConnection();
        try (InputStream in = conn.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            in.close();
            return bytes;
        }
    }

    /**
     * Fetches data from a forum API and processes it to a list.
     *
     * @param r The list of maps to which the processed forum data should be added.
     * @return True if the method completes successfully and all data is processed.
     */
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
                    result = readInput(connection.getInputStream());
                    if (!setData(result, r)) {
                        break;
                    }
                } else {
                    LOGGER.info(req + " - " + connection.getResponseCode());
                    return false;
                }
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Connection failed, error is: " + e.getMessage());
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
            path = String.format("%s/t/%s/%s.json?track_visit=true&forceLoad=true",
                   FORUM_DOMAIN, slug, id);
            try {
                connection = sendHTTP(path, "GET", null, null);
                if (connection.getResponseCode() == 200) {
                    result = readInput(connection.getInputStream());
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
                    LOGGER.info(path + " - " + connection.getResponseCode());
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            } finally {
                if (null != connection) {
                    connection.disconnect();
                }
            }
        }
        return true;
    }

    /**
     * Extracts topic information from the provided JSON data .
     *
     * @param r    A list for storing processing results.
     * @return Returns true if all topics are successfully processed.
     */
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
            LOGGER.error("load yaml failed, error is: " + e.getMessage());
            return false;
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
        return true;
    }

    /**
     * Fetches data from Gitee and processes it, storing the results in the provided
     * list.
     *
     * @param r A list of maps in which to store the processed data.
     * @return A Boolean value indicating whether the data fetching.
     */
    public static Boolean setGiteeData(List<Map<String, Object>> r) {
        LOGGER.info("开始更新gitee数据，初始size：" + r.size());
        if (GITEE_PROJS != null && !GITEE_PROJS.isEmpty()) {
            List<String> projectsList = Arrays.asList(new String(GITEE_PROJS).split(","));
            CountDownLatch countDownLatch = new CountDownLatch(projectsList.size());
            projectsList.parallelStream().forEach(p -> {
                String orgsUrl = String.valueOf(GITEE_REPOS_URL).replace("{org}", p);
                String readmeUrl = String.valueOf(GITEE_README_URL).replace("{org}", p);
                handGiteeData(orgsUrl, r, readmeUrl);
                LOGGER.info(p + "的gitee数据更新完成");
                countDownLatch.countDown();
            });
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                LOGGER.error(e.toString());
            }
            LOGGER.info("gitee数据更新完成，size：" + r.size());
            return true;
        }
        return false;
    }

    /**
     * Fetches data from Gitee repositories.
     *
     * @param orgsUrl The URL for fetching the organization's repositories from Gitee.
     * @param handleList A list of maps in which to store the processed data.
     * @param readmeUrl  The URL for fetching README files from Gitee.
     */
    public static void handGiteeData(String orgsUrl, List<Map<String, Object>> handleList, String readmeUrl) {
        try {
            JSONArray resultArray = new JSONArray();
            Integer page = 0;
            do {
                page++;
                StringBuilder urlBuilder = new StringBuilder(orgsUrl).append(
                    URLEncoder.encode(String.valueOf(page), "utf-8"));
                String httpResponse = getHttpResponse(urlBuilder.toString(), "GET", null, null);
                if (httpResponse != null) {
                    resultArray = JSONArray.parseArray(httpResponse);
                    if (resultArray != null && resultArray.size() > 0) {
                        getEsDataFromGitee(resultArray, handleList, readmeUrl);
                    }
                }
            } while (resultArray.size() == 20);

        } catch (Exception e) {
            LOGGER.error(e.toString());
        }
    }

    /**
     * Processes an array of JSON objects representing repositories from Gitee.
     *
     * @param resultArray A JSONArray of JSONObjects representing repositories from Gitee.
     * @param handleList  A list in which to store the processed data.
     * @param readmeUrl   A template URL for fetching README files from Gitee.
     */
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
                String readmeResponse = getHttpResponse(String.valueOf(readmeUrl).replace(
                    "{repo}", each.getString("path")), "GET", null, null);
                if (readmeResponse != null) {
                    JSONObject readmeJson = JSONObject.parseObject(readmeResponse);
                    textContentBuilder.append("   ").append(decodeBase64(readmeJson.getString("content")));
                }
                date.put("textContent", textContentBuilder.toString());
            } catch (Exception e) {
                LOGGER.error("gitee数据处理错误：" + e);
            }
        });
    }

    /**
     * Sends an HTTP request to a specified URL and returns the response body as a string.
     *
     * @param url    The URL to which the request is sent.
     * @param method The HTTP method to use.
     * @param param  The request parameters for GET requests.
     * @param header A map of HTTP headers to include in the request.
     * @return The response body as a string.
     */
    public static String getHttpResponse(String url, String method, String param, Map<String, String> header) {
        String response = null;
        HttpURLConnection connection = null;
        try {
            connection = sendHTTP(url, method, param, header);
            if (connection == null || (connection.getResponseCode() != HttpURLConnection.HTTP_OK
                && connection.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND)) {
                LOGGER.error("http请求失败：" + connection);
                return null;
            }
            response = readInput(connection.getInputStream());
        } catch (Exception e) {
            LOGGER.error("http请求失败：" + e.toString());
            LOGGER.error("http请求失败：" + e.getMessage());
            LOGGER.error("http请求失败參數：" + connection);
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
        return response;
    }

    /**
     * Decodes a Base64 encoded string and returns the decoded string in UTF-8 format.
     *
     * @param base64Str The Base64 encoded string to decode.
     * @return The decoded string in UTF-8 format.
     */
    public static String decodeBase64(String base64Str) {
        String decodeStr = "";
        byte[] base64Data = Base64.getDecoder().decode(base64Str);
        decodeStr = new String(base64Data, StandardCharsets.UTF_8);
        return decodeStr;
    }

    /**
     * Sends an HTTP request to a specified path with the given parameters.
     *
     * @param path   The URL path to which the request should be sent.
     * @param method The HTTP method to use (e.g., "GET", "POST").
     * @param param  The request parameters which will be appended to the URL.
     * @param header A map of headers to set on the request.
     * @return A object representing the connection to the URL.
     */
    private static HttpURLConnection sendHTTP(String path, String method,
    String param, Map<String, String> header) throws IOException {
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

    /**
     * Reads the contents and returns it as a String in UTF-8 format.
     *
     * @param is The input stream to read from.
     * @return A string containing the contents of the input stream.
     */
    private static String readInput(InputStream is) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sbf = new StringBuilder();
            String temp;
            while ((temp = br.readLine()) != null) {
                sbf.append(temp);
            }
            return sbf.toString();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    /**
     * Generates a random IPv4 address and populates a map with common HTTP headers.
     *
     * @return A map containing the specified HTTP headers with randomly IP address values.
     */
    public static Map<String, String> getRandomIpHeader() throws NoSuchAlgorithmException, NoSuchProviderException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        String ip = (random.nextInt(255) + 1) + "."
        + (random.nextInt(255) + 1) + "." + (random.nextInt(255) + 1) + "."
        + (random.nextInt(255) + 1);
        HashMap<String, String> header = new HashMap<>();
        header.put("X-Forwarded-For", ip);
        header.put("HTTP_X_FORWARDED_FOR", ip);
        header.put("HTTP_CLIENT_IP", ip);
        header.put("REMOTE_ADDR", ip);
        return header;
    }

    /**
     * Parses a YAML file to extract signature-related information.
     *
     * @param paresFile The YAML file to parse.
     * @param lang The language code to influence parsing behavior.
     * @return A map containing the parsed signature data.
     */
    public static Map<String, Object> parseSigYaml(File paresFile, String lang) throws Exception {
            Yaml yaml = new Yaml();
            Map<String, Object> resMap = new HashMap<>();

            try (InputStream inputStream = new FileInputStream(paresFile)) {
                Map<String, Object> dataMap = yaml.load(inputStream);
                resMap.put("title", dataMap.get("name"));
                sigSet.add(dataMap.get("name"));
                resMap.put("lang", lang);
                resMap.put("type", "sig");
                String path = lang + "/sig/" + dataMap.get("name");
                resMap.put("path", path);
                String textContent = "maintainers: ";
                if (dataMap.containsKey("maintainers") && dataMap.get("maintainers") instanceof List) {
                    List<?> maintainersList = (List<?>) dataMap.get("maintainers");
                    for (Object maintainerObj : maintainersList) {
                        if (maintainerObj instanceof Map) {
                            Map<String, Object> maintainerMap = (Map<String, Object>) maintainerObj;
                            textContent += maintainerMap.getOrDefault("name", "") + ",";
                            textContent += maintainerMap.getOrDefault("gitee_id", "") + ";";
                        }
                    }
                }
                textContent += "\n" + "committers: ";
                if (dataMap.containsKey("repositories") && dataMap.get("repositories") instanceof List) {
                    List<?> reposList = (List<?>) dataMap.get("repositories");
                    for (Object repoObj : reposList) {
                        if (repoObj instanceof Map) {
                            Map<String, Object> repoMap = (Map<String, Object>) repoObj;
                            if (repoMap.containsKey("committers") && repoMap.get("committers") instanceof List) {
                                List<?> committersList = (List<?>) repoMap.get("committers");
                                for (Object committerObj : committersList) {
                                    if (committerObj instanceof Map) {
                                        Map<String, Object> committerMap = (Map<String, Object>) committerObj;
                                        textContent += committerMap.getOrDefault("name", "") + ",";
                                        textContent += committerMap.getOrDefault("gitee_id", "") + ";";
                                    }
                                }
                            }
                        }
                    }
                }
                resMap.put("textContent", textContent);
            } catch (IOException e) {
                LOGGER.error("sig yaml parse error: {}", e.getMessage());
            }
            return resMap;
    }

    /**
     * Creates a map containing information related to an Etherpad document.
     *
     * @param text he text content to be associated with the Etherpad document.
     * @param padId The unique identifier for the Etherpad document.
     * @param etherpadPath The base path to the Etherpad server.
     * @return A map containing information related to the Etherpad document.
     */
    public static Map<String, Object> parseEtherPad(Object text, String padId, String etherpadPath) {
        Map<String, Object> resMap = new HashMap<>();
        for (Object prefix : sigSet) {
            if (padId.equals(prefix.toString() + "-meetings")) {
                resMap.put("textContent", text.toString());
                resMap.put("title", padId);
                resMap.put("path", etherpadPath + "p/" +  padId);
                resMap.put("lang", "zh");
                resMap.put("type", "etherpad");
                return resMap;
            }
        }
        return null;
    }

    /**
     * Parses release data from a file on Gitee and returns it as a list of maps.
     *
     * @param paresFile The file containing the release data to be parsed.
     * @return A list of maps where contains its attributes as key-value pairs.
     */
    public static List<Map<String, Object>> parseReleaseDataOnGitee(File paresFile) {
        List<Map<String, Object>> resList = new ArrayList<>();
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
            new FileInputStream(paresFile), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            LOGGER.error("文件读取异常: {}", e);
        }

        // 解析从gitee仓拿到的ts文件
        Map<String, String> map = Map.of("NAME", "title", "DESC", "textContent", "PUBLISH_DATE", "date");
        Map<String, Object> resMap = new HashMap<>();
        String langFlag = "";
        for (String line : lines) {
            line = line.trim();
            int preLength = line.indexOf(":");
            if (preLength == -1) {
                continue;
            }
            String keyString = line.substring(0, preLength);
            if ("zh".equals(keyString) || "en".equals(keyString)) {
                langFlag = keyString;
            }
            String valueString = line.substring(preLength + 1, line.length()).trim();
            int l = valueString.indexOf("'");
            int r = valueString.lastIndexOf("'");
            if (l < r && l != -1) {
                valueString = valueString.substring(l + 1, r);
            }
            if (resMap.containsKey(map.get(keyString))) {
                resMap.put("lang", langFlag);
                if ("download-commercial-release.ts".equals(paresFile.getName())) {
                    resMap.put("path", langFlag + "/download/commercial-release/?search=" + resMap.get("title"));
                } else if ("download.ts".equals(paresFile.getName())) {
                    resMap.put("path", langFlag + "/download/archive/detail?version=" +  resMap.get("title"));
                    resMap.put("version", resMap.get("title"));
                } else {
                    LOGGER.warn("file name changed");
                    return resList;
                }
                resMap.put("type", "release");
                resList.add(resMap);
                if ("download-commercial-release.ts".equals(paresFile.getName())) {
                    Map<String, Object> resCommercialMap = new HashMap<>(resMap);
                    resCommercialMap.put("type", "commercialRelease");
                    resList.add(resCommercialMap);
                }
                resMap = new HashMap<>();
                resMap.put(map.get(keyString), valueString);
            } else if (map.containsKey(keyString)) {
                if ("date".equals(map.get(keyString))) {
                    valueString = valueString.replaceAll("/", "-");
                    String[] parts = valueString.split("-");
                    if (parts.length - 1 == 1) {
                        valueString += "-01";
                    }
                }
                resMap.put(map.get(keyString), valueString);
            }
        }
        if (resMap.size() > 0) {
            resMap.put("lang", langFlag);
            if ("download-commercial-release.ts".equals(paresFile.getName())) {
                resMap.put("path", langFlag + "/download/commercial-release/?search=" + resMap.get("title"));
            } else if ("download.ts".equals(paresFile.getName())) {
                resMap.put("path", langFlag + "/download/archive/detail?version=" + resMap.get("title"));
                resMap.put("version", resMap.get("title"));
            }
            resMap.put("type", "release");
            resList.add(resMap);
            if ("download-commercial-release.ts".equals(paresFile.getName())) {
                Map<String, Object> resCommercialMap = new HashMap<>();
                resCommercialMap.put("type", "commercialRelease");
                resList.add(resCommercialMap);
            }
        }
        return resList;
    }

    /**
     * Parses release data from a release and returns it as a list of maps.
     *
     * @param releasePath The path of the release data on mirror to be parsed.
     * @param repoPath The path of the repo to be parsed.
     * @return A list of maps where contains its attributes as key-value pairs.
     */
    public static List<Map<String, Object>> parseReleaseDataOnMirror(String releasePath, String repoPath) {
        String countString = getHttpResponse(releasePath, "GET", null, null);
        List<Map<String, Object>> resList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode countNode = mapper.readTree(countString);
            if (countNode.get("RepoVersion") != null && countNode.get("RepoVersion").isArray()) {
                for (JsonNode versionNode : countNode.get("RepoVersion")) {
                    String version = versionNode.get("Version").asText();
                    String mirrorString = getHttpResponse(releasePath + version + "/", "GET", null, null);
                    resList.addAll(praseCommunityReleaseJson(mirrorString, repoPath, version));
                }
            } else {
                LOGGER.warn("RepoVersion field is missing or not an array.");
            }

        } catch (Exception e) {
            LOGGER.error("Error processing HTTP response or JSON parsing: {}", e);
        }
        return resList;
    }

    /**
     * Parses community release data as a list of maps.
     *
     * @param jsonString The json string of the release data to be parsed.
     * @param repoPath The path of the repo to be parsed.
     * @param version The version of the repo to insert to map.
     * @return A list of maps where contains its attributes as key-value pairs.
     */
    public static List<Map<String, Object>> praseCommunityReleaseJson(
        String jsonString, String repoPath, String version) {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> resList = new ArrayList<>();
        try {
            JsonNode mirrorNode = mapper.readTree(jsonString);
            if (mirrorNode.get("FileTree") != null && mirrorNode.get("FileTree").isArray()) {
                for (JsonNode fileTreeNode : mirrorNode.get("FileTree")) {
                    String arch = fileTreeNode.get("Arch").asText();
                    String scenario = fileTreeNode.get("Scenario").asText();
                    for (JsonNode treeNode : fileTreeNode.get("Tree")) {
                        Map<String, Object> resMapZh = new HashMap<>();
                        resMapZh.put("arch", arch);
                        resMapZh.put("scenario", scenario);
                        resMapZh.put("title", treeNode.get("Name").asText());
                        resMapZh.put("path", repoPath + treeNode.get("Path").asText());
                        resMapZh.put("textContent", "");
                        resMapZh.put("lang", "zh");
                        resMapZh.put("type", "communityRelease");
                        resMapZh.put("version", version);
                        resList.add(resMapZh);
                        Map<String, Object> resMapEn = new HashMap<>(resMapZh);
                        resMapEn.put("lang", "en");
                        resList.add(resMapEn);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("JSON parsing: {}", e);
        }
        return resList;
    }

    /**
     * Parses aggregate release data as a map.
     *
     * @param files The ts file of the aggregate release data to be parsed.
     * @return A list of maps where contains its attributes as key-value pairs.
     */
    public static Map<String, Object> parseAggreReleaseData(Collection<File> files) {
        Map<String, Object> resMap = new HashMap<>();
        resMap.put("title", "下载中心");
        resMap.put("lang", "zh");
        resMap.put("path", "zh/download/");
        resMap.put("type", "aggre");
        String textContent = "Offline Standard ISO, Offline Everything ISO, Network Install ISO";
        for (File file : files) {
            HashSet<String> tags = null;
            if ("download-zh.ts".equals(file.getName())) {
                tags = Stream.of("community", "communityIntro", "SERVER_IMAGE", "CLOUD_IMAGE",
                "EDGE_IMAGE", "EMBEDDEN_IMAGE", "INSTALL_GUIDENCE", "DETAIL1", "SCENARIOS", "ARCHITECTUREList")
                .collect(Collectors.toCollection(HashSet::new));
            } else {
                tags = Stream.of("title", "intro", "label")
                .collect(Collectors.toCollection(HashSet::new));
            }
            textContent = textContent + parseTsAsTxt(file, tags);
        }
        resMap.put("textContent", textContent);
        return resMap;
    }

    /**
     * Parses ts data in file as a string by hashset.
     *
     * @param file The ts file of the aggregate release data to be parsed.
     * @param hashSet The hashset to be taged.
     * @return A string as textContext.
     */
    public static String parseTsAsTxt(File file, HashSet<String> hashSet) {
        String resString = "";
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            LOGGER.error("文件读取异常: {}", e);
        }

        for (String line : lines) {
            int index = line.indexOf(":");
            if (index == -1) {
                continue;
            }
            String key = line.substring(0, index).trim();
            line = line.substring(index + 1).trim();
            if (hashSet.contains(key)) {
                resString = resString + line;
            }
        }
        return resString;
    }
}
