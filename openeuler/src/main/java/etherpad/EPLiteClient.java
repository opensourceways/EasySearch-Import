package etherpad;

import java.util.HashMap;
import java.util.Map;

public class EPLiteClient {
    private static final String DEFAULT_API_VERSION = "1.2.13";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private final EPLiteConnection connection;

    public EPLiteClient(String url, String apiKey) {
        this.connection = new EPLiteConnection(url, apiKey, DEFAULT_API_VERSION, DEFAULT_ENCODING);
    }

    public Map listAllPads() {
        return this.connection.get("listAllPads");
    }

    public Map getText(String padId) {
        Map<String,Object> args = new HashMap<>();
        args.put("padID", padId);
        return this.connection.get("getText", args);
    }

}
