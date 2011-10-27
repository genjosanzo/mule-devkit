/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mule.devkit.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Tree;
import org.eclipse.egit.github.core.TreeEntry;
import org.eclipse.egit.github.core.TypedResource;
import org.eclipse.egit.github.core.client.IGitHubConstants;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.util.EncodingUtils;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.mule.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Based on Maven plugin by Kevin Sawicki (kevin@github.com)
 */
@MojoGoal("github-upload-doc")
public class GitHubDocMojo extends AbstractGitHubMojo {
    /**
     * BRANCH_DEFAULT
     */
    public static final String BRANCH_DEFAULT = "refs/heads/gh-pages";
    private static final int BUFFER_LENGTH = 8192;

    /**
     * Branch to update
     */
    @MojoParameter(expression = "${branch}")
    private String branch = BRANCH_DEFAULT;

    /**
     * Path of tree
     */
    @MojoParameter(expression = "${path}")
    private String path;

    /**
     * Commit message
     */
    @MojoParameter(expression = "${message}", defaultValue = "Updating documentation")
    private String message;

    /**
     * Name of repository
     */
    @MojoParameter(expression = "${github.repositoryName}")
    private String repositoryName;

    /**
     * Owner of repository
     */
    @MojoParameter(expression = "${github.repositoryOwner}")
    private String repositoryOwner;

    /**
     * User name for authentication
     */
    @MojoParameter(expression = "${github.userName}")
    private String userName;

    /**
     * User name for authentication
     */
    @MojoParameter(expression = "${github.password}")
    private String password;

    /**
     * User name for authentication
     */
    @MojoParameter(expression = "${github.oauth2Token}")
    private String oauth2Token;

    /**
     * Host for API calls
     */
    @MojoParameter(expression = "${github.host}", defaultValue = "api.github.com")
    private String host;

    /**
     * Paths and patterns to include
     */
    @MojoParameter
    private String[] includes;

    /**
     * Paths and patterns to exclude
     */
    @MojoParameter
    private String[] excludes = new String[]{"**/current.xml"};

    /**
     * Base directory to commit files from
     */
    @MojoParameter(expression = "${siteOutputDirectory}", defaultValue = "${project.build.directory}/apidocs",
            required = true)
    private File outputDirectory;

    /**
     * Project being built
     */
    @MojoParameter(expression = "${project}",
            required = true)
    private MavenProject project;

    /**
     * Force reference update
     */
    @MojoParameter(expression = "${github.force}")
    private boolean force;

    /**
     * Merge with existing the existing tree that is referenced by the commit
     * that the ref currently points to
     */
    @MojoParameter(expression = "${github.merge}")
    private boolean merge;

    /**
     * Show what blob, trees, commits, and references would be created/updated
     * but don't actually perform any operations on the target GitHub
     * repository.
     */
    @MojoParameter(expression = "${github.dryRun}")
    private boolean dryRun;

    /**
     * In case of failure of the mojo, how many times retry the execution
     */
    @MojoParameter(expression = "${github.retry.count}", defaultValue = "3")
    private int retryCount;

