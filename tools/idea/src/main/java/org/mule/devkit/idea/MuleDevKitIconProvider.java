package org.mule.devkit.idea;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MuleDevKitIconProvider extends IconProvider implements DumbAware {
    private static final Icon ICON = IconLoader.getIcon("/mule64.png");

    @Nullable
    public Icon getIcon(@NotNull final PsiElement element, final int flags) {
        @NonNls final String pluginXml = "plugin.xml";
        if (element instanceof XmlFile && Comparing.strEqual(((XmlFile) element).getName(), pluginXml)) {
            return ICON;
        }
        return null;
    }
}
