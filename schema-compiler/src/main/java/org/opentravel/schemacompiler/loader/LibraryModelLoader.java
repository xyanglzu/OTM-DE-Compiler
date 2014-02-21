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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;

import org.opentravel.schemacompiler.ioc.SchemaCompilerApplicationContext;
import org.opentravel.schemacompiler.loader.impl.DefaultLibraryNamespaceResolver;
import org.opentravel.schemacompiler.loader.impl.LibraryValidationSource;
import org.opentravel.schemacompiler.loader.impl.MultiVersionLibraryModuleLoader;
import org.opentravel.schemacompiler.loader.impl.URLValidationSource;
import org.opentravel.schemacompiler.model.AbstractLibrary;
import org.opentravel.schemacompiler.model.BuiltInLibrary;
import org.opentravel.schemacompiler.model.LibraryMember;
import org.opentravel.schemacompiler.model.TLLibrary;
import org.opentravel.schemacompiler.model.TLModel;
import org.opentravel.schemacompiler.model.XSDComplexType;
import org.opentravel.schemacompiler.model.XSDElement;
import org.opentravel.schemacompiler.model.XSDLibrary;
import org.opentravel.schemacompiler.security.LibraryCrcCalculator;
import org.opentravel.schemacompiler.transform.ObjectTransformer;
import org.opentravel.schemacompiler.transform.TransformerFactory;
import org.opentravel.schemacompiler.transform.symbols.DefaultTransformerContext;
import org.opentravel.schemacompiler.transform.util.EntityReferenceResolutionVisitor;
import org.opentravel.schemacompiler.util.ExceptionUtils;
import org.opentravel.schemacompiler.util.URLUtils;
import org.opentravel.schemacompiler.validate.FindingType;
import org.opentravel.schemacompiler.validate.Validatable;
import org.opentravel.schemacompiler.validate.ValidationFinding;
import org.opentravel.schemacompiler.validate.ValidationFindings;
import org.opentravel.schemacompiler.validate.compile.TLModelCompileValidator;
import org.opentravel.schemacompiler.version.VersionScheme;
import org.opentravel.schemacompiler.version.VersionSchemeException;
import org.opentravel.schemacompiler.version.VersionSchemeFactory;
import org.opentravel.schemacompiler.visitor.ModelNavigator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3._2001.xmlschema.Schema;
import org.w3._2001.xmlschema.TopLevelElement;

/**
 * Handles the loading and parsing of a library model and all referenced modules that are imported.
 * The resulting data structure that is returned is a map of library namespaces to the JAXB
 * representations of the library modules themselves.
 * 
 * <p>
 * By default, the model loader employs an implementation of the <code>LibraryModuleLoader</code>
 * component that uses XML input streams to obtain library content. The default
 * <code>LibraryNamespaceResolver</code> is a <code>CatalogLibraryNamespaceResolver</code> whose
 * catalog file is expected at the default location.
 * 
 * @param <C>
 *            the content type must be returned by the input source used by the module loader
 * @author S. Livezey
 */
public final class LibraryModelLoader<C> implements LoaderValidationMessageKeys {

    private static final Logger log = LoggerFactory.getLogger(LibraryModelLoader.class);

    /** Internal indicator used to define the possible types of loader operations. */
    private static enum OperationType {
        CLIENT_REQUESTED, INCLUDE, IMPORT
    };

    private LibraryNamespaceResolver namespaceResolver = new DefaultLibraryNamespaceResolver();
    private LibraryModuleLoader<C> moduleLoader;
    private boolean resolveModelReferences = true;
    private TLModel libraryModel;

    private ValidationFindings loaderFindings = new ValidationFindings();
    private Map<String, ValidationFinding> importLoaderFindings = new HashMap<String, ValidationFinding>();

    /**
     * Default constructor that creates a new <code>TLModel</code> instance to be populated by the
     * loader.
     * 
     * @throws LibraryLoaderException
     *             thrown if a system-level exception occurs
     */
    public LibraryModelLoader() throws LibraryLoaderException {
        this(null);
    }

    /**
     * Default constructor that assigns an existing <code>TLModel</code> instance to be populated by
     * the loader.
     * 
     * @throws LibraryLoaderException
     *             thrown if a system-level exception occurs
     */
    public LibraryModelLoader(TLModel libraryModel) throws LibraryLoaderException {
        this.libraryModel = (libraryModel == null) ? new TLModel() : libraryModel;
        this.namespaceResolver.setModel(libraryModel);
        initializeDefaultModuleLoader();
    }

    /**
     * Returns the library model instance that was (or is to be) populated by this loader.
     * 
     * @return TLModel
     */
    public TLModel getLibraryModel() {
        return libraryModel;
    }

    /**
     * Returns a sub-set of the given validation findings that only includes findings from the
     * loader. The findings that are returned cannot be reproduced by a normal validation of a
     * <code>TLModel</code>.
     * 
     * @param findings
     *            the original set of validation findings to process
     * @return ValidationFindings
     */
    public static ValidationFindings filterLoaderFindings(ValidationFindings findings) {
        ValidationFindings loaderFindings = new ValidationFindings();

        if (findings != null) {
            for (ValidationFinding finding : findings.getAllFindingsAsList()) {
                String messageKey = finding.getMessageKey();

                if ((messageKey != null) && messageKey.startsWith(LOADER_VALIDATION_PREFIX)) {
                    loaderFindings.addFinding(finding.getType(), finding.getSource(), messageKey,
                            finding.getMessageParams());
                }
            }
        }
        return loaderFindings;
    }

