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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;

import java.text.MessageFormat;

/**
 * Based on Maven plugin by Kevin Sawicki (kevin@github.com)
 */
public abstract class AbstractGitHubMojo extends AbstractMojo {

    /**
     * Is debug logging enabled?
     *
     * @return true if enabled, false otherwise
     */
    protected boolean isDebug() {
        Log log = getLog();
        return log != null ? log.isDebugEnabled() : false;
    }

    /**
     * Is info logging enabled?
     *
     * @return true if enabled, false otherwise
     */
    protected boolean isInfo() {
        Log log = getLog();
        return log != null ? log.isInfoEnabled() : false;
    }

    /**
     * Log given message at debug level
     *
     * @param message
     */
    protected void debug(String message) {
        Log log = getLog();
        if (log != null) {
            log.debug(message);
        }
    }

    /**
     * Log given message and throwable at debug level
     *
     * @param message
     * @param throwable
     */
    protected void debug(String message, Throwable throwable) {
        Log log = getLog();
        if (log != null) {
            log.debug(message, throwable);
        }
    }

    /**
     * Log given message at info level
     *
     * @param message
     */
    protected void info(String message) {
        Log log = getLog();
        if (log != null) {
            log.info(message);
        }
    }

    /**
     * Log given message and throwable at info level
     *
     * @param message
     * @param throwable
     */
    protected void info(String message, Throwable throwable) {
        Log log = getLog();
        if (log != null) {
            log.info(message, throwable);
        }
    }

    /**
     * Log given message at warn level
     *
     * @param message
     */
    protected void warn(String message) {
        Log log = getLog();
        if (log != null) {
            log.warn(message);
        }
    }

    /**
     * Log given message and throwable at warn level
     *
     * @param message
     * @param throwable
     */
    protected void warn(String message, Throwable throwable) {
        Log log = getLog();
        if (log != null) {
            log.warn(message, throwable);
        }
    }

    /**
     * Log given message at error level
     *
     * @param message
     */
    protected void error(String message) {
        Log log = getLog();
        if (log != null) {
            log.error(message);
        }
    }

    /**
     * Log given message and throwable at error level
     *
     * @param message
     * @param throwable
     */
    protected void error(String message, Throwable throwable) {
        Log log = getLog();
        if (log != null) {
            log.error(message, throwable);
        }
    }

    /**
     * Create client
     *
     * @param host
     * @param userName
     * @param password
     * @param oauth2Token
     * @return client
     * @throws MojoExecutionException
     */
    protected GitHubClient createClient(String host, String userName,
                                        String password, String oauth2Token) throws MojoExecutionException {
        GitHubClient client;
        if (!StringUtils.isEmpty(host)) {
            client = new GitHubClient(host);
        } else {
            client = new GitHubClient();
        }
        if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(password)) {
            if (isDebug()) {
                debug("Using basic authentication with username: " + userName);
            }
            client.setCredentials(userName, password);
        } else if (!StringUtils.isEmpty(oauth2Token)) {
            if (isDebug()) {
                debug("Using OAuth2 access token authentication");
            }
            client.setOAuth2Token(oauth2Token);
        } else {
            throw new MojoExecutionException("No authentication credentials configured");
        }
        return client;
    }

    /**
     * Get repository and throw a {@link MojoExecutionException} on failures
     *
     * @param project
     * @param owner
     * @param name
     * @return non-null repository id
     * @throws MojoExecutionException
     */
    protected RepositoryId getRepository(MavenProject project, String owner,
                                         String name) throws MojoExecutionException {
        RepositoryId repository = RepositoryUtils.getRepository(project, owner, name);
        if (repository == null) {
            throw new MojoExecutionException("No GitHub repository (owner and name) configured");
        }
        if (isDebug()) {
            debug(MessageFormat.format("Using GitHub repository {0}", repository.generateId()));
        }
        return repository;
    }
}