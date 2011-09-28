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

import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_DEFAULT;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SUFFIX_GIT;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.eclipse.egit.github.core.RepositoryId;

/**
 * Repository utilities
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
public class RepositoryUtils {

	/**
	 * Extra repository id from given SCM URL
	 *
	 * @param url
	 * @return repository id or null if extraction fails
	 */
	public static RepositoryId extractRepositoryFromScmUrl(String url) {
		if (StringUtils.isEmpty(url))
			return null;
		int ghIndex = url.indexOf(HOST_DEFAULT);
		if (ghIndex == -1 || ghIndex + 1 >= url.length())
			return null;
		if (!url.endsWith(SUFFIX_GIT))
			return null;
		url = url.substring(ghIndex + HOST_DEFAULT.length() + 1, url.length()
				- SUFFIX_GIT.length());
		return RepositoryId.createFromId(url);
	}

	/**
	 * Get repository
	 *
	 * @param project
	 * @param owner
	 * @param name
	 *
	 * @return repository id or null if none configured
	 */
	public static RepositoryId getRepository(final MavenProject project,
			final String owner, final String name) {
		// Use owner and name if specified
		if (!StringUtils.isEmpty(owner) && !StringUtils.isEmpty(name))
			return RepositoryId.create(owner, name);

		if (project == null)
			return null;

		RepositoryId repo = null;

		// Extract repository from SCM URLs if present
		final Scm scm = project.getScm();
		if (scm == null)
			return null;
		if (!StringUtils.isEmpty(scm.getUrl()))
			repo = RepositoryId.createFromUrl(scm.getUrl());
		if (repo == null)
			repo = extractRepositoryFromScmUrl(scm.getConnection());
		if (repo == null)
			repo = extractRepositoryFromScmUrl(scm.getDeveloperConnection());
		return repo;
	}
}