    /**
     * Assigns the namespace resolver to be used for mapping library module namespaces to the
     * resource locations where those libraries can be retrieved.
     * 
     * @param namespaceResolver
     *            the namespace resolver to assign
     * @throws LibraryLoaderException
     *             thrown if a system-level exception occurs
     */
    public void setNamespaceResolver(LibraryNamespaceResolver namespaceResolver)
            throws LibraryLoaderException {
        this.namespaceResolver = namespaceResolver;
        this.namespaceResolver.setModel(libraryModel);
    }

    /**
     * Assigns the library module loader for this model loader.
     * 
     * @param libraryModuleLoader
     *            the module loader instance
     */
    public void setModuleLoader(LibraryModuleLoader<C> moduleLoader) {
        this.moduleLoader = moduleLoader;
    }

    /**
     * Returns the flag indicating whether inter-library model references should be resolved upon
     * completion of a library loading operation.
     * 
     * @return boolean
     */
    public boolean isResolveModelReferences() {
        return resolveModelReferences;
    }

    /**
     * Assigns the flag indicating whether inter-library model references should be resolved upon
     * completion of a library loading operation. By default, this value is set to true. Some
     * clients may choose to disable the automatic resolution of references as a performance
     * enhancement if multiple libraries are to be loaded in sequence. If disabled, callers can
     * invoke the resolution manually by calling the 'resolveModelEntityReferences()' method after
     * all library loads are complete.
     * 
     * @param resolveModelReferences
     */
    public void setResolveModelReferences(boolean resolveModelReferences) {
        this.resolveModelReferences = resolveModelReferences;
    }

    /**
     * Loads the library with the specified namespace and all dependent library modules.
     * 
     * @param libraryNamespace
     *            the namespace of the primary library module to load
     * @return ValidationFindings
     * @throws LibraryLoaderException
     *             thrown if a system-level exception occurs
     */
    public ValidationFindings loadLibraryModel(URI libraryNamespace) throws LibraryLoaderException {
        boolean listenerFlag = libraryModel.isListenersEnabled();
        try {
            Collection<LibraryInputSource<C>> namespaceInputSources = getInputSources(
                    libraryNamespace, null, (String) null);
            JAXBModelArtifacts jaxbArtifacts = new JAXBModelArtifacts();

            libraryModel.setListenersEnabled(false);

            for (LibraryInputSource<C> nsInputSource : namespaceInputSources) {
                loadModuleAndDependencies(nsInputSource, libraryNamespace.toString(),
                        OperationType.CLIENT_REQUESTED, jaxbArtifacts);
            }
            incorporateJaxbSchemas(jaxbArtifacts);
            incorporateJaxbLibraries(jaxbArtifacts);

            // Examine all entity references within the model and attempt to resolve them
            // before validating the model
            if (resolveModelReferences) {
                resolveModelEntityReferences();
            }
            validateModel(jaxbArtifacts);

            jaxbArtifacts.validationFindings.addAll(getLoaderFindings());
            return jaxbArtifacts.validationFindings;

        } finally {
            libraryModel.setListenersEnabled(listenerFlag);
        }
    }

    /**
     * Loads the library from the input source provided, as well as all dependent modules.
     * 
     * @param inputSource
     *            the input source for the primary library module to load
     * @return ValidationFindings
     * @throws LibraryLoaderException
     *             thrown if a system-level exception occurs
     */
    public ValidationFindings loadLibraryModel(LibraryInputSource<C> inputSource)
            throws LibraryLoaderException {
        boolean listenerFlag = libraryModel.isListenersEnabled();
        try {
            JAXBModelArtifacts jaxbArtifacts = new JAXBModelArtifacts();

            libraryModel.setListenersEnabled(false);
            loadModuleAndDependencies(inputSource, null, OperationType.CLIENT_REQUESTED,
                    jaxbArtifacts);

            // Build a list of namespaces for the modules we just loaded
            Collection<String> namespaces = new HashSet<String>();

            for (LibraryModuleInfo<Object> libraryInfo : jaxbArtifacts.libraryUrlMappings.values()) {
                namespaces.add(libraryInfo.getNamespace());
            }
            for (LibraryModuleInfo<Schema> schemaInfo : jaxbArtifacts.schemaUrlMappings.values()) {
                namespaces.add(schemaInfo.getJaxbArtifact().getTargetNamespace());
            }

            // Search the catalog (if one exists) to find any other dependencies that need to be
            // loaded
            for (String namespace : namespaces) {
                try {
                    if (namespace != null) { // ignore chameleon schemas

                    }
                    Collection<LibraryInputSource<C>> namespaceInputSources = getInputSources(
                            (namespace == null) ? null : new URI(namespace), null, (String) null);

                    for (LibraryInputSource<C> nsInputSource : namespaceInputSources) {
                        loadModuleAndDependencies(nsInputSource, namespace,
                                OperationType.CLIENT_REQUESTED, jaxbArtifacts);
                    }
                } catch (URISyntaxException e) {
                    addLoaderFinding(FindingType.ERROR,
                            new URLValidationSource(inputSource.getLibraryURL()),
                            ERROR_INVALID_NAMESPACE_URI_ON_IMPORT, namespace);
                    log.debug("Invalid namespace URI on import: " + namespace, e);
                }
            }

            // Incorporate the JAXB artifacts into the model
            incorporateJaxbSchemas(jaxbArtifacts);
            incorporateJaxbLibraries(jaxbArtifacts);

            // Examine all entity references within the model and attempt to resolve them
            // before validating the model
            if (resolveModelReferences) {
                resolveModelEntityReferences();
            }
            validateModel(jaxbArtifacts);

            jaxbArtifacts.validationFindings.addAll(getLoaderFindings());
            return jaxbArtifacts.validationFindings;

        } finally {
            libraryModel.setListenersEnabled(listenerFlag);
        }
    }

