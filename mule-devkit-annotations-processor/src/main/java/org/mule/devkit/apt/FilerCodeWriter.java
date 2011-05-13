package org.mule.devkit.apt;


import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JPackage;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileManager;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;

public final class FilerCodeWriter extends CodeWriter {

    private final Filer filer;

    public FilerCodeWriter(Filer filer) {
        this.filer = filer;
    }

    public OutputStream openBinary(JPackage pkg, String fileName) throws IOException {
        JavaFileManager.Location loc;

        if (fileName.endsWith(".java")) {
            // APT doesn't do the proper Unicode escaping on Java source files,
            // so we can't rely on Filer.createSourceFile.
            loc = SOURCE_OUTPUT;
        } else {
            // put non-Java files directly to the output folder
            loc = CLASS_OUTPUT;
        }
        if( pkg != null )
            return filer.createResource(loc, pkg.name(), fileName).openOutputStream();
        else
            return filer.createResource(loc, "", fileName).openOutputStream();
    }

    public Writer openSource(JPackage pkg, String fileName) throws IOException {
        String name;
        if (pkg.isUnnamed())
            name = fileName;
        else
            name = pkg.name() + '.' + fileName;

        name = name.substring(0, name.length() - 5);   // strip ".java"

        return filer.createSourceFile(name).openWriter();
    }

    public void close() {
        ; // noop
    }
}