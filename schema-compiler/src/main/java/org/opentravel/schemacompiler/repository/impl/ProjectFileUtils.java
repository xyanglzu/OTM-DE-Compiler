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

package org.opentravel.schemacompiler.repository.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opentravel.ns.ota2.project_v01_00.ManagedProjectItemType;
import org.opentravel.ns.ota2.project_v01_00.ObjectFactory;
import org.opentravel.ns.ota2.project_v01_00.ProjectItemType;
import org.opentravel.ns.ota2.project_v01_00.ProjectType;
import org.opentravel.ns.ota2.project_v01_00.RepositoryRefType;
import org.opentravel.ns.ota2.project_v01_00.RepositoryReferencesType;
import org.opentravel.ns.ota2.project_v01_00.UnmanagedProjectItemType;
import org.opentravel.schemacompiler.codegen.CodeGeneratorFactory;
import org.opentravel.schemacompiler.ioc.SchemaDeclarations;
import org.opentravel.schemacompiler.loader.LibraryLoaderException;
import org.opentravel.schemacompiler.loader.LoaderConstants;
import org.opentravel.schemacompiler.loader.impl.FileValidationSource;
import org.opentravel.schemacompiler.repository.Project;
import org.opentravel.schemacompiler.repository.ProjectItem;
import org.opentravel.schemacompiler.repository.ProjectManager;
import org.opentravel.schemacompiler.repository.RemoteRepository;
import org.opentravel.schemacompiler.repository.Repository;
import org.opentravel.schemacompiler.repository.RepositoryItemState;
import org.opentravel.schemacompiler.repository.RepositoryManager;
import org.opentravel.schemacompiler.saver.LibrarySaveException;
import org.opentravel.schemacompiler.util.ClasspathResourceResolver;
import org.opentravel.schemacompiler.util.ExceptionUtils;
import org.opentravel.schemacompiler.util.FileUtils;
import org.opentravel.schemacompiler.util.URLUtils;
import org.opentravel.schemacompiler.validate.FindingType;
import org.opentravel.schemacompiler.validate.ValidationFindings;
import org.opentravel.schemacompiler.version.VersionScheme;
import org.opentravel.schemacompiler.version.VersionSchemeException;
import org.opentravel.schemacompiler.version.VersionSchemeFactory;
import org.opentravel.schemacompiler.xml.NamespacePrefixMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

/**
 * Static helper methods that handle the loading and saving of projects to and from the local file system using JAXB.
 * 
 * @author S. Livezey
 */
public class ProjectFileUtils extends AbstractFileUtils {

    private static final String SCHEMA_CONTEXT = ":org.w3._2001.xmlschema:org.opentravel.ns.ota2.project_v01_00";
    private static final String PROJECT_FILE_NAMESPACE = "http://www.OpenTravel.org/ns/OTA2/Project_v01_00";

    private static Log log = LogFactory.getLog( ProjectFileUtils.class );

    private static javax.xml.validation.Schema projectValidationSchema;
    private static ObjectFactory objectFactory = new ObjectFactory();
    private static JAXBContext jaxbContext;

    /**
     * Default constructor.
     */
    public ProjectFileUtils() {
        this( null );
    }

    /**
     * Constructor that supplies the repository manager to be used during file utility functions.
     * 
     * @param repositoryManager the repository manager instance
     */
    public ProjectFileUtils(RepositoryManager repositoryManager) {
        super( repositoryManager );
    }

