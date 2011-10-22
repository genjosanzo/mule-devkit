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
package org.mule.devkit.utils;

import org.apache.commons.lang.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import java.util.StringTokenizer;

public class JavaDocUtils {
    private Elements elements;

    public JavaDocUtils(Elements elements) {
        this.elements = elements;
    }

    public String getSummary(Element element) {
        String comment = elements.getDocComment(element);
        if (comment == null || StringUtils.isBlank(comment)) {
            return null;
        }

        comment = comment.trim();

        String parsedComment = "";
        boolean tagsBegan = false;
        StringTokenizer st = new StringTokenizer(comment, "\n\r");
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken().trim();
            if (nextToken.startsWith("@")) {
                tagsBegan = true;
            }
            if (!tagsBegan) {
                parsedComment = parsedComment + nextToken + "\n";
            }
        }

        String strippedComments = "";
        boolean insideTag = false;
        for (int i = 0; i < parsedComment.length(); i++) {
            if (parsedComment.charAt(i) == '{' &&
                    parsedComment.charAt(i + 1) == '@') {
                insideTag = true;
            } else if (parsedComment.charAt(i) == '}') {
                insideTag = false;
            } else {
                if (!insideTag) {
                    strippedComments += parsedComment.charAt(i);
                }
            }
        }

        strippedComments = strippedComments.trim();
        while (strippedComments.length() > 0 &&
                strippedComments.charAt(strippedComments.length() - 1) == '\n') {
            strippedComments = StringUtils.chomp(strippedComments);
        }

        return strippedComments;
    }

    public boolean hasTag(String tagName, Element element) {
        String comment = elements.getDocComment(element);
        if (StringUtils.isBlank(comment)) {
            return false;
        }

        StringTokenizer st = new StringTokenizer(comment.trim(), "\n\r");
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken().trim();
            if (nextToken.startsWith("@" + tagName)) {
                String tagContent = StringUtils.difference("@" + tagName, nextToken);
                return !StringUtils.isBlank(tagContent);
            }
            if (nextToken.startsWith("{@" + tagName)) {
                return true;
            }
        }

        return false;
    }

    public String getTagContent(String tagName, ExecutableElement method) {
        String comment = elements.getDocComment(method);
        if (StringUtils.isBlank(comment)) {
            return "";
        }

        StringTokenizer st = new StringTokenizer(comment.trim(), "\n\r");
        boolean insideTag = false;
        StringBuilder tagContent = new StringBuilder();
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken().trim();
            if (nextToken.startsWith("@" + tagName)) {
                return StringUtils.difference("@" + tagName, nextToken).trim();
            }
            if (nextToken.startsWith("{@" + tagName)) {
                if (nextToken.endsWith("}")) {
                    return StringUtils.difference("{@" + tagName, nextToken).replaceAll("}", "").trim();
                } else {
                    tagContent.append(StringUtils.difference("{@" + tagName, nextToken).replaceAll("}", "").trim());
                    insideTag = true;
                }
            } else if (insideTag) {
                if (nextToken.endsWith("}")) {
                    tagContent.append(' ').append(nextToken.replaceAll("}", ""));
                    insideTag = false;
                } else {
                    tagContent.append(' ').append(nextToken);
                }
            }
        }

        return tagContent.toString();
    }

    public String getParameterSummary(String paramName, Element element) {
        String comment = elements.getDocComment(element);
        if (comment == null || StringUtils.isBlank(comment)) {
            return null;
        }

        comment = comment.trim();

        String parameterComment = "";
        boolean insideParameter = false;
        StringTokenizer st = new StringTokenizer(comment, "\n\r");
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken().trim();
            if (nextToken.startsWith("@param " + paramName)) {
                insideParameter = true;
            } else if (nextToken.startsWith("@")) {
                insideParameter = false;
            }
            if (insideParameter) {
                parameterComment += nextToken + " ";
            }
        }

        int startIndex = 7 + paramName.length() + 1;
        if (parameterComment.length() < startIndex) {
            return null;
        }

        parameterComment = parameterComment.substring(startIndex);

        String strippedComments = "";
        boolean insideTag = false;
        for (int i = 0; i < parameterComment.length(); i++) {
            if (parameterComment.charAt(i) == '{' &&
                    parameterComment.charAt(i + 1) == '@') {
                insideTag = true;
            } else if (parameterComment.charAt(i) == '}') {
                insideTag = false;
            } else {
                if (!insideTag) {
                    strippedComments += parameterComment.charAt(i);
                }
            }
        }

        strippedComments = strippedComments.trim();
        while (strippedComments.length() > 0 &&
                strippedComments.charAt(strippedComments.length() - 1) == '\n') {
            strippedComments = StringUtils.chomp(strippedComments);
        }

        return strippedComments;
    }
}
