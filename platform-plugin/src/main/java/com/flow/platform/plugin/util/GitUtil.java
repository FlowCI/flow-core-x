/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.plugin.util;

import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import com.google.gson.Gson;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yh@firim
 */
public class GitUtil {

    private final static String TAG_URL = "https://api.github.com/repos%s/git/refs/tags";

    private final static String TAG_ZIP_URL = "https://github.com%s/archive/%s.zip";

    /**
     * List full tags
     * @param fullName
     * @return
     */
    public static List<String> listTags(String fullName) {
        String url = String.format(TAG_URL, fullName);
        HttpResponse<String> response = HttpClient.build(url).get().bodyAsString();
        UriUtil.detectResponseIsOKAndThrowable(response);
        TagHelper[] tags = new Gson().fromJson(response.getBody(), TagHelper[].class);
        return doFetchTags(tags);
    }

    /**
     * get latest tag
     * @param fullName
     * @return
     */
    public static String fetchLatestTag(String fullName) {
        List<String> list = listTags(fullName);
        if (!list.isEmpty()) {
            return list.get(list.size() - 1);
        }

        return null;
    }

    public static void downloadLatestTag(Path path, String fullName) {
        downloadTagZip(path, fullName, fetchLatestTag(fullName));
    }

    public static void downloadTagZip(Path path, String fullName, String tag) {
        String url = String.format(TAG_ZIP_URL, fullName, tag);
        HttpClient.build(url).get().bodyAsStream((HttpResponse<InputStream> inputStreamHttpResponse) -> {
            try {
                InputStream in = inputStreamHttpResponse.getBody();
                FileOutputStream out = new FileOutputStream(Paths.get(path.toString(), tag + ".zip").toString());
                byte[] bytes = new byte[1024];
                int count;
                while ((count = in.read(bytes)) >= 0) {
                    out.write(bytes, 0, count);
                }
                out.flush();
                out.close();
                in.close();
            } catch (Throwable throwable) {
            }
        });
    }

    private static List<String> doFetchTags(TagHelper[] tags) {
        List<String> tagList = new ArrayList<>(tags.length);
        for (TagHelper tag : tags) {
            if (Objects.equals(tag.object.type, "tag")) {
                tagList.add(doProcessTag(tag));
            }
        }
        return tagList;
    }

    private static String doProcessTag(TagHelper tag) {
        String replaceKey = "refs/tags/";
        return tag.ref.replace(replaceKey, "");
    }

    /**
     * tag model
     */
    private class TagHelper {

        private String ref;
        private String url;
        private ObjectHelper object;
    }

    /**
     * object model
     */
    private class ObjectHelper {

        private String type;
        private String url;
    }

}