    /**
     * Loads the JAXB representation of the project from the specified file location.
     * 
     * @param projectFile the project file to load
     * @param findings the validation findings encountered during the load process
     * @return ProjectType
     * @throws LibraryLoaderException thrown if the project file cannot be loaded
     */
    @SuppressWarnings("unchecked")
    public ProjectType loadJaxbProjectFile(File projectFile, ValidationFindings findings)
        throws LibraryLoaderException {
        ProjectType jaxbProject = null;

        try (InputStream is = new FileInputStream( projectFile )) {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema( projectValidationSchema );

            JAXBElement<ProjectType> documentElement =
                (JAXBElement<ProjectType>) FileUtils.unmarshalStreamContent( is, unmarshaller );
            jaxbProject = documentElement.getValue();

        } catch (JAXBException e) {
            if (findings != null) {
                String filename = (projectFile == null) ? "[UNKNOWN FILE]" : projectFile.getName();

                findings.addFinding( FindingType.ERROR, new FileValidationSource( projectFile ),
                    LoaderConstants.ERROR_UNREADABLE_PROJECT_CONTENT, filename,
                    ExceptionUtils.getExceptionClass( e ).getSimpleName(), ExceptionUtils.getExceptionMessage( e ) );
            } else {
                throw new LibraryLoaderException( e.getMessage(), e );
            }

        } catch (IOException e) {
            if (findings != null) {
                findings.addFinding( FindingType.WARNING, new FileValidationSource( projectFile ),
                    LoaderConstants.WARNING_PROJECT_NOT_FOUND,
                    (projectFile == null) ? "[UNKNOWN FILE]" : projectFile.getName() );
            } else {
                throw new LibraryLoaderException( e.getMessage(), e );
            }

        } catch (Exception e) {
            throw new LibraryLoaderException( "Unknown error while loading project.", e );
        }
        return jaxbProject;
    }

    /**
     * Saves the JAXB representation of the project to the specified file location.
     * 
     * @param project the OTM project to be saved
     * @throws LibrarySaveException thrown if the project file cannot be saved
     */
    public void saveProjectFile(Project project) throws LibrarySaveException {
        saveProjectFile( convertToJaxbProject( project ), project.getProjectFile() );
    }