    /**
     * Returns the list of loader (non-validation) findings that have been discovered since the
     * model loader instance was created. In other words, loader findings are cumulative across
     * multiple calls to the 'loadLibraryModel()' methods.
     * 
     * @return ValidationFindings
     */
    public ValidationFindings getLoaderFindings() {
        ValidationFindings findings = new ValidationFindings();

        // Add the namespace/file import findings that are still valid
        Set<String> validLibraryKeys = new HashSet<String>();

        for (AbstractLibrary library : libraryModel.getAllLibraries()) {
            if (library instanceof BuiltInLibrary)
                continue;
            validLibraryKeys.add(new URLValidationSource(library.getLibraryUrl())
                    .getValidationIdentity());

        }
        for (String importFindingKey : importLoaderFindings.keySet()) {
            if (!validLibraryKeys.contains(importFindingKey)) {
                findings.addFinding(importLoaderFindings.get(importFindingKey));
            }
        }

        // Add the non-import loader findings as-is
        findings.addAll(loaderFindings);

        return findings;
    }

    /**
     * Scans the contents of the current model and automatically assigns any entity references that
     * were not explicitly assigned during the loading process. Typically, this call is invoked
     * automatically, but may be manually called by clients if the 'resolveModelReferences' flag is
     * set to false.
     */
    public void resolveModelEntityReferences() {
        boolean listenerFlag = libraryModel.isListenersEnabled();
        try {
            libraryModel.setListenersEnabled(false);
            ModelNavigator.navigate(libraryModel,
                    new EntityReferenceResolutionVisitor(libraryModel));

        } finally {
            libraryModel.setListenersEnabled(listenerFlag);
        }
    }

    /**
     * Recursive method that loads the library at the specified URL and all of its dependent
     * modules.
     * 
     * @param inputSource
     *            an input source for the library to be loaded
     * @param expectedNamespace
     *            the namespace to which the loaded library must be assigned
     * @param operationType
     *            the type of operation currently being processed by the loader
     * @param jaxbArtifacts
     *            intermediate storage for all JAXB model elements
     * @throws LibraryLoaderException
     *             thrown if a system-level exception occurs
     */
    private void loadModuleAndDependencies(LibraryInputSource<C> inputSource,
            String expectedNamespace, OperationType operationType, JAXBModelArtifacts jaxbArtifacts)
            throws LibraryLoaderException {
        URL libraryUrl = inputSource.getLibraryURL();

        if (!libraryModel.hasLibrary(libraryUrl)
                && !jaxbArtifacts.libraryUrlMappings.containsKey(libraryUrl)
                && !jaxbArtifacts.schemaUrlMappings.containsKey(libraryUrl)) {
            if (moduleLoader.isLibraryInputSource(inputSource)) {
                loadLibraryAndDependencies(inputSource, expectedNamespace, operationType,
                        jaxbArtifacts);

            } else { // input source for an XML schema
                loadSchemaAndDependencies(inputSource, expectedNamespace, operationType,
                        jaxbArtifacts);
            }
        }
    }

