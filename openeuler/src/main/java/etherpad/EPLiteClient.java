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
