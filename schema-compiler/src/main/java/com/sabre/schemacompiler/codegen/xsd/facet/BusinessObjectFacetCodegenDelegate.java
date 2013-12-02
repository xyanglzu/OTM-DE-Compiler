/*
 * Copyright (c) 2011, Sabre Inc.
 */
package com.sabre.schemacompiler.codegen.xsd.facet;

import com.sabre.schemacompiler.model.TLFacet;

/**
 * Base class for facet code generation delegates used to generate code artifacts for
 * <code>TLFacet</code model elements that are owned by <code>TLBusinessObject</code>
 * instances.
 *
 * @author S. Livezey
 */
public abstract class BusinessObjectFacetCodegenDelegate extends TLFacetCodegenDelegate {
	
	/**
	 * Constructor that specifies the source facet for which code artifacts are being
	 * generated.
	 * 
	 * @param sourceFacet  the source facet
	 */
	public BusinessObjectFacetCodegenDelegate(TLFacet sourceFacet) {
		super(sourceFacet);
	}

}
