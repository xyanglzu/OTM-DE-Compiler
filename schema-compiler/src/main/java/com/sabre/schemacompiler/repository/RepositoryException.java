/*
 * Copyright (c) 2012, Sabre Corporation and affiliates.
 * All Rights Reserved.
 * Use is subject to license agreement.
 */
package com.sabre.schemacompiler.repository;

import com.sabre.schemacompiler.util.SchemaCompilerException;

/**
 * Exception that is thrown when an error occurs while attempting to access or update a
 * <code>Repository</code> instance.
 * 
 * @author S. Livezey
 */
public class RepositoryException extends SchemaCompilerException {
	
	/**
	 * Default constructor.
	 */
	public RepositoryException() {}

	/**
	 * Constructs an exception with the specified message.
	 * 
	 * @param message  the exception message
	 */
	public RepositoryException(String message) {
		super(message);
	}

	/**
	 * Constructs an exception with the specified underlying cause.
	 * 
	 * @param cause  the underlying exception that caused this one
	 */
	public RepositoryException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs an exception with the specified message and underlying cause.
	 * 
	 * @param message  the exception message
	 * @param cause  the underlying exception that caused this one
	 */
	public RepositoryException(String message, Throwable cause) {
		super(message, cause);
	}

}
