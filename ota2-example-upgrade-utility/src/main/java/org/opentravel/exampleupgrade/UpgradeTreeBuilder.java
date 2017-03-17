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
package org.opentravel.exampleupgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.opentravel.schemacompiler.codegen.example.ExampleGeneratorOptions;
import org.opentravel.schemacompiler.codegen.example.ExampleValueGenerator;
import org.opentravel.schemacompiler.codegen.util.AliasCodegenUtils;
import org.opentravel.schemacompiler.codegen.util.FacetCodegenUtils;
import org.opentravel.schemacompiler.codegen.util.XsdCodegenUtils;
import org.opentravel.schemacompiler.ioc.SchemaDependency;
import org.opentravel.schemacompiler.model.AbstractLibrary;
import org.opentravel.schemacompiler.model.NamedEntity;
import org.opentravel.schemacompiler.model.TLAlias;
import org.opentravel.schemacompiler.model.TLAttribute;
import org.opentravel.schemacompiler.model.TLBusinessObject;
import org.opentravel.schemacompiler.model.TLChoiceObject;
import org.opentravel.schemacompiler.model.TLComplexTypeBase;
import org.opentravel.schemacompiler.model.TLCoreObject;
import org.opentravel.schemacompiler.model.TLExtensionPointFacet;
import org.opentravel.schemacompiler.model.TLFacet;
import org.opentravel.schemacompiler.model.TLFacetOwner;
import org.opentravel.schemacompiler.model.TLFacetType;
import org.opentravel.schemacompiler.model.TLIndicator;
import org.opentravel.schemacompiler.model.TLListFacet;
import org.opentravel.schemacompiler.model.TLMemberField;
import org.opentravel.schemacompiler.model.TLModel;
import org.opentravel.schemacompiler.model.TLProperty;
import org.opentravel.schemacompiler.model.TLSimpleFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;

/**
 * Utility class that handles the construction of the upgraded DOM tree structure
 * by comparing items from the OTM model and attempting to match them with the
 * original DOM example tree.
 */
public class UpgradeTreeBuilder {
	
    private static final Logger log = LoggerFactory.getLogger(UpgradeTreeBuilder.class);
    
    private Document upgradeDocument;
    private String defaultNamespace;
    private Map<String,String> namespaceMappings;
    private ExampleValueGenerator exampleValueGenerator;
    private ExampleGeneratorOptions exampleOptions;
	private Stack<UpgradeNodeContext> elementStack = new Stack<>();
	
	/**
	 * Constructor that specifies the options to use when generating unmatched sections
	 * of the upgraded example tree.
	 */
	public UpgradeTreeBuilder(ExampleGeneratorOptions exampleOptions) {
		try {
			upgradeDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
	        this.exampleValueGenerator = ExampleValueGenerator.getInstance( null );
			this.exampleOptions = exampleOptions;
			
		} catch (ParserConfigurationException e) {
			throw new Error("Unable to create DOM document instance.");
		}
	}
	
	/**
	 * Constructs the upgraded DOM tree along with the <code>TreeItem</code> structure that will
	 * be displayed in the visual interface.
	 * 
	 * @param otmEntity  the OTM entity for which to construct the upgraded DOM tree
	 * @param originalRoot  the root element of the original DOM tree
	 * @return TreeItem<DOMTreeUpgradeNode>
	 */
	public TreeItem<DOMTreeUpgradeNode> buildUpgradeDOMTree(NamedEntity otmEntity, Element originalRoot) {
		ExampleMatchType matchType = getMatchType( otmEntity, originalRoot );
		boolean foundMatch = ExampleMatchType.isMatch( matchType );
		
		initNamespaceMappings( otmEntity );
		clearReferenceFlags( originalRoot );
		
		if (ExampleMatchType.isMatch( matchType )) {
			markReferenced( originalRoot );
		}
		
		Element rootElement = createDomElement( otmEntity );
		DOMTreeUpgradeNode node = new DOMTreeUpgradeNode( otmEntity, rootElement, matchType );
		TreeItem<DOMTreeUpgradeNode> treeItem = new TreeItem<>( node );
		UpgradeModelVisitor visitor = new UMVisitor();
		UpgradeModelNavigator navigator = new UpgradeModelNavigator( visitor, otmEntity.getOwningModel(), exampleOptions );
		
		upgradeDocument.appendChild( rootElement );
		treeItem.setGraphic( new ImageView( AbstractDOMTreeNode.elementIcon ) );
		elementStack.push( new UpgradeNodeContext( treeItem,  originalRoot, otmEntity, !foundMatch ) );
		navigator.navigate( otmEntity );
		configureNamespaceDeclarations();
		
		return treeItem;
	}
	
	/**
	 * Constructs a new item for the upgrade tree structure using the given OTM attribute.
	 * 
	 * @param otmAttribute  the OTM attribute from which to create the tree item
	 */
	private void buildAttributeTreeItem(TLAttribute otmAttribute) {
		log.debug("buildAttributeTreeItem() : " + otmAttribute.getName());
		TreeItem<DOMTreeUpgradeNode> currentElementItem = elementStack.peek().getUpgradeItem();
		Element originalElement = elementStack.peek().getOriginalElement();
		Element upgradeElement = (Element) currentElementItem.getValue().getDomNode();
		Attr originalAttr = (originalElement == null) ?
				null : originalElement.getAttributeNode( otmAttribute.getName() );
		ExampleMatchType matchType = null;
		Attr upgradeAttr;
		
		if (originalAttr != null) {
			markReferenced( originalAttr );
			upgradeElement.setAttribute( otmAttribute.getName(), originalAttr.getValue() );
			upgradeAttr = upgradeElement.getAttributeNode( otmAttribute.getName() );
			matchType = ExampleMatchType.EXACT;
			
		} else if (otmAttribute.isMandatory() || elementStack.peek().isAutogenNode()) {
			upgradeElement.setAttribute( otmAttribute.getName(),
					exampleValueGenerator.getExampleValue( otmAttribute,
							currentElementItem.getValue().getOtmEntity() ) );
			upgradeAttr = upgradeElement.getAttributeNode( otmAttribute.getName() );
			matchType = ExampleMatchType.NONE;
			
		} else {
			matchType = ExampleMatchType.MISSING;
			upgradeAttr = upgradeDocument.createAttribute( otmAttribute.getName() );
		}
		
		TreeItem<DOMTreeUpgradeNode> attributeItem = new TreeItem<DOMTreeUpgradeNode>(
				new DOMTreeUpgradeNode( otmAttribute, upgradeAttr, matchType ) );
		
		attributeItem.setGraphic( new ImageView( AbstractDOMTreeNode.attributeIcon ) );
		currentElementItem.getChildren().add( attributeItem );
	}
	
