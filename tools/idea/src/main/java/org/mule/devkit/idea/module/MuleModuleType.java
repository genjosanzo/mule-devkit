package org.mule.devkit.idea.module;

import com.intellij.CommonBundle;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectJdkForModuleStep;
import com.intellij.ide.util.projectWizard.ProjectWizardStepFactory;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.ui.Messages;
import org.mule.devkit.idea.MuleDevKitBundle;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class MuleModuleType extends ModuleType<MuleModuleBuilder> {
  public static final String MODULE_TYPE_ID = "MULE_MODULE";

  public MuleModuleType() {
    super(MODULE_TYPE_ID);
  }

  public static MuleModuleType getInstance() {
    return (MuleModuleType)ModuleTypeManager.getInstance().findByID(MODULE_TYPE_ID);
  }

  public MuleModuleBuilder createModuleBuilder() {
    return new MuleModuleBuilder();
  }

  @Override
  public ModuleWizardStep[] createWizardSteps(final WizardContext wizardContext,
                                              final MuleModuleBuilder moduleBuilder,
                                              ModulesProvider modulesProvider) {
    List<ModuleWizardStep> steps = new ArrayList<ModuleWizardStep>();
    ProjectWizardStepFactory factory = ProjectWizardStepFactory.getInstance();
    steps.add(factory.createSourcePathsStep(wizardContext, moduleBuilder, null, "reference.dialogs.new.project.fromScratch.source"));

    if (!hasAppropriateJdk()) {
      steps.add(new ProjectJdkForModuleStep(wizardContext, JavaSdk.getInstance()) {
        @Override
        public void updateDataModel() {
          // do nothing
        }

        @Override
        public boolean validate() {
          for (Object o : getAllJdks()) {
            if (o instanceof Sdk) {
              Sdk sdk = (Sdk)o;
              if (MuleModuleType.isApplicableJdk(sdk)) {
                return true;
              }
            }
          }
          Messages.showErrorDialog(MuleDevKitBundle.message("no.jdk.error"), CommonBundle.getErrorTitle());
          return false;
        }



      });
    }

    //steps.add(new AndroidModuleWizardStep(moduleBuilder, wizardContext));
    return steps.toArray(new ModuleWizardStep[steps.size()]);
  }

  private static boolean hasAppropriateJdk() {
    for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
      if (isApplicableJdk(sdk)) {
        return true;
      }
    }
    return false;
  }

    public static boolean isApplicableJdk(Sdk jdk) {
  if (!(jdk.getSdkType() instanceof JavaSdk)) {
    return false;
  }
  JavaSdkVersion version = JavaSdk.getInstance().getVersion(jdk);
  return version == JavaSdkVersion.JDK_1_6;
}

  public String getName() {
    return MuleDevKitBundle.message("module.title");
  }

  public String getDescription() {
    return MuleDevKitBundle.message("module.description");
  }

    @Override
    public Icon getBigIcon() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Icon getNodeIcon(boolean b) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    /*
  public Icon getBigIcon() {
    return AndroidUtils.ANDROID_ICON_24;
  }

  public Icon getNodeIcon(boolean isOpened) {
    return AndroidUtils.ANDROID_ICON;
  }
  */
}