    /**
     * Recursive method that loads the library at the specified URL and all of its dependent
     * modules.
     * 
     * @param inputSource
     *            an input source for the library to be loaded
     * @param expectedNamespace
     *            the namespace to which the loaded library must be assigned
     * @param operationType
     *            the type of operation currently being processed by the loader
     * @param jaxbArtifacts
     *            intermediate storage for all JAXB model elements
     * @throws LibraryLoaderException
     *             thrown if a system-level exception occurs
     */
    private void loadLibraryAndDependencies(LibraryInputSource<C> inputSource,
            String expectedNamespace, OperationType operationType, JAXBModelArtifacts jaxbArtifacts)
            throws LibraryLoaderException {
        ValidationFindings moduleFindings = new ValidationFindings();
        LibraryModuleInfo<Object> libraryInfo = moduleLoader.loadLibrary(inputSource,
                moduleFindings);

        addLoaderFindings(moduleFindings);

        if (libraryInfo != null) {
            Object library = libraryInfo.getJaxbArtifact();
            URL libraryUrl = inputSource.getLibraryURL();

            // Verify that the namespace of the module we are adding matches the expectations
            if ((expectedNamespace != null)
                    && !expectedNamespace.equals(libraryInfo.getNamespace())
                    && !(operationType == OperationType.CLIENT_REQUESTED)
                    && !isAllowedNamespaceVariation(expectedNamespace, libraryInfo.getNamespace(),
                            libraryInfo.getVersionScheme())) {
                String errorKey = (operationType == OperationType.INCLUDE) ? ERROR_NAMESPACE_MISMATCH_ON_INCLUDE
                        : ERROR_NAMESPACE_MISMATCH_ON_IMPORT;

                addLoaderFinding(FindingType.ERROR, new LibraryValidationSource(library), errorKey,
                        expectedNamespace, libraryInfo.getNamespace());
                return; // return without adding the module to our JAXB artifacts
            }

            // Do not import/include a library that has already been loaded
            if (jaxbArtifacts.libraryUrlMappings.containsKey(libraryUrl)) {
                return;
            }

            jaxbArtifacts.addLibraryModule(libraryUrl, libraryInfo);

            // First, resolve includes from the library's local namespace
            for (String include : libraryInfo.getIncludes()) {
                try {
                    namespaceResolver.setContextLibrary(libraryInfo, libraryUrl);

                    URI libraryNamespace = new URI(libraryInfo.getNamespace());
                    LibraryInputSource<C> includeInputSource = moduleLoader
                            .newInputSource(namespaceResolver.resovleLibraryInclude(
                                    libraryNamespace, include));

                    if (includeInputSource != null) {
                        loadModuleAndDependencies(includeInputSource, libraryInfo.getNamespace(),
                                OperationType.INCLUDE, jaxbArtifacts);
                    }
                } catch (URISyntaxException e) {
                    addLoaderFinding(FindingType.ERROR, new LibraryValidationSource(library),
                            ERROR_INVALID_URL_ON_INCLUDE, include);
                }
            }

            // Identify and load explicit imports from other namespaces
            for (LibraryModuleImport nsImport : libraryInfo.getImports()) {
                try {
                    if ((nsImport.getNamespace() != null) && (nsImport.getNamespace().length() > 0)) {
                        if (!isBuiltInNamespace(nsImport.getNamespace())) {
                            namespaceResolver.setContextLibrary(libraryInfo, libraryUrl);

                            URI libraryNamespace = new URI(nsImport.getNamespace());
                            Collection<LibraryInputSource<C>> dlInputSources = getInputSources(
                                    libraryNamespace, libraryInfo.getVersionScheme(),
                                    nsImport.getFileHints());

                            if (!dlInputSources.isEmpty()) {
                                for (LibraryInputSource<C> dlInputSource : dlInputSources) {
                                    loadModuleAndDependencies(dlInputSource,
                                            nsImport.getNamespace(), OperationType.IMPORT,
                                            jaxbArtifacts);
                                }
                            } else {
                                addLoaderFinding(FindingType.WARNING, new LibraryValidationSource(
                                        library), WARNING_UNRESOLVED_LIBRARY_NAMESPACE,
                                        nsImport.getNamespace());
                            }
                        }
                    } else {
                        addLoaderFinding(FindingType.ERROR, new LibraryValidationSource(library),
                                ERROR_MISSING_NAMESPACE_URI_ON_IMPORT, libraryInfo.getLibraryName());
                    }
                } catch (URISyntaxException e) {
                    addLoaderFinding(FindingType.ERROR, new LibraryValidationSource(library),
                            ERROR_INVALID_NAMESPACE_URI_ON_IMPORT, nsImport.getNamespace());
                    log.debug("Invalid namespace URI on import: " + libraryInfo.getNamespace(), e);
                } catch (Throwable t) {
                    addLoaderFinding(FindingType.ERROR, new LibraryValidationSource(library),
                            ERROR_UNKNOWN_EXCEPTION_DURING_MODULE_LOAD,
                            libraryInfo.getLibraryName(), ExceptionUtils.getExceptionClass(t)
                                    .getSimpleName(), ExceptionUtils.getExceptionMessage(t));
                    log.debug(
                            "Unexpected exception loading liberary module: "
                                    + libraryInfo.getLibraryName(), t);
                }
            }

            // Identify previous versions of the library that are required but were not directly
            // loaded by import declarations
            try {
                VersionScheme vScheme = VersionSchemeFactory.getInstance().getVersionScheme(
                        libraryInfo.getVersionScheme());
                List<String> versionChain = vScheme
                        .getMajorVersionChain(libraryInfo.getNamespace());

                // This loop makes a best-effort to identify the library version immediately prior
                // to the
                // current one. There is no need to identify all previous versions, because they
                // will be
                // discovered during the recursive loading process of the previous version.
                for (int i = 1; i < versionChain.size(); i++) { // skip the first element since it
                                                                // represents the current library's
                                                                // version
                    String versionNS = versionChain.get(i);

                    if (jaxbArtifacts.hasLibrary(versionNS, libraryInfo.getLibraryName())) {
                        break; // the previous version was already loaded using explicit import
                               // declarations
                    }
                    try {
                        Collection<LibraryInputSource<C>> versionInputSources = getInputSources(
                                new URI(versionNS), libraryInfo.getVersionScheme(),
                                vScheme.getDefaultFileHint(versionNS, libraryInfo.getLibraryName()));

                        if (!versionInputSources.isEmpty()) {
                            for (LibraryInputSource<C> dlInputSource : versionInputSources) {
                                loadModuleAndDependencies(dlInputSource, versionNS,
                                        OperationType.IMPORT, jaxbArtifacts);
                            }
                            break; // stop after loading the version immediately prior to the
                                   // current library (recursion will
                                   // handle all of the prior versions in the chain).
                        }

                    } catch (URISyntaxException e) {
                        addLoaderFinding(FindingType.ERROR, new LibraryValidationSource(library),
                                ERROR_INVALID_NAMESPACE_URI_ON_IMPORT, versionNS);
                        log.debug("Invalid namespace URI on import: " + libraryInfo.getNamespace(),
                                e);
                    } catch (Throwable t) {
                        addLoaderFinding(FindingType.ERROR, new LibraryValidationSource(library),
                                ERROR_UNKNOWN_EXCEPTION_DURING_MODULE_LOAD,
                                libraryInfo.getLibraryName(), ExceptionUtils.getExceptionClass(t)
                                        .getSimpleName(), ExceptionUtils.getExceptionMessage(t));
                        log.debug(
                                "Unexpected exception loading liberary module: "
                                        + libraryInfo.getLibraryName(), t);
                    }
                }
            } catch (VersionSchemeException e) {
                // Ignore error and continue with the load
            }
        }
    }