	/**
	 * Constructs a new item for the upgrade tree structure using the given OTM indicator.
	 * 
	 * @param otmIndicator  the OTM indicator from which to create the tree item
	 */
	private void buildIndicatorTreeItem(TLIndicator otmIndicator) {
		log.debug("buildIndicatorTreeItem() : " + otmIndicator.getName());
		String indicatorName = otmIndicator.getName() + (otmIndicator.getName().endsWith( "Ind" ) ? "" : "Ind");
		TreeItem<DOMTreeUpgradeNode> currentElementItem = elementStack.peek().getUpgradeItem();
		Element originalElement = elementStack.peek().getOriginalElement();
		Element upgradeElement = (Element) currentElementItem.getValue().getDomNode();
		
		if (otmIndicator.isPublishAsElement()) {
			String ns = otmIndicator.getOwningLibrary().getNamespace();
			Element indicatorUpgradeElement = upgradeDocument.createElementNS(
					ns, qualifiedElementName( ns, indicatorName ) );
			Element originalIndicatorElement = elementStack.peek().findNextOriginalChild( indicatorUpgradeElement );
			TreeItem<DOMTreeUpgradeNode> elementItem;
			ExampleMatchType matchType;
			
			if ((originalIndicatorElement != null) || elementStack.peek().isAutogenNode())  {
				String indicatorValue = HelperUtils.getElementTextValue( originalIndicatorElement );
				
				if (indicatorValue == null) {
					indicatorValue = "false";
				}
				markReferenced( originalIndicatorElement );
				indicatorUpgradeElement.appendChild( upgradeDocument.createTextNode( indicatorValue ) );
				upgradeElement.appendChild( indicatorUpgradeElement );
				matchType = ExampleMatchType.EXACT;
				
			} else {
				matchType = ExampleMatchType.MISSING;
			}
			
			elementItem = new TreeItem<DOMTreeUpgradeNode>(
					new DOMTreeUpgradeNode( otmIndicator, indicatorUpgradeElement, matchType ) );
			elementItem.setGraphic( new ImageView( AbstractDOMTreeNode.elementIcon ) );
			currentElementItem.getChildren().add( elementItem );
			
		} else { // publish as attribute
			Attr originalAttr = (originalElement == null) ?
					null : originalElement.getAttributeNode( indicatorName );
			TreeItem<DOMTreeUpgradeNode> attributeItem;
			ExampleMatchType matchType;
			Attr upgradeAttr;
			
			if ((originalAttr != null) || elementStack.peek().isAutogenNode()) {
				markReferenced( originalAttr );
				upgradeElement.setAttribute( indicatorName,
						(originalAttr == null) ? "true" : originalAttr.getValue() );
				upgradeAttr = upgradeElement.getAttributeNode( indicatorName );
				matchType = ExampleMatchType.EXACT;
				
			} else {
				upgradeAttr = upgradeDocument.createAttribute( indicatorName );
				matchType = ExampleMatchType.MISSING;
			}
			
			attributeItem = new TreeItem<DOMTreeUpgradeNode>(
					new DOMTreeUpgradeNode( otmIndicator, upgradeAttr, matchType ) );
			attributeItem.setGraphic( new ImageView( AbstractDOMTreeNode.attributeIcon ) );
			currentElementItem.getChildren().add( attributeItem );
		}
	}
	
