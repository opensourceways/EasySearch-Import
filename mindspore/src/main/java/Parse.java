import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.FileUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Parse {

    private static final String BASEPATH = System.getenv("TARGET") + "/";
    private static final String LANG_EN = "/en/";
    private static final String LANG_ZH = "/zh-CN/";
    private static final String MINDSPORE_OFFICIAL = System.getenv("MINDSPORE_OFFICIAL");

    private static final HashMap<String, String> COMPONENTS_MAP = new HashMap<>() {{
        put("docs", "MindSpore");
        put("lite", "MindSpore Lite");
        put("mindinsight", "MindSpore Insight");
        put("mindarmour", "MindSpore Armour");
        put("serving", "MindSpore Serving");
        put("federated", "MindSpore Federated");
        put("golden_stick", "MindSpore Golden Stick");
        put("xai", "MindSpore XAI");
        put("devtoolkit", "MindSpore Dev Toolkit");
        put("recommender", "MindSpore Recommender");
        put("graphlearning", "MindSpore Graph Learning");
        put("reinforcement", "MindSpore Reinforcement");
        put("probability", "MindSpore Probability");
        put("mindpandas", "MindSpore Pandas");
        put("hub", "MindSpore Hub");
        put("mindelec", "MindSpore Elec");
        put("mindsponge", "MindSpore SPONGE");
        put("mindflow", "MindSpore Flow");
        put("mindquantum", "MindSpore Quantum");
        put("sciai", "MindSpore SciAI");
        put("mindearth", "MindSpore Earth");
        put("mindformers","MindSpore Transformers");
    }};

    public static Map<String, Object> parse(File file) throws Exception {
        String originalPath = file.getPath();
        String fileName = file.getName();

        String path = originalPath
                .replace("\\", "/")
                .replace(BASEPATH, "")
                .replace("\\\\", "/");

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("path", path);
        jsonMap.put("articleName", fileName);

        String c = path.substring(0, path.indexOf("/"));

        if (COMPONENTS_MAP.containsKey(c)) {
            jsonMap.put("components", COMPONENTS_MAP.get(c));
        }

        if (path.contains(LANG_EN)) {
            jsonMap.put("lang", "en");
            String v = path.substring(path.indexOf(LANG_EN) + LANG_EN.length());
            String version = v.substring(0, v.indexOf("/"));
            jsonMap.put("version", version);
        } else if (path.contains(LANG_ZH)) {
            jsonMap.put("lang", "zh");
            String v = path.substring(path.indexOf(LANG_ZH) + LANG_ZH.length());
            String version = v.substring(0, v.indexOf("/"));
            jsonMap.put("version", version);
        } else {
            if (!path.startsWith("install/")) {
                jsonMap.put("lang", "zh");
            }
        }

        String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

        if (path.contains("/api/") || path.contains("/api_python/")) {
            jsonMap.put("type", "api");
            if (!parseHtml(jsonMap, fileContent)) {
                return null;
            }
        } else if (path.startsWith("tutorials/")) {
            jsonMap.put("type", "tutorials");
            jsonMap.put("components", "MindSpore");
            if (!parseHtml(jsonMap, fileContent)) {
                return null;
            }
        } else if (path.startsWith("install/")) {
            jsonMap.put("type", "install");
            jsonMap.put("components", "MindSpore");
            if (!parseInstall(jsonMap, fileContent)) {
                return null;
            }
        } else {
            jsonMap.put("type", "docs");
            if (!parseHtml(jsonMap, fileContent)) {
                return null;
            }
        }
        return jsonMap;
    }
    public static List<Map<String, Object>> parsePaperJson(File jsonFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> ansList = new ArrayList<>();
        try {
            JsonNode rootNode = objectMapper.readTree(jsonFile);

            if(rootNode.isArray()) {
                for(JsonNode paperNode : rootNode) {
                    Map<String, Object> jsonMap = new HashMap<>();
                    String title = paperNode.get("title").asText();
                    String desc = paperNode.get("desc").asText();
                    String domainName = paperNode.get("domainName").asText();
                    String publishedBy = paperNode.get("publishedBy").asText();
                    String sourcesName = paperNode.get("sourcesName").asText();
                    String postedOn = paperNode.get("postedOn").asText();
                    String href = paperNode.get("href").asText();
                    String codeLink = paperNode.get("codeLink").asText();
                    jsonMap.put("title", title);
                    jsonMap.put("textContent", desc);
                    jsonMap.put("domainName", domainName);
                    jsonMap.put("publishedBy", publishedBy);
                    jsonMap.put("sourcesName", sourcesName);
                    jsonMap.put("postedOn", postedOn);
                    jsonMap.put("path", href);
                    jsonMap.put("codeLink", codeLink);
                    jsonMap.put("lang", "zh");
                    jsonMap.put("type", "paper");
                    ansList.add(jsonMap);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  
        }
        return ansList;
    }

    public static List<Map<String, Object>> parseCaseJson(File jsonFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> ansList = new ArrayList<>();
        try {
            JsonNode rootNode = objectMapper.readTree(jsonFile);

            if(rootNode.isArray()) {
                for(JsonNode caseNode : rootNode) {
                    Map<String, Object> jsonMap = new HashMap<>();
                    String title = caseNode.get("title").asText();
                    String desc = caseNode.get("desc").asText();
                    String category = caseNode.get("category").asText();
                    String caseType = caseNode.get("type").asText();
                    String logoImg = caseNode.get("logoImg").asText();
                    String href = caseNode.get("href").asText();

                    String logoImgDark = null;
                    if (caseNode.get("logoImgDark") != null) {
                        logoImgDark = caseNode.get("logoImgDark").asText();
                    }

                    JsonNode technologiesNode = caseNode.get("technologies");
                    List<String> technologies = new ArrayList<>();
                    if (technologiesNode != null && technologiesNode.isArray()) {
                        for (JsonNode techNode : technologiesNode) {

                            technologies.add(techNode.asText());
                        }
                    } else if (technologiesNode != null) {
                        technologies.add(technologiesNode.asText());
                    }

                    jsonMap.put("title", title);
                    jsonMap.put("textContent", desc);
                    jsonMap.put("lang", "zh");
                    jsonMap.put("type", "case");
                    jsonMap.put("path", href);
                    jsonMap.put("category", category);
                    jsonMap.put("technologies", technologies);
                    jsonMap.put("logoImg", logoImg);
                    jsonMap.put("caseType", caseType);
                    jsonMap.put("logoImgDark", logoImgDark);
                    ansList.add(jsonMap);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  
        }
        return ansList;
    }

    public static List<Map<String, Object>> parseCourseJson(File jsonFile) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> ansList = new ArrayList<>();
        try {
            JsonNode rootNode = objectMapper.readTree(jsonFile);
            if(rootNode.isArray()) {
                for (JsonNode courseNode : rootNode) {
                    String courseId = (courseNode.get("id") != null) ? courseNode.get("id").asText() : null;
                    String courseCatalog = (courseNode.get("catalog") != null) ? courseNode.get("catalog").asText() : null;
                    String courseDescription = (courseNode.get("description") != null) ? courseNode.get("description").asText() : null;
                    String courseSeries = (courseNode.get("series") != null) ? courseNode.get("series").asText() : null;
                    String courseClasses = (courseNode.get("classes") != null) ? courseNode.get("classes").asText() : null;
                    String courseCover = (courseNode.get("cover") != null) ? courseNode.get("cover").asText() : null;
                    String courseCatalogName = (courseNode.get("catalogName") != null) ? courseNode.get("catalogName").asText() : null;
                    String courseCatalogDesc = (courseNode.get("catalogDesc") != null) ? courseNode.get("catalogDesc").asText() : null;
                    JsonNode childrenNodes = courseNode.get("children");
                    if (childrenNodes != null && childrenNodes.isArray()) {
                        for (JsonNode childrenNode : childrenNodes) {
                            String childrenId = (childrenNode.get("id") != null) ? childrenNode.get("id").asText() : null;
                            String childrenName = (childrenNode.get("name") != null) ? childrenNode.get("name").asText() : null;
                            String childrenCount = (childrenNode.get("count") != null) ? childrenNode.get("count").asText() : null;
                            String childrenCoverImg = (childrenNode.get("coverImg") != null) ? childrenNode.get("coverImg").asText() : null;
                            JsonNode courseListNodes = (childrenNode.get("courseList") != null) ? childrenNode.get("courseList") : null;
                            if (courseListNodes != null && courseListNodes.isArray()) {
                                for (JsonNode courseListNode : courseListNodes) {
                                    Map<String, Object> jsonMap = new HashMap<>();
                                    String courseListId = (courseListNode.get("id") != null) ? courseListNode.get("id").asText() : null;
                                    String courseListCategoryId = (courseListNode.get("categoryId") != null) ? courseListNode.get("categoryId").asText() : null;
                                    String courseListTitle = (courseListNode.get("title") != null) ? courseListNode.get("title").asText() : null;
                                    String courseListLable = (courseListNode.get("lable") != null) ? courseListNode.get("lable").asText() : null;
                                    String courseListLang = (courseListNode.get("lang") != null) ? courseListNode.get("lang").asText() : null;
                                    String courseListForm = (courseListNode.get("form") != null) ? courseListNode.get("form").asText() : null;
                                    String courseListVideoType = (courseListNode.get("videoType") != null) ? courseListNode.get("videoType").asText() : null;
                                    String courseListVideoUrl = (courseListNode.get("videoUrl") != null) ? courseListNode.get("videoUrl").asText() : null;
                                    String courseListPlayMin = (courseListNode.get("playMin") != null) ? courseListNode.get("playMin").asText() : null;
                                    jsonMap.put("type", "course");
                                    jsonMap.put("courseListPlayMin", courseListPlayMin);
                                    jsonMap.put("path", courseListVideoUrl);
                                    jsonMap.put("courseListVideoType", courseListVideoType);
                                    jsonMap.put("courseListForm", courseListForm);
                                    jsonMap.put("lang", courseListLang);
                                    jsonMap.put("courseListLable", courseListLable);
                                    jsonMap.put("title", courseListTitle);
                                    jsonMap.put("courseListCategoryId", courseListCategoryId);
                                    jsonMap.put("courseListId", courseListId);
                                    jsonMap.put("childrenCoverImg", childrenCoverImg);
                                    jsonMap.put("childrenCount", childrenCount);
                                    jsonMap.put("textContent", childrenName);
                                    jsonMap.put("childrenId", childrenId);
                                    jsonMap.put("courseCatalogDesc", courseCatalogDesc);
                                    jsonMap.put("courseCatalogName", courseCatalogName);
                                    jsonMap.put("courseCover", courseCover);
                                    jsonMap.put("courseClasses", courseClasses);
                                    jsonMap.put("courseSeries", courseSeries);
                                    jsonMap.put("courseDescription", courseDescription);
                                    jsonMap.put("courseCatalog", courseCatalog);
                                    jsonMap.put("courseId", courseId);
                                    ansList.add(jsonMap);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  
        }
        return ansList;
    }

    public static Boolean parseHtml(Map<String, Object> jsonMap, String fileContent) {
        String title = "";
        String textContent = "";
        Document node = Jsoup.parse(fileContent);

        Elements sections = node.getElementsByTag("section");
        if (sections.size() > 0) {

            Element one = sections.get(0);

            Elements enTitle = one.getElementsByAttributeValue("title", "Permalink to this headline");
            Elements zhTitle = one.getElementsByAttributeValue("title", "永久链接至标题");
            if (enTitle.size() > 0) {
                Element t = enTitle.get(0).parent();
                title = t.text();
                t.remove();
            } else if (zhTitle.size() > 0) {
                Element t = zhTitle.get(0).parent();
                title = t.text();
                t.remove();
            } else {
                System.out.println("Permalink to this headline - " + enTitle.size());
                System.out.println("永久链接至标题 - " + zhTitle.size());
                System.out.println("html can not find title");
                return false;
            }
            title = title.replaceAll("¶", "");
            title = title.replaceAll("\uF0C1", "");
            textContent = one.text();
        } else {
            System.out.println("html can not find Elements with tag equal section");
            return false;
        }

        jsonMap.put("title", title);
        jsonMap.put("textContent", textContent);

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
        return true;
    }

    public static Boolean parseInstall(Map<String, Object> jsonMap, String fileContent) {
        String fileName = (String) jsonMap.get("articleName");
        if (fileName.endsWith("_en.md")) {
            jsonMap.put("lang", "en");
        } else {
            jsonMap.put("lang", "zh");
        }

        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        Node document = parser.parse(fileContent);

        Document node = Jsoup.parse(renderer.render(document));
        Element t = node.body().child(0);
        String title = t.text();
        t.remove();
        String textContent = node.text();

        String path = (String) jsonMap.get("path");
        String v = path.substring(path.indexOf("/") + 1);
        int location = v.indexOf("/");
        if (location > 0) {
            String version=v.substring(0, location);
            //install文档r2.3.q1版本替换成r2.3.0rc1
            if("r2.3.q1".equals(version)){
                version="r2.3.0rc1";
            }
            jsonMap.put("version", version);
        } else {
            jsonMap.put("version", "-");
        }

        jsonMap.put("title", title);
        jsonMap.put("textContent", textContent);
        jsonMap.put("path", "install/detail?path=" + path);

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
        return true;
    }

    public static List<Map<String, Object>> customizeData() throws Exception {
        List<Map<String, Object>> r = new ArrayList<>();
        String path = MINDSPORE_OFFICIAL + "/selectWebNews";

        HttpURLConnection connection = null;
        String result;  // 返回结果字符串
        for (int i = 1; ; i++) {
            System.out.println("get news page - " + i);
            TimeUnit.SECONDS.sleep(10);
            try {
                JSONObject param = new JSONObject();
                param.put("category", null);
                param.put("newsTime", "");
                param.put("pageCurrent", i);
                param.put("tag", "zh");
                param.put("type", 0);
                connection = sendHTTP(path, "POST", param.toJSONString());
                if (connection.getResponseCode() == 200) {
                    result = ReadInput(connection.getInputStream());
                    if (!setData(result, r, "zh")) {
                        break;
                    }
                } else {
                    System.out.println(path + " - " + connection.getResponseCode());
                    return null;
                }

                param.put("tag", "en");
                connection = sendHTTP(path, "POST", param.toJSONString());
                if (connection.getResponseCode() == 200) {
                    result = ReadInput(connection.getInputStream());
                    if (!setData(result, r, "en")) {
                        break;
                    }
                } else {
                    System.out.println(path + " - " + connection.getResponseCode());
                    return null;
                }
            } catch (Exception e) {
                System.out.println("Connection failed, error is: " + e.getMessage());
                break;
            } finally {
                if (null != connection) {
                    connection.disconnect();
                }
            }
        }
        return r;
    }

    private static boolean setData(String data, List<Map<String, Object>> r, String lang) {
        JSONObject post = JSON.parseObject(data).getJSONObject("obj");
        JSONArray records = post.getJSONArray("records");
        if (records.size() <= 0) {
            return false;
        }

        String path = "";
        HttpURLConnection connection = null;
        String result;
        for (int i = 0; i < records.size(); i++) {
            JSONObject topic = records.getJSONObject(i);
            int id = topic.getInteger("id");
            String type = "";
            if (lang.equals("zh")) {
                type = getInformationType(topic.getString("type"));
            } else {
                type = getInformationTypeEn(topic.getString("type"));
            }

            path = String.format(MINDSPORE_OFFICIAL + "/selectNewsInfo?id=%d", id);

            try {
                connection = sendGET(path, "GET");
                if (connection.getResponseCode() == 200) {
                    result = ReadInput(connection.getInputStream());
                    JSONObject st = JSON.parseObject(result).getJSONObject("obj");
                    JSONObject detail = st.getJSONObject("detail");
                    String newsDetail = detail.getString("newsDetail");
                    //双重解析转义
                    Document node = Jsoup.parse(Jsoup.parse(newsDetail).text());
                    String textContent = node.text();
                    String title = st.getString("titie");

                    String category = detail.getString("category");

                    Map<String, Object> jsonMap = new HashMap<>();

                    jsonMap.put("title", title);
                    jsonMap.put("textContent", textContent);
                    jsonMap.put("lang", lang);
                    jsonMap.put("category", category);
                    jsonMap.put("subclass", type);
                    jsonMap.put("type", "information");
                    jsonMap.put("path", "news/newschildren?id=" + id);

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
                    r.add(jsonMap);
                } else {
                    System.out.println(path + " - " + connection.getResponseCode());
                }
            } catch (IOException e) {
                System.out.println("Connection failed, error is: " + e.getMessage());
            } finally {
                if (null != connection) {
                    connection.disconnect();
                }
            }

        }
        return true;
    }

    public static String getInformationType(String t) {
        return switch (t) {
            case "1" -> "版本发布";
            case "2" -> "技术博客";
            case "3" -> "社区活动";
            case "4" -> "新闻";
            case "5" -> "案例";
            default -> "新闻";
        };
    }

    public static String getInformationTypeEn(String t) {
        return switch (t) {
            case "1" -> "Version Release";
            case "2" -> "Blogs";
            case "3" -> "Activities";
            case "4" -> "News";
            default -> "News";
        };
    }


    private static HttpURLConnection sendHTTP(String path, String method, String param) throws IOException {
        URL url = new URL(path);
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-type", "application/json");
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
        writer.write(param);
        writer.flush();
        connection.connect();
        return connection;
    }

    private static HttpURLConnection sendGET(String path, String method) throws IOException {
        URL url = new URL(path);
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.connect();
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
            System.out.println("read input failed, error is: " + e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e) {
            System.out.println("close stream failed, error is: " + e.getMessage());
        }
        return sbf.toString();

    }

}
