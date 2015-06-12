/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.provisioning.test.impl;

import static com.evolveum.midpoint.test.util.TestUtil.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.getAttributeValue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;

import org.apache.commons.lang.StringUtils;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CreateCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.DeleteCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType.Host;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.sun.mail.imap.protocol.UID;

/**
 * Test for provisioning service implementation.
 * 
 * This test will initialize mock repository and fill-in some test data. The
 * "default" repository objects cannot be used, as the new provisioning service
 * implementation assumes a slightly different connector configuration that was
 * used in the OpenIDM.
 * 
 * This test will initialize embedded OpenDJ as a target resource.
 * 
 * The test calls the new Provisioning Service Interface (java). No WSDL mess.
 * 
 * @author Radovan Semancik
 * @author Katka Valalikova
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDJ extends AbstractOpenDJTest {
	
	private static Trace LOGGER = TraceManager.getTrace(TestOpenDJ.class);

	@Autowired
	TaskManager taskManager;
	
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}
	
	@BeforeClass
	public static void startLdap() throws Exception {
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("START:  ProvisioningServiceImplOpenDJTest");
		LOGGER.info("------------------------------------------------------------------------------");
		try {
		openDJController.startCleanServer();
		} catch (IOException ex) {
			LOGGER.error("Couldn't start LDAP.", ex);
			throw ex;
		}
	}

	@AfterClass
	public static void stopLdap() throws Exception {
		openDJController.stop();
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("STOP:  ProvisioningServiceImplOpenDJTest");
		LOGGER.info("------------------------------------------------------------------------------");
	}
				
	/**
	 * This should be the very first test that works with the resource.
	 * 
	 * The original repository object does not have resource schema. The schema should be generated from
	 * the resource on the first use. This is the test that executes testResource and checks whether the
	 * schema was generated.
	 */
	@Test
	public void test003Connection() throws Exception {
		final String TEST_NAME = "test003Connection";
		TestUtil.displayTestTile(TEST_NAME);

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()+"."+TEST_NAME);
		ResourceType resourceTypeBefore = repositoryService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, result).asObjectable();
		assertNotNull("No connector ref",resourceTypeBefore.getConnectorRef());
		assertNotNull("No connector ref OID",resourceTypeBefore.getConnectorRef().getOid());
		connector = repositoryService.getObject(ConnectorType.class, resourceTypeBefore.getConnectorRef().getOid(), null, result);
		ConnectorType connectorType = connector.asObjectable();
		assertNotNull(connectorType);
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceTypeBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);
		
		OperationResult	operationResult = provisioningService.testResource(RESOURCE_OPENDJ_OID);
		
		display("Test connection result",operationResult);
		TestUtil.assertSuccess("Test connection failed",operationResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		
		display("Resource after testResource (repository)",resourceTypeRepoAfter);
		
		display("Resource after testResource (repository, XML)", PrismTestUtil.serializeObjectToString(resourceTypeRepoAfter.asPrismObject(), PrismContext.LANG_XML));
		
		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNotNull("No schema after test connection",xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);
		
		CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata",cachingMetadata);
		assertNotNull("No retrievalTimestamp",cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber",cachingMetadata.getSerialNumber());
		
		Element xsdElement = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, resourceTypeRepoAfter.toString(), prismContext);
		assertNotNull("No schema after parsing",parsedSchema);
		
		ObjectClassComplexTypeDefinition inetOrgPersonDefinition = parsedSchema.findObjectClassDefinition(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
		assertNull("The _PASSSWORD_ attribute sneaked into schema", inetOrgPersonDefinition.findAttributeDefinition(
				new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));
		assertNull("The userPassword attribute sneaked into schema", inetOrgPersonDefinition.findAttributeDefinition(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeRepoAfter),"userPassword")));
		
		assertShadows(1);
	}
	
	@Test
	public void test004ResourceAndConnectorCaching() throws Exception {
		TestUtil.displayTestTile("test004ResourceAndConnectorCaching");

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()+".test004ResourceAndConnectorCaching");
		resource = provisioningService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, null, result);
		resourceType = resource.asObjectable();
		ConnectorInstance configuredConnectorInstance = connectorManager.getConfiguredConnectorInstance(
				resource, false, result);
		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		assertNotNull("No resource schema", resourceSchema);
		
		// WHEN
		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, null, result);
		
		// THEN
		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
		assertNotNull("No connector ref",resourceTypeAgain.getConnectorRef());
		assertNotNull("No connector ref OID",resourceTypeAgain.getConnectorRef().getOid());
		
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainer<Containerable> configurationContainerAgain = resourceAgain.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);		
		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));
		assertTrue("Configurations not equals", configurationContainer.equals(configurationContainerAgain));

		ResourceSchema resourceSchemaAgain = RefinedResourceSchema.getResourceSchema(resourceAgain, prismContext);
		assertNotNull("No resource schema (again)", resourceSchemaAgain);
		assertEquals("Schema serial number mismatch", resourceType.getSchema().getCachingMetadata().getSerialNumber(),
				resourceTypeAgain.getSchema().getCachingMetadata().getSerialNumber());
		assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);
		
		// Now we stick our nose deep inside the provisioning impl. But we need to make sure that the
		// configured connector is properly cached
		ConnectorInstance configuredConnectorInstanceAgain = connectorManager.getConfiguredConnectorInstance(
				resourceAgain, false, result);
		assertTrue("Connector instance was not cached", configuredConnectorInstance == configuredConnectorInstanceAgain);
		
		assertShadows(1);
	}
	
	@Test
	public void test005Capabilities() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		TestUtil.displayTestTile("test005Capabilities");

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()+".test005Capabilities");

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, null, result).asObjectable();
		
		// THEN
		display("Resource from provisioninig", resource);
		display("Resource from provisioninig (XML)", PrismTestUtil.serializeObjectToString(resource.asPrismObject(), PrismContext.LANG_XML));
		
		CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
        assertFalse("Empty capabilities returned",nativeCapabilitiesList.isEmpty());
        CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilitiesList, CredentialsCapabilityType.class);
        assertNotNull("credentials capability not found",capCred);
        assertNotNull("password capability not present",capCred.getPassword());
        
        // Connector cannot do activation, this should be null
        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList, ActivationCapabilityType.class);
        assertNull("Found activation capability while not expecting it" ,capAct);
        
        ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilitiesList, ScriptCapabilityType.class);
        assertNotNull("No script capability", capScript);
        List<Host> scriptHosts = capScript.getHost();
        assertEquals("Wrong number of script hosts", 1, scriptHosts.size());
        Host scriptHost = scriptHosts.get(0);
        assertEquals("Wrong script host type", ProvisioningScriptHostType.CONNECTOR, scriptHost.getType());