	/**
	 * Constructs a tree item for the given OTM element.  If the element is not generated
	 * for the upgrade tree, this method will return false; true otherwise.
	 * 
	 * @param otmElement  the OTM element for which to construct a tree item
	 * @return boolean
	 */
	private boolean buildElementTreeItem(TLProperty otmElement) {
		log.debug("buildElementTreeItem() : " + otmElement.getName());
		TreeItem<DOMTreeUpgradeNode> currentElementItem = elementStack.peek().getUpgradeItem();
		Element upgradeElement = (Element) currentElementItem.getValue().getDomNode();
		String upgradeElementNS = otmElement.getOwningLibrary().getNamespace();
		Element simpleChildUpgradeElement = upgradeDocument.createElementNS(
				upgradeElementNS, qualifiedElementName( upgradeElementNS, otmElement.getName() ) );
		Element originalChildElement = elementStack.peek().findNextOriginalChild( simpleChildUpgradeElement );
		boolean navigateChildren = true;
		boolean isAutoGen = false;
		NamedEntity elementType;
		
		// Find the next child element that matches the name/type of the OTM element
		if (originalChildElement != null) {
			elementType = otmElement.getType();
			
		} else {
			List<NamedEntity> candidateTypes = new ArrayList<>();
			NamedEntity otmElementType = otmElement.getType();
			
			if (otmElementType instanceof TLComplexTypeBase) {
				candidateTypes.addAll( FacetCodegenUtils.getAvailableFacets( (TLComplexTypeBase) otmElementType ) );
				
			} else if (isFacetOwnerAlias( otmElementType )) {
				candidateTypes.addAll( FacetCodegenUtils.getAvailableFacetAliases( (TLAlias) otmElementType ) );
				
			} else {
				candidateTypes.add( otmElementType );
			}
			elementType = candidateTypes.get( 0 ); // Default, just in case we cannot find a match
			
			for (NamedEntity candidateType : candidateTypes) {
				originalChildElement = elementStack.peek().findNextOriginalChild( candidateType );
				
				if (originalChildElement != null) {
					elementType = candidateType;
					break;
				}
			}
		}
		
		// Now we can process the element; differently depending on whether it is a globally-
		// defined or a simple type (or VWA)
		QName elementName = XsdCodegenUtils.getGlobalElementName( elementType );
		TreeItem<DOMTreeUpgradeNode> childElementItem;
		
		if ((elementName == null) || otmElement.isReference() ||
				((elementType instanceof TLListFacet) &&
				(((TLListFacet) elementType).getItemFacet() instanceof TLSimpleFacet))) {
			// Type is a simple value, simple list facet, open/closed enumeration, or VWA
			ExampleMatchType matchType;
			String elementValue = null;
			
			if (originalChildElement != null) {
				matchType = getMatchType( otmElement, originalChildElement );
				elementValue = HelperUtils.getElementTextValue( originalChildElement );
				markReferenced( originalChildElement );
				
				if (elementValue == null) {
					elementValue = exampleValueGenerator.getExampleValue(
							otmElement, currentElementItem.getValue().getOtmEntity() );
				}
				
			} else if (otmElement.isMandatory() || elementStack.peek().isAutogenNode()) {
				// Only create if a required element is missing from the original document
				matchType = ExampleMatchType.NONE;
				elementValue = exampleValueGenerator.getExampleValue(
						otmElement, currentElementItem.getValue().getOtmEntity() );
				isAutoGen = true;
				
			} else {
				matchType = ExampleMatchType.MISSING;
				childElementItem = new TreeItem<DOMTreeUpgradeNode>(
						new DOMTreeUpgradeNode( otmElement, simpleChildUpgradeElement, matchType ) );
				childElementItem.setGraphic( new ImageView( AbstractDOMTreeNode.elementIcon ) );
				currentElementItem.getChildren().add( childElementItem );
				navigateChildren = false;
			}
			
			if (navigateChildren) {
				simpleChildUpgradeElement.appendChild( upgradeDocument.createTextNode( elementValue ) );
				upgradeElement.appendChild( simpleChildUpgradeElement );
				childElementItem = new TreeItem<DOMTreeUpgradeNode>(
						new DOMTreeUpgradeNode( otmElement, simpleChildUpgradeElement, matchType ) );
				childElementItem.setGraphic( new ImageView( AbstractDOMTreeNode.elementIcon ) );
				currentElementItem.getChildren().add( childElementItem );
				elementStack.peek().nextOriginalChild();
				elementStack.push( new UpgradeNodeContext( childElementItem, originalChildElement, elementType, isAutoGen ) );
			}
			return navigateChildren;
			
		} else { // Must be a global element (not a list facet)
			ExampleMatchType matchType = getMatchType( elementType, originalChildElement );
			Element childUpgradeElement;
			
			if (ExampleMatchType.isMatch( matchType )) { // Found a match in the original document
				if ((matchType == ExampleMatchType.EXACT_SUBSTITUTABLE) ||
						(matchType == ExampleMatchType.PARTIAL_SUBSTITUTABLE)) {
					// Handle special case for substitutable element matches
					if (elementType instanceof TLFacet) {
						QName substitutableName = XsdCodegenUtils.getSubstitutableElementName( (TLFacet) elementType );
						
						childUpgradeElement = upgradeDocument.createElementNS(
								substitutableName.getNamespaceURI(), qualifiedElementName(
										substitutableName.getNamespaceURI(), substitutableName.getLocalPart() ) );
						
					} else if (isFacetAlias( elementType )) {
						QName substitutableName = XsdCodegenUtils.getSubstitutableElementName( (TLAlias) elementType );
						
						childUpgradeElement = upgradeDocument.createElementNS(
								substitutableName.getNamespaceURI(), qualifiedElementName(
										substitutableName.getNamespaceURI(), substitutableName.getLocalPart() ) );
						
					} else {
						childUpgradeElement = upgradeDocument.createElementNS(
								elementName.getNamespaceURI(), qualifiedElementName(
										elementName.getNamespaceURI(), elementName.getLocalPart() ) );
					}
					
				} else {
					childUpgradeElement = upgradeDocument.createElementNS(
							elementName.getNamespaceURI(), qualifiedElementName(
									elementName.getNamespaceURI(), elementName.getLocalPart() ) );
				}
				upgradeElement.appendChild( childUpgradeElement );
				markReferenced( originalChildElement );
				
			} else {
				// Find out if the match type is NONE or MISSING
				if (!otmElement.isMandatory() && !elementStack.peek().isAutogenNode()) {
					matchType = ExampleMatchType.MISSING;
					navigateChildren = false;
				}
				
				// No match from original document, so we must auto-generate the element
				if (elementType instanceof TLComplexTypeBase) {
					QName substitutableName;
					
					elementType = getPreferredFacet( elementType );
					substitutableName = XsdCodegenUtils.getSubstitutableElementName( (TLFacet) elementType );
					childUpgradeElement = upgradeDocument.createElementNS(
							substitutableName.getNamespaceURI(), qualifiedElementName(
									substitutableName.getNamespaceURI(), substitutableName.getLocalPart() ) );
					
				} else if (isFacetOwnerAlias( elementType )) {
					QName substitutableName;
					
					elementType = getPreferredFacet( elementType );
					substitutableName = XsdCodegenUtils.getSubstitutableElementName( (TLAlias) elementType );
					childUpgradeElement = upgradeDocument.createElementNS(
							substitutableName.getNamespaceURI(), qualifiedElementName(
									substitutableName.getNamespaceURI(), substitutableName.getLocalPart() ) );
					
				} else {
					childUpgradeElement = upgradeDocument.createElementNS(
							elementName.getNamespaceURI(), qualifiedElementName(
									elementName.getNamespaceURI(), elementName.getLocalPart() ) );
				}
				
				if (matchType == ExampleMatchType.NONE) {
					upgradeElement.appendChild( childUpgradeElement );
				}
				isAutoGen = true;
			}
			
			childElementItem = new TreeItem<DOMTreeUpgradeNode>(
					new DOMTreeUpgradeNode( elementType, childUpgradeElement, matchType ) );
			childElementItem.setGraphic( new ImageView( AbstractDOMTreeNode.elementIcon ) );
			currentElementItem.getChildren().add( childElementItem );
			
			if (ExampleMatchType.isMatch( matchType )) {
				elementStack.peek().nextOriginalChild();
			}
			
			if (navigateChildren) {
				elementStack.push( new UpgradeNodeContext(
						childElementItem, originalChildElement, elementType, isAutoGen ) );
			}
			
			return navigateChildren;
		}
	}
	
