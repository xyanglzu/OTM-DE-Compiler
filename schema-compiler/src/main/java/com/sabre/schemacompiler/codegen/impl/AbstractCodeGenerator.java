/*
 * Copyright (c) 2011, Sabre Inc.
 */
package com.sabre.schemacompiler.codegen.impl;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;

import com.sabre.schemacompiler.codegen.CodeGenerationContext;
import com.sabre.schemacompiler.codegen.CodeGenerationException;
import com.sabre.schemacompiler.codegen.CodeGenerationFilenameBuilder;
import com.sabre.schemacompiler.codegen.CodeGenerationFilter;
import com.sabre.schemacompiler.codegen.CodeGenerator;
import com.sabre.schemacompiler.model.AbstractLibrary;
import com.sabre.schemacompiler.model.ModelElement;
import com.sabre.schemacompiler.util.URLUtils;
import com.sabre.schemacompiler.validate.ValidationException;

/**
 * <code>CodeGenerator</code> base class that provides validation and orchestration functions for
 * the code generation process.
 * 
 * @param <S>  the source type for which output content will be generated
 * @author S. Livezey
 */
public abstract class AbstractCodeGenerator<S extends ModelElement> implements CodeGenerator<S> {
	
	private CodeGenerationFilter filter;
	private CodeGenerationFilenameBuilder<S> filenameBuilder;
	private Map<String,File> generatedFiles = new TreeMap<String,File>();
	protected Logger log = NOPLogger.NOP_LOGGER;
	
	/**
	 * Default constructor.
	 */
	public AbstractCodeGenerator() {
	}
	
	/**
	 * @see com.sabre.schemacompiler.codegen.CodeGenerator#getFilter()
	 */
	@Override
	public CodeGenerationFilter getFilter() {
		return filter;
	}

	/**
	 * @see com.sabre.schemacompiler.codegen.CodeGenerator#setFilter(com.sabre.schemacompiler.codegen.CodeGenerationFilter)
	 */
	@Override
	public void setFilter(CodeGenerationFilter filter) {
		this.filter = filter;
	}

	/**
	 * @see com.sabre.schemacompiler.codegen.CodeGenerator#getFilenameBuilder()
	 */
	@Override
	public CodeGenerationFilenameBuilder<S> getFilenameBuilder() {
		if (filenameBuilder == null) {
			filenameBuilder = getDefaultFilenameBuilder();
			
			if (filenameBuilder == null) {
				throw new IllegalStateException("No default filename builder provided.");
			}
		}
		return filenameBuilder;
	}

	/**
	 * @see com.sabre.schemacompiler.codegen.CodeGenerator#setFilenameBuilder(com.sabre.schemacompiler.codegen.CodeGenerationFilenameBuilder)
	 */
	@Override
	public void setFilenameBuilder(CodeGenerationFilenameBuilder<S> filenameBuilder) {
		if (filenameBuilder != null) {
			this.filenameBuilder = filenameBuilder;
		}
	}
	
	/**
	 * Returns the default filename builder implementation for this code generator.
	 * 
	 * <p>NOTE: The constructor of this abstract class will throw an <code>IllegalArgumentException</code>
	 * if this method returns null.
	 * 
	 * @return CodeGenerationFilenameBuilder
	 */
	protected abstract CodeGenerationFilenameBuilder<S> getDefaultFilenameBuilder();
	
	/**
	 * @see com.sabre.schemacompiler.codegen.CodeGenerator#setLogger(org.slf4j.Logger)
	 */
	@Override
	public void setLogger(Logger log) {
		if (log == null) {
			throw new NullPointerException("Logger cannot be null.");
		}
		this.log = log;
	}
	
	/**
	 * @see com.sabre.schemacompiler.codegen.CodeGenerator#generateOutput(java.lang.Object, com.sabre.schemacompiler.codegen.CodeGenerationContext)
	 */
	@Override
	public Collection<File> generateOutput(S source, CodeGenerationContext context)
			throws ValidationException, CodeGenerationException {
		if (!isSupportedSourceObject(source)) {
			throw new IllegalArgumentException(
					"Model element not supported by " + this.getClass().getSimpleName()
					+ ": " + source.getClass().getName());
		}
		generatedFiles.clear();
		
		if (canGenerateOutput(source, context)) {
			doGenerateOutput(source, context);
		}
		return getGeneratedFiles();
	}

	/**
	 * Performs all tasks necessary to generate the output specified by the 'source' model
	 * element provided.  When calling this method, all generated output is directed to the
	 * <code>OutputStream</code> provided.
	 * 
	 * @param source  the source element that defines the content to be rendered
	 * @param context  the code generation context
	 * @throws CodeGenerationException  thrown if a system or I/O exception occurs during output generation
	 * @throws IllegalArgumentException  thrown if the code generator instance does not support code generation
	 *									 for the type of source element provided
	 */
	public abstract void doGenerateOutput(S source, CodeGenerationContext context) throws CodeGenerationException;
	
