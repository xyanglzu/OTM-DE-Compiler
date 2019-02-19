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
package org.opentravel.schemacompiler.console;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Verifies the operation of the <code>BrowseController</code> when the repository is
 * configured with the JMS indexing service.
 */
public class TestBrowseControllerRealTime extends TestBrowseController {
    
    @BeforeClass
    public static void setupTests() throws Exception {
        startSmtpTestServer( 1595 );
        setupWorkInProcessArea( TestBrowseControllerRealTime.class );
        startTestServer( "versions-repository", 9294, defaultRepositoryConfig,
                true, true, TestBrowseControllerRealTime.class );
    }
    
    @AfterClass
    public static void tearDownTests() throws Exception {
        shutdownTestServer();
    }
    
}