	/**
	 * Completes the processing of the given OTM element.
	 * 
	 * @param otmElement  the OTM element being navigated
	 */
	private void finishElementTreeItem(TLProperty otmElement) {
		log.debug("finishElementTreeItem() : name=" + otmElement.getName() + " / type=" + otmElement.getType().getLocalName());
		elementStack.pop();
	}
	
	/**
	 * Constructs a tree item for an extension point group of the given type.
	 * 
	 * @param extensionPointType  the facet type of the extension point group
	 */
	public boolean buildExtensionPointGroupTreeItemStart(TLFacetType extensionPointType) {
		log.debug("buildExtensionPointGroupTreeItemStart() : " + extensionPointType);
		boolean processChildren = false;
		QName elementName;
		
		switch (extensionPointType) {
			case SUMMARY:
				elementName = SchemaDependency.getExtensionPointSummaryElement().toQName();
				break;
			case SHARED:
				elementName = SchemaDependency.getExtensionPointSharedElement().toQName();
				break;
			default:
				elementName = SchemaDependency.getExtensionPointElement().toQName();
				break;
		}
		Element childUpgradeElement = upgradeDocument.createElementNS(
				elementName.getNamespaceURI(), qualifiedElementName(
						elementName.getNamespaceURI(), elementName.getLocalPart() ) );
		Element originalChildElement = elementStack.peek().findNextOriginalChild( childUpgradeElement );
		
		if (originalChildElement != null) {
			TreeItem<DOMTreeUpgradeNode> childElementItem = new TreeItem<DOMTreeUpgradeNode>(
					new DOMTreeUpgradeNode( (NamedEntity) null, childUpgradeElement, ExampleMatchType.EXACT ) );
			
			childElementItem.setGraphic( new ImageView( AbstractDOMTreeNode.elementIcon ) );
			elementStack.push( new UpgradeNodeContext( childElementItem, originalChildElement, null, false ) );
			markReferenced( originalChildElement );
			processChildren = true;
		}
		return processChildren;
	}

	/**
	 * Completes the tree item for an extension point group of the given type.  The
	 * extension point group element is only added to the parent DOM element if one
	 * or more extension point child elements were actually created.
	 * 
	 * @param extensionPointType  the facet type of the extension point group
	 */
	public void buildExtensionPointGroupTreeItemEnd(TLFacetType extensionPointType) {
		log.debug("buildExtensionPointGroupTreeItemEnd() : " + extensionPointType);
		TreeItem<DOMTreeUpgradeNode> extensionPointElementItem = elementStack.pop().getUpgradeItem();
		TreeItem<DOMTreeUpgradeNode> parentElementItem = elementStack.peek().getUpgradeItem();
		
		if (extensionPointElementItem.getChildren().size() > 0) {
			Element extensionPointElement = (Element) extensionPointElementItem.getValue().getDomNode();
			Element parentElement = (Element) parentElementItem.getValue().getDomNode();
			
			parentElement.appendChild( extensionPointElement );
			parentElementItem.getChildren().add( extensionPointElementItem );
		}
	}
	
	/**
	 * Constructs a tree item for the given OTM extension point facet.  If the
	 * extension point facet is not generated for the upgrade tree, this method
	 * will return false; true otherwise.
	 * 
	 * @param otmExtensionPoint  the OTM extension point facet for which to construct a tree item
	 * @return boolean
	 */
	private boolean buildExtensionPointFacetTreeItem(TLExtensionPointFacet otmExtensionPoint) {
		log.debug("buildExtensionPointFacetTreeItem() : " + otmExtensionPoint.getLocalName());
		TreeItem<DOMTreeUpgradeNode> extensionPointElementItem = elementStack.peek().getUpgradeItem();
		QName elementName = XsdCodegenUtils.getGlobalElementName( otmExtensionPoint );
		Element childUpgradeElement = upgradeDocument.createElementNS(
				elementName.getNamespaceURI(), qualifiedElementName(
						elementName.getNamespaceURI(), elementName.getLocalPart() ) );
		Node originalChildNode = elementStack.peek().getOriginalElement().getFirstChild();
		boolean isAutoGen = elementStack.peek().isAutogenNode();
		ExampleMatchType matchType = null;
		boolean processChildren = false;
		Element originalChild = null;
		
		
		// Since extension points are unordered, we need to search the entire list instead
		// of treating it as a sequence (as with normal elements)
		while (originalChildNode != null) {
			if (originalChildNode.getNodeType() == Node.ELEMENT_NODE) {
				matchType = getMatchType( childUpgradeElement, (Element) originalChildNode );
				
				if (ExampleMatchType.isMatch( matchType )) {
					originalChild = (Element) originalChildNode;
					break;
				}
			}
			originalChildNode = originalChildNode.getNextSibling();
		}
		
		// Only create a new element if we find a match in the original
		// document (or we are auto-generating)
		if ((originalChild != null) || isAutoGen) {
			TreeItem<DOMTreeUpgradeNode> childElementItem = new TreeItem<DOMTreeUpgradeNode>(
					new DOMTreeUpgradeNode( otmExtensionPoint, childUpgradeElement, matchType ) );
			
			childElementItem.setGraphic( new ImageView( AbstractDOMTreeNode.elementIcon ) );
			extensionPointElementItem.getValue().getDomNode().appendChild( childUpgradeElement );
			extensionPointElementItem.getChildren().add( childElementItem );
			elementStack.push( new UpgradeNodeContext( childElementItem, originalChild, otmExtensionPoint, isAutoGen ) );
			markReferenced( originalChild );
			processChildren = true;
		}
		return processChildren;
	}
	