	/**
	 * Returns true if the <code>CodeGenerator</code> can proceed with output generation.  By
	 * default, this returns true; sub-classes may override to implement target-specific behavior.
	 * 
	 * @param source  the source object for the output generation
	 * @param context  the code generation context
	 * @return boolean
	 */
	protected boolean canGenerateOutput(S source, CodeGenerationContext context) {
		return true;
	}
	
	/**
	 * Returns true if output generation is supported for the given source object by this
	 * <code>CodeGenerator</code>.
	 *  
	 * @param source  the source object to check
	 * @return boolean
	 */
	protected abstract boolean isSupportedSourceObject(S source);
	
	/**
	 * Returns the library that owns or is associated with the given source object.
	 * 
	 * @param source  the source object for which to return the associated library
	 * @return AbstractLibrary
	 */
	protected abstract AbstractLibrary getLibrary(S source);
	
	/**
	 * Returns the absolute file location of the generated output file, including its folder
	 * location as reported by the 'getOutputFolder()' method.
	 * 
	 * @param source  the source object for the output generation
	 * @param context  the code generation context
	 * @return OutputStream
	 */
	protected abstract File getOutputFile(S source, CodeGenerationContext context);
	
	/**
	 * Returns the output folder to use for file generation.  By default, this method obtains
	 * the location from the <code>"schemacompiler.OutputFolder"</code> key of the
	 * <code>CodeGenerationContext</code>.  Once identified, this method will check for the existence
	 * of the folder and attempt to create it (and any required parents) if it does not yet exist.
	 * 
	 * <p>If the expected context key is not defined or the folder does not exist (and cannot be
	 * created), the folder location of the given library URL will be returned.
	 * 
	 * @param context  the code generator context
	 * @param libraryUrl  the URL of the library whose folder location will be used as the default return value
	 * @return File
	 */
	protected File getOutputFolder(CodeGenerationContext context, URL libraryUrl) {
		String folderPath = context.getValue(CodeGenerationContext.CK_OUTPUT_FOLDER);
		File outputFolder = null;
		
		if (folderPath != null) {
			File folder = new File(folderPath);
			
			if (!folder.exists()) {
				folder.mkdirs();
			}
			if (folder.exists() && folder.isDirectory()) {
				outputFolder = folder;
			}
		}
		if (outputFolder == null) {
			if ((libraryUrl != null) && URLUtils.isFileURL(libraryUrl)) {
				outputFolder = URLUtils.toFile(libraryUrl).getParentFile();
			} else {
				outputFolder = new File(System.getProperty("user.dir"));
			}
		}
		return outputFolder;
	}
	
	/**
	 * Returns the sub-folder location (relative to the target output folder) where built-in schemas should
	 * be stored during the code generation process.  If no sub-folder location is specified by the code
	 * generation context, this method will return null, indicating that built-ins schemas should be saved in
	 * the same target output folder as the user-defined library/service output.
	 * 
	 * @param context  the code generator context
	 * @return String
	 */
	protected String getBuiltInSchemaOutputLocation(CodeGenerationContext context) {
		return context.getValue(CodeGenerationContext.CK_BUILTIN_SCHEMA_FOLDER);
	}
	
	/**
	 * Returns the sub-folder location (relative to the target output folder) where legacy schemas should
	 * be stored during the code generation process.  If no sub-folder location is specified by the code
	 * generation context, this method will return null, indicating that legacy schemas should be saved in
	 * the same target output folder as the user-defined library/service output.
	 * 
	 * @param context  the code generator context
	 * @return String
	 */
	protected String getLegacySchemaOutputLocation(CodeGenerationContext context) {
		return context.getValue(CodeGenerationContext.CK_LEGACY_SCHEMA_FOLDER);
	}
	
	/**
	 * Adds the specified file to the list of artifacts created by this code generator instance.
	 * 
	 * @param file  the generated output file to add
	 */
	protected void addGeneratedFile(File file) {
		String filePath = (file == null) ? null : file.getAbsolutePath();
		
		if ((filePath != null) && !generatedFiles.containsKey(filePath)) {
			generatedFiles.put(filePath, file);
		}
	}
	
	/**
	 * Adds all of the given files to the list of artifacts created by this code generator instance.
	 * 
	 * @param files  the collection of generated output files to add
	 */
	protected void addGeneratedFiles(Collection<File> files) {
		if (files != null) {
			for (File file : files) {
				addGeneratedFile(file);
			}
		}
	}
	
	/**
	 * Returns the collection of all output artifacts created by this code generator instance.
	 * 
	 * @return Collection<File>
	 */
	protected Collection<File> getGeneratedFiles() {
		Collection<File> fileList = new ArrayList<File>();
		
		for (String filePath : generatedFiles.keySet()) {
			fileList.add( generatedFiles.get(filePath) );
		}
		return fileList;
	}
	
}
