/*
 This project is licensed under the Mulan PSL v2.
 You can use this software according to the terms and conditions of the Mulan PSL v2.
 You may obtain a copy of Mulan PSL v2 at:
     http://license.coscl.org.cn/MulanPSL2
 THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 See the Mulan PSL v2 for more details.
 Created: 2024
*/
package etherpad;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EPLiteConnection {

    /**
     * Logger for logging messages in App class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(EPLiteConnection.class);

     /**
     * The operation was successful and completed without any errors.
     */
    private static final int CODE_OK = 0;

    /**
     * The operation failed because the input parameters were invalid or missing.
     */
    private static final int CODE_INVALID_PARAMETERS = 1;

    /**
     * The operation failed due to an internal error within the system.
     */
    private static final int CODE_INTERNAL_ERROR = 2;

    /**
     * The operation failed because the requested method is not supported or does not exist.
     */
    private static final int CODE_INVALID_METHOD = 3;

    /**
     * The operation failed because the provided API key is invalid or has expired.
     */
    private static final int CODE_INVALID_API_KEY = 4;

    /**
     * The URI of the API endpoint.
     */
    private final URI uri;

    /**
     * The API key required for authentication.
     */
    private final String apiKey;

    /**
     * The version of the API to use.
     */
    private final String apiVersion;

    /**
     * The character encoding to apply when making requests.
     */
    private final String encoding;

    /**
     * Creates a new instance of the {@link EPLiteConnection} class.
     *
     * @param url        the base URL of the EPLite API endpoint
     * @param apiKey     the API key required for authentication with the EPLite API
     * @param apiVersion the version of the EPLite API to use (e.g., "v1", "v2")
     * @param encoding   the character encoding to use for requests and responses
     */
    public EPLiteConnection(String url, String apiKey, String apiVersion, String encoding) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        this.uri = URI.create(url);
        this.apiKey = apiKey;
        this.apiVersion = apiVersion;
        this.encoding = encoding;
    }

    /**
     * Executes an API method and returns the response as a map.
     *
     * @param apiMethod the name of the API method to execute
     * @return a map containing the response from the API
     */
    public Map get(String apiMethod) {
        Map response = (Map) this.getObject(apiMethod, new HashMap<>());
        return (response != null) ? response : new HashMap();
    }

    /**
     * Executes an API method and returns the response as a map.
     *
     * @param apiMethod the name of the API method
     * @param apiArgs the params of the API method
     * @return a map containing the response from the API
     */
    public Map get(String apiMethod, Map<String, Object> apiArgs) {
        Map response = (Map) this.getObject(apiMethod, apiArgs);
        return (response != null) ? response : new HashMap();
    }

    /**
     * Executes a GET request to an API endpoint and returns the response object.
     *
     * @param apiMethod the name of the API method to call
     * @param apiArgs   a map of query string parameters to include in the request
     * @return the response object from the API, which can be of any type
     */
    public Object getObject(String apiMethod, Map<String, Object> apiArgs) {
        String path = this.apiPath(apiMethod);
        String query = this.queryString(apiArgs, true);
        URL url = apiUrl(path, query);
        Request request = new GETRequest(url);
        return this.call(request);
    }

    /**
     * Constructs the full path for an API method.
     *
     * @param apiMethod the name of the API method to construct the path
     * @return the full path for the specified API method
     */
    protected String apiPath(String apiMethod) {
        return this.uri.getPath() + "/api/" + this.apiVersion + "/" + apiMethod;
    }

    /**
     * Constructs a query string from a map of API arguments.
     *
     * @param apiArgs a map of API arguments
     * @param urlEncode whether to URL encode the keys and values
     * @return the constructed query string
     */
    protected String queryString(Map<String, Object> apiArgs, boolean urlEncode) {
        StringBuilder strArgs = new StringBuilder();
        apiArgs.put("apikey", this.apiKey);
        Iterator i = apiArgs.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (urlEncode) {
                try {
                    if (key instanceof String) {
                        URLEncoder.encode((String) key, this.encoding);
                    }
                    if (value instanceof String) {
                        value = URLEncoder.encode((String) value, this.encoding);
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(String.format(
                            "Unable to URLEncode using encoding '%s'", this.encoding), e);
                }
            }
            strArgs.append(key).append("=").append(value);
            if (i.hasNext()) {
                strArgs.append("&");
            }
        }
        return strArgs.toString();
    }

    /**
     * Constructs and returns a URL pointing to an Etherpad Lite instance.
     *
     * @param path  The path segment to add to the base URI.
     * @param query The query string to add to the URL.
     * @return The constructed URL object.
     */
    protected URL apiUrl(String path, String query) {
        try {
            return new URL(new URI(this.uri.getScheme(), null, this.uri.getHost(),
            this.uri.getPort(), path, query, null).toString());
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException("Error in the URL to the Etherpad Lite instance ("
            + e.getClass() + "): " + e.getMessage());
        }
    }

    /**
     * Sends a request to an Etherpad Lite instance and processes the response.
     *
     * @param request The request object to be sent
     * @return The processed response result
     */
    private Object call(Request request) {
        trustServerAndCertificate();

        try {
            String response = request.send();
            return this.handleResponse(response);
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Unable to connect to Etherpad Lite instance (" + e.getClass() + "): " + e.getMessage());
        }
        return null;
    }

    /**
     * Handles the JSON response from an Etherpad Lite API call.
     *
     * @param jsonString The JSON response string from the Etherpad Lite API call
     * @return The data field from the response if the API call was successful
     */
    protected Object handleResponse(String jsonString) {
        try {
            JSONParser parser = new JSONParser();
            Map response = (Map) parser.parse(jsonString);
            if (null != response.get("code"))  {
                int code = ((Long) response.get("code")).intValue();
                switch (code) {
                    case CODE_OK:
                        return response.get("data");
                    case CODE_INVALID_PARAMETERS:
                        break;
                    case CODE_INTERNAL_ERROR:
                        break;
                    case CODE_INVALID_METHOD:
                        break;
                    case CODE_INVALID_API_KEY:
                        throw new RuntimeException((String) response.get("message") + jsonString);
                    default:
                        throw new RuntimeException("An unknown error has occurred : " + jsonString);
                }
            } else {
                throw new RuntimeException("An unexpected response from the server: " + jsonString);
            }
        } catch (ParseException e) {
            LOGGER.error("Unable to parse JSON response + jsonString + {}", e.getMessage());
        }
        return null;
    }

    /**
     * Configures the SSL context to trust all server certificates and hostnames.
     */
    private void trustServerAndCertificate() {
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
         };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOGGER.error("Unable to create SSL context {}", e);
        }
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }
}