	/**
	 * Completes the processing of the given OTM extension point facet.
	 * 
	 * @param otmExtensionPoint  the OTM extension point facet being navigated
	 */
	private void finishExtensionPointFacetTreeItem(TLExtensionPointFacet otmExtensionPoint) {
		log.debug("finishExtensionPointFacetTreeItem() : " + otmExtensionPoint.getLocalName());
		elementStack.pop();
	}
	
	/**
	 * Returns the type of match between the given OTM entity and the DOM element provided.
	 * 
	 * @param otmEntity  the OTM entity with which to compare the DOM element
	 * @param domElement  the DOM element to be compared
	 * @return ExampleMatchType
	 */
	private ExampleMatchType getMatchType(NamedEntity otmEntity, Element domElement) {
		QName entityName = XsdCodegenUtils.getGlobalElementName( otmEntity );
		ExampleMatchType matchType = ExampleMatchType.NONE;
		
		if ((entityName != null) && (domElement != null)) {
			// First, attempt to match against the non-substitutable element name
			if (entityName.getLocalPart().equals( domElement.getLocalName() )) {
				matchType = getMatchType( entityName.getNamespaceURI(), domElement.getNamespaceURI() );
			}
			
			// If no match was found yet, check the non-substitutable element name (if one exists for the entity)
			if (!ExampleMatchType.isMatch( matchType )) {
				QName substitutableName = null;
				
				if (otmEntity instanceof TLFacet) {
					substitutableName = XsdCodegenUtils.getSubstitutableElementName( (TLFacet) otmEntity );
					
				} else if (isFacetAlias( otmEntity )) {
					substitutableName = XsdCodegenUtils.getSubstitutableElementName( (TLAlias) otmEntity );
				}
				
				if ((substitutableName != null) &&
						(substitutableName.getLocalPart().equals( domElement.getLocalName() ) ) ) {
					matchType = getMatchType( substitutableName.getNamespaceURI(), domElement.getNamespaceURI() );
					
					if (matchType == ExampleMatchType.EXACT) {
						matchType = ExampleMatchType.EXACT_SUBSTITUTABLE;
						
					} else if (matchType == ExampleMatchType.PARTIAL) {
						matchType = ExampleMatchType.PARTIAL_SUBSTITUTABLE;
					}
				}
			}
		}
		return matchType;
	}
	
	/**
	 * Returns the type of match between the given OTM entity and the DOM element provided.
	 * 
	 * @param otmEntity  the OTM entity with which to compare the DOM element
	 * @param domElement  the DOM element to be compared
	 * @return ExampleMatchType
	 */
	private ExampleMatchType getMatchType(Element originalElement, Element upgradeElement) {
		QName entityName = new QName( upgradeElement.getNamespaceURI(), upgradeElement.getLocalName() );
		ExampleMatchType matchType = ExampleMatchType.NONE;
		
		if (entityName.getLocalPart().equals( originalElement.getLocalName() )) {
			matchType = getMatchType( entityName.getNamespaceURI(), originalElement.getNamespaceURI() );
		}
		return matchType;
	}
	
	/**
	 * Returns the type of match between the given OTM entity and the DOM element provided.
	 * 
	 * @param otmField  the OTM entity with which to compare the DOM element
	 * @param domElement  the DOM element to be compared
	 * @return ExampleMatchType
	 */
	private ExampleMatchType getMatchType(TLMemberField<?> otmField, Element domElement) {
		ExampleMatchType matchType;
		
		if (otmField.getName().equals( domElement.getLocalName() )) {
			matchType = ExampleMatchType.EXACT;
		} else {
			matchType = ExampleMatchType.NONE;
		}
		return matchType;
	}
	
	/**
	 * Returns the match type between the two namespace URI's provided.  A partial match
	 * will result if the two base namespaces are the same, but versions are different.
	 * 
	 * @param ns1  the first namespace URI
	 * @param ns2  the second namespace URI
	 * @return ExampleMatchType
	 */
	private ExampleMatchType getMatchType(String ns1, String ns2) {
		ExampleMatchType matchType = ExampleMatchType.NONE;
		
		if (ns1.equals( ns2 )) {
			matchType = ExampleMatchType.EXACT;
			
		} else {
			String baseNS1 = HelperUtils.getBaseNamespace( ns1 );
			String baseNS2 = HelperUtils.getBaseNamespace( ns2 );
			
			if (baseNS1.equals( baseNS2 )) {
				matchType = ExampleMatchType.PARTIAL;
			}
		}
		return matchType;
	}
	
