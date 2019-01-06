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
package org.opentravel.schemacompiler.loader;

/**
 * Defines the message keys for all error/warning messages that can be generated by the model
 * loader.
 */
public class LoaderConstants {
	
    public static final String LOADER_VALIDATION_PREFIX = "schemacompiler.loader.";
    
	public static final String ERROR_MISSING_NAMESPACE_URI = LOADER_VALIDATION_PREFIX + "MISSING_NAMESPACE_URI";
	public static final String ERROR_INVALID_NAMESPACE_URI = LOADER_VALIDATION_PREFIX + "INVALID_NAMESPACE_URI";
	public static final String ERROR_MISSING_NAMESPACE_URI_ON_IMPORT = LOADER_VALIDATION_PREFIX + "MISSING_NAMESPACE_URI_ON_IMPORT";
	public static final String ERROR_INVALID_NAMESPACE_URI_ON_IMPORT = LOADER_VALIDATION_PREFIX + "INVALID_NAMESPACE_URI_ON_IMPORT";
	public static final String ERROR_NAMESPACE_MISMATCH_ON_INCLUDE = LOADER_VALIDATION_PREFIX + "NAMESPACE_MISMATCH_ON_INCLUDE";
	public static final String ERROR_NAMESPACE_MISMATCH_ON_IMPORT = LOADER_VALIDATION_PREFIX + "NAMESPACE_MISMATCH_ON_IMPORT";
	public static final String ERROR_ILLEGAL_CHAMELEON_IMPORT = LOADER_VALIDATION_PREFIX + "ILLEGAL_CHAMELEON_IMPORT";
	public static final String ERROR_INVALID_URL_ON_INCLUDE = LOADER_VALIDATION_PREFIX + "INVALID_URL_ON_INCLUDE";
	public static final String ERROR_INVALID_PROJECT_ITEM_LOCATION = LOADER_VALIDATION_PREFIX + "INVALID_PROJECT_ITEM_LOCATION";
	public static final String ERROR_PROJECT_ITEM_FILE_NOT_FOUND = LOADER_VALIDATION_PREFIX + "PROJECT_ITEM_FILE_NOT_FOUND";
	public static final String ERROR_UNREADABLE_LIBRARY_CONTENT = LOADER_VALIDATION_PREFIX + "UNREADABLE_LIBRARY_CONTENT";
	public static final String ERROR_UNREADABLE_SCHEMA_CONTENT = LOADER_VALIDATION_PREFIX + "UNREADABLE_SCHEMA_CONTENT";
	public static final String ERROR_UNREADABLE_PROJECT_CONTENT = LOADER_VALIDATION_PREFIX + "UNREADABLE_PROJECT_CONTENT";
	public static final String ERROR_UNREADABLE_RELEASE_CONTENT = LOADER_VALIDATION_PREFIX + "UNREADABLE_RELEASE_CONTENT";
	public static final String ERROR_RELEASE_NOT_FOUND = LOADER_VALIDATION_PREFIX + "ERROR_RELEASE_NOT_FOUND";
	public static final String ERROR_UNREADABLE_ASSEMBLY_CONTENT = LOADER_VALIDATION_PREFIX + "UNREADABLE_ASSEMBLY_CONTENT";
	public static final String ERROR_ASSEMBLY_NOT_FOUND = LOADER_VALIDATION_PREFIX + "ERROR_ASSEMBLY_NOT_FOUND";
	public static final String ERROR_INVALID_REPOSITORY_ITEM = LOADER_VALIDATION_PREFIX + "INVALID_REPOSITORY_ITEM";
	public static final String ERROR_MISSING_CRC = LOADER_VALIDATION_PREFIX + "MISSING_CRC";
	public static final String ERROR_INVALID_CRC = LOADER_VALIDATION_PREFIX + "INVALID_CRC";
	public static final String ERROR_LOADING_FROM_REPOSITORY = LOADER_VALIDATION_PREFIX + "ERROR_LOADING_FROM_REPOSITORY";
	public static final String ERROR_REPOSITORY_UNAVAILABLE = LOADER_VALIDATION_PREFIX + "REPOSITORY_UNAVAILABLE";
	public static final String ERROR_MANAGED_LIBRARY_NAMESPACE_ERROR = LOADER_VALIDATION_PREFIX + "MANAGED_LIBRARY_NAMESPACE_ERROR";
	public static final String ERROR_MISSING_REPOSITORY = LOADER_VALIDATION_PREFIX + "ERROR_MISSING_REPOSITORY";
	public static final String ERROR_UNKNOWN_EXCEPTION_DURING_PROJECT_LOAD = LOADER_VALIDATION_PREFIX + "UNKNOWN_EXCEPTION_DURING_PROJECT_LOAD";
	public static final String ERROR_UNKNOWN_EXCEPTION_DURING_MODULE_LOAD = LOADER_VALIDATION_PREFIX + "UNKNOWN_EXCEPTION_DURING_MODULE_LOAD";
	public static final String ERROR_UNKNOWN_EXCEPTION_DURING_TRANSFORMATION = LOADER_VALIDATION_PREFIX + "UNKNOWN_EXCEPTION_DURING_TRANSFORMATION";
	public static final String ERROR_UNKNOWN_EXCEPTION_DURING_VALIDATION = LOADER_VALIDATION_PREFIX + "UNKNOWN_EXCEPTION_DURING_VALIDATION";
	public static final String WARNING_UNRESOLVED_LIBRARY_NAMESPACE = LOADER_VALIDATION_PREFIX + "UNRESOLVED_LIBRARY_NAMESPACE";
	public static final String WARNING_CORRUPT_LIBRARY_CONTENT = LOADER_VALIDATION_PREFIX + "CORRUPT_LIBRARY_CONTENT";
	public static final String WARNING_LIBRARY_NOT_FOUND = LOADER_VALIDATION_PREFIX + "WARNING_LIBRARY_NOT_FOUND";
	public static final String WARNING_SCHEMA_NOT_FOUND = LOADER_VALIDATION_PREFIX + "WARNING_SCHEMA_NOT_FOUND";
	public static final String WARNING_PROJECT_NOT_FOUND = LOADER_VALIDATION_PREFIX + "WARNING_PROJECT_NOT_FOUND";
	public static final String WARNING_DUPLICATE_LIBRARY = LOADER_VALIDATION_PREFIX + "DUPLICATE_LIBRARY";
	public static final String WARNING_DUPLICATE_SCHEMA = LOADER_VALIDATION_PREFIX + "DUPLICATE_SCHEMA";
	public static final String WARNING_MANAGED_LIBRARY_NAMESPACE_MISMATCH = LOADER_VALIDATION_PREFIX + "MANAGED_LIBRARY_NAMESPACE_MISMATCH";
	public static final String WARNING_INVALID_REPOSITORY_URL = LOADER_VALIDATION_PREFIX + "INVALID_REPOSITORY_URL";

	/**
	 * Private constructor to prevent instantiation.
	 */
	private LoaderConstants() {}
	
}
