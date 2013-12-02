/*
 * Copyright (c) 2011, Sabre Inc.
 */
package com.sabre.schemacompiler.transform.tl2jaxb;

import org.opentravel.ns.ota2.librarymodel_v01_04.Documentation;
import org.opentravel.ns.ota2.librarymodel_v01_04.Facet;

import com.sabre.schemacompiler.model.TLDocumentation;
import com.sabre.schemacompiler.model.TLFacet;
import com.sabre.schemacompiler.transform.ObjectTransformer;
import com.sabre.schemacompiler.transform.symbols.SymbolResolverTransformerContext;

/**
 * Handles the transformation of objects from the <code>TLFacet</code> type to the
 * <code>Facet</code> type.
 *
 * @author S. Livezey
 */
public class TLFacetTransformer extends TLComplexTypeTransformer<TLFacet,Facet> {
	
	/**
	 * @see com.sabre.schemacompiler.transform.ObjectTransformer#transform(java.lang.Object)
	 */
	@Override
	public Facet transform(TLFacet source) {
		Facet facet = new Facet();
		
		if ((source.getDocumentation() != null) && !source.getDocumentation().isEmpty()) {
			ObjectTransformer<TLDocumentation,Documentation,SymbolResolverTransformerContext> docTransformer =
					getTransformerFactory().getTransformer(TLDocumentation.class, Documentation.class);
			
			facet.setDocumentation( docTransformer.transform(source.getDocumentation()) );
		}
		facet.getAttribute().addAll(transformAttributes(source.getAttributes()));
		facet.getElement().addAll(transformElements(source.getElements()));
		facet.getIndicator().addAll(transformIndicators(source.getIndicators()));
		return facet;
	}
	
}
