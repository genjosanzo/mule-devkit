package org.mule.devkit.it;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Before;
import org.junit.Test;

public class TransformerArchetypeIT
{

    private static final File ROOT = new File("target/integration-tests/");
    private static final String ARCHETYPE_PROPERTIES = "/transformer-archetype.properties";

    private Properties archetypeProperties;
    private Properties verifierProperties;

    @Before
    public void setUp() throws VerificationException, IOException
    {
        InputStream stream = getClass().getResourceAsStream(ARCHETYPE_PROPERTIES);
        archetypeProperties = new Properties();
        archetypeProperties.load(stream);

        verifierProperties = new Properties();
        verifierProperties.setProperty("use.mavenRepoLocal", "false");

        Verifier verifier = new Verifier(ROOT.getAbsolutePath());

        // deleting a former created artifact from the archetype to be tested
        verifier.deleteArtifact(getGroupId(), getArtifactId(), getVersion(), null);

        // delete the created maven project
        verifier.deleteDirectory(getArtifactId());
    }

    private String getVersion()
    {
        return archetypeProperties.getProperty("version");
    }

    private String getArtifactId()
    {
        return archetypeProperties.getProperty("artifactId");
    }

    private String getGroupId()
    {
        return archetypeProperties.getProperty("groupId");
    }

    @Test
    public void testGenerateArchetype() throws VerificationException
    {
        Verifier verifier = new Verifier(ROOT.getAbsolutePath());
        verifier.setSystemProperties(archetypeProperties);
        verifier.setVerifierProperties(verifierProperties);
        verifier.setAutoclean(false);
        verifier.setMavenDebug(true);
        verifier.setDebug(true);

        verifier.executeGoal("archetype:generate");

        verifier.verifyErrorFreeLog();

        verifier = new Verifier(ROOT.getAbsolutePath() + "/" + getArtifactId());
        verifier.setAutoclean(true);
        verifier.executeGoal("package");

        verifier.verifyErrorFreeLog();

        verifier.verifyTextInLog("generate-sources");
    }
}
