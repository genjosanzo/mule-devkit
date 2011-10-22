package org.mule.devkit.idea.compiler;

import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathsList;
import org.jetbrains.annotations.NotNull;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MuleApt {
    private MuleApt() {
    }

    private static void ensureDirectoryExists(String path) {
        final File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        return true;
    }

    @NotNull
    public static Map<CompilerMessageCategory, List<String>> compile(@NotNull MuleAptGenerationItem aptGenerationItem) throws IOException {
        final Map<CompilerMessageCategory, List<String>> messages = new HashMap<CompilerMessageCategory, List<String>>();
        messages.put(CompilerMessageCategory.ERROR, new ArrayList<String>());
        messages.put(CompilerMessageCategory.WARNING, new ArrayList<String>());
        messages.put(CompilerMessageCategory.INFORMATION, new ArrayList<String>());
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        List<File> files = new ArrayList<File>();
        files.add(new File(aptGenerationItem.muleModule.getPath()));
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(files);

        PathsList classPath = OrderEnumerator.orderEntries(aptGenerationItem.getModule().getProject()).withoutModuleSourceEntries().getPathsList();
        for (String path : classPath.getPathList()) {
            if (path.contains("mule-devkit-annotations")) {
                String newpath = null;
                newpath = path.replace("mule-devkit-annotations", "mule-devkit-annotations-processor");
                classPath.add(newpath);
                newpath = path.replace("mule-devkit-annotations", "mule-devkit-codemodel");
                classPath.add(newpath);
                newpath = path.replace("mule-devkit-annotations", "mule-devkit-schemamodel");
                classPath.add(newpath);
                newpath = path.replace("mule-devkit-annotations", "mule-devkit-studiomodel");
                classPath.add(newpath);
            }
        }
        String compileClassPath = classPath.getPathsString();

        List<String> options = new ArrayList<String>(10);
        String modulePath = aptGenerationItem.getModule().getModuleFilePath();
        modulePath = modulePath.substring(0, modulePath.lastIndexOf("/"));
        String classesPath = modulePath + "/target/classes";

        String generatedSourcesPath = modulePath + "/target/generated-sources/mule";

        List<File> filesToDelete = new ArrayList<File>();
        CompilerUtil.collectFiles(filesToDelete, new File(generatedSourcesPath), new FileFilter() {
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                }

                return file.getAbsolutePath().endsWith("java");
            }
        });
        for (File file : filesToDelete) {
            VirtualFile generatedVFile = LocalFileSystem.getInstance().findFileByIoFile(file);
            generatedVFile.delete(null);
        }

        ensureDirectoryExists(classesPath);
        ensureDirectoryExists(generatedSourcesPath);

        options.add("-cp");
        options.add(compileClassPath);
        options.add("-proc:only");

        //addCompilerArguments(options);

        options.add("-processor");
        options.add("org.mule.devkit.apt.ModuleAnnotationProcessor");
        options.add("-d");
        options.add(classesPath);

        options.add("-s");
        options.add(generatedSourcesPath);


        DiagnosticListener<JavaFileObject> dl = null;
        dl = new DiagnosticListener<JavaFileObject>() {

            public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    messages.get(CompilerMessageCategory.ERROR).add(diagnostic.toString().replace("error: ", ""));
                } else if (diagnostic.getKind() == Diagnostic.Kind.WARNING) {
                    messages.get(CompilerMessageCategory.WARNING).add(diagnostic.toString());

                } else {
                    messages.get(CompilerMessageCategory.INFORMATION).add(diagnostic.toString());

                }
            }

        };

        JavaCompiler.CompilationTask task = compiler.getTask(
                new PrintWriter(System.out),
                fileManager,
                dl,
                options,
                null,
                compilationUnits);

        // Perform the compilation task.
        if (!task.call()) {
            if (messages.get(CompilerMessageCategory.ERROR).isEmpty()) {
                messages.get(CompilerMessageCategory.ERROR).add("An unknown error has occurred.");
            }
        }

        VirtualFile generatedSourcesVFile = LocalFileSystem.getInstance().findFileByIoFile(new File(generatedSourcesPath));
        generatedSourcesVFile.refresh(false, true);

        return messages;
    }
}
