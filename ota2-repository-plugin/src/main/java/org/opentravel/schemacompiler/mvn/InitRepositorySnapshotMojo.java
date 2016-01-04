/**
 * Copyright (C) 2014 OpenTravel Alliance (info@opentravel.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opentravel.schemacompiler.mvn;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * OTA2.0 repository plugin that executes during the <code>initialize</code> phase of the build
 * to ensure that a snapshot has been created if one does not already exist.  If a snapshot does
 * exist, this mojo will exit successfully without action.
 */
@Mojo( name = "initialize-snapshot", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe=true  )
@Execute( goal="initialize-snapshot", phase = LifecyclePhase.INITIALIZE )
public class InitRepositorySnapshotMojo extends AbstractOTA2RepositoryMojo {
	
	/**
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		validate();
		File snapshotProjectFile = getSnapshotProjectFile();
		File snapshotFolder = getSnapshotFolder();
		
		if (snapshotProjectFile.exists() && snapshotFolder.exists()) {
			getLog().info("OTA2 repository snapshot already initialized.");
			
		} else {
			createOrUpdateSnapshot();
		}
	}
	
}
