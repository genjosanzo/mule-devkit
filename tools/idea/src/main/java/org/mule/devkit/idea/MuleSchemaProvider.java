package org.mule.devkit.idea;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.impl.scopes.ModuleWithDependenciesScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.xml.XmlSchemaProvider;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MuleSchemaProvider extends XmlSchemaProvider {

    private static final Logger LOG = Logger.getInstance(MuleSchemaProvider.class.getName());
    private static final Key<CachedValue<Map<String, XmlFile>>> SCHEMAS_BUNDLE_KEY = Key.create("spring_schemas");

    @Override
    public boolean isAvailable(@NotNull XmlFile file) {
        return true;
    }

    /**
     * Looks for the schema file to handle the given namespace (url) within the schemas supported by this provider.
     * These schemas are read from spring.schemas file and searched in project files and dependencies. If a schema
     * declared in spring.schemas is not present within project files and project dependencies it will not be resolved.
     *
     * @param url      the url of the namespace
     * @param module   the module where the baseFile is
     * @param baseFile the file where the namespace is declared
     * @return the schema file for the given url if it is supported by this provider (declared in spring.schemas), otherwise null
     */
    @Override
    public XmlFile getSchema(@NotNull @NonNls String url, @Nullable final Module module, @NotNull PsiFile baseFile) {
        if (module == null) {
            return null;
        }
        return getSupportedSchemasByUrl(module).get(url);
    }

    @NotNull
    public Map<String, XmlFile> getSupportedSchemasByUrl(@NotNull final Module module) {
        CachedValuesManager manager = CachedValuesManager.getManager(module.getProject());
        return manager.getCachedValue(module, SCHEMAS_BUNDLE_KEY, new CachedValueProvider() {
                    public CachedValueProvider.Result<Map<String, XmlFile>> compute() {
                        return computeSchemas(module);
                    }
                }
                , false);
    }

    @NotNull
    private CachedValueProvider.Result<Map<String, XmlFile>> computeSchemas(@NotNull Module module) {
        Project project = module.getProject();
        Map<String, String> schemaUrlsAndFileNames = getSchemasFromSpringSchemas(module.getProject());
        Map<String, VirtualFile> allSchemaFilesByFileName = getAllSchemaFilesInProjectAndDepsByFileName(module);
        Map<String, XmlFile> schemasByUrl = createSupportedSchemasByUrl(schemaUrlsAndFileNames, allSchemaFilesByFileName, project);
        return new CachedValueProvider.Result<Map<String, XmlFile>>(schemasByUrl);
    }

    private Map<String, String> getSchemasFromSpringSchemas(@NotNull Project project) {
        ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
        for (VirtualFile virtualFile : projectRootManager.getContentRoots()) {
            VirtualFile springSchemas = virtualFile.findFileByRelativePath("target/generated-resources/mule/META-INF/spring.schemas");
            if (springSchemas != null) {
                try {
                    String springSchemasContent = new String(springSchemas.contentsToByteArray());
                    return parseSpringSchemas(springSchemasContent);
                } catch (IOException e) {
                    throw new RuntimeException("Could not read spring.schemas file content", e);
                }
            }
        }
        throw new RuntimeException("Cannot find spring.schemas");
    }

    private Map<String, XmlFile> createSupportedSchemasByUrl(Map<String, String> supportedSchemas, Map<String, VirtualFile> allSchemaFilesByFileName, Project project) {
        Map<String, XmlFile> schemasByUrl = new THashMap<String, XmlFile>();
        for (Map.Entry<String, String> supportedSchema : supportedSchemas.entrySet()) {
            String fileName = supportedSchema.getValue();
            VirtualFile virtualFile = allSchemaFilesByFileName.get(fileName);
            if (virtualFile != null) {
                String url = supportedSchema.getKey();
                schemasByUrl.put(url, findOrCreateXmlFile(virtualFile, project));
            } else {
                LOG.warn("Schema declared in spring.schemas but not available within project files or depedencies: " + supportedSchema);
            }
        }
        return schemasByUrl;
    }

    private XmlFile findOrCreateXmlFile(VirtualFile virtualFile, Project project) {
        XmlFile xmlFile = findXmlFile(virtualFile, project);
        if (xmlFile != null) {
            return xmlFile;
        } else {
            return createXmlFile(virtualFile, project);
        }
    }

    private XmlFile findXmlFile(VirtualFile virtualFile, Project project) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (psiFile != null) {
            return (XmlFile) psiFile.copy();
        }
        return null;
    }

    private XmlFile createXmlFile(VirtualFile virtualFile, Project project) {
        PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);
        try {
            return (XmlFile) psiFileFactory.createFileFromText(virtualFile.getName(), StdFileTypes.XML, new String(virtualFile.contentsToByteArray()));
        } catch (IOException e) {
            LOG.warn("Cannot read schema file: " + virtualFile.getPath());
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> parseSpringSchemas(String springSchemasContent) {
        Map<String, String> schemaUrlsAndFileNames = new HashMap<String, String>();
        for (String line : springSchemasContent.split("\n")) {
            if (!line.startsWith("#")) {
                String url = line.substring(0, line.indexOf("=")).replaceAll("\\\\", "");
                String fileName = line.substring(line.indexOf("="));
                if (fileName.contains("/")) {
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                }
                schemaUrlsAndFileNames.put(url, fileName);
            }
        }
        return schemaUrlsAndFileNames;
    }

    private Map<String, VirtualFile> getAllSchemaFilesInProjectAndDepsByFileName(Module module) {
        ProjectRootManager manager = ProjectRootManager.getInstance(module.getProject());
        Map<String, VirtualFile> allSchemaFiles = new HashMap<String, VirtualFile>();
        for (VirtualFile virtualFile : manager.getContentRoots()) {
            allSchemaFiles.putAll(findAllSchemaFiles(virtualFile));
        }
        ModuleWithDependenciesScope scope = (ModuleWithDependenciesScope) module.getModuleWithDependenciesAndLibrariesScope(false);
        for (VirtualFile virtualFile : scope.getRoots()) {
            allSchemaFiles.putAll(findAllSchemaFiles(virtualFile));
        }
        return allSchemaFiles;
    }

    private Map<String, VirtualFile> findAllSchemaFiles(VirtualFile virtualFile) {
        Map<String, VirtualFile> files = new HashMap<String, VirtualFile>();
        VirtualFile[] entries = virtualFile.getChildren();

        for (VirtualFile entry : entries) {
            if (entry.getName().endsWith(".xsd")) {
                files.put(entry.getName(), entry);
            }
            if (entry.isDirectory()) {
                files.putAll(findAllSchemaFiles(entry));
            }
        }

        return files;
    }
}