    /**
     * The number of milliseconds to wait before retrying.
     */
    @MojoParameter(expression = "${github.sleep.time}", defaultValue = "10000")
    private int sleepTime;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            if (retryCount-- > 0) {
                executeMojo();
            }
        } catch (MojoExecutionException e) {
            warn(String.format("Exception caught while uploading the documentation to GitHub, %s retries left", retryCount), e);
            if (retryCount == 0) {
                error("Cannot upload documentation to GitHub after retrying");
                throw e;
            }
            sleep(sleepTime);
            execute();
        }
    }

    /**
     * Create blob
     *
     * @param service
     * @param repository
     * @param path
     * @return blob SHA-1
     * @throws org.apache.maven.plugin.MojoExecutionException
     *
     */
    protected String createBlob(DataService service, RepositoryId repository,
                                String path) throws MojoExecutionException {
        File file = new File(outputDirectory, path);
        long length = file.length();
        int size = length > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) length;
        ByteArrayOutputStream output = new ByteArrayOutputStream(size);
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            byte[] buffer = new byte[BUFFER_LENGTH];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error reading file: " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(stream);
        }

        Blob blob = new Blob().setEncoding(Blob.ENCODING_BASE64);

        try {
            byte[] encoded = EncodingUtils.toBase64(output.toByteArray());
            blob.setContent(new String(encoded, IGitHubConstants.CHARSET_UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new MojoExecutionException("Error encoding blob contents: " + e.getMessage(), e);
        }

        try {
            if (isDebug()) {
                debug(MessageFormat.format("Creating blob from {0}", file.getAbsolutePath()));
            }

            if (!dryRun) {
                return service.createBlob(repository, blob);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating blob: " + e.getMessage(), e);
        }
    }

    /**
     * Create an array with only the non-null and non-empty values
     *
     * @param values
     * @return non-null but possibly empty array of non-null/non-empty strings
     */
    public static String[] removeEmpties(String... values) {
        if (values == null || values.length == 0) {
            return new String[0];
        }
        List<String> validValues = new ArrayList<String>();
        for (String value : values) {
            if (value != null && value.length() > 0) {
                validValues.add(value);
            }
        }
        return validValues.toArray(new String[validValues.size()]);
    }

    /**
     * Get matching paths found in given base directory
     *
     * @param includes
     * @param excludes
     * @param baseDir
     * @return non-null but possibly empty array of string paths relative to the
     *         base directory
     */
    public static String[] getMatchingPaths(String[] includes, String[] excludes, String baseDir) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(baseDir);
        if (includes != null && includes.length > 0) {
            scanner.setIncludes(includes);
        }
        if (excludes != null && excludes.length > 0) {
            scanner.setExcludes(excludes);
        }
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    private void executeMojo() throws MojoExecutionException {
        RepositoryId repository = getRepository(project, repositoryOwner, repositoryName);

        if (dryRun) {
            info("Dry run mode, repository will not be modified");
        }

        // Find files to include
        String baseDir = outputDirectory.getAbsolutePath();
        String[] includePaths = removeEmpties(includes);
        String[] excludePaths = removeEmpties(excludes);
        if (isDebug()) {
            debug(MessageFormat.format(
                    "Scanning {0} and including {1} and exluding {2}", baseDir,
                    Arrays.toString(includePaths),
                    Arrays.toString(excludePaths)));
        }
        String[] paths = getMatchingPaths(includePaths, excludePaths, baseDir);
        if (paths.length != 1) {
            info(MessageFormat.format("Creating {0} blobs", paths.length));
        } else {
            info("Creating 1 blob");
        }
        if (isDebug()) {
            debug(MessageFormat.format("Scanned files to include: {0}", Arrays.toString(paths)));
        }

        DataService service = new DataService(createClient(host, userName, password, oauth2Token));

        // Write blobs and build tree entries
        List<TreeEntry> entries = new ArrayList<TreeEntry>(paths.length);
        String prefix = path;
        if (prefix == null) {
            prefix = "";
        }
        if (prefix.length() > 0 && !prefix.endsWith("/")) {
            prefix += "/";
        }

        // Convert separator to forward slash '/'
        if ('\\' == File.separatorChar) {
            for (int i = 0; i < paths.length; i++) {
                paths[i] = paths[i].replace('\\', '/');
            }
        }

        for (String path : paths) {
            TreeEntry entry = new TreeEntry();
            entry.setPath(prefix + path);
            entry.setType(TreeEntry.TYPE_BLOB);
            entry.setMode(TreeEntry.MODE_BLOB);
            entry.setSha(createBlob(service, repository, path));
            entries.add(entry);
        }

        Reference ref = null;
        try {
            ref = service.getReference(repository, branch);
        } catch (RequestException e) {
            if (404 != e.getStatus()) {
                throw new MojoExecutionException("Error getting reference: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error getting reference: " + e.getMessage(), e);
        }

        if (ref != null && !TypedResource.TYPE_COMMIT.equals(ref.getObject().getType())) {
            throw new MojoExecutionException(
                    MessageFormat
                            .format("Existing ref {0} points to a {1} ({2}) instead of a commmit",
                                    ref.getRef(), ref.getObject().getType(),
                                    ref.getObject().getSha()));
        }

        // Write tree
        Tree tree;
        try {
            int size = entries.size();
            if (size != 1) {
                info(MessageFormat.format("Creating tree with {0} blob entries", size));
            } else {
                info("Creating tree with 1 blob entry");
            }
            String baseTree = null;
            if (merge && ref != null) {
                Tree currentTree = service.getCommit(repository,
                        ref.getObject().getSha()).getTree();
                if (currentTree != null) {
                    baseTree = currentTree.getSha();
                }
                info(MessageFormat.format("Merging with tree {0}", baseTree));
            }
            if (!dryRun) {
                tree = service.createTree(repository, entries, baseTree);
            } else {
                tree = new Tree();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating tree: " + e.getMessage(), e);
        }

        // Build commit
        Commit commit = new Commit();
        commit.setMessage(message);
        commit.setTree(tree);

        // Set parent commit SHA-1 if reference exists
        if (ref != null) {
            commit.setParents(Collections.singletonList(new Commit().setSha(ref.getObject().getSha())));
        }

        Commit created;
        try {
            if (!dryRun) {
                created = service.createCommit(repository, commit);
            } else {
                created = new Commit();
            }
            info(MessageFormat.format("Creating commit with SHA-1: {0}", created.getSha()));
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating commit: " + e.getMessage(), e);
        }

        TypedResource object = new TypedResource();
        object.setType(TypedResource.TYPE_COMMIT).setSha(created.getSha());
        if (ref != null) {
            // Update existing reference
            ref.setObject(object);
            try {
                info(MessageFormat.format(
                        "Updating reference {0} from {1} to {2}", branch,
                        commit.getParents().get(0).getSha(), created.getSha()));
                if (!dryRun) {
                    service.editReference(repository, ref, force);
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Error editing reference: " + e.getMessage(), e);
            }
        } else {
            // Create new reference
            ref = new Reference().setObject(object).setRef(branch);
            try {
                info(MessageFormat.format(
                        "Creating reference {0} starting at commit {1}",
                        branch, created.getSha()));
                if (!dryRun) {
                    service.createReference(repository, ref);
                }
            } catch (IOException e) {
                throw new MojoExecutionException("Error creating reference: " + e.getMessage(), e);
            }
        }
    }

    private void sleep(int sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            // ignore
        }
    }
}