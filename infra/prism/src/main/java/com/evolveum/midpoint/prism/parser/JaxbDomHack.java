/*
 * Copyright (c) 2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * A set of ugly hacks that are needed for prism and "real" JAXB to coexist. We hate it be we need it.
 * This is a mix of DOM and JAXB code that allows the use of "any" methods on JAXB-generated objects.
 * Prism normally does not use of of that. But JAXB code (such as JAX-WS) can invoke it and therefore
 * it has to return correct DOM/JAXB elements as expected.
 *  
 * @author Radovan Semancik
 */
public class JaxbDomHack {
	
	private static final Trace LOGGER = TraceManager.getTrace(JaxbDomHack.class);
	
	private PrismContext prismContext;
	private DomParser domParser;
	private JAXBContext jaxbContext;

	public JaxbDomHack(DomParser domParser, PrismContext prismContext) {
		super();
		this.domParser = domParser;
		this.prismContext = prismContext;
		initializeJaxbContext();
	}

	private void initializeJaxbContext() {
		StringBuilder sb = new StringBuilder();
		Iterator<Package> iterator = prismContext.getSchemaRegistry().getCompileTimePackages().iterator();
		while (iterator.hasNext()) {
			Package jaxbPackage = iterator.next();
			sb.append(jaxbPackage.getName());
			if (iterator.hasNext()) {
				sb.append(":");
			}
		}
		String jaxbPaths = sb.toString();
		if (jaxbPaths.isEmpty()) {
			LOGGER.debug("No JAXB paths, skipping creation of JAXB context");
		} else {
			try {
				jaxbContext = JAXBContext.newInstance(jaxbPaths);
			} catch (JAXBException ex) {
				throw new SystemException("Couldn't create JAXBContext for: " + jaxbPaths, ex);
			}
		}
	}
	
	/**
	 * This is used in a form of "fromAny" to parse elements from a JAXB getAny method to prism. 
	 */
	public <V extends PrismValue,C extends Containerable> Item<V> parseRawElement(Object element, PrismContainerDefinition<C> definition) throws SchemaException {
		Validate.notNull(definition, "Attempt to parse raw element in a container without definition");
		
		QName elementName = JAXBUtil.getElementQName(element);
		ItemDefinition itemDefinition = definition.findItemDefinition(elementName);
		PrismContext prismContext = definition.getPrismContext();
		Item<V> subItem;
		if (element instanceof Element) {
			// DOM Element
			DomParser domParser = prismContext.getParserDom();
			XNode xnode = domParser.parseElementContent((Element)element);
			subItem = prismContext.getXnodeProcessor().parseItem(xnode, elementName, itemDefinition);
		} else if (element instanceof JAXBElement<?>) {
			// JAXB Element
			JAXBElement<?> jaxbElement = (JAXBElement<?>)element;
			Object jaxbBean = jaxbElement.getValue();
			if (itemDefinition == null) {
				throw new SchemaException("No definition for item "+elementName+" in container "+definition+" (parsed from raw element)", elementName);
			}
			if (itemDefinition instanceof PrismPropertyDefinition<?>) {
				// property
				PrismProperty<?> property = ((PrismPropertyDefinition<?>)itemDefinition).instantiate();
				property.setRealValue(jaxbBean);
				subItem = (Item<V>) property;
			} else if (itemDefinition instanceof PrismContainerDefinition<?>) {
				if (jaxbBean instanceof Containerable) {
					PrismContainer<?> container = ((PrismContainerDefinition<?>)itemDefinition).instantiate();
					PrismContainerValue subValue = ((Containerable)jaxbBean).asPrismContainerValue();
					container.add(subValue);
					subItem = (Item<V>) container;
				} else {
					throw new IllegalArgumentException("Unsupported JAXB bean "+jaxbBean.getClass());
				}
			} else if (itemDefinition instanceof PrismReferenceDefinition) {
			
//				TODO;
				throw new UnsupportedOperationException();
				
			} else {
				throw new IllegalArgumentException("Unsupported definition type "+itemDefinition.getClass());
			}
		} else {
			throw new IllegalArgumentException("Unsupported element type "+element.getClass());
		}	
		return subItem;
	}
	
	public <V extends PrismValue, C extends Containerable> Collection<Item<V>> fromAny(List<Object> anyObjects, PrismContainerDefinition<C> definition) throws SchemaException {
		Collection<Item<V>> items = new ArrayList<>();
		for (Object anyObject: anyObjects) {
			Item<V> newItem = parseRawElement(anyObject, definition);
			boolean merged = false;
			for (Item<V> existingItem: items) {
				if (newItem.getElementName().equals(existingItem.getElementName())) {
					existingItem.merge(newItem);
					merged = true;
					break;
				}
			}
			if (!merged) {
				items.add(newItem);
			}
		}
		return items;
	}