	/**
	 * Creates a new DOM element for the given entity.
	 * 
	 * @param entity  the OTM entity for which to create a DOM element
	 * @return Element
	 */
	private Element createDomElement(NamedEntity entity) {
		QName entityName = XsdCodegenUtils.getGlobalElementName( entity );
		Element domElement;
		
		if (entityName != null) {
			domElement = upgradeDocument.createElementNS(
					entityName.getNamespaceURI(), qualifiedElementName(
							entityName.getNamespaceURI(), entityName.getLocalPart() ) );
			
		} else {
			throw new IllegalArgumentException("The OTM entity does not define a global XML element.");
		}
		return domElement;
	}
	
	/**
	 * Returns true if the given OTM entity is an alias of a <code>TLFacet</code> object.
	 * 
	 * @param otmEntity  the OTM entity to check
	 * @return boolean
	 */
	private boolean isFacetAlias(NamedEntity otmEntity) {
		return (otmEntity instanceof TLAlias) &&
				(((TLAlias) otmEntity).getOwningEntity() instanceof TLFacet);
	}
	
	/**
	 * Returns true if the given OTM entity is an alias of a <code>TLComplexTypeBase</code> object.
	 * 
	 * @param otmEntity  the OTM entity to check
	 * @return boolean
	 */
	private boolean isFacetOwnerAlias(NamedEntity otmEntity) {
		return (otmEntity instanceof TLAlias) &&
				(((TLAlias) otmEntity).getOwningEntity() instanceof TLComplexTypeBase);
	}
	
	/**
	 * For an OTM parent (facet owner), the "preferred" or default facet will be returned.
	 * 
	 * @param otmParentEntity  the OTM parent entity that owns the facet to be returned
	 * @return NamedEntity
	 */
	private NamedEntity getPreferredFacet(NamedEntity otmParentEntity) {
		TLComplexTypeBase owner = null;
		TLAlias ownerAlias = null;
		TLFacet preferredFacet;
		
		if (otmParentEntity instanceof TLAlias) {
			ownerAlias = (TLAlias) otmParentEntity;
			otmParentEntity = ownerAlias.getOwningEntity();
		}
		if (otmParentEntity instanceof TLComplexTypeBase) {
			owner = (TLComplexTypeBase) otmParentEntity;
		} else {
			throw new IllegalArgumentException("Entity must be an OTM complex type or an alias of one.");
		}
		
		preferredFacet = exampleOptions.getPreferredFacet( (TLFacetOwner) owner );
		
		// Apply default lookups if no preferred facet selection is specified by the user
		if (preferredFacet == null) {
			if (owner instanceof TLBusinessObject) {
				preferredFacet = ((TLBusinessObject) owner).getSummaryFacet();
				
			} else if (owner instanceof TLChoiceObject) {
				TLChoiceObject choice = (TLChoiceObject) owner;
				
				preferredFacet = (choice.getChoiceFacets().size() == 0) ?
						choice.getSharedFacet() : choice.getChoiceFacets().get( 0 );
						
			} else if (owner instanceof TLCoreObject) {
				preferredFacet = ((TLCoreObject) owner).getSummaryFacet();
				
			} else {
				throw new IllegalArgumentException("Unknown complex type: " + owner.getClass().getSimpleName());
			}
		}
		
		if (ownerAlias != null) {
			return AliasCodegenUtils.getFacetAlias( ownerAlias, preferredFacet.getFacetType() );
			
		} else {
			return preferredFacet;
		}
	}
	
	/**
	 * Initializes the namespace mappings that will be used throughout the upgraded
	 * XML document.
	 * 
	 * @param otmEntity  the OTM entity that will serve as the root of the XML document
	 */
	private void initNamespaceMappings(NamedEntity otmEntity) {
		TLModel model = otmEntity.getOwningModel();
		
		this.defaultNamespace = otmEntity.getNamespace();
		this.namespaceMappings = new HashMap<>();
		
		for (AbstractLibrary library : model.getAllLibraries()) {
			String ns = library.getNamespace();
			
			if (!namespaceMappings.containsKey( ns )) {
				String prefix = library.getPrefix();
				int count = 0;
				
				// Make sure this prefix is unique to the entire model
				while (namespaceMappings.containsValue( prefix )) {
					prefix = library.getPrefix() + (++count);
				}
				namespaceMappings.put( ns, prefix );
			}
		}
	}
	
	/**
	 * Returns the qualified name of a DOM element with the given namespace and
	 * local name.
	 * 
	 * @param namespace  the namespace URI of the element
	 * @param localName  the local name of the element
	 * @return
	 */
	private String qualifiedElementName(String namespace, String localName) {
		String prefix = defaultNamespace.equals( namespace ) ? null : namespaceMappings.get( namespace );
		String qName;
		
		if (prefix != null) {
			qName = prefix + ":" + localName;
		} else {
			qName = localName;
		}
		return qName;
	}
	
	/**
	 * Adds the necessary namespace declarations to the root element of the upgrade
	 * DOM document tree.
	 */
	private void configureNamespaceDeclarations() {
		Element rootElement = upgradeDocument.getDocumentElement();
		Set<String> usedNamespaces = new HashSet<>();
		
		// Start by deleting all existing namespace declarations
		NamedNodeMap attrs = rootElement.getAttributes();
		int attrCount = attrs.getLength();
		
		for (int i = 0; i < attrCount; i++) {
			Attr domAttr = (Attr) attrs.item( i );
			
			if (domAttr.getNodeName().equals("xmlns")
					|| domAttr.getNodeName().contains(":")) {
				rootElement.removeAttributeNode( domAttr );
			}
		}
		
		// Now create namespace declarations for every namespace used in the document
		findNamespaceURIs( rootElement, usedNamespaces );
		
		for (String ns : usedNamespaces) {
			if (!ns.equals( defaultNamespace )) {
				rootElement.setAttribute( "xmlns:" + namespaceMappings.get( ns ), ns );
			}
		}
	}
	
