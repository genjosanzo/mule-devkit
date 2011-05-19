package org.mule.devkit.it;

import java.io.File;
import java.io.IOException;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractMavenIT
{

    @Before
    public void setUp() throws VerificationException, IOException
    {
        Verifier verifier = new Verifier(getRoot().getAbsolutePath());

        // Deleting a former created artefact from the archetype to be tested
        verifier.deleteArtifact(getGroupId(), getArtifactId(), getArtifactVersion(), null);

        // Delete the created maven project
        verifier.deleteDirectory(getArtifactId());
    }

    protected abstract String getArtifactVersion();

    protected abstract String getArtifactId();

    protected abstract String getGroupId();

    protected abstract File getRoot();

    @Test
    public void buildExecutable() throws VerificationException
    {
        Verifier verifier = new Verifier(getRoot().getAbsolutePath(), null, true, false);
        verifier.setAutoclean(false);
        verifier.setMavenDebug(true);
        verifier.setDebug(true);

        verifier.executeGoal("package");

        verifier.verifyErrorFreeLog();
    }
}