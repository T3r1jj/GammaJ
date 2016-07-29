/* 
 * Copyright 2016 Damian Terlecki.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.t3r1jj.gammaj.info;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpVersionUtility {

    private static final String VERSION_URL = "https://script.google.com/macros/s/AKfycbzuQ2SYv2qmrS03cNqz_Lc_Qxa4NePdHrwoc82ZElx65dinmSVx/exec?sheet=GammaJ&cellx=2&celly=2";
    private static final String LINK_URL = "https://script.google.com/macros/s/AKfycbzuQ2SYv2qmrS03cNqz_Lc_Qxa4NePdHrwoc82ZElx65dinmSVx/exec?sheet=GammaJ&cellx=3&celly=2";
    private static final String DONATE_URL = "https://script.google.com/macros/s/AKfycbzuQ2SYv2qmrS03cNqz_Lc_Qxa4NePdHrwoc82ZElx65dinmSVx/exec?sheet=Donate&cellx=1&celly=1";

    private static HttpURLConnection httpConn;

    public String getVersion() throws IOException {
        requestGet(VERSION_URL);
        String version = readSingleLineRespone();
        disconnect();
        return version;
    }

    public String getLink() throws IOException {
        requestGet(LINK_URL);
        String version = readSingleLineRespone();
        disconnect();
        return version;
    }

    public String getDonateLink() throws IOException {
        requestGet(DONATE_URL);
        String link = readSingleLineRespone();
        disconnect();
        return link;
    }

    private void requestGet(String urlString) throws MalformedURLException, IOException {
        URL url = new URL(urlString);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoInput(true);
        httpConn.setDoOutput(false);
    }

    private String readSingleLineRespone() throws IOException {
        if (httpConn == null) {
            throw new IOException("Connection has not been established.");
        }
        InputStream inputStream = httpConn.getInputStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream))) {
            return reader.readLine();
        }
    }

    private void disconnect() {
        if (httpConn != null) {
            httpConn.disconnect();
        }
    }
}
