package org.mule.devkit.idea;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.SourceGeneratingCompiler;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.IOException;

public class MuleAptCompiler implements SourceGeneratingCompiler {
    public VirtualFile getPresentableFile(CompileContext compileContext, Module module, VirtualFile virtualFile, VirtualFile virtualFile1) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public GenerationItem[] getGenerationItems(CompileContext compileContext) {
        return new GenerationItem[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public GenerationItem[] generate(CompileContext compileContext, GenerationItem[] generationItems, VirtualFile virtualFile) {
        return new GenerationItem[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @NotNull
    public String getDescription() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean validateConfiguration(CompileScope compileScope) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ValidityState createValidityState(DataInput dataInput) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
