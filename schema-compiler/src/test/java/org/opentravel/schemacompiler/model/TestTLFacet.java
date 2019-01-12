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
package org.opentravel.schemacompiler.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Verifies the functions of the <code>TLFacet</code> class.
 */
public class TestTLFacet extends AbstractModelTest {
	
	@Test
	public void testIdentityFunctions() throws Exception {
		TLCoreObject core = addCore( "TestObject", library1 );
		TLFacet facet = core.getSummaryFacet();
		
		assertEquals( library1.getNamespace(), core.getSimpleFacet().getNamespace() );
		assertEquals( library1.getNamespace(), facet.getNamespace() );
		assertEquals( core.getName() + "_Summary", facet.getLocalName() );
		assertEquals( "TestLibrary1.otm : TestObject/Summary", facet.getValidationIdentity() );
	}
	
	@Test
	public void testDocumentationFunctions() throws Exception {
		TLCoreObject core = addCore( "TestObject", library1 );
		
		testDocumentationFunctions( core.getSimpleFacet() );
		testDocumentationFunctions( core.getSummaryFacet() );
	}
	
	@Test
	public void testEquivalentFunctions() throws Exception {
		testEquivalentFunctions( addCore( "TestObject", library1 ).getSimpleFacet() );
	}
	
	@Test
	public void testMemberFieldFunctions() throws Exception {
		TLCoreObject core = addCore( "TestObject", library1 );
		
		testAttributeFunctions( core.getDetailFacet() );
		testPropertyFunctions( core.getDetailFacet() );
		testIndicatorFunctions( core.getDetailFacet() );
	}
	
}
