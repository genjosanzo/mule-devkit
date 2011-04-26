package org.mule.devkit.apt;

import com.sun.codemodel.JCodeModel;

import javax.lang.model.util.Elements;

public class AnnotationProcessorContext {
    private JCodeModel codeModel;
    private Elements elements;

    public JCodeModel getCodeModel() {
        return codeModel;
    }

    public void setCodeModel(JCodeModel codeModel) {
        this.codeModel = codeModel;
    }

    public Elements getElements() {
        return elements;
    }

    public void setElements(Elements elements) {
        this.elements = elements;
    }
}
