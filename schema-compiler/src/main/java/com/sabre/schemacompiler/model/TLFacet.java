/*
 * Copyright (c) 2011, Sabre Inc.
 */
package com.sabre.schemacompiler.model;

import java.util.Comparator;
import java.util.List;

import com.sabre.schemacompiler.event.ModelEvent;
import com.sabre.schemacompiler.event.ModelEventBuilder;
import com.sabre.schemacompiler.event.ModelEventType;
import com.sabre.schemacompiler.model.TLAlias.AliasListManager;
import com.sabre.schemacompiler.model.TLAttribute.AttributeListManager;
import com.sabre.schemacompiler.model.TLIndicator.IndicatorListManager;
import com.sabre.schemacompiler.model.TLProperty.PropertyListManager;

/**
 * Facet definition for complex library types.
 * 
 * @author S. Livezey
 */
public class TLFacet extends TLAbstractFacet
		implements TLAttributeOwner, TLPropertyOwner, TLIndicatorOwner, TLAliasOwner, TLContextReferrer  {
	
	private AliasListManager aliasManager = new AliasListManager(this);
	private AttributeListManager attributeManager = new AttributeListManager(this);
	private PropertyListManager elementManager = new PropertyListManager(this);
	private IndicatorListManager indicatorManager = new IndicatorListManager(this);
	private boolean notExtendable;
	private String context;
	private String label;
	
	/**
	 * @see com.sabre.schemacompiler.model.TLAbstractFacet#setOwningEntity(com.sabre.schemacompiler.model.TLFacetOwner)
	 */
	@Override
	public void setOwningEntity(TLFacetOwner owningEntity) {
		super.setOwningEntity(owningEntity);
		
		// Register a derived entity list manager to synchronize the aliases of the owner
		// with those of this facet.
		AliasListManager ownerAliasManager = null;
		
		if (owningEntity instanceof TLBusinessObject) {
			ownerAliasManager = ((TLBusinessObject) owningEntity).aliasManager;
			
		} else if (owningEntity instanceof TLCoreObject) {
			ownerAliasManager = ((TLCoreObject) owningEntity).aliasManager;
		}
		if (ownerAliasManager != null) {
			ownerAliasManager.addDerivedListManager(new FacetAliasManager());
		}
	}

	/**
	 * @see com.sabre.schemacompiler.model.TLAbstractFacet#declaresContent()
	 */
	@Override
	public boolean declaresContent() {
		return super.declaresContent()
				|| (attributeManager.getChildren().size() > 0)
				|| (elementManager.getChildren().size() > 0)
				|| (indicatorManager.getChildren().size() > 0);
	}

	/**
	 * Clears the contents of this facet.
	 */
	public void clearFacet() {
		aliasManager.clearChildren();
		attributeManager.clearChildren();
		elementManager.clearChildren();
		indicatorManager.clearChildren();
		setDocumentation(null);
		setNotExtendable(false);
		setContext(null);
		publishEvent( new ModelEventBuilder(ModelEventType.FACET_CLEARED, this).setAffectedItem(this).buildEvent() );
	}

	/**
	 * Returns the <code>AliasListManager</code> for this facet.  NOTE: This method is used to coordinate
	 * the internal integrity of the model and should not be accessed by external components or services.
	 * 
	 * @return AliasListManager
	 */
	protected AliasListManager getAliasManager() {
		return aliasManager;
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.NamedEntity#getLocalName()
	 */
	@Override
	public String getLocalName() {
		TLFacetOwner owningEntity = getOwningEntity();
		TLFacetType facetType = getFacetType();
		StringBuilder localName = new StringBuilder();
		
		if (owningEntity != null) {
			localName.append(owningEntity.getLocalName()).append('_');
		}
		if (facetType != null) {
			localName.append(facetType.getIdentityName(context, label));
		} else {
			localName.append("Unnamed_Facet");
		}
		return localName.toString();
	}

	/**
	 * @see com.sabre.schemacompiler.validate.Validatable#getValidationIdentity()
	 */
	@Override
	public String getValidationIdentity() {
		TLFacetOwner owningEntity = getOwningEntity();
		TLFacetType facetType = getFacetType();
		StringBuilder identity = new StringBuilder();
		
		if (owningEntity != null) {
			identity.append(owningEntity.getValidationIdentity()).append("/");
		}
		if (facetType == null) {
			identity.append("[Unnamed Facet]");
		} else {
			identity.append(facetType.getIdentityName(context, label));
		}
		return identity.toString();
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLAliasOwner#getAliases()
	 */
	public List<TLAlias> getAliases() {
		return aliasManager.getChildren();
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLAliasOwner#getAlias(java.lang.String)
	 */
	public TLAlias getAlias(String aliasName) {
		return aliasManager.getChild(aliasName);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLAliasOwner#addAlias(com.sabre.schemacompiler.model.TLAlias)
	 */
	public void addAlias(TLAlias alias) {
		throw new UnsupportedOperationException("Operation not supported for facets.");
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLAliasOwner#addAlias(int, com.sabre.schemacompiler.model.TLAlias)
	 */
	@Override
	public void addAlias(int index, TLAlias alias) {
		throw new UnsupportedOperationException("Operation not supported for facets.");
	}

	/**
	 * @see com.sabre.schemacompiler.model.TLAliasOwner#removeAlias(com.sabre.schemacompiler.model.TLAlias)
	 */
	public void removeAlias(TLAlias alias) {
		throw new UnsupportedOperationException("Operation not supported for facets.");
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLAliasOwner#moveUp(com.sabre.schemacompiler.model.TLAlias)
	 */
	@Override
	public void moveUp(TLAlias alias) {
		throw new UnsupportedOperationException("Operation not supported for facets.");
	}

	/**
	 * @see com.sabre.schemacompiler.model.TLAliasOwner#moveDown(com.sabre.schemacompiler.model.TLAlias)
	 */
	@Override
	public void moveDown(TLAlias alias) {
		throw new UnsupportedOperationException("Operation not supported for facets.");
	}

	/**
	 * @see com.sabre.schemacompiler.model.TLAliasOwner#sortAliases(java.util.Comparator)
	 */
	@Override
	public void sortAliases(Comparator<TLAlias> comparator) {
		throw new UnsupportedOperationException("Operation not supported for facets.");
	}

	/**
	 * @see com.sabre.schemacompiler.model.TLAttributeOwner#getAttributes()
	 */
	public List<TLAttribute> getAttributes() {
		return attributeManager.getChildren();
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLAttributeOwner#getAttribute(java.lang.String)
	 */
	public TLAttribute getAttribute(String attributeName) {
		return attributeManager.getChild(attributeName);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLAttributeOwner#addAttribute(com.sabre.schemacompiler.model.TLAttribute)
	 */
	public void addAttribute(TLAttribute attribute) {
		attributeManager.addChild(attribute);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLAttributeOwner#addAttribute(int, com.sabre.schemacompiler.model.TLAttribute)
	 */
	@Override
	public void addAttribute(int index, TLAttribute attribute) {
		attributeManager.addChild(index, attribute);
	}

	/**
	 * @see com.sabre.schemacompiler.model.TLAttributeOwner#removeAttribute(com.sabre.schemacompiler.model.TLAttribute)
	 */
	public void removeAttribute(TLAttribute attribute) {
		attributeManager.removeChild(attribute);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLAttributeOwner#moveUp(com.sabre.schemacompiler.model.TLAttribute)
	 */
	@Override
	public void moveUp(TLAttribute attribute) {
		attributeManager.moveUp(attribute);
	}

	/**
	 * @see com.sabre.schemacompiler.model.TLAttributeOwner#moveDown(com.sabre.schemacompiler.model.TLAttribute)
	 */
	@Override
	public void moveDown(TLAttribute attribute) {
		attributeManager.moveDown(attribute);
	}

	/**
	 * @see com.sabre.schemacompiler.model.TLAttributeOwner#sortAttributes(java.util.Comparator)
	 */
	@Override
	public void sortAttributes(Comparator<TLAttribute> comparator) {
		attributeManager.sortChildren(comparator);
	}

	/**
	 * @see com.sabre.schemacompiler.model.TLPropertyOwner#getElements()
	 */
	@Override
	public List<TLProperty> getElements() {
		return elementManager.getChildren();
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLPropertyOwner#getElement(java.lang.String)
	 */
	@Override
	public TLProperty getElement(String elementName) {
		return elementManager.getChild(elementName);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLPropertyOwner#addElement(com.sabre.schemacompiler.model.TLProperty)
	 */
	@Override
	public void addElement(TLProperty element) {
		elementManager.addChild(element);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLPropertyOwner#addElement(int, com.sabre.schemacompiler.model.TLProperty)
	 */
	@Override
	public void addElement(int index, TLProperty element) {
		elementManager.addChild(index, element);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLPropertyOwner#removeProperty(com.sabre.schemacompiler.model.TLProperty)
	 */
	@Override
	public void removeProperty(TLProperty element) {
		elementManager.removeChild(element);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLPropertyOwner#moveUp(com.sabre.schemacompiler.model.TLProperty)
	 */
	@Override
	public void moveUp(TLProperty element) {
		elementManager.moveUp(element);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLPropertyOwner#moveDown(com.sabre.schemacompiler.model.TLProperty)
	 */
	@Override
	public void moveDown(TLProperty element) {
		elementManager.moveDown(element);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLPropertyOwner#sortElements(java.util.Comparator)
	 */
	@Override
	public void sortElements(Comparator<TLProperty> comparator) {
		elementManager.sortChildren(comparator);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLIndicatorOwner#getIndicators()
	 */
	@Override
	public List<TLIndicator> getIndicators() {
		return indicatorManager.getChildren();
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLIndicatorOwner#getIndicator(java.lang.String)
	 */
	@Override
	public TLIndicator getIndicator(String indicatorName) {
		return indicatorManager.getChild(indicatorName);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLIndicatorOwner#addIndicator(com.sabre.schemacompiler.model.TLIndicator)
	 */
	@Override
	public void addIndicator(TLIndicator indicator) {
		indicatorManager.addChild(indicator);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLIndicatorOwner#addIndicator(int, com.sabre.schemacompiler.model.TLIndicator)
	 */
	@Override
	public void addIndicator(int index, TLIndicator indicator) {
		indicatorManager.addChild(index, indicator);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLIndicatorOwner#removeIndicator(com.sabre.schemacompiler.model.TLIndicator)
	 */
	@Override
	public void removeIndicator(TLIndicator indicator) {
		indicatorManager.removeChild(indicator);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLIndicatorOwner#moveUp(com.sabre.schemacompiler.model.TLIndicator)
	 */
	@Override
	public void moveUp(TLIndicator indicator) {
		indicatorManager.moveUp(indicator);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLIndicatorOwner#moveDown(com.sabre.schemacompiler.model.TLIndicator)
	 */
	@Override
	public void moveDown(TLIndicator indicator) {
		indicatorManager.moveDown(indicator);
	}
	
	/**
	 * @see com.sabre.schemacompiler.model.TLIndicatorOwner#sortIndicators(java.util.Comparator)
	 */
	@Override
	public void sortIndicators(Comparator<TLIndicator> comparator) {
		indicatorManager.sortChildren(comparator);
	}
	
	/**
	 * Returns the value of the 'notExtendable' field.
	 *
	 * @return boolean
	 */
	public boolean isNotExtendable() {
		return notExtendable;
	}

	/**
	 * Assigns the value of the 'notExtendable' field.
	 *
	 * @param notExtendable  the field value to assign
	 */
	public void setNotExtendable(boolean notExtendable) {
		ModelEvent<?> event = new ModelEventBuilder(ModelEventType.NOT_EXTENDABLE_FLAG_MODIFIED, this)
				.setOldValue(this.notExtendable).setNewValue(notExtendable).buildEvent();

		this.notExtendable = notExtendable;
		publishEvent(event);
	}

	/**
	 * @see com.sabre.schemacompiler.model.TLContextReferrer#getContext()
	 */
	public String getContext() {
		return context;
	}

	/**
	 * @see com.sabre.schemacompiler.model.TLContextReferrer#setContext(java.lang.String)
	 */
	public void setContext(String context) {
		ModelEvent<?> event = new ModelEventBuilder(ModelEventType.CONTEXT_MODIFIED, this)
				.setOldValue(this.context).setNewValue(context).buildEvent();

		this.context = context;
		publishEvent(event);
	}
	
	/**
	 * Returns the value of the 'label' field.
	 *
	 * @return String
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Assigns the value of the 'label' field.
	 *
	 * @param label  the field value to assign
	 */
	public void setLabel(String label) {
		ModelEvent<?> event = new ModelEventBuilder(ModelEventType.LABEL_MODIFIED, this)
			.setOldValue(this.label).setNewValue(label).buildEvent();
		
		this.label = label;
		publishEvent(event);
	}

	/**
	 * List manager that synchronizes the list of derived aliases with those of the owning business
	 * or core object.
	 *
	 * @author S. Livezey
	 */
	private class FacetAliasManager extends DerivedChildEntityListManager<TLAlias,TLAlias> {
		
		/**
		 * Default constructor.
		 */
		public FacetAliasManager() {
			super(aliasManager);
		}

		/**
		 * @see com.sabre.schemacompiler.model.DerivedChildEntityListManager#getOriginalEntityName(java.lang.Object)
		 */
		@Override
		protected String getOriginalEntityName(TLAlias originalEntity) {
			return (originalEntity == null) ? null : originalEntity.getName();
		}

		/**
		 * @see com.sabre.schemacompiler.model.DerivedChildEntityListManager#getDerivedEntityName(java.lang.String)
		 */
		@Override
		protected String getDerivedEntityName(String originalEntityName) {
			return (originalEntityName == null) ?
					null : (originalEntityName + "_" + getFacetType().getIdentityName(getContext(), getLabel()));
		}

		/**
		 * @see com.sabre.schemacompiler.model.DerivedChildEntityListManager#createDerivedEntity(java.lang.Object)
		 */
		@Override
		protected TLAlias createDerivedEntity(TLAlias originalEntity) {
			TLAlias derivedAlias = new TLAlias();
			
			derivedAlias.setName( getDerivedEntityName( getOriginalEntityName(originalEntity) ) );
			return derivedAlias;
		}
		
	}
	
	/**
	 * Manages lists of <code>TLFacet</code> entities.
	 *
	 * @author S. Livezey
	 */
	protected static class FacetListManager extends ChildEntityListManager<TLFacet,TLFacetOwner> {
		
		private TLFacetType childFacetType;
		
		/**
		 * Constructor that specifies the owner of the unerlying list.
		 * 
		 * @param owner  the owner of the underlying list of children
		 * @param addEventType  the type of event to publish when a child entity is added
		 * @param removeEventType  the type of event to publish when a child entity is removed
		 */
		public FacetListManager(TLFacetOwner owner, TLFacetType childFacetType, ModelEventType addEventType, ModelEventType removeEventType) {
			super(owner, addEventType, removeEventType);
			this.childFacetType = childFacetType;
		}

		/**
		 * @see com.sabre.schemacompiler.model.ChildEntityListManager#getChildName(java.lang.Object)
		 */
		@Override
		protected String getChildName(TLFacet child) {
			StringBuilder childName = new StringBuilder();
			
			childName.append( (child.getContext() == null) ? "Unknown" : child.getContext() );
			
			if ((child.getLabel() != null) && (child.getLabel().length() > 0)) {
				childName.append(':').append(child.getLabel());
			}
			return childName.toString();
		}

		/**
		 * @see com.sabre.schemacompiler.model.ChildEntityListManager#assignOwner(java.lang.Object, java.lang.Object)
		 */
		@Override
		protected void assignOwner(TLFacet child, TLFacetOwner owner) {
			child.setFacetType(childFacetType);
			child.setOwningEntity(owner);
		}

		/**
		 * @see com.sabre.schemacompiler.model.ChildEntityListManager#publishEvent(java.lang.Object, com.sabre.schemacompiler.event.ModelEvent)
		 */
		@Override
		protected void publishEvent(TLFacetOwner owner, ModelEvent<?> event) {
			TLModel owningModel = owner.getOwningModel();
			
			if (owningModel != null) {
				owningModel.publishEvent(event);
			}
		}

	}
	
}
