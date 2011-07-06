package org.mule.devkit.idea;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlFile;
import com.intellij.xml.XmlSchemaProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MuleSchemaProvider extends XmlSchemaProvider {
    private static final Logger LOG = Logger.getInstance("#org.mule.devkit.idea.MuleSchemaProvider");
    private static final Key<CachedValue<Map<String, VirtualFile>>> SCHEMAS_BUNDLE_KEY = Key.create("spring schemas");
    private static final CachedValueProvider.Result<Map<String, VirtualFile>> EMPTY_MAP_RESULT =
            new CachedValueProvider.Result(Collections.emptyMap(), new Object[]{PsiModificationTracker.MODIFICATION_COUNT});

    @Override
    public boolean isAvailable(@NotNull XmlFile file) {
        return "mule".equals(file.getRootTag().getLocalName()) &&
                "http://www.mulesoft.org/schema/mule/core".equals(file.getRootTag().getNamespace());
    }

    @NotNull
    @Override
    public Set<String> getAvailableNamespaces(@NotNull XmlFile file, @Nullable String tagName) {
        return super.getAvailableNamespaces(file, tagName);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public String getDefaultPrefix(@NotNull @NonNls String namespace, @NotNull XmlFile context) {
        return super.getDefaultPrefix(namespace, context);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public Set<String> getLocations(@NotNull @NonNls String namespace, @NotNull XmlFile context) {
        return super.getLocations(namespace, context);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public XmlFile getSchema(@NotNull @NonNls String url, @Nullable Module module, @NotNull PsiFile baseFile) {
        if (module == null) {
            PsiDirectory directory = baseFile.getParent();
            if (directory != null) {
                module = ModuleUtil.findModuleForPsiElement(directory);
            }
        }
        if (module == null) {
            return null;
        }
        Map schemas = getSchemas(module);
        Project project = module.getProject();
        VirtualFile file = (VirtualFile) schemas.get(url);
        if (file == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (!(psiFile instanceof XmlFile)) {
            return null;
        }
        return (XmlFile) psiFile;
    }

    @NotNull
    public static Map<String, VirtualFile> getSchemas(@NotNull final Module module) {
        Project project = module.getProject();
        CachedValuesManager manager = CachedValuesManager.getManager(project);
        Map bundle = (Map) manager.getCachedValue(module, SCHEMAS_BUNDLE_KEY, new CachedValueProvider() {
            public CachedValueProvider.Result<Map<String, VirtualFile>> compute() {
                return computeSchemas(module);
            }
        }
                , false);
        return bundle == null ? Collections.emptyMap() : bundle;
    }

    @NotNull
    private static CachedValueProvider.Result<Map<String, VirtualFile>> computeSchemas(@NotNull Module module) {
        Map<String, VirtualFile> schemas = new HashMap<String, VirtualFile>();

        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(module.getProject());
        GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
        ProjectRootManager manager = ProjectRootManager.getInstance(module.getProject());
        for (VirtualFile virtualFile : manager.getContentSourceRoots()) {
            VirtualFile springSchemas = virtualFile.findFileByRelativePath("META-INF/spring.schemas");
            if (springSchemas != null) {
                LOG.info(virtualFile.getUrl());
            }
        }

        return new CachedValueProvider.Result<Map<String, VirtualFile>>(schemas);
    }
}
