import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.io.FileUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
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
    public static final String USERPRACTICE = "userPractice";
    public static  Map WEXIN_TOKEN =null;
    private static final String GITEE_REPOS_URL = System.getenv("GITEE_REPOS_URL");
    private static final String GITEE_README_URL = System.getenv("GITEE_README_URL");
    private static final String GITEE_PROJS = System.getenv("GITEE_PROJS");
    private static final String WEXIN_TOKEN_URL = System.getenv("WEXIN_TOKEN_URL");
    private static final String WEXIN_PUBLICATION_RECORDS_URL = System.getenv("WEXIN_PUBLICATION_RECORDS_URL");
    private static final String FORUM_DOMAIN = System.getenv("FORUM_DOMAIN");
    private static final String SERVICE_URL = System.getenv("SERVICE_URL");


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
        System.out.println("begin update customizeData");
        List<Map<String, Object>> r = new ArrayList<>();
        if (!setForum(r)) {
            System.out.println("Failed to add forum data");
            return null;
        }
        if (!setService(r)) {
            System.out.println("Failed to add service data");
            return null;
        }
        if (!setGiteeData(r)) {
            System.out.println("Failed to add setGitee data");
            return null;
        }
        return r;
    }

    private static boolean setForum(List<Map<String, Object>> r) {
        String path = FORUM_DOMAIN + "/latest.json?no_definitions=true&page=";

        String req = "";
        HttpURLConnection connection = null;
        String result;  // 返回结果字符串

        for (int i = 0; ; i++) {
            req = path + i;
            try {
                connection = sendHTTP(req, "GET",null);
                TimeUnit.SECONDS.sleep(30);
                if (connection.getResponseCode() == 200) {
                    result = ReadInput(connection.getInputStream());
                    if (!setData(result, r)) {
                        break;
                    }
                } else {
                    System.out.println(req + " - " + connection.getResponseCode());
                    return false;
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Connection failed, error is: " + e.getMessage());
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
                connection = sendHTTP(path, "GET",null);
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
                    System.out.println(path + " - " + connection.getResponseCode());
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
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
            connection = sendHTTP(SERVICE_URL, "GET",null);
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
            System.out.println("load yaml failed, error is: " + e.getMessage());
            return false;
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
        return true;
    }

    public static Boolean setGiteeData(List<Map<String, Object>> r) {
        if (GITEE_PROJS != null && !GITEE_PROJS.isEmpty()) {
            List<String> projectsList = Arrays.asList(new String(GITEE_PROJS).split(","));
            projectsList.stream().forEach(p -> {
                String orgsUrl = String.valueOf(GITEE_REPOS_URL).replace("{org}", p);
                String readmeUrl = String.valueOf(GITEE_README_URL).replace("{org}", p);
                handGiteeData(orgsUrl, r,readmeUrl);
            });
        }
        return true;
    }

    public static Boolean setWeChaData(List<Map<String, Object>> r) {
        HashMap<String, Object> requestParam = new HashMap<>();
        Integer offset=0;
        Integer totalCount=10;
        //每页获取数量最大写死
        requestParam.put("count",20);
        requestParam.put("no_content",0);
        do {
            String wechatToken = getWechatToken(Boolean.FALSE);
            if(wechatToken !=null) {
                requestParam.put("offset",offset);
                String httpResponse = getHttpResponse(new StringBuilder(WEXIN_PUBLICATION_RECORDS_URL).append(wechatToken).toString(),"POST",JSONObject.toJSONString(requestParam));
                if(httpResponse!=null){
                    JSONObject httpResponseJson = JSONObject.parseObject(httpResponse);
                    if(httpResponseJson.containsKey("errcode")){
                        if (httpResponseJson.getInteger("errcode").equals(40001)){
                            getWechatToken(Boolean.TRUE);
                            continue;
                        }
                    }
                    Integer itemCount= httpResponseJson.getInteger("item_count");
                    offset +=itemCount;
                    totalCount=httpResponseJson.getInteger("total_count");
                    if(itemCount>0){
                        JSONArray item = httpResponseJson.getJSONArray("item");
                        item.stream().forEach(i->{
                            HashMap<String, Object> result = new HashMap<>();
                            JSONObject itemData = (JSONObject) i;
                            result.put("title",itemData.getString("title"));
                            result.put("type", "wechat");
                            result.put("lang","zh");
                            result.put("path",itemData.getString("url"));
                            result.put("textContent",itemData.getString("content"));
                            result.put("description",itemData.getString("digest"));
                            r.add(result);
                        });
                    }
                }
            }
        }while (offset<totalCount);

        return true;
    }

    public static String getWechatToken(Boolean isForceRefresh) {
        long currentTimeMillis = System.currentTimeMillis();
        if( isForceRefresh||Objects.isNull(WEXIN_TOKEN) ||currentTimeMillis-Long.getLong(String.valueOf(WEXIN_TOKEN.get("expires_in")))>0){
            String httpResponse = getHttpResponse(WEXIN_TOKEN_URL,"GET",null);
            if(httpResponse !=null ){
                JSONObject  tokenJson = JSONObject.parseObject(httpResponse);
                tokenJson.put("expires_in", currentTimeMillis+tokenJson.getLong("expires_in")*1000);
                WEXIN_TOKEN=tokenJson;
            }
        }
        return String.valueOf(WEXIN_TOKEN.get("access_token"));
    }

    public static void handGiteeData(String orgsUrl, List<Map<String, Object>> handleList,String readmeUrl) {
        HttpURLConnection connection = null;
        try {
            JSONArray resultArray = new JSONArray();
            Integer page = 0;
            do {
                page++;
                StringBuilder urlBuilder = new StringBuilder(orgsUrl).append(URLEncoder.encode(String.valueOf(page), "utf-8"));
                String httpResponse = getHttpResponse(urlBuilder.toString(),"GET",null);
                if (httpResponse != null) {
                    resultArray = JSONArray.parseArray(httpResponse);
                    if (resultArray != null && resultArray.size() > 0) {
                        resultArray.stream().forEach(a -> {
                            try{
                                JSONObject each = (JSONObject) a;
                                Map<String, Object> date = new HashMap<>();
                                String fullName = each.getString("full_name");
                                date.put("title",fullName);
                                date.put("path", each.getString("html_url"));
                                date.put("type", "gitee");
                                date.put("lang", "zh");
                                date.put("textContent","");
                                handleList.add(date);
                                String description = each.getString("description");
                                if(description !=null && !description.isEmpty() && !"null".equals(description)){
                                    date.put("title",String.valueOf( new StringBuilder(fullName).append(" (").append(description).append(")")));
                                }
                                String readmeResponse = getHttpResponse(String.valueOf(readmeUrl).replace("{repo}",each.getString("path")),"GET",null);
                                if(readmeResponse!=null){
                                    JSONObject readmeJson = JSONObject.parseObject(readmeResponse);
                                    date.put("textContent",decodeBase64(readmeJson.getString("content")));
                                }
                            }catch (Exception e){
                                System.out.println("gitee数据处理错误："+e);
                            }
                        });
                    }
                }
            } while (resultArray.size() == 20);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getHttpResponse(String url ,String method,String param) {
        String response = null;
        HttpURLConnection connection = null;
        try {
            connection = sendHTTP(url, method,param);
            if (connection == null || (connection.getResponseCode() != HttpURLConnection.HTTP_OK && connection.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND)) {
                System.out.println("http请求失败：" + connection);
                return response;
            }
            response = ReadInput(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("http请求失败：" + e.toString());
        }finally {
            if (null != connection) {
                connection.disconnect();
            }
        }
        return response;
    }
    public static String decodeBase64(String base64Str) {
        String decodeStr="";
        byte[] base64Data = Base64.getDecoder().decode(base64Str);
        decodeStr=new String(base64Data, StandardCharsets.UTF_8);
        return decodeStr;
    }

    private static HttpURLConnection sendHTTP(String path, String method,String param) throws IOException {
        URL url = new URL(path);
        HttpURLConnection connection = null;
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(60000);
        connection.setReadTimeout(60000);
        if(param !=null){
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.connect();
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = param.getBytes("utf-8");
                os.write(input);
            }
        }else {
            connection.connect();
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
            System.out.println(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return sbf.toString();
    }
}
