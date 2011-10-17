package org.mule.devkit.idea.module;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SourcePathsBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArchetype;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.utils.MavenUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class MuleModuleBuilder extends ModuleBuilder implements SourcePathsBuilder {
    private static final Icon ICON = IconLoader.getIcon("/mule24.png");

    private MavenProject myAggregatorProject;
    private MavenProject myParentProject;

    private boolean myInheritGroupId;
    private boolean myInheritVersion;

    private MavenId myProjectId;

    private String myContentEntryPath;

    private String myModuleName;
    private String myModulePackage;

    @Override
    public void setupRootModel(ModifiableRootModel rootModel) throws ConfigurationException {
        final Project project = rootModel.getProject();

        final VirtualFile root = createAndGetContentEntry();
        rootModel.addContentEntry(root);

        rootModel.inheritSdk();

        MavenUtil.runWhenInitialized(project, new DumbAwareRunnable() {
            public void run() {
                new MuleModuleBuilderHelper(myProjectId, myAggregatorProject, myParentProject, myInheritGroupId,
                        myInheritVersion, getArchetype(), myModuleName, myModulePackage, getDescription()).configure(project, root, false);
            }
        });
    }

    private VirtualFile createAndGetContentEntry() {
        String path = FileUtil.toSystemIndependentName(getContentEntryPath());
        new File(path).mkdirs();
        return LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
    }

    @Override
    public ModuleWizardStep[] createWizardSteps(WizardContext wizardContext, ModulesProvider modulesProvider) {
        return new ModuleWizardStep[]{new MuleModuleWizardStep(wizardContext.getProject(), this)};
    }

    @Override
    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    public List<Pair<String, String>> getSourcePaths() {
        return Collections.emptyList();
    }

    public void setSourcePaths(List<Pair<String, String>> pairs) {
    }

    public void addSourcePath(Pair<String, String> stringStringPair) {
    }

    public void setAggregatorProject(MavenProject project) {
        myAggregatorProject = project;
    }

    public MavenProject getAggregatorProject() {
        return myAggregatorProject;
    }

    public void setParentProject(MavenProject project) {
        myParentProject = project;
    }

    public MavenProject getParentProject() {
        return myParentProject;
    }

    public void setProjectId(MavenId id) {
        myProjectId = id;
    }

    public MavenId getProjectId() {
        return myProjectId;
    }

    public String getModuleName() {
        return myModuleName;
    }

    public void setModuleName(String myModuleName) {
        this.myModuleName = myModuleName;
    }

    public String getModulePackage() {
        return myModulePackage;
    }

    public void setModulePackage(String myModulePackage) {
        this.myModulePackage = myModulePackage;
    }

    @Override
    public String getBuilderId() {
        return getClass().getName();
    }

    public void setInheritedOptions(boolean groupId, boolean version) {
        myInheritGroupId = groupId;
        myInheritVersion = version;
    }

    public boolean isInheritGroupId() {
        return myInheritGroupId;
    }

    public boolean isInheritVersion() {
        return myInheritVersion;
    }

    @Nullable
    public String getContentEntryPath() {
        if (myContentEntryPath == null) {
            final String directory = getModuleFileDirectory();
            if (directory == null) {
                return null;
            }
            new File(directory).mkdirs();
            return directory;
        }
        return myContentEntryPath;
    }

    public void setContentEntryPath(String moduleRootPath) {
        final String path = acceptParameter(moduleRootPath);
        if (path != null) {
            try {
                myContentEntryPath = FileUtil.resolveShortWindowsName(path);
            } catch (IOException e) {
                myContentEntryPath = path;
            }
        } else {
            myContentEntryPath = null;
        }
        if (myContentEntryPath != null) {
            myContentEntryPath = myContentEntryPath.replace(File.separatorChar, '/');
        }
    }

    public MavenProject findPotentialParentProject(Project project) {
        if (!MavenProjectsManager.getInstance(project).isMavenizedProject()) {
            return null;
        }

        File parentDir = new File(getContentEntryPath()).getParentFile();
        if (parentDir == null) {
            return null;
        }
        VirtualFile parentPom = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(parentDir, "pom.xml"));
        if (parentPom == null) {
            return null;
        }

        return MavenProjectsManager.getInstance(project).findProject(parentPom);
    }

    public abstract String getSubName();

    public abstract MavenArchetype getArchetype();

    public abstract MavenId getDefaultProjectId();

    @Override
    public String getName() {
        return "Mule " + getSubName();
    }

    @Override
    public String getDescription() {
        return "Create a new Mule Module.";
    }

    @Override
    public Icon getBigIcon() {
        return ICON;
    }

    @Override
    public String getPresentableName() {
        return getName();
    }
}