	/**
	 * Serializes prism value to JAXB "any" format as returned by JAXB getAny() methods. 
	 */
	public Object toAny(PrismValue value) throws SchemaException {
		Document document = DOMUtil.getDocument();
		if (value == null) {
			return value;
		}
		QName elementName = value.getParent().getElementName();
		Object xmlValue;
		if (value instanceof PrismPropertyValue) {
			PrismPropertyValue<Object> pval = (PrismPropertyValue)value;
			Object realValue = pval.getValue();
        	xmlValue = realValue;
        	if (XmlTypeConverter.canConvert(realValue.getClass())) {
        		// Always record xsi:type. This is FIXME, but should work OK for now (until we put definition into deltas)
        		xmlValue = XmlTypeConverter.toXsdElement(realValue, elementName, document, true);
        	}
		} else if (value instanceof PrismReferenceValue) {
			PrismReferenceValue rval = (PrismReferenceValue)value;
			xmlValue =  domParser.serializeValueToDom(rval, elementName, document);
		} else if (value instanceof PrismContainerValue<?>) {
			PrismContainerValue<?> pval = (PrismContainerValue<?>)value;
			if (pval.getParent().getCompileTimeClass() == null) {
				// This has to be runtime schema without a compile-time representation.
				// We need to convert it to DOM
				xmlValue =  domParser.serializeValueToDom(pval, elementName, document);
			} else {
				xmlValue = pval.asContainerable();
			}
		} else {
			throw new IllegalArgumentException("Unknown type "+value);
		}
		if (!(xmlValue instanceof Element) && !(xmlValue instanceof JAXBElement)) {
    		xmlValue = new JAXBElement(elementName, xmlValue.getClass(), xmlValue);
    	}
        return xmlValue;
	}

	public <O extends Objectable> PrismObject<O> parseObjectFromJaxb(Object objectElement) throws SchemaException {
		if (objectElement instanceof Element) {
			// DOM
			XNode objectXNode = domParser.parseElementContent((Element)objectElement);
			return prismContext.getXnodeProcessor().parseObject(objectXNode);
		} else if (objectElement instanceof JAXBElement<?>) {
			O jaxbValue = ((JAXBElement<O>)objectElement).getValue();
			prismContext.adopt(jaxbValue);
			return jaxbValue.asPrismObject();
		} else {
			throw new IllegalArgumentException("Unknown element type "+objectElement.getClass());
		}
	}
		
	public <O extends Objectable> Element serializeObjectToJaxb(PrismObject<O> object) throws SchemaException {
		RootXNode xroot = prismContext.getXnodeProcessor().serializeObject(object);
		return domParser.serializeToElement(xroot);
	}
	
	public <T> Element marshalJaxbObjectToDom(T jaxbObject, QName elementQName) throws JAXBException {
        return marshalJaxbObjectToDom(jaxbObject, elementQName, (Document) null);
    }
	
	public <T> Element marshalJaxbObjectToDom(T jaxbObject, QName elementQName, Document doc) throws JAXBException {
		if (doc == null) {
			doc = DOMUtil.getDocument();
		}

		JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(),
				jaxbObject);
		Element element = doc.createElementNS(elementQName.getNamespaceURI(), elementQName.getLocalPart());
		marshalElementToDom(jaxbElement, element);

		return (Element) element.getFirstChild();
	}
	
	private void marshalElementToDom(JAXBElement<?> jaxbElement, Node parentNode) throws JAXBException {
		createMarshaller(null).marshal(jaxbElement, parentNode);		
	}
	
	private Marshaller createMarshaller(Map<String, Object> jaxbProperties) throws JAXBException {
		Marshaller marshaller = jaxbContext.createMarshaller();
		// set default properties
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		DynamicNamespacePrefixMapper namespacePrefixMapper = prismContext.getSchemaRegistry().getNamespacePrefixMapper().clone();
		namespacePrefixMapper.setAlwaysExplicit(true);
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", namespacePrefixMapper);
		// set custom properties
		if (jaxbProperties != null) {
			for (Entry<String, Object> property : jaxbProperties.entrySet()) {
				marshaller.setProperty(property.getKey(), property.getValue());
			}
		}

		return marshaller;
	}
}