	/**
	 * Recursively searches the given element, recording all namespace URI's that are
	 * encountered.
	 * 
	 * @param element  the DOM element to search
	 * @param usedNamespaces  the list of namespace URI's being assembled
	 */
	private void findNamespaceURIs(Element element, Set<String> usedNamespaces) {
		Node domChild = element.getFirstChild();
		String ns = element.getNamespaceURI();
		
		if (ns != null) {
			usedNamespaces.add( ns );
		}
		
		while (domChild != null) {
			if (domChild.getNodeType() == Node.ELEMENT_NODE) {
				findNamespaceURIs( (Element) domChild, usedNamespaces );
			}
			domChild = domChild.getNextSibling();
		}
	}
	
	/**
	 * Clears all of the 'isReferenced' flags from the elements and attributes
	 * of the original DOM tree.
	 * 
	 * @param originalDomNode  the root of the DOM tree for which to clear all flags
	 */
	private void clearReferenceFlags(Node originalDomNode) {
		Node domChild = originalDomNode.getFirstChild();
		
		originalDomNode.setUserData( DOMTreeOriginalNode.IS_REFERENCED_KEY, null, null );
		
		while (domChild != null) {
			if ((domChild.getNodeType() == Node.ELEMENT_NODE)
					|| (domChild.getNodeType() == Node.ATTRIBUTE_NODE)) {
				clearReferenceFlags( domChild );
			}
			domChild = domChild.getNextSibling();
		}
	}
	
	/**
	 * Marks the given original DOM node as referenced by the upgrade tree.
	 * 
	 * @param originalDomNode  the original DOM node to mark as referenced
	 */
	private void markReferenced(Node originalDomNode) {
		if (originalDomNode != null) {
			originalDomNode.setUserData( DOMTreeOriginalNode.IS_REFERENCED_KEY, Boolean.TRUE, null );
		}
	}
	
	/**
	 * Context that captures the pairing between the upgrade tree node and the original
	 * DOM element (if any).
	 */
	private class UpgradeNodeContext {
		
		private TreeItem<DOMTreeUpgradeNode> upgradeItem;
		private Element originalElement;
		private Node nextOriginalChild;
		private NamedEntity otmElementType;
		private boolean autogenNode = false;
		
		/**
		 * Full constructor.
		 * 
		 * @param upgradeItem  the tree item for the upgrade tree
		 * @param originalElement  the original DOM element from which the upgrade item was created (may be null)
		 * @param otmElementType  the actual type of the OTM element
		 * @param autoGenNode flag value indicating whethe the node's context was created from an auto-generated example
		 */
		public UpgradeNodeContext(TreeItem<DOMTreeUpgradeNode> upgradeItem,
				Element originalElement, NamedEntity otmElementType, boolean autoGenNode) {
			this.upgradeItem = upgradeItem;
			this.originalElement = originalElement;
			this.otmElementType = otmElementType;
			this.autogenNode = autoGenNode;
			this.nextOriginalChild = (originalElement == null) ? null : originalElement.getFirstChild();
			advanceToNextOriginalChild();
		}
		
		/**
		 * Returns the tree item for the upgrade tree.
		 *
		 * @return TreeItem<DOMTreeUpgradeNode>
		 */
		public TreeItem<DOMTreeUpgradeNode> getUpgradeItem() {
			return upgradeItem;
		}
		
		/**
		 * Returns the original DOM element from which the upgrade item was created (may be null).
		 *
		 * @return Element
		 */
		public Element getOriginalElement() {
			return originalElement;
		}
		
		/**
		 * Returns the actual type of the OTM element (may be different than the
		 * model element due to substitution group selections in the original DOM
		 * document).
		 *
		 * @return NamedEntity
		 */
		public NamedEntity getOtmElementType() {
			return otmElementType;
		}

		/**
		 * Returns true if the node's context was created from an auto-generated
		 * example.
		 *
		 * @return boolean
		 */
		public boolean isAutogenNode() {
			return autogenNode;
		}

		/**
		 * Returns the next DOM child element under the original child element and advances
		 * to the subsequent child.
		 *
		 * @return Element
		 */
		public Element nextOriginalChild() {
			Element nextChild = (Element) nextOriginalChild;
			advanceToNextOriginalChild();
			
			return nextChild;
		}
		
		/**
		 * Returns the next DOM child element under the original child element without
		 * advancing to the subsequent child.
		 *
		 * @return Element
		 */
		public Element peekNextOriginalChild() {
			return (Element) nextOriginalChild;
		}
		
		/**
		 * Returns the next original child in the list which matches (either partial or exact)
		 * the upgraded element provided.  If a matching element is found, it will be assigned as
		 * the next-original-child.
		 * 
		 * @param upgradeElement  the upgraded element for which to return a matching child
		 * @return Element
		 */
		public Element findNextOriginalChild(Element upgradeElement) {
			Element nextChild = peekNextOriginalChild();
			
			while (nextChild != null) {
				Node nextChildNode = nextChild;
				
				if (ExampleMatchType.isMatch( getMatchType( nextChild, upgradeElement ) )) {
					nextOriginalChild = nextChild;
					break;
				}
				
				nextChild = null;
				
				while (nextChildNode != null) {
					nextChildNode = nextChildNode.getNextSibling();
					
					if ((nextChildNode != null) &&
							(nextChildNode.getNodeType() == Node.ELEMENT_NODE)) {
						nextChild = (Element) nextChildNode;
						break;
					}
				}
			}
			return nextChild;
		}
		
