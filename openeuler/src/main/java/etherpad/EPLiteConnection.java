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

    private static final Logger logger = LoggerFactory.getLogger(EPLiteConnection.class);
    public static final int CODE_OK = 0;
    public static final int CODE_INVALID_PARAMETERS = 1;
    public static final int CODE_INTERNAL_ERROR = 2;
    public static final int CODE_INVALID_METHOD = 3;
    public static final int CODE_INVALID_API_KEY = 4;

    public final URI uri;

    public final String apiKey;

    public final String apiVersion;

    public final String encoding;

    public EPLiteConnection(String url, String apiKey, String apiVersion, String encoding) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length()-1);
        }
        this.uri = URI.create(url);
        this.apiKey = apiKey;
        this.apiVersion = apiVersion;
        this.encoding = encoding;
    }

    public Map get(String apiMethod) {
        Map response = (Map) this.getObject(apiMethod, new HashMap<>());
        return (response != null) ? response : new HashMap();
    }

    public Map get(String apiMethod, Map<String,Object> apiArgs) {
        Map response = (Map) this.getObject(apiMethod, apiArgs);
        return (response != null) ? response : new HashMap();
    }

    public Object getObject(String apiMethod, Map<String, Object> apiArgs) {
        String path = this.apiPath(apiMethod);
        String query = this.queryString(apiArgs, true);
        URL url = apiUrl(path, query);
        Request request = new GETRequest(url);
        return this.call(request);
    }

    protected String apiPath(String apiMethod) {
        return this.uri.getPath() + "/api/" + this.apiVersion + "/" + apiMethod;
    }

    protected String queryString(Map<String,Object> apiArgs, boolean urlEncode) {
        StringBuilder strArgs = new StringBuilder();
        apiArgs.put("apikey", this.apiKey);
        Iterator i = apiArgs.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry)i.next();
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

    protected URL apiUrl(String path, String query) {
        try {
            return new URL(new URI(this.uri.getScheme(), null, this.uri.getHost(),
            this.uri.getPort(), path, query, null).toString());
        } catch (MalformedURLException | URISyntaxException e) {
            throw new RuntimeException("Error in the URL to the Etherpad Lite instance (" + e.getClass() + "): " + e.getMessage());
        }
    }

    private Object call(Request request) {
        trustServerAndCertificate();

        try {
            String response = request.send();
            return this.handleResponse(response);
        }
        catch (RuntimeException e) {
            logger.error(e.getMessage());
        }
        catch (Exception e) {
            logger.error("Unable to connect to Etherpad Lite instance (" + e.getClass() + "): " + e.getMessage());
        }
        return null;
    }

    protected Object handleResponse(String jsonString) {
        try {
            JSONParser parser = new JSONParser();
            Map response = (Map) parser.parse(jsonString);
            if (null != response.get("code"))  {
                int code = ((Long) response.get("code")).intValue();
                switch ( code ) {
                    case CODE_OK: 
                        return response.get("data");
                    case CODE_INVALID_PARAMETERS: 
                        break;
                    case CODE_INTERNAL_ERROR:
                        break;
                    case CODE_INVALID_METHOD:
                        break;
                    case CODE_INVALID_API_KEY:
                        throw new RuntimeException((String)response.get("message") + jsonString);
                    default:
                        throw new RuntimeException("An unknown error has occurred while handling the response: " + jsonString);
                }
            } else {
                throw new RuntimeException("An unexpected response from the server: " + jsonString);
            }
        } catch (ParseException e) {
            logger.error("Unable to parse JSON response + jsonString + {}", e.getMessage());
        }
        return null;
    }

    private void trustServerAndCertificate() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
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
            logger.error("Unable to create SSL context {}", e);
        }

		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}
    
}
