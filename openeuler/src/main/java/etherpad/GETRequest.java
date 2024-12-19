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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

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
