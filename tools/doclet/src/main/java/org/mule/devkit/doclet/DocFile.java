/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.devkit.doclet;

import com.google.clearsilver.jsilver.data.Data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DocFile {
    private static final Pattern LINE = Pattern.compile("(.*)[\r]?\n", Pattern.MULTILINE);
    private static final Pattern PROP = Pattern.compile("([^=]+)=(.*)");

    public static String readFile(String filename) {
        try {
            File f = new File(filename);
            int length = (int) f.length();
            FileInputStream is = new FileInputStream(f);
            InputStreamReader reader = new InputStreamReader(is, "UTF-8");
            char[] buf = new char[length];
            int index = 0;
            int amt;
            while (true) {
                amt = reader.read(buf, index, length - index);

                if (amt < 1) {
                    break;
                }

                index += amt;
            }
            return new String(buf, 0, index);
        } catch (IOException e) {
            return null;
        }
    }

    public static String getProperty(String docfile, String property) {
        String filedata = readFile(docfile);
        Matcher lines = LINE.matcher(filedata);
        String line = null;
        while (lines.find()) {
            line = lines.group(1);
            if (line.length() > 0) {
                if (line.equals("@jd:body")) {
                    break;
                }
                Matcher prop = PROP.matcher(line);
                if (prop.matches()) {
                    String key = prop.group(1);
                    String value = prop.group(2);
                    if (key.equalsIgnoreCase(property)) {
                        return value;
                    }
                } else {
                    break;
                }
            }
        }

        return null;
    }

    public static void writePage(Map<String, List<TocInfo>> toc, String docfile, String relative, String outfile) {
        Data hdf = Doclava.makeHDF();

        int i = 0;
        for (String topic : toc.keySet()) {
            hdf.setValue("topics." + i + ".title", topic == null ? "Root" : topic);
            int j = 0;
            for (TocInfo page : toc.get(topic)) {
                hdf.setValue("topics." + i + ".topics." + j + ".title", page.getTitle());
                hdf.setValue("topics." + i + ".topics." + j + ".link", "../" + page.getFilename());
                j++;
            }
            i++;
        }

        /*
        * System.out.println("docfile='" + docfile + "' relative='" + relative + "'" + "' outfile='" +
        * outfile + "'");
        */

        String filedata = readFile(docfile);

        // The document is properties up until the line "@jd:body".
        // Any blank lines are ignored.
        int start = -1;
        int lineno = 1;
        Matcher lines = LINE.matcher(filedata);
        String line = null;
        while (lines.find()) {
            line = lines.group(1);
            if (line.length() > 0) {
                if (line.equals("@jd:body")) {
                    start = lines.end();
                    break;
                }
                Matcher prop = PROP.matcher(line);
                if (prop.matches()) {
                    String key = prop.group(1);
                    String value = prop.group(2);
                    hdf.setValue(key, value);
                } else {
                    break;
                }
            }
            lineno++;
        }
        if (start < 0) {
            System.err.println(docfile + ":" + lineno + ": error parsing docfile");
            if (line != null) {
                System.err.println(docfile + ":" + lineno + ":" + line);
            }
            System.exit(1);
        }

        // if they asked to only be for a certain template, maybe skip it
        String fromTemplate = hdf.getValue("template.which", "");
        String fromPage = hdf.getValue("page.onlyfortemplate", "");
        if (!"".equals(fromPage) && !fromTemplate.equals(fromPage)) {
            return;
        }

        // and the actual text after that
        String commentText = filedata.substring(start);

        Comment comment = new Comment(commentText, null, new SourcePositionInfo(docfile, lineno, 1));
        TagInfo[] tags = comment.tags();

        TagInfo.makeHDF(hdf, "root.descr", tags);

        hdf.setValue("commentText", commentText);

        ClearPage.write(hdf, "docpage.cs", outfile);

    } // writePage
}
