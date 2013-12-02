/*
 * Copyright (c) 2011, Sabre Inc.
 */
package com.sabre.schemacompiler.transform.jaxb14_2tl;

import org.opentravel.ns.ota2.librarymodel_v01_04.BusinessObject;
import org.opentravel.ns.ota2.librarymodel_v01_04.Documentation;
import org.opentravel.ns.ota2.librarymodel_v01_04.Equivalent;
import org.opentravel.ns.ota2.librarymodel_v01_04.Extension;
import org.opentravel.ns.ota2.librarymodel_v01_04.Facet;
import org.opentravel.ns.ota2.librarymodel_v01_04.FacetContextual;

import com.sabre.schemacompiler.model.TLAlias;
import com.sabre.schemacompiler.model.TLBusinessObject;
import com.sabre.schemacompiler.model.TLDocumentation;
import com.sabre.schemacompiler.model.TLEquivalent;
import com.sabre.schemacompiler.model.TLExtension;
import com.sabre.schemacompiler.model.TLFacet;
import com.sabre.schemacompiler.transform.ObjectTransformer;
import com.sabre.schemacompiler.transform.symbols.DefaultTransformerContext;

/**
 * Handles the transformation of objects from the <code>BusinessObject</code> type to the
 * <code>TLBusinessObject</code> type.
 *
 * @author S. Livezey
 */
public class BusinessObjectTransformer extends ComplexTypeTransformer<BusinessObject,TLBusinessObject> {
	
	/**
	 * @see com.sabre.schemacompiler.transform.ObjectTransformer#transform(java.lang.Object)
	 */
	@Override
	public TLBusinessObject transform(BusinessObject source) {
		ObjectTransformer<Facet,TLFacet,DefaultTransformerContext> facetTransformer =
				getTransformerFactory().getTransformer(Facet.class, TLFacet.class);
		ObjectTransformer<FacetContextual,TLFacet,DefaultTransformerContext> facetContextualTransformer =
				getTransformerFactory().getTransformer(FacetContextual.class, TLFacet.class);
		ObjectTransformer<Equivalent,TLEquivalent,DefaultTransformerContext> equivTransformer =
				getTransformerFactory().getTransformer(Equivalent.class, TLEquivalent.class);
		TLBusinessObject businessObject = new TLBusinessObject();
		
		businessObject.setName( trimString(source.getName()) );
		businessObject.setNotExtendable( (source.isNotExtendable() == null) ? false : source.isNotExtendable() );
		
		if (source.getDocumentation() != null) {
			ObjectTransformer<Documentation,TLDocumentation,DefaultTransformerContext> docTransformer =
					getTransformerFactory().getTransformer(Documentation.class, TLDocumentation.class);
			
			businessObject.setDocumentation( docTransformer.transform(source.getDocumentation()) );
		}
		
		if (source.getExtension() != null) {
			ObjectTransformer<Extension,TLExtension,DefaultTransformerContext> extensionTransformer =
					getTransformerFactory().getTransformer(Extension.class, TLExtension.class);
			
			businessObject.setExtension( extensionTransformer.transform(source.getExtension()) );
		}
		
		for (Equivalent sourceEquiv : source.getEquivalent()) {
			businessObject.addEquivalent( equivTransformer.transform(sourceEquiv) );
		}
		
		for (String aliasName : trimStrings(source.getAliases())) {
			TLAlias alias = new TLAlias();
			
			alias.setName(aliasName);
			businessObject.addAlias(alias);
		}
		
		if (source.getID() != null) {
			businessObject.setIdFacet( facetTransformer.transform(source.getID()) );
		}
		if (source.getSummary() != null) {
			businessObject.setSummaryFacet( facetTransformer.transform(source.getSummary()) );
		}
		if (source.getDetail() != null) {
			businessObject.setDetailFacet( facetTransformer.transform(source.getDetail()) );
		}
		
		if (source.getCustom() != null) {
			for (FacetContextual sourceFacet : source.getCustom()) {
				businessObject.addCustomFacet( facetContextualTransformer.transform(sourceFacet) );
			}
		}
		if (source.getQuery() != null) {
			for (FacetContextual sourceFacet : source.getQuery()) {
				businessObject.addQueryFacet( facetContextualTransformer.transform(sourceFacet) );
			}
		}
		
		return businessObject;
	}
	
}
