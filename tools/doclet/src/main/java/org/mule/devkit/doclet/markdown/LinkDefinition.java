package org.mule.devkit.doclet.markdown;


public class LinkDefinition {
    private String url;
    private String title;

    public LinkDefinition(String url, String title) {
        this.url = url;
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return url + " (" + title + ")";
    }
}
