package org.mule.devkit.idea.module;

import org.jetbrains.idea.maven.model.MavenArchetype;
import org.jetbrains.idea.maven.model.MavenId;

public class MuleTransformerBuilder extends MuleModuleBuilder {
    @Override
    public String getSubName() {
        return "Transformer";
    }

    @Override
    public MavenArchetype getArchetype() {
        return new MavenArchetype("org.mule.tools.devkit", "mule-devkit-archetype-transformer", "3.0-SNAPSHOT", "http://repository.mulesoft.org/releases/", null);
    }

    @Override
    public MavenId getDefaultProjectId() {
        return new MavenId("org.mule.modules", "mule-module-mytransformer", "1.0-SNAPSHOT");
    }
}