    /**
     * Saves the JAXB representation of the project to the specified file location.
     * 
     * @param jaxbProject the JAXB representation of the project's contents
     * @param projectFile the file to which the project contents should be saved
     * @throws LibrarySaveException thrown if the project file cannot be saved
     */
    public void saveProjectFile(ProjectType jaxbProject, File projectFile) throws LibrarySaveException {
        boolean success = false;
        File backupFile = null;
        try {
            backupFile = createBackupFile( projectFile );

        } catch (IOException e) {
            // If we could not create the backup file, proceed without one
        }

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();

            if (!projectFile.exists()) {
                projectFile.getParentFile().mkdirs();
            }
            marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
            marshaller.setProperty( "com.sun.xml.bind.namespacePrefixMapper", new NamespacePrefixMapper() {

                @Override
                public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
                    return PROJECT_FILE_NAMESPACE.equals( namespaceUri )
                        ? SchemaDeclarations.OTA2_PROJECT_SCHEMA.getDefaultPrefix()
                        : suggestion;
                }

                @Override
                public String[] getPreDeclaredNamespaceUris() {
                    return new String[] {PROJECT_FILE_NAMESPACE};
                }

            } );
            marshaller.setSchema( projectValidationSchema );
            marshaller.marshal( objectFactory.createProject( jaxbProject ), projectFile );
            success = true;

        } catch (JAXBException e) {
            throw new LibrarySaveException( "Unknown error while saving project.", e );

        } finally {
            if (!success && (backupFile != null)) {
                try {
                    restoreBackupFile( backupFile, projectFile.getName() );

                } catch (Exception e) {
                    log.warn( "Error restoring backup file from failed operation." );
                }
            }
        }
    }

    /**
     * Transforms the given model project into its JAXB equivalent.
     * 
     * @param project the model project to transform
     * @return ProjectType
     */
    private ProjectType convertToJaxbProject(Project project) {
        URL projectFileUrl = URLUtils.toURL( project.getProjectFile() );
        ProjectItem defaultItem = project.getDefaultItem();
        Set<String> repositoryIds = new HashSet<>();
        ProjectType jaxbProject = new ProjectType();

        jaxbProject.setProjectId( project.getProjectId() );
        jaxbProject.setName( project.getName() );
        jaxbProject.setDescription( project.getDescription() );
        jaxbProject.setDefaultContextId( project.getDefaultContextId() );

        for (ProjectItem item : purgeImpliedManagedVersions( project.getProjectItems() )) {
            convertToJaxbProjectItem( item, jaxbProject, defaultItem, projectFileUrl, repositoryIds );
        }
        assignFailedProjectItems( jaxbProject, project );
        assignRepositoryReferences( jaxbProject, project, repositoryIds );

        return jaxbProject;
    }

    /**
     * Converts the given project item and adds it to the JAXB project.
     * 
     * @param item the project item to convert
     * @param jaxbProject the JAXB project to which the converted item will be added
     * @param defaultItem the default item of the owning project
     * @param projectFileUrl the URL where the project file will be saved
     * @param repositoryIds the collection of all known repository IDs from the project
     */
    private void convertToJaxbProjectItem(ProjectItem item, ProjectType jaxbProject, ProjectItem defaultItem,
        URL projectFileUrl, Set<String> repositoryIds) {
        if (item.getState() == RepositoryItemState.UNMANAGED) {
            UnmanagedProjectItemType jaxbItem = new UnmanagedProjectItemType();

            jaxbItem.setDefaultItem( (defaultItem == item) ? Boolean.TRUE : null );
            jaxbItem
                .setFileLocation( URLUtils.getRelativeURL( projectFileUrl, item.getContent().getLibraryUrl(), true ) );
            jaxbProject.getProjectItemBase().add( objectFactory.createUnmanagedProjectItem( jaxbItem ) );

        } else {
            String repositoryId = (item.getRepository() == null) ? null : item.getRepository().getId();
            ManagedProjectItemType jaxbItem = new ManagedProjectItemType();

            if (repositoryId != null) {
                repositoryIds.add( repositoryId );
            }
            jaxbItem.setDefaultItem( (defaultItem == item) ? Boolean.TRUE : null );
            jaxbItem.setRepository( repositoryId );
            jaxbItem.setBaseNamespace( item.getBaseNamespace() );
            jaxbItem.setFilename( item.getFilename() );
            jaxbItem.setVersion( item.getVersion() );
            jaxbProject.getProjectItemBase().add( objectFactory.createManagedProjectItem( jaxbItem ) );
        }
    }

    /**
     * Add any failed project items to the JAXB project so they will not be lost when the file is re-opened.
     * 
     * @param jaxbProject the JAXB project being constructed
     * @param project the project from which the JAXB instance is being created
     */
    private void assignFailedProjectItems(ProjectType jaxbProject, Project project) {
        for (ProjectItemType failedItem : project.getFailedProjectItems()) {
            if (failedItem instanceof UnmanagedProjectItemType) {
                jaxbProject.getProjectItemBase()
                    .add( objectFactory.createUnmanagedProjectItem( (UnmanagedProjectItemType) failedItem ) );

            } else { // Must be a ManagedProjectItemType
                jaxbProject.getProjectItemBase()
                    .add( objectFactory.createManagedProjectItem( (ManagedProjectItemType) failedItem ) );
            }
            failedItem.setDefaultItem( null );
        }
    }

    /**
     * Assign the repositories that are referenced and their endpoint URL's to the given JAXB project.
     * 
     * @param jaxbProject the JAXB project being constructed
     * @param project the project from which the JAXB instance is being created
     * @param repositoryIds the collection of all known repository IDs from the project
     */
    private void assignRepositoryReferences(ProjectType jaxbProject, Project project, Set<String> repositoryIds) {
        ProjectManager projectManager = project.getProjectManager();
        RepositoryManager repositoryManager = (projectManager == null) ? null : projectManager.getRepositoryManager();

        if ((repositoryManager != null) && !repositoryIds.isEmpty()) {
            RepositoryReferencesType repositoryRefs = new RepositoryReferencesType();

            for (String repositoryId : repositoryIds) {
                Repository repository = repositoryManager.getRepository( repositoryId );

                if (repository instanceof RemoteRepository) {
                    RepositoryRefType repositoryRef = new RepositoryRefType();

                    repositoryRef.setRepositoryId( repositoryId );
                    repositoryRef.setValue( ((RemoteRepository) repository).getEndpointUrl() );
                    repositoryRefs.getRepositoryRef().add( repositoryRef );
                }
            }
            jaxbProject.setRepositoryReferences( repositoryRefs );
        }
    }

    /**
     * Purges any project items from the given list that are implied by the existence of later versions. Only managed
     * project items are processed by this method.
     * 
     * @param projectItems the list of project items to process
     * @return List&lt;ProjectItem&gt;
     */
    private List<ProjectItem> purgeImpliedManagedVersions(List<ProjectItem> projectItems) {
        Map<ProjectItem,List<String>> itemVersionChains = new HashMap<>();
        List<ProjectItem> keepItems = new ArrayList<>();

        for (ProjectItem item : projectItems) {
            if (item.getState() == RepositoryItemState.UNMANAGED) {
                continue; // only purge managed versions
            }
            addKeepItemIfNotEclipsed( item, keepItems, itemVersionChains );
        }

        // Scan the original list, and assemble the final list of items that should be included in
        // project's save file
        List<ProjectItem> result = new ArrayList<>();

        for (ProjectItem item : projectItems) {
            if ((item.getState() == RepositoryItemState.UNMANAGED) || keepItems.contains( item )) {
                result.add( item );
            }
        }
        return result;
    }

    /**
     * If the given project item is not eclisped by a later minor version, it will be added to the given list of keep
     * items.
     * 
     * @param item the item to be kept if not eclipsed
     * @param keepItems the list of keep items that have been discovered so far
     * @param itemVersionChains the version identifier chain for each project item
     */
    private void addKeepItemIfNotEclipsed(ProjectItem item, List<ProjectItem> keepItems,
        Map<ProjectItem,List<String>> itemVersionChains) {
        try {
            VersionScheme vScheme = VersionSchemeFactory.getInstance().getVersionScheme( item.getVersionScheme() );
            List<String> itemVersionChain = vScheme.getMajorVersionChain( item.getNamespace() );
            boolean purgeCurrentItem = false;

            // Determine if the current item is eclipsed by one of the existing keep item
            for (ProjectItem keepItem : keepItems) {
                if (item.getLibraryName().equals( keepItem.getLibraryName() )
                    && item.getBaseNamespace().equals( keepItem.getBaseNamespace() )) {
                    List<String> keepItemChain = itemVersionChains.get( keepItem );

                    if (isInVersionChain( item.getNamespace(), keepItemChain, vScheme )) {
                        purgeCurrentItem = true;
                        break;
                    }
                }
            }

            if (!purgeCurrentItem) {
                keepProjectItem( item, keepItems, vScheme, itemVersionChain );
            }
            itemVersionChains.put( item, itemVersionChain );

        } catch (VersionSchemeException e) {
            // On error, we DO NOT want to purge the item from the list
            keepItems.add( item );
        }
    }

    /**
     * Adds the given project item to the given keep list and purges any existing keep items that are eclipsed by the
     * new one.
     * 
     * @param item the project item to keep
     * @param keepItems the list of keep items that have been discovered so far
     * @param vScheme the version scheme of the project item
     * @param itemVersionChains the version identifier chain for each project item
     */
    private void keepProjectItem(ProjectItem item, List<ProjectItem> keepItems, VersionScheme vScheme,
        List<String> itemVersionChain) {
        Iterator<ProjectItem> keepIterator = keepItems.iterator();

        // Discard any existing keep items that are eclipsed by the current item
        while (keepIterator.hasNext()) {
            ProjectItem keepItem = keepIterator.next();

            if (!item.getLibraryName().equals( keepItem.getLibraryName() )
                || !item.getBaseNamespace().equals( keepItem.getBaseNamespace() )) {
                continue; // purge candidates must have the same library name and base namespace
            }

            if (isInVersionChain( keepItem.getNamespace(), itemVersionChain, vScheme )) {
                keepIterator.remove();
            }
        }
        keepItems.add( item );
    }

    /**
     * Returns true if the specified namespace is a member of the version chain provided.
     * 
     * <p>
     * NOTE: This method assumed that the base namespaces of the given namespace and the chain have already been
     * matched.
     * 
     * @param namespace the namespace to analyze
     * @param versionChain the version chain to check for membership
     * @param scheme the version scheme for the namespace and version chain
     * @return boolean
     */
    private boolean isInVersionChain(String namespace, List<String> versionChain, VersionScheme scheme) {
        String nsVersion = scheme.getVersionIdentifier( namespace );
        boolean result = false;

        if (nsVersion != null) {
            for (String chainNS : versionChain) {
                if (nsVersion.equals( scheme.getVersionIdentifier( chainNS ) )) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Initializes the validation schema and shared JAXB context.
     */
    static {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
            InputStream schemaStream =
                SchemaDeclarations.OTA2_PROJECT_SCHEMA.getContent( CodeGeneratorFactory.XSD_TARGET_FORMAT );

            schemaFactory.setResourceResolver( new ClasspathResourceResolver() );
            projectValidationSchema = schemaFactory.newSchema( new StreamSource( schemaStream ) );
            jaxbContext = JAXBContext.newInstance( SCHEMA_CONTEXT );

        } catch (Exception e) {
            throw new ExceptionInInitializerError( e );
        }
    }

}
