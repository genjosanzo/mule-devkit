package org.mule.devkit.apt.code;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.util.ClassNameUtils;

import javax.lang.model.element.TypeElement;

public class MetadataCodeGenerator extends AbstractCodeGenerator {
    public MetadataCodeGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement type) throws CodeGenerationException {
        String metadataInterfaceName = getContext().getElements().getBinaryName(type) + "Metadata";

        try {
            JPackage pkg = getContext().getCodeModel()._package(ClassNameUtils.getPackageName(metadataInterfaceName));
            JDefinedClass jc = pkg._interface(ClassNameUtils.getClassName(metadataInterfaceName));
        } catch (JClassAlreadyExistsException e) {
            throw new CodeGenerationException("Internal Error: Class " + metadataInterfaceName + " already exists");
        }
    }
}
