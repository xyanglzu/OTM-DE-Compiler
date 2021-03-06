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

package org.opentravel.schemacompiler.repository;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.opentravel.schemacompiler.model.TLLibraryStatus;
import org.opentravel.schemacompiler.repository.impl.RepositoryItemImpl;

import java.io.File;

/**
 * Verifies the functions of the <code>ServiceAssembly</code> class.
 */
public class TestServiceAssembly {

    private RepositoryManager repositoryManager;
    private ServiceAssembly assembly;

    @Before
    public void setup() throws Exception {
        File assemblyFile = new File( System.getProperty( "user.dir" ), "/TestAssembly.osm" );

        repositoryManager = RepositoryManager.getDefault();
        assembly = new ServiceAssemblyManager( repositoryManager ).newAssembly( "http://www.opentravel.org/assemblies",
            "TestAssembly", "1.0.0", assemblyFile );
    }

    @Test
    public void testIdentityFunctions() throws Exception {
        ServiceAssemblyMember providerMember = new ServiceAssemblyMember();

        assertEquals( "[Unidentified Assembly Member Type]", providerMember.getValidationIdentity() );

        providerMember.setReleaseItem( newReleaseItem( "ProviderRelease" ) );
        assertEquals( "ProviderRelease_1_0_0.otr", providerMember.getValidationIdentity() );

        assembly.addProviderApi( providerMember );
        assertEquals( "TestAssembly.osm : ProviderRelease_1_0_0.otr", providerMember.getValidationIdentity() );

        assertEquals( "http://www.opentravel.org/assemblies", assembly.getBaseNamespace() );
        assertEquals( "http://www.opentravel.org/assemblies/v1", assembly.getNamespace() );
        assertEquals( "TestAssembly", assembly.getName() );
        assertEquals( "1.0.0", assembly.getVersion() );
        assertEquals( "TestAssembly.osm", assembly.getValidationIdentity() );

        assembly.setAssemblyUrl( null );
        assertEquals( "TestAssembly", assembly.getValidationIdentity() );

        assembly.setName( null );
        assertEquals( "[ UNKNOWN ASSEMBLY ]", assembly.getValidationIdentity() );
    }

    @Test
    public void testAssemblyMemberManagement() throws Exception {
        ServiceAssemblyMember providerMember = new ServiceAssemblyMember();
        ServiceAssemblyMember consumerMember = new ServiceAssemblyMember();

        providerMember.setReleaseItem( newReleaseItem( "ProviderRelease" ) );
        consumerMember.setReleaseItem( newReleaseItem( "ConsumerRelease" ) );
        assembly.addProviderApi( providerMember );
        assembly.addConsumerApi( consumerMember );

        assertEquals( 2, assembly.getAllApis().size() );
        assertEquals( 1, assembly.getProviderApis().size() );
        assertEquals( 1, assembly.getConsumerApis().size() );
        assertEquals( providerMember, assembly.getProviderApis().get( 0 ) );
        assertEquals( consumerMember, assembly.getConsumerApis().get( 0 ) );
        assertEquals( assembly, providerMember.getOwner() );
        assertEquals( assembly, consumerMember.getOwner() );

        assembly.removeProviderApi( providerMember );
        assertEquals( 1, assembly.getAllApis().size() );

        assembly.removeConsumerApi( consumerMember );
        assertEquals( 0, assembly.getAllApis().size() );
    }

    private RepositoryItem newReleaseItem(String releaseName) {
        RepositoryItemImpl item = new RepositoryItemImpl();

        item.setRepository( repositoryManager );
        item.setBaseNamespace( "http://www.opentravel.org/releases" );
        item.setNamespace( "http://www.opentravel.org/releases/v1" );
        item.setFilename( releaseName + "_1_0_0.otr" );
        item.setLibraryName( releaseName );
        item.setStatus( TLLibraryStatus.FINAL );
        item.setState( RepositoryItemState.MANAGED_UNLOCKED );
        return item;
    }
}
