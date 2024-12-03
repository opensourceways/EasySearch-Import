import java.io.InputStream;
import java.util.Properties;

public class ImportConfig {
    private String url;
    private String apiKey;

    public ImportConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("unable to find related config");
                return;
            }
            Properties prop = new Properties();
            prop.load(input);
            this.url = prop.getProperty("url");
            this.apiKey = prop.getProperty("apiKey");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getUrl() {
        return url;
    }

    public String getApiKey() {
        return apiKey;
    }
}