//        assertEquals("Wrong script host language", ....., scriptHost.getLanguage());
        
        CreateCapabilityType capCreate = CapabilityUtil.getCapability(nativeCapabilitiesList, CreateCapabilityType.class);
        assertNotNull("No create capability", capCreate);
        
        ReadCapabilityType capRead = CapabilityUtil.getCapability(nativeCapabilitiesList, ReadCapabilityType.class);
        assertNotNull("No read capability", capRead);
        
        UpdateCapabilityType capUpdate = CapabilityUtil.getCapability(nativeCapabilitiesList, UpdateCapabilityType.class);
        assertNotNull("No update capability", capUpdate);
        
        DeleteCapabilityType capDelete = CapabilityUtil.getCapability(nativeCapabilitiesList, DeleteCapabilityType.class);
        assertNotNull("No delete capability", capDelete);
        
        
        List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resource);
        for (Object capability : effectiveCapabilities) {
        	System.out.println("Capability: "+CapabilityUtil.getCapabilityDisplayName(capability)+" : "+capability);
        }
        
        capCred = ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
        assertNotNull("credentials effective capability not found",capCred);
        assertNotNull("password effective capability not found",capCred.getPassword());
        // Although connector does not support activation, the resource specifies a way how to simulate it.
        // Therefore the following should succeed
        capAct = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
        assertNotNull("activation capability not found",capAct);
        
        PagedSearchCapabilityType capPage =  ResourceTypeUtil.getEffectiveCapability(resource, PagedSearchCapabilityType.class);
		assertNotNull("paged search capability not present", capPage);
        
        assertShadows(1);
	}
	
	@Test
	public void test006Schema() throws Exception {
		final String TEST_NAME = "test006RefinedSchema";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN

		// WHEN
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);
		display("Resource schema", resourceSchema);


		ObjectClassComplexTypeDefinition accountDef = resourceSchema.findObjectClassDefinition(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		ResourceAttributeDefinition<String> idPrimaryDef = accountDef.findAttributeDefinition(getPrimaryIdentifierQName());
		assertEquals(1, idPrimaryDef.getMaxOccurs());
		assertEquals(0, idPrimaryDef.getMinOccurs());
		assertFalse("UID has create", idPrimaryDef.canAdd());
		assertFalse("UID has update", idPrimaryDef.canModify());
		assertTrue("No UID read", idPrimaryDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(idPrimaryDef));
		assertEquals("Wrong "+ProvisioningTestUtil.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", Uid.NAME, idPrimaryDef.getFrameworkAttributeName());

		ResourceAttributeDefinition<String> idSecondaryDef = accountDef.findAttributeDefinition(getSecondaryIdentifierQName());
		assertEquals(1, idSecondaryDef.getMaxOccurs());
		assertEquals(1, idSecondaryDef.getMinOccurs());
		assertTrue("No NAME create", idSecondaryDef.canAdd());
		assertTrue("No NAME update", idSecondaryDef.canModify());
		assertTrue("No NAME read", idSecondaryDef.canRead());
		assertTrue("NAME definition not in secondary identifiers", accountDef.getSecondaryIdentifiers().contains(idSecondaryDef));
		assertEquals("Wrong "+ProvisioningTestUtil.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", Name.NAME, idSecondaryDef.getFrameworkAttributeName());

		ResourceAttributeDefinition<String> cnDef = accountDef.findAttributeDefinition("cn");
		assertNotNull("No definition for cn", cnDef);
		assertEquals(-1, cnDef.getMaxOccurs());
		assertEquals(1, cnDef.getMinOccurs());
		assertTrue("No cn create", cnDef.canAdd());
		assertTrue("No cn update", cnDef.canModify());
		assertTrue("No cn read", cnDef.canRead());
		
		ResourceAttributeDefinition<String> dsDef = accountDef.findAttributeDefinition("ds-pwp-account-disabled");
		assertNotNull("No definition for ds-pwp-account-disabled", dsDef);
		assertEquals(1, dsDef.getMaxOccurs());
		assertEquals(0, dsDef.getMinOccurs());
		assertTrue("No ds-pwp-account-disabled read", dsDef.canRead());
		assertTrue("No ds-pwp-account-disabled create", dsDef.canAdd());
		assertTrue("No ds-pwp-account-disabled update", dsDef.canModify());
		// TODO: MID-2358
//		assertTrue("ds-pwp-account-disabled is NOT operational", dsDef.isOperational());
		
		ResourceAttributeDefinition<String> createTimestampDef = accountDef.findAttributeDefinition("createTimestamp");
		assertNotNull("No definition for createTimestamp", createTimestampDef);
		assertEquals(1, createTimestampDef.getMaxOccurs());
		assertEquals(0, createTimestampDef.getMinOccurs());
		assertTrue("No createTimestamp read", createTimestampDef.canRead());
		assertFalse("Bad createTimestamp create", createTimestampDef.canAdd());
		assertFalse("Bad createTimestamp update", createTimestampDef.canModify());

		assertNull("The _PASSSWORD_ attribute sneaked into schema",
				accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));

		assertShadows(1);
	}
	
	@Test
	public void test007RefinedSchema() throws Exception {
		final String TEST_NAME = "test007RefinedSchema";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN

		// WHEN
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType, prismContext);
		display("Refined schema", refinedSchema);

		// Check whether it is reusing the existing schema and not parsing it
		// all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching",
				refinedSchema == RefinedResourceSchema.getRefinedSchema(resourceType, prismContext));

		RefinedObjectClassDefinition accountDef = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		RefinedAttributeDefinition<String> idPrimaryDef = accountDef.findAttributeDefinition(getPrimaryIdentifierQName());
		assertEquals(1, idPrimaryDef.getMaxOccurs());
		assertEquals(0, idPrimaryDef.getMinOccurs());
		assertFalse("UID has create", idPrimaryDef.canAdd());
		assertFalse("UID has update", idPrimaryDef.canModify());
		assertTrue("No UID read", idPrimaryDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(idPrimaryDef));
		assertEquals("Wrong "+ProvisioningTestUtil.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", Uid.NAME, idPrimaryDef.getFrameworkAttributeName());

		RefinedAttributeDefinition<String> idSecondaryDef = accountDef.findAttributeDefinition(getSecondaryIdentifierQName());
		assertEquals(1, idSecondaryDef.getMaxOccurs());
		assertEquals(1, idSecondaryDef.getMinOccurs());
		assertTrue("No NAME create", idSecondaryDef.canAdd());
		assertTrue("No NAME update", idSecondaryDef.canModify());
		assertTrue("No NAME read", idSecondaryDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(idSecondaryDef));
		assertEquals("Wrong NAME matching rule", StringIgnoreCaseMatchingRule.NAME, idSecondaryDef.getMatchingRuleQName());
		assertEquals("Wrong "+ProvisioningTestUtil.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME+" frameworkAttributeName", Name.NAME, idSecondaryDef.getFrameworkAttributeName());

		RefinedAttributeDefinition<String> cnDef = accountDef.findAttributeDefinition("cn");
		assertNotNull("No definition for cn", cnDef);
		assertEquals(-1, cnDef.getMaxOccurs());
		assertEquals(1, cnDef.getMinOccurs());
		assertTrue("No cn create", cnDef.canAdd());
		assertTrue("No cn update", cnDef.canModify());
		assertTrue("No cn read", cnDef.canRead());
		
		RefinedAttributeDefinition<String> dsDef = accountDef.findAttributeDefinition("ds-pwp-account-disabled");
		assertNotNull("No definition for cn", dsDef);
		assertEquals(1, dsDef.getMaxOccurs());
		assertEquals(0, dsDef.getMinOccurs());
		assertTrue("No ds-pwp-account-disabled create", dsDef.canAdd());
		assertTrue("No ds-pwp-account-disabled update", dsDef.canModify());
		assertTrue("No ds-pwp-account-disabled read", dsDef.canRead());
		// TODO: MID-2358
//		assertTrue("ds-pwp-account-disabled is NOT operational", dsDef.isOperational());
		assertTrue("ds-pwp-account-disabled is NOT ignored", dsDef.isIgnored());

		assertNull("The _PASSSWORD_ attribute sneaked into schema",
				accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));

		assertShadows(1);
	}
	
	
	@Test
	public void test020ListResourceObjects() throws Exception {
		final String TEST_NAME = "test020ListResourceObjects";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()+"." + TEST_NAME);
		// WHEN
		List<PrismObject<? extends ShadowType>> objectList = provisioningService.listResourceObjects(
				RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS, null, result);
		// THEN
		assertNotNull(objectList);
		assertFalse("Empty list returned",objectList.isEmpty());
		display("Resource object list "+RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS,objectList);
		
		assertShadows(1);
	}

	@Test
	public void test110GetObject() throws Exception {
		final String TEST_NAME = "test110GetObject";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		ShadowType objectToAdd = parseObjectType(ACCOUNT1_FILE, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(objectToAdd));
		System.out.println(objectToAdd.asPrismObject().debugDump());

		Task task = taskManager.createTaskInstance();
		String addedObjectOid = provisioningService.addObject(objectToAdd.asPrismObject(), null, null, task, result);
		assertEquals(ACCOUNT1_OID, addedObjectOid);
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		ShadowType acct = provisioningService.getObject(ShadowType.class, ACCOUNT1_OID, null, task, result).asObjectable();

		assertNotNull(acct);

		System.out.println(SchemaDebugUtil.prettyPrint(acct));
		System.out.println(acct.asPrismObject().debugDump());
		
		PrismAsserts.assertEqualsPolyString("Name not equals.", "uid=jbond,ou=People,dc=example,dc=com", acct.getName());
			
		// TODO: check values
		
		assertShadows(2);
	}

	/**
	 * Let's try to fetch object that does not exist in the repository.
	 */
	@Test
	public void test111GetObjectNotFoundRepo() throws Exception {
		final String TEST_NAME = "test111GetObjectNotFoundRepo";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		try {
			ObjectType object = provisioningService.getObject(ObjectType.class, NON_EXISTENT_OID, null, null, result).asObjectable();
			Assert.fail("Expected exception, but haven't got one");
		} catch (ObjectNotFoundException e) {
			// This is expected

			// Just to close the top-level result.
			result.recordFatalError("Error :-)");

			System.out.println("NOT FOUND REPO result:");
			System.out.println(result.debugDump());

			assertFalse(result.hasUnknownStatus());
			// TODO: check result
		} catch (CommunicationException e) {
			Assert.fail("Expected ObjectNotFoundException, but got" + e);
		} catch (SchemaException e) {
			Assert.fail("Expected ObjectNotFoundException, but got" + e);
		}

		assertShadows(2);
	}

	/**
	 * Let's try to fetch object that does exit in the repository but does not
	 * exist in the resource.
	 */
	@Test
	public void test112GetObjectNotFoundResource() throws Exception {
		final String TEST_NAME = "test112GetObjectNotFoundResource";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		try {
			ObjectType object = provisioningService.getObject(ObjectType.class, ACCOUNT_BAD_OID, null, null, result).asObjectable();
			Assert.fail("Expected exception, but haven't got one");
		} catch (ObjectNotFoundException e) {
			// This is expected

			// Just to close the top-level result.
			result.recordFatalError("Error :-)");

			System.out.println("NOT FOUND RESOURCE result:");
			System.out.println(result.debugDump());

			assertFalse(result.hasUnknownStatus());
			// TODO: check result
		} catch (CommunicationException e) {
			Assert.fail("Expected ObjectNotFoundException, but got" + e);
		} catch (SchemaException e) {
			Assert.fail("Expected ObjectNotFoundException, but got" + e);
		} finally {
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
		}

		assertShadows(0);
	}

	@Test
	public void test120AddObject() throws Exception {
		final String TEST_NAME = "test120AddObject";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		ShadowType object = parseObjectTypeFromFile(ACCOUNT_NEW_FILENAME, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		Task task = taskManager.createTaskInstance();
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
		assertEquals(ACCOUNT_NEW_OID, addedObjectOid);

		ShadowType repoShadowType =  repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal (repo)", "uid=will,ou=People,dc=example,dc=com", repoShadowType.getName());
		assertAttribute(repoShadowType, getSecondaryIdentifierQName(), StringUtils.lowerCase(ACCOUNT_NEW_DN));

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_OID,
				null, task, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", "uid=will,ou=People,dc=example,dc=com", provisioningAccountType.getName());
		
		assertShadows(1);
	}

	@Test
	public void test121RenameAccountWillOnResource() throws Exception{
		String TEST_NAME = "test121RenameAccountWillOnResource";
		TestUtil.displayTestTile(TEST_NAME);
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		
		openDJController.executeRenameChange(TEST_DIR_NAME + "/rename.ldif");
		
		SearchResultEntry entry = openDJController.fetchEntry("uid=will123,ou=People,dc=example,dc=com");
		assertNotNull("Entry with dn uid=will123,ou=People,dc=example,dc=com does not exist", entry);
		
		ShadowType repoShadowType =  repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal (repo)", "uid=will,ou=People,dc=example,dc=com", repoShadowType.getName());
		assertAttribute(repoShadowType, getSecondaryIdentifierQName(), StringUtils.lowerCase(ACCOUNT_NEW_DN));

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_OID,
				null, task, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", "uid=will123,ou=People,dc=example,dc=com", provisioningAccountType.getName());
		assertAttribute(provisioningAccountType, getSecondaryIdentifierQName(), "uid=will123,ou=people,dc=example,dc=com");
		
		repoShadowType =  repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal (repo after provisioning)", "uid=will123,ou=People,dc=example,dc=com", repoShadowType.getName());
		assertAttribute(repoShadowType, getSecondaryIdentifierQName(), "uid=will123,ou=people,dc=example,dc=com");
		
		assertShadows(1);
	}
	
	@Test
	public void test125AddObjectNull() throws Exception {
		final String TEST_NAME = "test125AddObjectNull";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		String addedObjectOid = null;
		
		try {
		
			// WHEN
			addedObjectOid = provisioningService.addObject(null, null, null, taskManager.createTaskInstance(), result);
			
			Assert.fail("Expected IllegalArgumentException but haven't got one.");
		} catch(IllegalArgumentException ex){
			assertEquals("Object to add must not be null.", ex.getMessage());
		}
		
		assertShadows(1);
	}

	
	@Test
	public void test130DeleteObject() throws Exception {
		final String TEST_NAME = "test130DeleteObject";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

	
		ShadowType object = parseObjectTypeFromFile(ACCOUNT_DELETE_FILENAME, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		Task task = taskManager.createTaskInstance();
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
		assertEquals(ACCOUNT_DELETE_OID, addedObjectOid);

		provisioningService.deleteObject(ShadowType.class, ACCOUNT_DELETE_OID, null, null, task, result);

		ShadowType objType = null;

		try {
			objType = provisioningService.getObject(ShadowType.class, ACCOUNT_DELETE_OID,
					null, task, result).asObjectable();
			Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
		} catch (ObjectNotFoundException ex) {
			System.out.println("Catched ObjectNotFoundException.");
			assertNull(objType);
		}

		try {
			objType = repositoryService.getObject(ShadowType.class, ACCOUNT_DELETE_OID,
					null, result).asObjectable();
			// objType = container.getObject();
			Assert.fail("Expected exception, but haven't got one.");
		} catch (Exception ex) {
			assertNull(objType);
            assertEquals(ex.getClass(), ObjectNotFoundException.class);
            assertTrue(ex.getMessage().contains(ACCOUNT_DELETE_OID));
		}
		
		assertShadows(1);
	}

	@Test
	public void test140ModifyObject() throws Exception {
		final String TEST_NAME = "test140ModifyObject";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		ShadowType object = unmarshallValueFromFile(ACCOUNT_MODIFY_FILE, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		assertEquals(ACCOUNT_MODIFY_OID, addedObjectOid);

		ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(
                new File("src/test/resources/impl/account-change-description.xml"), ObjectModificationType.COMPLEX_TYPE);
		ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, object.asPrismObject().getDefinition());
		
		ItemPath icfNamePath = new ItemPath(
				ShadowType.F_ATTRIBUTES, getSecondaryIdentifierQName());
		PrismPropertyDefinition icfNameDef = object
				.asPrismObject().getDefinition().findPropertyDefinition(icfNamePath);
		ItemDelta renameDelta = PropertyDelta.createModificationReplaceProperty(icfNamePath, icfNameDef, "uid=rename,ou=People,dc=example,dc=com");
//			renameDelta.addValueToDelete(new PrismPropertyValue("uid=jack,ou=People,dc=example,dc=com"));
		((Collection)delta.getModifications()).add(renameDelta);
		
//			renameDelta = PropertyDelta.createModificationDeleteProperty(icfNamePath, icfNameDef, "uid=jack,ou=People,dc=example,dc=com");
//			((Collection)delta.getModifications()).add(renameDelta);
		
		display("Object change",delta);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
				delta.getModifications(), null, null, taskManager.createTaskInstance(), result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		ShadowType accountType = provisioningService.getObject(ShadowType.class,
				ACCOUNT_MODIFY_OID, null, taskManager.createTaskInstance(), result).asObjectable();
		
		display("Object after change",accountType);
		

		String uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());
		List<Object> snValues = ShadowUtil.getAttributeValues(accountType, new QName(RESOURCE_NS, "sn"));
		assertNotNull("No 'sn' attribute", snValues);
		assertFalse("Surname attributes must not be empty", snValues.isEmpty());
		assertEquals(1, snValues.size());
		
		
		//check icf_name in the shadow object fetched only from the repository
		ShadowType repoShadow = repositoryService.getObject(ShadowType.class, objectChange.getOid(), null, result).asObjectable();
		String name = ShadowUtil.getSingleStringAttributeValue(repoShadow, getSecondaryIdentifierQName());
		assertEquals("After rename, dn is not equal.", "uid=rename,ou=people,dc=example,dc=com", name);
		assertEquals("shadow name not changed after rename", "uid=rename,ou=People,dc=example,dc=com", repoShadow.getName().getOrig());
		
		String changedSn = (String) snValues.get(0);
		
		assertNotNull(uid);
		
		// Check if object was modified in LDAP
		
		SearchResultEntry response = openDJController.searchAndAssertByEntryUuid(uid);			
		display("LDAP account", response);
		
		OpenDJController.assertAttribute(response, "sn", "First");
		
		assertEquals("First", changedSn);
		
		assertShadows(2);
	}

	@Test
	public void test150ChangePassword() throws Exception {
		final String TEST_NAME = "test150ChangePassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		ShadowType object = parseObjectType(ACCOUNT_MODIFY_PASSWORD_FILE, ShadowType.class);

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);

		assertEquals(ACCOUNT_MODIFY_PASSWORD_OID, addedObjectOid);
		
		ShadowType accountType = provisioningService.getObject(ShadowType.class,
				ACCOUNT_MODIFY_PASSWORD_OID, null, taskManager.createTaskInstance(), result).asObjectable();
		
		display("Object before password change",accountType);
		
		String uid = null;
		uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());
		assertNotNull(uid);
		
		SearchResultEntry entryBefore = openDJController.searchAndAssertByEntryUuid(uid);			
		display("LDAP account before", entryBefore);

		String passwordBefore = OpenDJController.getAttributeValue(entryBefore, "userPassword");
		assertNull("Unexpected password before change",passwordBefore);
		
		ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(
                new File("src/test/resources/impl/account-change-password.xml"), ObjectModificationType.COMPLEX_TYPE);
		ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, accountType.asPrismObject().getDefinition());
		display("Object change",delta);

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(), null, null, taskManager.createTaskInstance(), result);

		// THEN
		
		// Check if object was modified in LDAP
		
		SearchResultEntry entryAfter = openDJController.searchAndAssertByEntryUuid(uid);			
		display("LDAP account after", entryAfter);

		String passwordAfter = OpenDJController.getAttributeValue(entryAfter, "userPassword");
		assertNotNull("The password was not changed",passwordAfter);
		
		System.out.println("Changed password: "+passwordAfter);

		assertShadows(3);
	}

	@Test
	public void test151AddObjectWithPassword() throws Exception {
		final String TEST_NAME = "test151AddObjectWithPassword";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		ShadowType object = parseObjectTypeFromFile(ACCOUNT_NEW_WITH_PASSWORD_FILENAME, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		assertEquals(ACCOUNT_NEW_WITH_PASSWORD_OID, addedObjectOid);

		ShadowType accountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID,
				null, result).asObjectable();
//			assertEquals("lechuck", accountType.getName());
		PrismAsserts.assertEqualsPolyString("Name not equal.", "uid=lechuck,ou=People,dc=example,dc=com", accountType.getName());

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID,
				null,taskManager.createTaskInstance(), result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal.", "uid=lechuck,ou=People,dc=example,dc=com", provisioningAccountType.getName());
//			assertEquals("lechuck", provisioningAccountType.getName());
		
		String uid = null;
		for (Object e : accountType.getAttributes().getAny()) {
			if (getPrimaryIdentifierQName().equals(JAXBUtil.getElementQName(e))) {
				uid = ((Element)e).getTextContent();
			}
		}
		assertNotNull(uid);
		
		// Check if object was created in LDAP and that there is a password
		
		SearchResultEntry entryAfter = openDJController.searchAndAssertByEntryUuid(uid);			
		display("LDAP account after", entryAfter);

		String passwordAfter = OpenDJController.getAttributeValue(entryAfter, "userPassword");
		assertNotNull("The password was not changed",passwordAfter);
		
		System.out.println("Account password: "+passwordAfter);
			
		assertShadows(4);
	}
	
	@Test
    public void test160SearchAccountsIterative() throws Exception {
		final String TEST_NAME = "test160SearchAccountsIterative";
		TestUtil.displayTestTile(TEST_NAME);
		
        // GIVEN
    	OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

        final String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
        QName objectClass = new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME);

        ObjectQuery query = ObjectQueryUtil.createResourceAndAccountQuery(resource.getOid(), objectClass, prismContext);
        
        final Collection<ObjectType> objects = new HashSet<ObjectType>();

        ResultHandler handler = new ResultHandler<ObjectType>() {

            @Override
            public boolean handle(PrismObject<ObjectType> prismObject, OperationResult parentResult) {
                ObjectType objectType = prismObject.asObjectable();
                objects.add(objectType);

                display("Found object", objectType);

                assertTrue(objectType instanceof ShadowType);
                ShadowType shadow = (ShadowType) objectType;
                assertNotNull(shadow.getOid());
                assertNotNull(shadow.getName());
                assertEquals(new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME), shadow.getObjectClass());
                assertEquals(RESOURCE_OPENDJ_OID, shadow.getResourceRef().getOid());
                String icfUid = getAttributeValue(shadow, getPrimaryIdentifierQName());
                assertNotNull("No ICF UID", icfUid);
                String icfName = getAttributeValue(shadow, getSecondaryIdentifierQName());
                assertNotNull("No ICF NAME", icfName);
                PrismAsserts.assertEqualsPolyString("Wrong shadow name", icfName, shadow.getName());
                assertNotNull("Missing LDAP uid", getAttributeValue(shadow, new QName(resourceNamespace, "uid")));
                assertNotNull("Missing LDAP cn", getAttributeValue(shadow, new QName(resourceNamespace, "cn")));
                assertNotNull("Missing LDAP sn", getAttributeValue(shadow, new QName(resourceNamespace, "sn")));
                assertNotNull("Missing activation", shadow.getActivation());
                assertNotNull("Missing activation status", shadow.getActivation().getAdministrativeStatus());
                assertEquals("Not enabled", ActivationStatusType.ENABLED, shadow.getActivation().getAdministrativeStatus());
                return true;
            }
        };

        // WHEN
        provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, result);

        // THEN
        display("Count", objects.size());
        assertEquals("Unexpected number of shadows", 9, objects.size());

        // The extra shadow is a group shadow 
        assertShadows(10);
        
        // Bad things may happen, so let's check if the shadow is still there and that is has the same OID
        PrismObject<ShadowType> accountNew = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_OID, null, taskManager.createTaskInstance(), result);
    }

	private void assertShadows(int expectedCount) throws SchemaException {
		OperationResult result = new OperationResult(TestOpenDJ.class.getName() + ".assertShadows");
		int actualCount = repositoryService.countObjects(ShadowType.class, null, result);
		if (actualCount != expectedCount) {
			ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
				@Override
				public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
					display("Repo shadow", object);
					return true;
				}
			};
			repositoryService.searchObjectsIterative(ShadowType.class, null, handler, null, result);
			assertEquals("Unexpected number of shadows in the repo", expectedCount, actualCount);
		}
	}

	@Test
	public void test170DisableAccount() throws Exception{
		final String TEST_NAME = "test170DisableAccount";
		TestUtil.displayTestTile(TEST_NAME);
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()+"."+TEST_NAME);

		ShadowType object = parseObjectTypeFromFile(ACCOUNT_DISABLE_SIMULATED_FILENAME, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		assertEquals(ACCOUNT_DISABLE_SIMULATED_OID, addedObjectOid);

		ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(
                new File(REQUEST_DISABLE_ACCOUNT_SIMULATED_FILENAME), ObjectModificationType.COMPLEX_TYPE);
		ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, object.asPrismObject().getDefinition());
		display("Object change",delta);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
				delta.getModifications(), null, null, taskManager.createTaskInstance(), result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		ShadowType accountType = provisioningService.getObject(ShadowType.class,
				ACCOUNT_DISABLE_SIMULATED_OID, null, taskManager.createTaskInstance(), result).asObjectable();
		
		display("Object after change",accountType);
		
		assertEquals("The account was not disabled in the shadow", ActivationStatusType.DISABLED, accountType.getActivation().getAdministrativeStatus());
		
		String uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		
		// Check if object was modified in LDAP
		
		SearchResultEntry response = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP account", response);
		
		String disabled = openDJController.getAttributeValue(response, "ds-pwp-account-disabled");
		assertNotNull("no ds-pwp-account-disabled attribute in account "+uid, disabled);

        display("ds-pwp-account-disabled after change: " + disabled);

        assertEquals("ds-pwp-account-disabled not set to \"true\"", "true", disabled);
        
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, ACCOUNT_DISABLE_SIMULATED_OID, null, result);
        ActivationType repoActivation = repoShadow.asObjectable().getActivation();
        assertNotNull("No activation in repo", repoActivation);
        XMLGregorianCalendar repoDisableTimestamp = repoActivation.getDisableTimestamp();
        assertNotNull("No activation disableTimestamp in repo", repoDisableTimestamp);
        assertEquals("Wrong activation disableTimestamp in repo", 
        		XmlTypeConverter.createXMLGregorianCalendar(2001, 2, 3, 4, 5, 6), repoDisableTimestamp);
	}

	@Test
	public void test175AddDisabledAccount() throws Exception {
		final String TEST_NAME = "test175AddDisabledAccount";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName() + "." + TEST_NAME);

		ShadowType object = parseObjectType(ACCOUNT_NEW_DISABLED_FILE, ShadowType.class);

		IntegrationTestTools.display("Adding object", object);

		// WHEN
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		
		// THEN
		assertEquals(ACCOUNT_NEW_DISABLED_OID, addedObjectOid);

		ShadowType accountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_DISABLED_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (repo)", "uid=rapp,ou=People,dc=example,dc=com", accountType.getName());

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_DISABLED_OID,
				null, taskManager.createTaskInstance(), result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (provisioning)", "uid=rapp,ou=People,dc=example,dc=com", provisioningAccountType.getName());
		
		String uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		
		// Check if object was modified in LDAP
		
		SearchResultEntry response = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP account", response);
		
		String disabled = openDJController.getAttributeValue(response, "ds-pwp-account-disabled");
		assertNotNull("no ds-pwp-account-disabled attribute in account "+uid, disabled);

        System.out.println("ds-pwp-account-disabled after change: " + disabled);

        assertEquals("ds-pwp-account-disabled not set to \"true\"", "true", disabled);
        
        ActivationType repoActivation = accountType.getActivation();
        assertNotNull("No activation in repo", repoActivation);
        XMLGregorianCalendar repoDisableTimestamp = repoActivation.getDisableTimestamp();
        assertNotNull("No activation disableTimestamp in repo", repoDisableTimestamp);
        assertEquals("Wrong activation disableTimestamp in repo", 
        		XmlTypeConverter.createXMLGregorianCalendar(1999, 8, 7, 6, 5, 4), repoDisableTimestamp);
	}
	
	/**
	 * Adding account with EXPLICIT enable. This triggers simulated activation in a different way.
	 */
	@Test
	public void test176AddEnabledAccount() throws Exception {
		final String TEST_NAME = "test176AddEnabledAccount";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName() + "." + TEST_NAME);

		ShadowType object = parseObjectType(ACCOUNT_NEW_ENABLED_FILE, ShadowType.class);

		IntegrationTestTools.display("Adding object", object);

		// WHEN
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		
		// THEN
		assertEquals(ACCOUNT_NEW_ENABLED_OID, addedObjectOid);

		ShadowType accountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_ENABLED_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (repo)", "uid=cook,ou=People,dc=example,dc=com", accountType.getName());

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_ENABLED_OID,
				null, taskManager.createTaskInstance(), result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (provisioning)", "uid=cook,ou=People,dc=example,dc=com", provisioningAccountType.getName());
		
		String uid = ShadowUtil.getSingleStringAttributeValue(accountType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		
		// Check if object was modified in LDAP
		
		SearchResultEntry response = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP account", response);
		
		String disabled = openDJController.getAttributeValue(response, "ds-pwp-account-disabled");
		assertNull("unexpected ds-pwp-account-disabled attribute in account "+uid, disabled);
	}
	
	@Test
	public void test180GetUnlockedAccount() throws Exception {
		final String TEST_NAME = "test180GetUnlockedAccount";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName() + "." + TEST_NAME);
		Task task = taskManager.createTaskInstance();

		// WHEN
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_OID,
				null, task, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		
		PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, 
				LockoutStatusType.NORMAL);
	}
	
	@Test
	public void test182GetLockedAccount() throws Exception {
		final String TEST_NAME = "test182GetLockedAccount";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName() + "." + TEST_NAME);
		Task task = taskManager.createTaskInstance();
		
		openDJController.executeLdifChange(
				"dn: uid=will123,ou=People,dc=example,dc=com\n" +
				"changetype: modify\n" +
				"replace: pager\n" +
				"pager: 1"
				);

		// WHEN
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_OID,
				null, task, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		
		PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, 
				LockoutStatusType.LOCKED);
	}
	
	@Test
	public void test184UnlockAccount() throws Exception{
		final String TEST_NAME = "test184UnlockAccount";
		TestUtil.displayTestTile(TEST_NAME);
		Task task = taskManager.createTaskInstance(TestOpenDJ.class.getName()+"."+TEST_NAME);
		OperationResult result = task.getResult();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_NEW_OID, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, prismContext, LockoutStatusType.NORMAL);

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(),
				delta.getModifications(), null, null, task, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		
		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class,
				ACCOUNT_NEW_OID, null, taskManager.createTaskInstance(), result);
		
		display("Object after change",shadow);
		
		String uid = ShadowUtil.getSingleStringAttributeValue(shadow.asObjectable(), getPrimaryIdentifierQName());		
		assertNotNull(uid);
		
		// Check if object was modified in LDAP
		
		SearchResultEntry response = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP account", response);
		
		String pager = openDJController.getAttributeValue(response, "pager");
		assertNull("Pager attribute found in account "+uid+": "+pager, pager);
		
		PrismAsserts.assertPropertyValue(shadow, SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS, 
				LockoutStatusType.NORMAL);
	}
	
	@Test
	public void test190AddGroupSwashbucklers() throws Exception {
		final String TEST_NAME = "test190AddGroupSwashbucklers";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);
		
		ShadowType object = parseObjectType(GROUP_SWASHBUCKLERS_FILE, ShadowType.class);
		IntegrationTestTools.display("Adding object", object);

		// WHEN
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		
		// THEN
		assertEquals(GROUP_SWASHBUCKLERS_OID, addedObjectOid);

		ShadowType shadowType =  repositoryService.getObject(ShadowType.class, GROUP_SWASHBUCKLERS_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (repo)", GROUP_SWASHBUCKLERS_DN, shadowType.getName());

		ShadowType provisioningShadowType = provisioningService.getObject(ShadowType.class, GROUP_SWASHBUCKLERS_OID,
				null, taskManager.createTaskInstance(), result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (provisioning)", GROUP_SWASHBUCKLERS_DN, provisioningShadowType.getName());
		
		String uid = ShadowUtil.getSingleStringAttributeValue(shadowType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		
		SearchResultEntry ldapEntry = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP group", ldapEntry);
		assertNotNull("No LDAP group entry");
		String groupDn = ldapEntry.getDN().toString();
		assertEquals("Wrong group DN", GROUP_SWASHBUCKLERS_DN, groupDn);
	}
	
	@Test
	public void test192AddAccountMorganWithAssociation() throws Exception {
		final String TEST_NAME = "test192AddAccountMorganWithAssociation";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ "." + TEST_NAME);

		ShadowType object = parseObjectType(ACCOUNT_MORGAN_FILE, ShadowType.class);
		IntegrationTestTools.display("Adding object", object);

		// WHEN
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		
		// THEN
		assertEquals(ACCOUNT_MORGAN_OID, addedObjectOid);

		ShadowType shadowType =  repositoryService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID,
				null, result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (repo)", ACCOUNT_MORGAN_DN, shadowType.getName());

		ShadowType provisioningShadowType = provisioningService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID,
				null, taskManager.createTaskInstance(), result).asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong ICF name (provisioning)", ACCOUNT_MORGAN_DN, provisioningShadowType.getName());
		
		String uid = ShadowUtil.getSingleStringAttributeValue(shadowType, getPrimaryIdentifierQName());		
		assertNotNull(uid);
		
		List<ShadowAssociationType> associations = provisioningShadowType.getAssociation();
		assertEquals("Unexpected number of associations", 1, associations.size());
		ShadowAssociationType association = associations.get(0);
		assertEquals("Wrong group OID in association", GROUP_SWASHBUCKLERS_OID, association.getShadowRef().getOid());
		
		SearchResultEntry accountEntry = openDJController.searchAndAssertByEntryUuid(uid);
		display("LDAP account", accountEntry);
		assertNotNull("No LDAP account entry");
		String accountDn = accountEntry.getDN().toString();
		assertEquals("Wrong account DN", ACCOUNT_MORGAN_DN, accountDn);
		
		SearchResultEntry groupEntry = openDJController.fetchEntry(GROUP_SWASHBUCKLERS_DN);
		display("LDAP group", groupEntry);
		assertNotNull("No LDAP group entry");
		openDJController.assertUniqueMember(groupEntry, accountDn);
	}

	/**
	 * Morgan has a group association. If the account is gone the group membership should also be gone.
	 */
	@Test
	public void test199DeleteAccountMorgan() throws Exception {
		final String TEST_NAME = "test199DeleteAccountMorgan";
		TestUtil.displayTestTile(TEST_NAME);
		Task task = taskManager.createTaskInstance(TestOpenDJ.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();
	
		// WHEN
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_MORGAN_OID, null, null, task, result);

		ShadowType objType = null;

		try {
			objType = provisioningService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID,
					null, task, result).asObjectable();
			Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
		} catch (ObjectNotFoundException ex) {
			System.out.println("Catched ObjectNotFoundException.");
			assertNull(objType);
		}

		try {
			objType = repositoryService.getObject(ShadowType.class, ACCOUNT_MORGAN_OID,
					null, result).asObjectable();
			// objType = container.getObject();
			Assert.fail("Expected exception, but haven't got one.");
		} catch (Exception ex) {
			assertNull(objType);
            assertEquals(ex.getClass(), ObjectNotFoundException.class);
            assertTrue(ex.getMessage().contains(ACCOUNT_MORGAN_OID));
		}
		
		SearchResultEntry groupEntry = openDJController.fetchEntry(GROUP_SWASHBUCKLERS_DN);
		display("LDAP group", groupEntry);
		assertNotNull("No LDAP group entry");
		openDJController.assertNoUniqueMember(groupEntry, ACCOUNT_MORGAN_DN);
	}
	
	@Test
	public void test200SearchObjectsIterative() throws Exception {
		TestUtil.displayTestTile("test200SearchObjectsIterative");

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".searchObjectsIterativeTest");
		ShadowType object = parseObjectTypeFromFile(ACCOUNT_SEARCH_ITERATIVE_FILENAME, ShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(object));
		System.out.println(object.asPrismObject().debugDump());

		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		assertEquals(ACCOUNT_SEARCH_ITERATIVE_OID, addedObjectOid);

		final List<ShadowType> objectTypeList = new ArrayList<ShadowType>();

		QueryType queryType = PrismTestUtil.parseAtomicValue(new File(
                "src/test/resources/impl/query-filter-all-accounts.xml"), QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		provisioningService.searchObjectsIterative(ShadowType.class, query, null, new ResultHandler<ShadowType>() {

			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {

				return objectTypeList.add(object.asObjectable());
			}
		}, result);

		// TODO: check result
		System.out.println("ObjectType list size: " + objectTypeList.size());

		for (ObjectType objType : objectTypeList) {
			if (objType == null) {
				System.out.println("Object not found in repo");
			} else {
				//System.out.println("obj name: " + objType.getName());
                System.out.println(object.asPrismObject().debugDump());
			}
		}
	}

	@Test
	public void test201SearchObjects() throws Exception {
		final String TEST_NAME = "test201SearchObjects";
		TestUtil.displayTestTile(TEST_NAME);

		OperationResult result = new OperationResult(TestOpenDJ.class.getName() + "." + TEST_NAME);

		ShadowType object = parseObjectTypeFromFile(ACCOUNT_SEARCH_FILENAME, ShadowType.class); 

		display("New object", object);
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
		assertEquals(ACCOUNT_SEARCH_OID, addedObjectOid);

		QueryType queryType = PrismTestUtil.parseAtomicValue(new File("src/test/resources/impl/query-filter-all-accounts.xml"),
                QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();

		// WHEN
		SearchResultList<PrismObject<ShadowType>> searchResults = provisioningService.searchObjects(ShadowType.class, query, null, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		display("Search resutls", searchResults);
		
		assertEquals("Unexpected number of search results", 14, searchResults.size());
		
		assertConnectorOperationIncrement(1);
		assertConnectorSimulatedPagingSearchIncrement(0);
	}

	@Test
	public void test202SearchObjectsCompexFilter() throws Exception {
		TestUtil.displayTestTile("test202SearchObjectsCompexFilter");

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test202SearchObjectsCompexFilter");

		QueryType queryType = PrismTestUtil.parseAtomicValue(QUERY_COMPLEX_FILTER_FILE,
                QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);

		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
		
		// WHEN
		List<PrismObject<ShadowType>> objListType = 
			provisioningService.searchObjects(ShadowType.class, query, null, result);
		
		// THEN
		for (PrismObject<ShadowType> objType : objListType) {
			if (objType == null) {
				System.out.println("Object not found in repository.");
			} else {
				System.out.println("found object: " + objType.asObjectable().getName());
			}
		}
		
		assertConnectorOperationIncrement(1);
		assertConnectorSimulatedPagingSearchIncrement(0);
	}

	@Test
	public void test230SearchObjectsPagedNoOffset() throws Exception {
		final String TEST_NAME = "test230SearchObjectsPagedNoOffset";
		TestUtil.displayTestTile(TEST_NAME);

		OperationResult result = new OperationResult(TestOpenDJ.class.getName() + "." + TEST_NAME);

		QueryType queryType = PrismTestUtil.parseAtomicValue(new File("src/test/resources/impl/query-filter-all-accounts.xml"),
                QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		ObjectPaging paging = ObjectPaging.createPaging(null, 3);
		query.setPaging(paging);
		
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();

		// WHEN
		List<PrismObject<ShadowType>> searchResults = 
			provisioningService.searchObjects(ShadowType.class, query, null, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		display("Search resutls", searchResults);
		
		assertSearchResults(searchResults, "cook", "drake", "hbarbossa" );
		
		assertConnectorOperationIncrement(1);
		assertConnectorSimulatedPagingSearchIncrement(0);
	}
	
	@Test
	public void test231SearchObjectsPagedOffsetZero() throws Exception {
		final String TEST_NAME = "test231SearchObjectsPagedOffsetZero";
		TestUtil.displayTestTile(TEST_NAME);

		OperationResult result = new OperationResult(TestOpenDJ.class.getName() + "." + TEST_NAME);

		QueryType queryType = PrismTestUtil.parseAtomicValue(new File("src/test/resources/impl/query-filter-all-accounts.xml"),
                QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		ObjectPaging paging = ObjectPaging.createPaging(0, 4);
		query.setPaging(paging);
		
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();

		// WHEN
		List<PrismObject<ShadowType>> searchResults = 
			provisioningService.searchObjects(ShadowType.class, query, null, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		display("Search resutls", searchResults);
		
		assertSearchResults(searchResults, "drake", "hbarbossa", "idm", "jbeckett");
		
		assertConnectorOperationIncrement(1);
		assertConnectorSimulatedPagingSearchIncrement(0);
	}
	
	@Test
	public void test232SearchObjectsPagedOffset() throws Exception {
		final String TEST_NAME = "test232SearchObjectsPagedOffset";
		TestUtil.displayTestTile(TEST_NAME);

		OperationResult result = new OperationResult(TestOpenDJ.class.getName() + "." + TEST_NAME);

		QueryType queryType = PrismTestUtil.parseAtomicValue(new File("src/test/resources/impl/query-filter-all-accounts.xml"),
                QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
		
		ObjectPaging paging = ObjectPaging.createPaging(2, 5);
		query.setPaging(paging);
		
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();

		// WHEN
		SearchResultList<PrismObject<ShadowType>> searchResults = provisioningService.searchObjects(ShadowType.class, query, null, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		display("Search resutls", searchResults);
		
		// The results should be this:
//		assertSearchResults(searchResults, "hbarbossa", "idm", "jbeckett", "jbond", "jgibbs" );
		// But there seems to be a VLV bug in OpenJD. So the results are like this:
		assertSearchResults(searchResults, "drake", "hbarbossa", "idm", "jbeckett", "jbond");
		
		assertConnectorOperationIncrement(1);
		assertConnectorSimulatedPagingSearchIncrement(0);
	}
		
	private void assertSearchResults(List<PrismObject<ShadowType>> searchResults, String... expectedUids) {
		assertEquals("Unexpected number of search results", expectedUids.length, searchResults.size());
		int i = 0;
		for (PrismObject<ShadowType> searchResult: searchResults) {
			assertShadow(searchResult);
			ResourceAttribute<String> uidAttr = ShadowUtil.getAttribute(searchResult, new QName(RESOURCE_NS, "uid"));
			String uid = uidAttr.getRealValues().iterator().next();
			display("found uid", uid);
			assertEquals("Wrong uid (index "+i+")", expectedUids[i], uid);
			i++;
		}
	}
	
	@Test
	public void test250CountObjects() throws Exception {
		final String TEST_NAME = "test250CountObjects";
		TestUtil.displayTestTile(TEST_NAME);

		OperationResult result = new OperationResult(TestOpenDJ.class.getName() + "." + TEST_NAME);

		QueryType queryType = PrismTestUtil.parseAtomicValue(new File("src/test/resources/impl/query-filter-all-accounts.xml"),
                QueryType.COMPLEX_TYPE);
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);

		// WHEN
		Integer count = provisioningService.countObjects(ShadowType.class, query, result);
		
		// THEN
		result.computeStatus();
		assertSuccess(result);
		
		assertEquals("Unexpected number of search results", (Integer)14, count);
	}
	
	/**
	 * The exception comes from the resource. There is no shadow for this object.
	 */
	@Test
	public void test300AddObjectObjectAlreadyExistResource() throws Exception{
		TestUtil.displayTestTile("test300AddObjectObjectAlreadyExistResource");
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test300AddObjectObjectAlreadyExist");
		
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_SEARCH_FILENAME));
		display("Account to add", account);
		
		try {
			// WHEN
			provisioningService.addObject(account, null, null, taskManager.createTaskInstance(), result);
			
			AssertJUnit.fail("Expected addObject operation to fail but it was successful");
			
		} catch (ObjectAlreadyExistsException e) {
			// This is expected
			display("Expected exception", e);
			
			// The exception should originate from the LDAP layers
			IntegrationTestTools.assertInMessageRecursive(e, "LDAP");
		}
		
		// TODO: search to check that the shadow with the same NAME exists (search for OID will not do)

	}
	
	@Test
	public void test310AddObjectNoSn() throws Exception{
		TestUtil.displayTestTile("test310AddObjectNoSn");
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test300AddObjectObjectAlreadyExist");

		PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_NO_SN_FILENAME));
		display("Account to add", account);
		
		try {
			// WHEN
			provisioningService.addObject(account, null, null, taskManager.createTaskInstance(), result);
			
			AssertJUnit.fail("Expected addObject operation to fail but it was successful");
			
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
			
			// This error should be detectable before it reaches a resource. Therefore we check that the
			// cause was not a LDAP exception
			
			// MID-1007
//			IntegrationTestTools.assertNotInMessageRecursive(e, "LDAP");
		}
		
		// TODO: search to check that the shadow with the same NAME exists (search for OID will not do)

	}
	
	@Test
	public void test401ConfiguredCapabilityNoRead() throws Exception{
		
		OperationResult parentResult = new OperationResult("test401noReadNativeCapability");
		
		addResourceFromFile(new File(ProvisioningTestUtil.COMMON_TEST_DIR_FILE, "resource-opendj-no-read.xml"), LDAP_CONNECTOR_TYPE, true, parentResult);
		
		Task task = taskManager.createTaskInstance();
		
		
		try {
			provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_OID,
					null, task, parentResult);
			AssertJUnit
					.fail("Expected unsupported operation exception, but haven't got one.");
		} catch (SystemException ex) {
				// this is expected..				
		}
	}

	@Test
	public void test402ConfiguredCapabilityNoCreate() throws Exception{
		
		OperationResult parentResult = new OperationResult("test401noReadNativeCapability");
		
		addResourceFromFile(new File(ProvisioningTestUtil.COMMON_TEST_DIR_FILE, "/resource-opendj-no-create.xml"), LDAP_CONNECTOR_TYPE, true, parentResult);
		
		Task task = taskManager.createTaskInstance();
		
		
		try {
			PrismObject<ShadowType> shadow = parseObjectType(new File(ACCOUNT_NEW_FILENAME), ShadowType.class).asPrismObject();
			provisioningService.addObject(shadow, null, null, task, parentResult);
			AssertJUnit
					.fail("Expected unsupported operation exception, but haven't got one.");
		} catch (SystemException ex) {
			LOGGER.info("exception: {}", ex.getMessage(), ex);
				// this is expected..				
		}
	}
	
	@Test
	public void test403ConfiguredCapabilityNoDelete() throws Exception{
		
		OperationResult parentResult = new OperationResult("test401noReadNativeCapability");
		
		addResourceFromFile(new File(ProvisioningTestUtil.COMMON_TEST_DIR_FILE, "/resource-opendj-no-delete.xml"), LDAP_CONNECTOR_TYPE, true, parentResult);
		
		Task task = taskManager.createTaskInstance();
		
		
		try {
			provisioningService.deleteObject(ShadowType.class, ACCOUNT_NEW_OID, null, null, task, parentResult);
			AssertJUnit
					.fail("Expected unsupported operation exception, but haven't got one.");
		} catch (SystemException ex) {
				// this is expected..				
		}
	}
	
	@Test
	public void test404ConfiguredCapabilityNoUpdate() throws Exception{
		
		OperationResult parentResult = new OperationResult("test401noReadNativeCapability");
		
		addResourceFromFile(new File(ProvisioningTestUtil.COMMON_TEST_DIR_FILE, "/resource-opendj-no-update.xml"), LDAP_CONNECTOR_TYPE, true, parentResult);
		
		Task task = taskManager.createTaskInstance();
		
		
		try {
			PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(new ItemPath(ShadowType.F_ATTRIBUTES, new QName(resourceType.getNamespace(), "sn")), prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class), "doesnotmatter");
			Collection modifications = MiscUtil.createCollection(delta);
			provisioningService.modifyObject(ShadowType.class, ACCOUNT_NEW_OID, modifications, null, null, task, parentResult);
			AssertJUnit
					.fail("Expected unsupported operation exception, but haven't got one.");
		} catch (SystemException ex) {
				// this is expected..				
		}
	}
	
	@Test
	public void test600AddResourceOpenDjBadCredentials() throws Exception {
		final String TEST_NAME = "test600AddResourceOpenDjBadCredentials";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJNegative.class.getName()
				+ "." + TEST_NAME);

		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_OPENDJ_BAD_CREDENTIALS_FILE);
		fillInConnectorRef(resource, LDAP_CONNECTOR_TYPE, result);

		// WHEN
		String addedObjectOid = provisioningService.addObject(resource, null, null, taskManager.createTaskInstance(), result);
		
		// THEN
		result.computeStatus();
		display(result);
		assertSuccess(result);
		
		assertEquals("Wrong oid", RESOURCE_OPENDJ_BAD_CREDENTIALS_OID, addedObjectOid);
	}
	
	@Test
	public void test603ConnectionBadCredentials() throws Exception {
		final String TEST_NAME = "test603ConnectionBadCredentials";
		TestUtil.displayTestTile(TEST_NAME);

		// WHEN
		OperationResult	operationResult = provisioningService.testResource(RESOURCE_OPENDJ_BAD_CREDENTIALS_OID);
		
		display("Test connection result (expected failure)",operationResult);
		TestUtil.assertFailure(operationResult);
		
		OperationResult initResult = operationResult.findSubresult(ConnectorTestOperation.CONNECTOR_INITIALIZATION.getOperation());
		assertTrue("Unexpected connector initialization message: "+initResult.getMessage(), initResult.getMessage().contains("invalidCredentials"));
		assertTrue("Unexpected connector initialization message: "+initResult.getMessage(), initResult.getMessage().contains("49"));
	}
	
	@Test
	public void test610AddResourceOpenDjBadBindDn() throws Exception {
		final String TEST_NAME = "test610AddResourceOpenDjBadBindDn";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJNegative.class.getName()
				+ "." + TEST_NAME);

		PrismObject<ResourceType> resource = prismContext.parseObject(RESOURCE_OPENDJ_BAD_BIND_DN_FILE);
		fillInConnectorRef(resource, LDAP_CONNECTOR_TYPE, result);

		// WHEN
		String addedObjectOid = provisioningService.addObject(resource, null, null, taskManager.createTaskInstance(), result);
		
		// THEN
		result.computeStatus();
		display(result);
		assertSuccess(result);
		
		assertEquals("Wrong oid", RESOURCE_OPENDJ_BAD_BIND_DN_OID, addedObjectOid);
	}
	
	@Test
	public void test613ConnectionBadBindDn() throws Exception {
		final String TEST_NAME = "test613ConnectionBadBindDn";
		TestUtil.displayTestTile(TEST_NAME);

		// WHEN
		OperationResult	operationResult = provisioningService.testResource(RESOURCE_OPENDJ_BAD_BIND_DN_OID);
		
		display("Test connection result (expected failure)",operationResult);
		TestUtil.assertFailure(operationResult);
		
		OperationResult initResult = operationResult.findSubresult(ConnectorTestOperation.CONNECTOR_INITIALIZATION.getOperation());
		assertTrue("Unexpected connector initialization message: "+initResult.getMessage(), initResult.getMessage().contains("invalidCredentials"));
		assertTrue("Unexpected connector initialization message: "+initResult.getMessage(), initResult.getMessage().contains("49"));
	}
}
