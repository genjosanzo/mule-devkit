package org.mule.devkit.doclet.markdown;

import java.util.regex.Matcher;

public interface Replacement {
    String replacement(Matcher m);
}
