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

package org.opentravel.schemacompiler.transform.jaxb2xsd;

import org.opentravel.schemacompiler.model.XSDElement;
import org.opentravel.schemacompiler.transform.symbols.DefaultTransformerContext;
import org.opentravel.schemacompiler.transform.util.BaseTransformer;
import org.w3._2001.xmlschema.TopLevelElement;

/**
 * Handles the transformation of objects from the <code>TopLevelElement</code> type to the <code>XSDElement</code> type.
 * 
 * @author S. Livezey
 */
public class TopLevelElementTransformer extends BaseTransformer<TopLevelElement,XSDElement,DefaultTransformerContext> {

    /**
     * @see org.opentravel.schemacompiler.transform.ObjectTransformer#transform(java.lang.Object)
     */
    @Override
    public XSDElement transform(TopLevelElement source) {
        return new XSDElement( source.getName(), source );
    }

}
