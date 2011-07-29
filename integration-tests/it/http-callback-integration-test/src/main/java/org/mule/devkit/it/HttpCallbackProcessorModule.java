/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.devkit.it;

import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.callback.HttpCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Module(name = "callback")
public class HttpCallbackProcessorModule {

    @Processor
    public Object sendSms(HttpCallback onFail) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(onFail.getUrl()).openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("GET");
            return readResponseFromServer(urlConnection);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readResponseFromServer(HttpURLConnection con) throws IOException {
        BufferedReader bufferedReader = null;
        try {
            if (con.getInputStream() != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }
        } catch (IOException e) {
            if (con.getErrorStream() != null) {
                bufferedReader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
            }
        }

        if (bufferedReader == null) {
            throw new IOException("Unable to read response from server");
        }

        StringBuilder decodedString = new StringBuilder();

        try {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                decodedString.append(line).append("\n");
            }
        } finally {
            bufferedReader.close();
        }
        return decodedString.toString();
    }
}