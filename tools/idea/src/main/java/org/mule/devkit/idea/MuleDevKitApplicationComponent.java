package org.mule.devkit.idea;

import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

public class MuleDevKitApplicationComponent implements ApplicationComponent {
    public MuleDevKitApplicationComponent() {
    }

    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "org.mule.devkit.idea.MuleDevKitApplicationComponent";
    }
}
