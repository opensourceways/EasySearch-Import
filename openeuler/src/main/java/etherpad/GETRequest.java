package etherpad;

import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
public class GETRequest implements Request {
    private final URL url;

    public GETRequest(URL url) {
        this.url = url;
    }

    public String send() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
        StringBuilder response = new StringBuilder();
        String buffer;
        while ((buffer = in.readLine()) != null) {
            response.append(buffer);
        }
        in.close();
        return response.toString();
    }
}
