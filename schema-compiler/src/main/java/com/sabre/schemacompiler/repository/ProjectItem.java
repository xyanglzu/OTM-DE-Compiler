/*
 * Copyright (c) 2012, Sabre Corporation and affiliates.
 * All Rights Reserved.
 * Use is subject to license agreement.
 */
package com.sabre.schemacompiler.repository;

import java.util.List;

import com.sabre.schemacompiler.model.AbstractLibrary;

/**
 * Represents a <code>RepositoryItem</code> component that is accessible from a local model project.
 * 
 * @author S. Livezey
 */
public interface ProjectItem extends RepositoryItem {
	
	/**
	 * Returns the <code>ProjectManager</code> that owns this item.
	 * 
	 * @return ProjectManager
	 */
	public ProjectManager getProjectManager();
	
	/**
	 * Returns the list of projects of which this <code>ProjectItem</code> is a member.
	 * 
	 * @return List<Project>
	 */
	public List<Project> memberOfProjects();
	
	/**
	 * Returns the library content of this repository item.
	 * 
	 * @return AbstractLibrary
	 */
	public AbstractLibrary getContent();
	
	/**
	 * Returns true if the project item's content is to be considered read-only by an editor application.
	 * 
	 * @return boolean
	 */
	public boolean isReadOnly();
	
}
