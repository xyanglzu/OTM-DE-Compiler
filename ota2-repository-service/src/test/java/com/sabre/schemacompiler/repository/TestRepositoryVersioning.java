/*
 * Copyright (c) 2012, Sabre Corporation and affiliates.
 * All Rights Reserved.
 * Use is subject to license agreement.
 */
package com.sabre.schemacompiler.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sabre.schemacompiler.model.TLLibrary;
import com.sabre.schemacompiler.model.TLModel;
import com.sabre.schemacompiler.util.RepositoryTestUtils;
import com.sabre.schemacompiler.validate.FindingType;
import com.sabre.schemacompiler.validate.ValidationFindings;
import com.sabre.schemacompiler.version.MajorVersionHelper;
import com.sabre.schemacompiler.version.MinorVersionHelper;
import com.sabre.schemacompiler.version.PatchVersionHelper;

/**
 * Verifies the operation of the Repository Web Service by launching a Jetty server to run the
 * web service.  Operations are accessed via remote URL connection to the Jetty server running on
 * the local host.
 * 
 * @author S. Livezey
 */
public class TestRepositoryVersioning extends RepositoryTestBase {
	
	@BeforeClass
	public static void setupTests() throws Exception {
		setupWorkInProcessArea( TestRepositoryVersioning.class );
		startTestServer( "versions-repository", 9293, TestRepositoryVersioning.class );
	}
	
	@AfterClass
	public static void tearDownTests() throws Exception {
		shutdownTestServer();
	}
	
	@Test
	public void testMajorVersionHelper() throws Exception {
		ProjectManager projectManager = new ProjectManager( new TLModel(), false, repositoryManager.get() );
		File projectFile = new File(wipFolder.get(), "/projects/version_test_1.xml");
		
		if (!projectFile.exists()) {
			throw new FileNotFoundException("Test File Not Found: " + projectFile.getAbsolutePath());
		}
		
		ValidationFindings findings = new ValidationFindings();
		Project project = projectManager.loadProject(projectFile, findings);
		ProjectItem projectItem = findProjectItem(project, "Version_Test_1_0_0.otm");
		
		// Verify that the project loaded correctly
		if (findings.hasFinding(FindingType.ERROR)) {
			RepositoryTestUtils.printFindings( findings );
		}
		assertFalse( findings.hasFinding(FindingType.ERROR) );
		assertNotNull( projectItem );
		
		// Create a new major version
		TLLibrary library = (TLLibrary) projectItem.getContent();
		TLLibrary newVersion = new MajorVersionHelper( project ).createNewMajorVersion( library );
		
		// Ensure the new version includes types from the later minor/patch versions in the repository
		// that were not originally in the project
		assertNotNull( newVersion );
		assertEquals( "2.0.0", newVersion.getVersion() );
		assertNotNull( newVersion.getSimpleType("SimpleType_01_00") );
		assertNotNull( newVersion.getSimpleType("SimpleType_01_01") );
		assertNotNull( newVersion.getSimpleType("SimpleType_01_01_01") );
	}
	
	@Test
	public void testMinorVersionHelper() throws Exception {
		ProjectManager projectManager = new ProjectManager( new TLModel(), false, repositoryManager.get() );
		File projectFile = new File(wipFolder.get(), "/projects/version_test_1.xml");
		
		if (!projectFile.exists()) {
			throw new FileNotFoundException("Test File Not Found: " + projectFile.getAbsolutePath());
		}
		
		ValidationFindings findings = new ValidationFindings();
		Project project = projectManager.loadProject(projectFile, findings);
		ProjectItem projectItem = findProjectItem(project, "Version_Test_1_0_0.otm");
		
		// Verify that the project loaded correctly
		if (findings.hasFinding(FindingType.ERROR)) {
			RepositoryTestUtils.printFindings( findings );
		}
		assertFalse( findings.hasFinding(FindingType.ERROR) );
		assertNotNull( projectItem );
		
		// Create a new major version
		TLLibrary library = (TLLibrary) projectItem.getContent();
		TLLibrary newVersion = new MinorVersionHelper( project ).createNewMinorVersion( library );
		
		// Ensure the new version includes types from the later minor/patch versions in the repository
		// that were not originally in the project
		assertNotNull( newVersion );
		assertEquals( "1.2.0", newVersion.getVersion() );
		assertNotNull( newVersion.getSimpleType("SimpleType_01_01_01") );
	}
	
	@Test
	public void testPatchVersionHelper() throws Exception {
		ProjectManager projectManager = new ProjectManager( new TLModel(), false, repositoryManager.get() );
		File projectFile = new File(wipFolder.get(), "/projects/version_test_2.xml");
		
		if (!projectFile.exists()) {
			throw new FileNotFoundException("Test File Not Found: " + projectFile.getAbsolutePath());
		}
		
		ValidationFindings findings = new ValidationFindings();
		Project project = projectManager.loadProject(projectFile, findings);
		ProjectItem projectItem = findProjectItem(project, "Version_Test_1_1_0.otm");
		
		// Verify that the project loaded correctly
		if (findings.hasFinding(FindingType.ERROR)) {
			RepositoryTestUtils.printFindings( findings );
		}
		assertFalse( findings.hasFinding(FindingType.ERROR) );
		assertNotNull( projectItem );
		
		// Create a new major version
		TLLibrary library = (TLLibrary) projectItem.getContent();
		TLLibrary newVersion = new PatchVersionHelper( project ).createNewPatchVersion( library );
		
		// Ensure the new version skips the patch that exists in the repository, but was not loaded
		// into the project
		assertNotNull( newVersion );
		assertEquals( "1.1.2", newVersion.getVersion() );
	}
	
	@Test
	public void testAutoLoadPriorVersions() throws Exception {
		ProjectManager projectManager = new ProjectManager( new TLModel(), false, repositoryManager.get() );
		File projectFile = new File(wipFolder.get(), "/projects/version_test_3.xml");
		
		if (!projectFile.exists()) {
			throw new FileNotFoundException("Test File Not Found: " + projectFile.getAbsolutePath());
		}
		
		ValidationFindings findings = new ValidationFindings();
		Project project = projectManager.loadProject(projectFile, findings);
		
		// Verify that the project loaded correctly
		if (findings.hasFinding(FindingType.ERROR)) {
			RepositoryTestUtils.printFindings( findings );
		}
		assertFalse( findings.hasFinding(FindingType.ERROR) );
		
		// Make sure the project manager automatically loaded previous versions of the library that
		// were not explicitly called out in the project
		assertNotNull( findProjectItem(project, "Version_Test_1_1_1.otm") );
		assertNotNull( findProjectItem(project, "Version_Test_1_1_0.otm") );
		assertNotNull( findProjectItem(project, "Version_Test_1_0_0.otm") );
	}
	
}
