/*
 * Copyright (c) 2011, Sabre Inc.
 */
package com.sabre.schemacompiler.transform.symbols;

import com.sabre.schemacompiler.transform.SymbolResolver;

/**
 * Transformer context that provides access to a <code>SymbolTable</code> instance.
 * 
 * @author S. Livezey
 */
public class SymbolResolverTransformerContext extends DefaultTransformerContext {
	
	private SymbolResolver symbolResolver;
	
	/**
	 * Returns the symbol resolver instance associated with this context.
	 * 
	 * @return SymbolTable
	 */
	public SymbolResolver getSymbolResolver() {
		return symbolResolver;
	}
	
	/**
	 * Assigns the symbol resolver instance associated with this context.
	 * 
	 * @param symbolResolver  the symbol resolver to associate with this context
	 */
	public void setSymbolResolver(SymbolResolver symbolResolver) {
		this.symbolResolver = symbolResolver;
	}
	
}
