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

/**
 * Indicates the type or purpose of a <code>DocumentationItem</code> element.
 * 
 * @author S. Livezey
 */
public enum TLDocumentationType {

    DEPRECATION("Deprecation"), DESCRIPTION("Description"), REFERENCE("Reference"), IMPLEMENTER(
            "Implementer"), MORE_INFO("More Info"), OTHER_DOC("Other Doc");

    private String displayIdentity;

    /**
     * Constructor that specifies the display identity of the value.
     * 
     * @param displayIdentity
     *            the display identity string
     */
    private TLDocumentationType(String displayIdentity) {
        this.displayIdentity = displayIdentity;
    }

    /**
     * Returns the display identity string for this value.
     * 
     * @return String
     */
    public String getDisplayIdentity() {
        return displayIdentity;
    }

}