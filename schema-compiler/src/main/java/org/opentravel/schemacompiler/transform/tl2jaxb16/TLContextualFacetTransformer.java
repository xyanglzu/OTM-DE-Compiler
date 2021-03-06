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

package org.opentravel.schemacompiler.transform.tl2jaxb16;

import org.opentravel.ns.ota2.librarymodel_v01_06.Documentation;
import org.opentravel.ns.ota2.librarymodel_v01_06.FacetContextual;
import org.opentravel.ns.ota2.librarymodel_v01_06.FacetContextualType;
import org.opentravel.schemacompiler.model.NamedEntity;
import org.opentravel.schemacompiler.model.TLContextualFacet;
import org.opentravel.schemacompiler.model.TLDocumentation;
import org.opentravel.schemacompiler.model.TLFacetType;
import org.opentravel.schemacompiler.transform.ObjectTransformer;
import org.opentravel.schemacompiler.transform.symbols.SymbolResolverTransformerContext;

/**
 * Handles the transformation of objects from the <code>TLContextualFacet</code> type to the
 * <code>FacetContextual</code> type.
 * 
 * @author S. Livezey
 */
public class TLContextualFacetTransformer extends TLComplexTypeTransformer<TLContextualFacet,FacetContextual> {

    /**
     * @see org.opentravel.schemacompiler.transform.ObjectTransformer#transform(java.lang.Object)
     */
    @Override
    public FacetContextual transform(TLContextualFacet source) {
        FacetContextual facet = new FacetContextual();

        if ((source.getDocumentation() != null) && !source.getDocumentation().isEmpty()) {
            ObjectTransformer<TLDocumentation,Documentation,SymbolResolverTransformerContext> docTransformer =
                getTransformerFactory().getTransformer( TLDocumentation.class, Documentation.class );

            facet.setDocumentation( docTransformer.transform( source.getDocumentation() ) );
        }

        if (source.getOwningEntity() != null) {
            NamedEntity owningEntity = source.getOwningEntity();

            facet.setFacetOwner( context.getSymbolResolver().buildEntityName( owningEntity.getNamespace(),
                owningEntity.getLocalName() ) );
        }
        if (facet.getFacetOwner() == null) {
            facet.setFacetOwner( source.getOwningEntityName() );
        }

        facet.setName( trimString( source.getName(), false ) );
        facet.setType( getTargetType( source.getFacetType() ) );
        facet.setNotExtendable( source.isNotExtendable() );
        facet.setFacetNamespace( trimString( source.getFacetNamespace(), false ) );
        facet.getAttribute().addAll( transformAttributes( source.getAttributes() ) );
        facet.getElement().addAll( transformElements( source.getElements() ) );
        facet.getIndicator().addAll( transformIndicators( source.getIndicators() ) );

        return facet;
    }

    /**
     * Returns the JAXB contextual facet type that corresponds with the given model facet type.
     * 
     * @param sourceType the model facet type
     * @return FacetContextualType
     */
    private FacetContextualType getTargetType(TLFacetType sourceType) {
        FacetContextualType targetType = null;

        if (sourceType != null) {
            switch (sourceType) {
                case CHOICE:
                    targetType = FacetContextualType.CHOICE;
                    break;
                case CUSTOM:
                    targetType = FacetContextualType.CUSTOM;
                    break;
                case QUERY:
                    targetType = FacetContextualType.QUERY;
                    break;
                case UPDATE:
                    targetType = FacetContextualType.UPDATE;
                    break;
                default:
                    break;
            }
        }
        return targetType;
    }

}
