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

import java.util.List;

/**
 * Interface to be implemented by named entities that are capable of owning facets.
 * 
 * @author S. Livezey
 */
public interface TLFacetOwner extends NamedEntity {

    /**
     * Returns a list of all facets owned by this entity.
     * 
     * @return List&lt;TLFacet&gt;
     */
    public List<TLFacet> getAllFacets();

}
