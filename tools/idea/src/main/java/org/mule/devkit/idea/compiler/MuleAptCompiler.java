package org.mule.devkit.idea.compiler;

import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.SourceGeneratingCompiler;
import com.intellij.openapi.compiler.ValidityState;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MuleAptCompiler implements SourceGeneratingCompiler {
    private static final GenerationItem[] EMPTY_GENERATION_ITEM_ARRAY = {};
    private static final Pattern ourMessagePattern = Pattern.compile("(.+):(\\d+):.+");

    public VirtualFile getPresentableFile(CompileContext compileContext, Module module, VirtualFile virtualFile, VirtualFile virtualFile1) {
        return null;
    }

    public GenerationItem[] getGenerationItems(CompileContext compileContext) {
        return ApplicationManager.getApplication().runReadAction(new MuleAptPrepareAction(compileContext));
    }

    private static void addMessages(final CompileContext context, final Map<CompilerMessageCategory, List<String>> messages) {
        addMessages(context, messages, null);
    }

    @NotNull
    private static String getPresentableFile(@NotNull String url, @Nullable Map<VirtualFile, VirtualFile> presentableFilesMap) {
        final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
        if (file == null) {
            return url;
        }

        if (presentableFilesMap == null) {
            return url;
        }

        for (Map.Entry<VirtualFile, VirtualFile> entry : presentableFilesMap.entrySet()) {
            if (file == entry.getValue()) {
                return entry.getKey().getUrl();
            }
        }
        return url;
    }

    private static void addMessages(final CompileContext context, final Map<CompilerMessageCategory, List<String>> messages,
                                    @Nullable final Map<VirtualFile, VirtualFile> presentableFilesMap) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            public void run() {
                if (context.getProject().isDisposed()) {
                    return;
                }
                for (CompilerMessageCategory category : messages.keySet()) {
                    List<String> messageList = messages.get(category);
                    for (String message : messageList) {
                        String url = null;
                        int line = -1;
                        Matcher matcher = ourMessagePattern.matcher(message);
                        if (matcher.matches()) {
                            String fileName = matcher.group(1);
                            if (new File(fileName).exists()) {
                                url = getPresentableFile("file://" + fileName, presentableFilesMap);
                                line = Integer.parseInt(matcher.group(2));
                            }
                        }
                        context.addMessage(category, message, url, line, -1);
                    }
                }
            }
        });
    }

    private static GenerationItem[] doGenerate(final CompileContext context, GenerationItem[] items) {
        List<GenerationItem> results = new ArrayList<GenerationItem>(items.length);
        for (GenerationItem item : items) {
            if (item instanceof MuleAptGenerationItem) {
                final MuleAptGenerationItem aptItem = (MuleAptGenerationItem) item;

                if (!isModuleAffected(context, aptItem.getModule())) {
                    continue;
                }

                try {
                    Map<CompilerMessageCategory, List<String>> messages = MuleApt.compile(aptItem);
                    addMessages(context, messages);
                    if (messages.get(CompilerMessageCategory.ERROR).isEmpty()) {
                        results.add(aptItem);
                    }
                } catch (final IOException e) {
                    ApplicationManager.getApplication().runReadAction(new Runnable() {
                        public void run() {
                            if (context.getProject().isDisposed()) {
                                return;
                            }
                            context.addMessage(CompilerMessageCategory.ERROR, e.getMessage(), null, -1, -1);
                        }
                    });
                }
            }
        }
        return results.toArray(new GenerationItem[results.size()]);
    }

    public static boolean isModuleAffected(CompileContext context, Module module) {
        return ArrayUtil.find(context.getCompileScope().getAffectedModules(), module) >= 0;
    }


    public GenerationItem[] generate(final CompileContext compileContext, final GenerationItem[] items, VirtualFile virtualFile) {
        if (items != null && items.length > 0) {
            compileContext.getProgressIndicator().setText("Generating Mule Code");
            Computable<GenerationItem[]> computation = new Computable<GenerationItem[]>() {
                public GenerationItem[] compute() {
                    if (compileContext.getProject().isDisposed()) {
                        return EMPTY_GENERATION_ITEM_ARRAY;
                    }
                    return doGenerate(compileContext, items);
                }
            };
            GenerationItem[] generationItems = computation.compute();
            List<VirtualFile> generatedVFiles = new ArrayList<VirtualFile>();
            for (GenerationItem aptGenerationItem : generationItems) {
                List<File> files = new ArrayList<File>();
                CompilerUtil.collectFiles(files, new File(aptGenerationItem.getModule().getModuleFile().getParent().getPath() + "/target/generated-sources"), new FileFilter() {
                    public boolean accept(File file) {
                        return true;
                    }
                });
                CompilerUtil.refreshIOFiles(files);
                for (File file : files) {
                    VirtualFile generatedVFile = LocalFileSystem.getInstance().findFileByIoFile(file);
                    generatedVFiles.add(generatedVFile);
                }
            }
            if (compileContext instanceof CompileContextEx) {
                ((CompileContextEx) compileContext).markGenerated(generatedVFiles);
            }
            return generationItems;
        }
        return EMPTY_GENERATION_ITEM_ARRAY;
    }

    @NotNull
    public String getDescription() {
        return "Mule DevKit Code Generator";
    }

    public boolean validateConfiguration(CompileScope compileScope) {
        return true;
    }

    public ValidityState createValidityState(DataInput dataInput) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