    /**
     * Recursive method that loads the XML schema at the specified URL and all of its dependent
     * modules.
     * 
     * @param inputSource
     *            an input source for the XML schema to be loaded
     * @param expectedNamespace
     *            the namespace to which the loaded library must be assigned
     * @param operationType
     *            the type of operation currently being processed by the loader
     * @param jaxbArtifacts
     *            intermediate storage for all JAXB model elements
     * @throws LibraryLoaderException
     *             thrown if a system-level exception occurs
     */
    private void loadSchemaAndDependencies(LibraryInputSource<C> inputSource,
            String expectedNamespace, OperationType operationType, JAXBModelArtifacts jaxbArtifacts)
            throws LibraryLoaderException {
        ValidationFindings moduleFindings = new ValidationFindings();
        LibraryModuleInfo<Schema> schemaInfo = moduleLoader.loadSchema(inputSource, moduleFindings);

        addLoaderFindings(moduleFindings);

        if (schemaInfo != null) {
            Schema schema = schemaInfo.getJaxbArtifact();
            URL schemaUrl = inputSource.getLibraryURL();
            URL folderUrl = URLUtils.getParentURL(schemaUrl);

            if (expectedNamespace != null) {
                // Verify that the namespace of the module we are adding matches the expectations
                boolean namespaceMatches = (expectedNamespace == null)
                        || expectedNamespace.equals(schema.getTargetNamespace());
                boolean chameleonInclude = (operationType == OperationType.INCLUDE)
                        && (schema.getTargetNamespace() == null);
                boolean chameleonImport = (operationType == OperationType.IMPORT)
                        && (schema.getTargetNamespace() == null);

                if (chameleonImport) {
                    addLoaderFinding(FindingType.ERROR, new URLValidationSource(schemaUrl),
                            ERROR_ILLEGAL_CHAMELEON_IMPORT, expectedNamespace);

                } else if ((operationType != OperationType.CLIENT_REQUESTED) && !namespaceMatches
                        && !chameleonInclude) {
                    String errorKey = (operationType == OperationType.INCLUDE) ? ERROR_NAMESPACE_MISMATCH_ON_INCLUDE
                            : ERROR_NAMESPACE_MISMATCH_ON_IMPORT;

                    addLoaderFinding(FindingType.ERROR, new URLValidationSource(schemaUrl),
                            errorKey, expectedNamespace, schema.getTargetNamespace());
                    return; // return without adding the module to our JAXB artifacts
                }
            }

            // Do not import/include a library that has already been loaded
            if (jaxbArtifacts.schemaUrlMappings.containsKey(schemaUrl)) {
                return;
            }

            jaxbArtifacts.addSchemaModule(schemaUrl, schemaInfo);

            // First, resolve includes from the schema's local namespace
            for (String include : schemaInfo.getIncludes()) {
                try {
                    namespaceResolver.setContextSchema(schemaInfo, schemaUrl);

                    URL includeUrl = URLUtils.getResolvedURL(include, folderUrl);
                    LibraryInputSource<C> includeInputSource = moduleLoader
                            .newInputSource(includeUrl);

                    loadModuleAndDependencies(includeInputSource, schema.getTargetNamespace(),
                            OperationType.INCLUDE, jaxbArtifacts);

                } catch (MalformedURLException e) {
                    addLoaderFinding(FindingType.ERROR, new URLValidationSource(schemaUrl),
                            ERROR_INVALID_URL_ON_INCLUDE, include);
                }
            }

            // Next, resolve imports from other namespaces
            for (LibraryModuleImport nsImport : schemaInfo.getImports()) {
                try {
                    if ((nsImport.getNamespace() != null) && (nsImport.getNamespace().length() > 0)) {
                        if (!isBuiltInNamespace(nsImport.getNamespace())) {
                            namespaceResolver.setContextSchema(schemaInfo, schemaUrl);

                            URI libraryNamespace = new URI(nsImport.getNamespace());
                            Collection<LibraryInputSource<C>> dlInputSources = getInputSources(
                                    libraryNamespace, null, nsImport.getFileHints());

                            if (!dlInputSources.isEmpty()) {
                                for (LibraryInputSource<C> dlInputSource : dlInputSources) {
                                    loadModuleAndDependencies(dlInputSource,
                                            nsImport.getNamespace(), OperationType.IMPORT,
                                            jaxbArtifacts);
                                }
                            } else {
                                addLoaderFinding(FindingType.WARNING, new URLValidationSource(
                                        schemaUrl), WARNING_UNRESOLVED_LIBRARY_NAMESPACE,
                                        nsImport.getNamespace());
                            }
                        }
                    } else {
                        addLoaderFinding(FindingType.ERROR, new URLValidationSource(schemaUrl),
                                ERROR_MISSING_NAMESPACE_URI_ON_IMPORT,
                                URLUtils.getShortRepresentation(schemaUrl));
                    }
                } catch (URISyntaxException e) {
                    addLoaderFinding(FindingType.ERROR, new URLValidationSource(schemaUrl),
                            ERROR_INVALID_NAMESPACE_URI_ON_IMPORT, nsImport.getNamespace());
                    log.debug("Invalid namespace URI on import: " + schema.getTargetNamespace(), e);
                } catch (Throwable t) {
                    addLoaderFinding(FindingType.ERROR, new URLValidationSource(schemaUrl),
                            ERROR_UNKNOWN_EXCEPTION_DURING_MODULE_LOAD,
                            URLUtils.getShortRepresentation(schemaUrl), ExceptionUtils
                                    .getExceptionClass(t).getSimpleName(),
                            ExceptionUtils.getExceptionMessage(t));
                    log.debug(
                            "Unexpected exception loading schema module: "
                                    + URLUtils.getShortRepresentation(schemaUrl), t);
                }
            }
        }
    }