		/**
		 * Returns the next original child in the list which matches (either partial or exact)
		 * the OTM entity provided.  If a matching element is found, it will be assigned as
		 * the next-original-child.
		 * 
		 * @param upgradeElement  the upgraded element for which to return a matching child
		 * @return Element
		 */
		public Element findNextOriginalChild(NamedEntity otmEntity) {
			Element nextChild = peekNextOriginalChild();
			
			while (nextChild != null) {
				Node nextChildNode = nextChild;
				
				if (ExampleMatchType.isMatch( getMatchType( otmEntity, nextChild ) )) {
					nextOriginalChild = nextChild;
					break;
				}
				
				nextChild = null;
				
				while (nextChildNode != null) {
					nextChildNode = nextChildNode.getNextSibling();
					
					if ((nextChildNode != null) &&
							(nextChildNode.getNodeType() == Node.ELEMENT_NODE)) {
						nextChild = (Element) nextChildNode;
						break;
					}
				}
			}
			return nextChild;
		}
		
		/**
		 * Advances to the next DOM element child of the original DOM element.  If the end
		 * of the child list has been reached, this method will return null.
		 */
		private void advanceToNextOriginalChild() {
			while (nextOriginalChild != null) {
				nextOriginalChild = nextOriginalChild.getNextSibling();
				
				if ((nextOriginalChild != null) &&
						(nextOriginalChild.getNodeType() == Node.ELEMENT_NODE)) {
					break;
				}
			}
		}
		
	}
	
	/**
	 * Implementation of the <code>UpgradeModelVisitor</code> that invokes callbacks
	 * to the private methods of this class.
	 */
	private class UMVisitor extends UpgradeModelVisitor {

		/**
		 * @see org.opentravel.exampleupgrade.UpgradeModelVisitor#getResolvedElementType()
		 */
		@Override
		public NamedEntity getResolvedElementType() {
			return elementStack.peek().getOtmElementType();
		}

		/**
		 * @see org.opentravel.exampleupgrade.UpgradeModelVisitor#canRepeat(org.opentravel.schemacompiler.model.TLProperty, org.opentravel.schemacompiler.model.NamedEntity)
		 */
		@Override
		public boolean canRepeat(TLProperty otmElement, NamedEntity resolvedElementType) {
			Element nextOriginalChild = elementStack.peek().peekNextOriginalChild();
			boolean repeatAllowed = false;
			
			if (nextOriginalChild != null) {
				boolean hasGlobalElement = (XsdCodegenUtils.getGlobalElementName( resolvedElementType ) != null);
				ExampleMatchType matchType;
				
				if (hasGlobalElement) {
					matchType = getMatchType( resolvedElementType, nextOriginalChild );
					
				} else { // check for same element name
					matchType = getMatchType( otmElement, nextOriginalChild );
				}
				repeatAllowed = ExampleMatchType.isMatch( matchType );
			}
			return repeatAllowed;
		}

		/**
		 * @see org.opentravel.exampleupgrade.UpgradeModelVisitor#isAutoGenerationEnabled()
		 */
		@Override
		public boolean isAutoGenerationEnabled() {
			return elementStack.peek().isAutogenNode();
		}

		/**
		 * @see org.opentravel.schemacompiler.visitor.ModelElementVisitorAdapter#visitAttribute(org.opentravel.schemacompiler.model.TLAttribute)
		 */
		@Override
		public boolean visitAttribute(TLAttribute attribute) {
			buildAttributeTreeItem( attribute );
			return true;
		}

		/**
		 * @see org.opentravel.schemacompiler.visitor.ModelElementVisitorAdapter#visitIndicator(org.opentravel.schemacompiler.model.TLIndicator)
		 */
		@Override
		public boolean visitIndicator(TLIndicator indicator) {
			buildIndicatorTreeItem( indicator );
			return true;
		}

		/**
		 * @see org.opentravel.schemacompiler.visitor.ModelElementVisitorAdapter#visitElement(org.opentravel.schemacompiler.model.TLProperty)
		 */
		@Override
		public boolean visitElement(TLProperty element) {
			return buildElementTreeItem( element );
		}

		/**
		 * @see org.opentravel.exampleupgrade.UpgradeModelVisitor#visitElementEnd(org.opentravel.schemacompiler.model.TLProperty)
		 */
		@Override
		public boolean visitElementEnd(TLProperty element) {
			finishElementTreeItem( element );
			return true;
		}

		/**
		 * @see org.opentravel.exampleupgrade.UpgradeModelVisitor#visitExtensionPointGroupStart(org.opentravel.schemacompiler.model.TLFacetType)
		 */
		@Override
		public boolean visitExtensionPointGroupStart(TLFacetType extensionPointType) {
			return buildExtensionPointGroupTreeItemStart( extensionPointType );
		}

		/**
		 * @see org.opentravel.exampleupgrade.UpgradeModelVisitor#visitExtensionPointGroupEnd(org.opentravel.schemacompiler.model.TLFacetType)
		 */
		@Override
		public void visitExtensionPointGroupEnd(TLFacetType extensionPointType) {
			buildExtensionPointGroupTreeItemEnd( extensionPointType );
		}

		/**
		 * @see org.opentravel.schemacompiler.visitor.ModelElementVisitorAdapter#visitExtensionPointFacet(org.opentravel.schemacompiler.model.TLExtensionPointFacet)
		 */
		@Override
		public boolean visitExtensionPointFacet(TLExtensionPointFacet extensionPointFacet) {
			return buildExtensionPointFacetTreeItem( extensionPointFacet );
		}
		
		/**
		 * @see org.opentravel.exampleupgrade.UpgradeModelVisitor#visitExtensionPointEnd(org.opentravel.schemacompiler.model.TLExtensionPointFacet)
		 */
		@Override
		public boolean visitExtensionPointEnd(TLExtensionPointFacet extensionPointFacet) {
			finishExtensionPointFacetTreeItem( extensionPointFacet );
			return true;
		}

	}
	
}
