/*
 * Copyright 2019 flow.ci
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

package com.flowci.common.helper;

import java.util.regex.Pattern;

/**
 * @author yang
 */
public abstract class PatternHelper {

    private static final String UCS_CHAR = "[" +
        "\u00A0-\uD7FF" +
        "\uF900-\uFDCF" +
        "\uFDF0-\uFFEF" +
        "\uD800\uDC00-\uD83F\uDFFD" +
        "\uD840\uDC00-\uD87F\uDFFD" +
        "\uD880\uDC00-\uD8BF\uDFFD" +
        "\uD8C0\uDC00-\uD8FF\uDFFD" +
        "\uD900\uDC00-\uD93F\uDFFD" +
        "\uD940\uDC00-\uD97F\uDFFD" +
        "\uD980\uDC00-\uD9BF\uDFFD" +
        "\uD9C0\uDC00-\uD9FF\uDFFD" +
        "\uDA00\uDC00-\uDA3F\uDFFD" +
        "\uDA40\uDC00-\uDA7F\uDFFD" +
        "\uDA80\uDC00-\uDABF\uDFFD" +
        "\uDAC0\uDC00-\uDAFF\uDFFD" +
        "\uDB00\uDC00-\uDB3F\uDFFD" +
        "\uDB44\uDC00-\uDB7F\uDFFD" +
        "&&[^\u00A0[\u2000-\u200A]\u2028\u2029\u202F\u3000]]";

    private static final String LABEL_CHAR = "a-zA-Z0-9" + UCS_CHAR;

    private static final String IRI_LABEL =
        "[" + LABEL_CHAR + "](?:[" + LABEL_CHAR + "_\\-]{0,61}[" + LABEL_CHAR + "]){0,1}";

    private static final String PROTOCOL = "(?i:http|https|rtsp)://";

    private static final String WORD_BOUNDARY = "(?:\\b|$|^)";

    private static final String USER_INFO = "(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
        + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
        + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@";

    private static final String IP_ADDRESS_STRING =
        "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
            + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
            + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
            + "|[1-9][0-9]|[0-9]))";

    private static final String RELAXED_DOMAIN_NAME =
        "(?:" + "(?:" + IRI_LABEL + "(?:\\.(?=\\S))" + "?)+" + "|" + IP_ADDRESS_STRING + ")";

    private static final String PORT_NUMBER = "\\:\\d{1,5}";

    private static final String PATH_AND_QUERY = "[/\\?](?:(?:[" + LABEL_CHAR
        + ";/\\?:@&=#~"  // plus optional query params
        + "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*";

    private static final String WEB_URL_RE = "("
        + WORD_BOUNDARY
        + "(?:"
        + "(?:" + PROTOCOL + "(?:" + USER_INFO + ")?" + ")"
        + "(?:" + RELAXED_DOMAIN_NAME + ")?"
        + "(?:" + PORT_NUMBER + ")?"
        + ")"
        + "(?:" + PATH_AND_QUERY + ")?"
        + WORD_BOUNDARY
        + ")";

    private static final String GIT_URL_RE = "((git|ssh|http(s)?|file)|(.+@[\\w\\.]+))(:(//)?)([\\w\\.@\\:/\\-~]+)(\\.git)(/)?";

    private static final String EMAIL_RE = "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
        "\\@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+";

    private static final Pattern GIT_URL = Pattern.compile(GIT_URL_RE);

    private static final Pattern WEB_URL = Pattern.compile(WEB_URL_RE);

    private static final Pattern EMAIL_ADDRESS = Pattern.compile(EMAIL_RE);

    public static boolean isGitURL(String str) {
        return GIT_URL.matcher(str).find();
    }

    public static boolean isWebURL(String str) {
        return WEB_URL.matcher(str).find();
    }

    public static boolean isEmail(String str) {
        return EMAIL_ADDRESS.matcher(str).find();
    }
}