    /**
     * Transforms each of the JAXB schema artifacts provided and incorporates them into the current
     * model. The contents of the model are not validated by this method.
     * 
     * @param jaxbArtifacts
     *            the JAXB schema modules to transform and incorporate
     */
    private void incorporateJaxbSchemas(JAXBModelArtifacts jaxbArtifacts) {
        DefaultTransformerContext transformContext = new DefaultTransformerContext();
        TransformerFactory<DefaultTransformerContext> transformerFactory = TransformerFactory
                .getInstance(SchemaCompilerApplicationContext.LOADER_TRANSFORMER_FACTORY,
                        transformContext);

        // Transform each of the JAXB schemas into an XSDLibrary and add it to the model
        for (URL schemaUrl : jaxbArtifacts.schemaUrlMappings.keySet()) {
            LibraryModuleInfo<Schema> schemaInfo = jaxbArtifacts.schemaUrlMappings.get(schemaUrl);

            try {
                ObjectTransformer<Schema, XSDLibrary, DefaultTransformerContext> transformer = transformerFactory
                        .getTransformer(schemaInfo.getJaxbArtifact(), XSDLibrary.class);

                XSDLibrary modelLibrary = transformer.transform(schemaInfo.getJaxbArtifact());

                modelLibrary.setLibraryUrl(schemaUrl);

                // Only add the schema to the model if a duplicate does not already exist
                if (!libraryModel.hasLibrary(modelLibrary.getNamespace(), modelLibrary.getName())) {
                    libraryModel.addLibrary(modelLibrary);

                } else {
                    addLoaderFinding(FindingType.WARNING, new URLValidationSource(schemaUrl),
                            WARNING_DUPLICATE_SCHEMA, URLUtils.getShortRepresentation(schemaUrl));
                }

            } catch (Throwable t) {
                addLoaderFinding(FindingType.ERROR, new URLValidationSource(schemaUrl),
                        ERROR_UNKNOWN_EXCEPTION_DURING_TRANSFORMATION,
                        URLUtils.getShortRepresentation(schemaUrl), ExceptionUtils
                                .getExceptionClass(t).getSimpleName(),
                        ExceptionUtils.getExceptionMessage(t));
                log.debug(
                        "Unexpected exception during library transformation: "
                                + URLUtils.getShortRepresentation(schemaUrl), t);
            }
        }
        resolveLegacySchemaAliases();
    }

    /**
     * Transforms each of the JAXB library module artifacts provided, validates them, and
     * incorporates them into the current model.
     * 
     * <p>
     * <b>IMPORTANT NOTE</b>: The JAXB elements will be incorporated into the model, regardless of
     * whether errors and/or warnings are detected during the validation process. It is up to the
     * caller to determine whether or not to proceed based on the contents of the validation
     * findings.
     * 
     * @param jaxbArtifacts
     *            the JAXB library modules to transform and incorporate
     */
    private void incorporateJaxbLibraries(JAXBModelArtifacts jaxbArtifacts) {
        DefaultTransformerContext transformContext = new DefaultTransformerContext();
        TransformerFactory<DefaultTransformerContext> transformerFactory = TransformerFactory
                .getInstance(SchemaCompilerApplicationContext.LOADER_TRANSFORMER_FACTORY,
                        transformContext);

        // Transform each of the JAXB libraries into a TLLibrary and add it to the model
        for (URL libraryUrl : jaxbArtifacts.libraryUrlMappings.keySet()) {
            LibraryModuleInfo<Object> libraryInfo = jaxbArtifacts.libraryUrlMappings
                    .get(libraryUrl);

            try {
                ObjectTransformer<Object, TLLibrary, DefaultTransformerContext> transformer = transformerFactory
                        .getTransformer(libraryInfo.getJaxbArtifact(), TLLibrary.class);
                TLLibrary modelLibrary = transformer.transform(libraryInfo.getJaxbArtifact());

                modelLibrary.setLibraryUrl(libraryUrl);

                // If a CRC was specified in the library, we need to verify it before incorporating
                // the new library into the model
                Long libraryCrcValue = LibraryCrcCalculator.getLibraryCrcValue(libraryInfo
                        .getJaxbArtifact());
                boolean isValidCrc = true;

                if ((libraryCrcValue != null) || LibraryCrcCalculator.isCrcRequired(modelLibrary)) {
                    if (libraryCrcValue == null) {
                        addLoaderFinding(FindingType.ERROR, new URLValidationSource(libraryUrl),
                                ERROR_MISSING_CRC);
                        isValidCrc = false; // CRC is required, but not provided by the library

                    } else {
                        long calculatedCrc = LibraryCrcCalculator.calculate(modelLibrary);

                        if (libraryCrcValue.longValue() != calculatedCrc) {
                            addLoaderFinding(FindingType.ERROR,
                                    new URLValidationSource(libraryUrl), ERROR_INVALID_CRC);
                            isValidCrc = false; // CRC is present, but does not match the calculated
                                                // value
                        }
                    }
                }

                if (isValidCrc) {
                    // Only add the library to the model if a duplicate does not already exist
                    if (!libraryModel.hasLibrary(modelLibrary.getNamespace(),
                            modelLibrary.getName())) {
                        libraryModel.addLibrary(modelLibrary);

                    } else {
                        addLoaderFinding(FindingType.WARNING, new URLValidationSource(libraryUrl),
                                WARNING_DUPLICATE_LIBRARY,
                                URLUtils.getShortRepresentation(libraryUrl));
                    }
                }

            } catch (Throwable t) {
                addLoaderFinding(FindingType.ERROR,
                        new LibraryValidationSource(libraryInfo.getJaxbArtifact()),
                        ERROR_UNKNOWN_EXCEPTION_DURING_TRANSFORMATION,
                        libraryInfo.getLibraryName(), ExceptionUtils.getExceptionClass(t)
                                .getSimpleName(), ExceptionUtils.getExceptionMessage(t));
                log.debug(
                        "Unexpected exception during library transformation: "
                                + libraryInfo.getLibraryName(), t);
            }
        }
    }

    /**
     * Completes the loading process by validating the model and capturing any errors/warnings that
     * exist in the model.
     * 
     * @param jaxbArtifacts
     *            the JAXB artifacts where the validation findings are to be stored
     */
    private void validateModel(JAXBModelArtifacts jaxbArtifacts) {
        // Validate each user-defined library of the model we just assembled
        for (TLLibrary library : libraryModel.getUserDefinedLibraries()) {
            try {
                jaxbArtifacts.validationFindings.addAll(TLModelCompileValidator
                        .validateModelElement(library, false));

            } catch (Throwable t) {
                addLoaderFinding(FindingType.ERROR, library,
                        ERROR_UNKNOWN_EXCEPTION_DURING_VALIDATION, library.getName(),
                        ExceptionUtils.getExceptionClass(t).getSimpleName(),
                        ExceptionUtils.getExceptionMessage(t));
                log.debug("Unexpected exception validating liberary module: " + library.getName(),
                        t);
            }
        }
    }

