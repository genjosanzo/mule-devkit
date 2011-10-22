package org.mule.devkit.idea.compiler;

import com.intellij.openapi.compiler.GeneratingCompiler;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;

public class MuleAptGenerationItem implements GeneratingCompiler.GenerationItem {
    final VirtualFile muleModule;
    final Module module;

    public MuleAptGenerationItem(VirtualFile muleModule, Module module) {
        this.muleModule = muleModule;
        this.module = module;
    }

    public String getPath() {
        return muleModule.getPath();
    }

    public ValidityState getValidityState() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Module getModule() {
        return module;
    }

    public boolean isTestSource() {
        return false;
    }
}