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
import java.util.ArrayList;
import java.util.List;

/**
 * @author yh@firim
 */
public class GitUtil {

    private static String tagUrl = "https://api.github.com/repos/%s/git/refs/tags";

    /**
     * List full tags
     * @param fullName
     * @return
     */
    public static List<String> listTags(String fullName) {
        String url = String.format(tagUrl, fullName);
        HttpResponse<String> response = HttpClient.build(url).get().bodyAsString();
        UriUtil.detectResponseIsOKAndThrowable(response);
        Tag[] tags = new Gson().fromJson(response.getBody(), Tag[].class);
        return doFetchTags(tags);
    }

    private static List<String> doFetchTags(Tag[] tags) {
        List<String> tagList = new ArrayList<>(tags.length);
        for (Tag tag : tags) {
            tagList.add(doProcessTag(tag));
        }
        return tagList;
    }

    private static String doProcessTag(Tag tag) {
        String replaceKey = "refs/tags/";
        return tag.ref.replace(replaceKey, "");
    }

    /**
     * tag model
     */
    private class Tag {

        private String ref;
        private String url;
    }
}