    /**
     * Scans all <code>XSDLibrary</code> instances within the current model to identify
     * <code>XSDElement</code> members that are used to alias <code>XSDComplexType</code>
     * definitions.
     */
    private void resolveLegacySchemaAliases() {
        Map<String, List<XSDLibrary>> schemasByNamespace = new HashMap<String, List<XSDLibrary>>();

        // First, collect a list of XSDLibraries by namespace
        for (String namespace : libraryModel.getNamespaces()) {
            for (AbstractLibrary library : libraryModel.getLibrariesForNamespace(namespace)) {
                if (library instanceof XSDLibrary) {
                    List<XSDLibrary> schemaList = schemasByNamespace.get(namespace);

                    if (schemaList == null) {
                        schemaList = new ArrayList<XSDLibrary>();
                        schemasByNamespace.put(namespace, schemaList);
                    }
                    schemaList.add((XSDLibrary) library);
                }
            }
        }

        // Resolve element references within each respective namespace
        for (String namespace : schemasByNamespace.keySet()) {
            Map<String, XSDComplexType> typesByName = new HashMap<String, XSDComplexType>();
            List<XSDElement> elementList = new ArrayList<XSDElement>();

            // Organize collections of all types and elements within this namespace
            for (XSDLibrary schema : schemasByNamespace.get(namespace)) {
                for (LibraryMember member : schema.getNamedMembers()) {
                    if (member instanceof XSDComplexType) {
                        typesByName.put(member.getLocalName(), (XSDComplexType) member);
                    } else if (member instanceof XSDElement) {
                        elementList.add((XSDElement) member);
                    }
                }
            }

            // Search for elements that reference a complex type in the 'typesByName' map
            for (XSDElement element : elementList) {
                TopLevelElement jaxbElement = element.getJaxbElement();

                if ((element.getName() == null) || (jaxbElement.getType() == null)
                        || !namespace.equals(jaxbElement.getType().getNamespaceURI())) {
                    // If this element references an anonymous type or a type in another namespace,
                    // we can skip it
                    // since it is not considered to be an alias
                    continue;
                }
                String referencedTypeName = jaxbElement.getType().getLocalPart();
                XSDComplexType referencedType = typesByName.get(referencedTypeName);

                // If we found a referenced type definition in this namespace, register the element
                // as an alias
                if (referencedType != null) {
                    if (element.getName().equals(referencedType.getName())) {
                        referencedType.setIdentityAlias(element);

                    } else {
                        referencedType.addAlias(element);
                    }
                }
            }
        }
    }

    /**
     * Returns an input source to used for accessing the content of the library module with the
     * specified namespace. If a module with the requested namespace cannot be located, this method
     * will return null. Sub-classes may override this method if a different input source
     * implementation is needed.
     * 
     * @param libraryNamespace
     *            the namespace of the library to be loaded
     * @param versionScheme
     *            the version scheme to apply when interpreting the namespace provided (may be null)
     * @param fileHints
     *            the list of file hints that should be consindered when attempting to identify the
     *            input source(s)
     * @return Collection<LibraryInputSource<C>>
     * @throws LibraryLoaderException
     *             thrown if a system-level exception occurs
     */
    private Collection<LibraryInputSource<C>> getInputSources(URI libraryNamespace,
            String versionScheme, String fileHint) throws LibraryLoaderException {
        List<String> fileHints = new ArrayList<String>();

        if (fileHint != null) {
            fileHints.add(fileHint);
        }
        return getInputSources(libraryNamespace, versionScheme, fileHints);
    }

