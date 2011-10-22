package org.mule.devkit.idea.compiler;

import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.GeneratingCompiler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MuleAptPrepareAction implements Computable<GeneratingCompiler.GenerationItem[]> {
    private static final GeneratingCompiler.GenerationItem[] EMPTY_GENERATION_ITEM_ARRAY = {};
    private static final Logger LOG = Logger.getInstance(MuleAptPrepareAction.class.getName());

    private final CompileContext myContext;

    public MuleAptPrepareAction(CompileContext context) {
        myContext = context;
    }

    public GeneratingCompiler.GenerationItem[] compute() {
        if (myContext.getProject().isDisposed()) {
            return EMPTY_GENERATION_ITEM_ARRAY;
        }
        Module[] modules = ModuleManager.getInstance(myContext.getProject()).getModules();
        ProjectRootManager rootManager = ProjectRootManager.getInstance(myContext.getProject());
        List<GeneratingCompiler.GenerationItem> items = new ArrayList<GeneratingCompiler.GenerationItem>();
        for (VirtualFile sourceRoot : rootManager.getContentSourceRoots()) {
            for (VirtualFile muleModule : getModules(sourceRoot)) {
                for (Module mod : modules) {
                    if (mod.getModuleScope().contains(muleModule)) {
                        items.add(new MuleAptGenerationItem(muleModule, mod));
                    }
                }
            }
        }


        return items.toArray(new GeneratingCompiler.GenerationItem[items.size()]);
    }

    private List<VirtualFile> getModules(VirtualFile virtualFile) {
        List<VirtualFile> ret = new ArrayList<VirtualFile>();

        if (virtualFile.getName().endsWith(".java")) {
            String content = null;
            try {
                content = new String(virtualFile.contentsToByteArray());
            } catch (IOException e) {
                LOG.error("Cannot parse content for " + virtualFile.getName());
            }
            if (content != null && (content.contains("@Module") || content.contains("@Connector"))) {
                ret.add(virtualFile);
            }
        }

        for (VirtualFile child : virtualFile.getChildren()) {
            ret.addAll(getModules(child));
        }

        return ret;
    }
}