    /**
     * Returns an input source to used for accessing the content of the library module with the
     * specified namespace. If a module with the requested namespace cannot be located, this method
     * will return null. Sub-classes may override this method if a different input source
     * implementation is needed.
     * 
     * @param libraryNamespace
     *            the namespace of the library to be loaded
     * @param versionScheme
     *            the version scheme to apply when interpreting the namespace provided (may be null)
     * @param fileHints
     *            the list of file hints that should be consindered when attempting to identify the
     *            input source(s)
     * @return Collection<LibraryInputSource<C>>
     * @throws LibraryLoaderException
     *             thrown if a system-level exception occurs
     */
    private Collection<LibraryInputSource<C>> getInputSources(URI libraryNamespace,
            String versionScheme, List<String> fileHints) throws LibraryLoaderException {
        Collection<LibraryInputSource<C>> inputSources = new ArrayList<LibraryInputSource<C>>();

        // Special Case: Ignore the XML schema namespace since it is predefined in every TLModel
        // instance
        if ((libraryNamespace != null)
                && (!libraryNamespace.toString().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI))) {
            String[] fileHintArray = (fileHints == null) ? new String[0] : fileHints
                    .toArray(new String[fileHints.size()]);
            Collection<URL> libraryUrls = namespaceResolver.resovleLibraryImport(libraryNamespace,
                    versionScheme, fileHintArray);

            for (URL libraryUrl : libraryUrls) {
                inputSources.add(moduleLoader.newInputSource(libraryUrl));
            }
        }
        return inputSources;
    }

    /**
     * Returns true if the given namespace is assigned to a built-in library within the current
     * model.
     * 
     * @param namespace
     *            the namespace to analyze
     * @return boolean
     */
    private boolean isBuiltInNamespace(String namespace) {
        boolean result = false;

        if (namespace != null) {
            for (BuiltInLibrary library : libraryModel.getBuiltInLibraries()) {
                result |= namespace.equals(library.getNamespace());
            }
        }
        return result;
    }

    /**
     * Returns true if the actual namespace is an allowed variation from the expected namespace
     * according to the specified version scheme. For example, "http://opentravel.org/ns/v01_00" would be
     * an allowed variation of "http://opentravel.org/ns/v01" according to the OTA2 version scheme.
     * 
     * @param expectedNamespace
     *            the expected namespace to analyze
     * @param actualNamespace
     *            the actual namespace to analyze
     * @param versionScheme
     *            the version scheme that establishes the rules for versioned URI formats
     * @return boolean
     */
    private boolean isAllowedNamespaceVariation(String expectedNamespace, String actualNamespace,
            String versionScheme) {
        boolean result = false;
        try {
            VersionScheme vScheme = VersionSchemeFactory.getInstance().getVersionScheme(
                    versionScheme);
            String expectedNS = vScheme.setVersionIdentifier(expectedNamespace,
                    vScheme.getVersionIdentifier(expectedNamespace));
            String actualNS = vScheme.setVersionIdentifier(actualNamespace,
                    vScheme.getVersionIdentifier(actualNamespace));

            result = expectedNS.equals(actualNS);

        } catch (VersionSchemeException e) {
            // No error - return false
        }
        return result;
    }

    /**
     * Adds the given collection of loader findings to the current list maintained by this loader
     * instance. If the finding is related to an import error message, the finding is posted to the
     * 'importLoaderFindings' map instead of the 'loaderFindings' collection.
     * 
     * @param loaderFindings
     *            the collection of loader findings to add
     */
    private void addLoaderFindings(ValidationFindings loaderFindings) {
        if (loaderFindings != null) {
            for (ValidationFinding finding : loaderFindings.getAllFindingsAsList()) {
                addLoaderFinding(finding.getType(), finding.getSource(), finding.getMessageKey(),
                        finding.getMessageParams());
            }
        }
    }

    /**
     * Adds a loader finding to the current list maintained by this loader. If the finding is
     * related to an import error message, the finding is posted to the 'importLoaderFindings' map
     * instead of the 'loaderFindings' collection.
     * 
     * @param type
     *            the type of the finding
     * @param source
     *            the object that is the source of the finding
     * @param messageKey
     *            the message key for the finding (required)
     * @param messageParams
     *            the optional message parameters for the finding
     */
    private void addLoaderFinding(FindingType type, Validatable source, String messageKey,
            Object... messageParams) {
        if (isImportValidationMessageKey(messageKey)) {
            importLoaderFindings.put(source.getValidationIdentity(), new ValidationFinding(source,
                    type, messageKey, messageParams));
        } else {
            loaderFindings.addFinding(type, source, messageKey, messageParams);
        }
    }

    /**
     * Returns true if the given validation message key applies to an error or warning related to
     * failed imports or includes of dependent libraries and schemas.
     * 
     * @param messageKey
     *            the message key to analyze
     * @return boolean
     */
    private boolean isImportValidationMessageKey(String messageKey) {
        return (messageKey != null) && messageKey.equals(WARNING_LIBRARY_NOT_FOUND);
    }

    /**
     * Initializes the module loader component with a default implementation.
     * 
     * @throws LibraryLoaderException
     *             thrown if a system-level exception occurs
     */
    @SuppressWarnings("unchecked")
    private void initializeDefaultModuleLoader() throws LibraryLoaderException {
        try {
            Class<?> moduleLoaderClass = MultiVersionLibraryModuleLoader.class;
            setModuleLoader((LibraryModuleLoader<C>) moduleLoaderClass.newInstance());

        } catch (Throwable t) {
            throw new LibraryLoaderException(t);
        }

    }

    /**
     * Container for all loader artifacts that have not yet been loaded into the main library model.
     * 
     * @author S. Livezey
     */
    private class JAXBModelArtifacts {

        public Map<URL, LibraryModuleInfo<Object>> libraryUrlMappings = new HashMap<URL, LibraryModuleInfo<Object>>();
        public Map<URL, LibraryModuleInfo<Schema>> schemaUrlMappings = new HashMap<URL, LibraryModuleInfo<Schema>>();
        public ValidationFindings validationFindings = new ValidationFindings();

        public void addLibraryModule(URL libraryUrl, LibraryModuleInfo<Object> libraryModule) {
            if ((libraryModule != null) && !libraryUrlMappings.containsKey(libraryUrl)) {
                if ((libraryModule.getNamespace() != null)
                        && (libraryModule.getNamespace().length() > 0)) {
                    libraryUrlMappings.put(libraryUrl, libraryModule);

                } else {
                    validationFindings.addFinding(FindingType.ERROR, new LibraryValidationSource(
                            libraryModule), ERROR_MISSING_NAMESPACE_URI, URLUtils
                            .getShortRepresentation(libraryUrl));
                }
            }
        }

        public void addSchemaModule(URL schemaUrl, LibraryModuleInfo<Schema> schemaModule) {
            if ((schemaModule != null) && !libraryUrlMappings.containsKey(schemaUrl)) {
                schemaUrlMappings.put(schemaUrl, schemaModule);
            }
        }

        public boolean hasLibrary(String namespaceUri, String libraryName) {
            boolean result = false;

            if ((namespaceUri != null) && (libraryName != null)) {
                for (LibraryModuleInfo<?> libraryInfo : libraryUrlMappings.values()) {
                    if (namespaceUri.equals(libraryInfo.getNamespace())
                            && libraryName.equals(libraryInfo.getLibraryName())) {
                        result = true;
                        break;
                    }
                }
            }
            return result;
        }

    }

}