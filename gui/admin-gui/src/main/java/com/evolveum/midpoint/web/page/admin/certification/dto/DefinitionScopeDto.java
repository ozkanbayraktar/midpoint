package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * Created by Kate on 13.12.2015.
 */
public class DefinitionScopeDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_OBJECT_TYPE = "objectType";
    public static final String F_SEARCH_FILTER_TEXT = "searchFilterText";
    public static final String F_INCLUDE_ASSIGNMENTS = "includeAssignments";
    public static final String F_INCLUDE_INDUCEMENTS = "includeInducements";
    public static final String F_INCLUDE_RESOURCES = "includeResources";
    public static final String F_INCLUDE_ROLES = "includeRoles";
    public static final String F_INCLUDE_ORGS = "includeOrgs";
    public static final String F_INCLUDE_SERVICES = "includeServices";
    public static final String F_INCLUDE_ENABLED_ITEMS_ONLY = "enabledItemsOnly";

    private String name;
    private String description;
    private DefinitionScopeObjectType objectType;
    private String searchFilterText;
    private boolean includeAssignments;
    private boolean includeInducements;
    private boolean includeResources;
    private boolean includeRoles;
    private boolean includeOrgs;
    private boolean includeServices;
    private boolean enabledItemsOnly;

    public void loadSearchFilter(SearchFilterType searchFilterType, PrismContext prismContext)  {
        if (searchFilterType == null) {
            return;
        }

        try {
            RootXNode clause = searchFilterType.getFilterClauseAsRootXNode();
            searchFilterText = prismContext.serializeXNodeToString(clause, PrismContext.LANG_XML);
        } catch (SchemaException e) {
            throw new SystemException("Cannot serialize search filter " + searchFilterType + ": " + e.getMessage(), e);
        }
    }

    public SearchFilterType getParsedSearchFilter(PrismContext context) {
        if (searchFilterText == null || searchFilterText.isEmpty()) {
            return null;
        }

        SearchFilterType rv = new SearchFilterType();
        RootXNode filterClauseNode;
        try {
            filterClauseNode = (RootXNode) context.parseToXNode(searchFilterText, PrismContext.LANG_XML);
        } catch (SchemaException e) {
            throw new SystemException("Cannot parse search filter " + searchFilterText + ": " + e.getMessage(), e);
        }
        rv.setFilterClauseXNode(filterClauseNode);
        return rv;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DefinitionScopeObjectType getObjectType() {
        return objectType;
    }

    public void setObjectType(DefinitionScopeObjectType objectType) {
        this.objectType = objectType;
    }

    public String getSearchFilterText() {
        return searchFilterText;
    }

    public void setSearchFilterText(String searchFilterText) {
        this.searchFilterText = searchFilterText;
    }

    public boolean isIncludeAssignments() {
        return includeAssignments;
    }

    public void setIncludeAssignments(boolean includeAssignments) {
        this.includeAssignments = includeAssignments;
    }

    public boolean isIncludeInducements() {
        return includeInducements;
    }

    public void setIncludeInducements(boolean includeInducements) {
        this.includeInducements = includeInducements;
    }

    public boolean isIncludeResources() {
        return includeResources;
    }

    public void setIncludeResources(boolean includeResources) {
        this.includeResources = includeResources;
    }

    public boolean isIncludeRoles() {
        return includeRoles;
    }

    public void setIncludeRoles(boolean includeRoles) {
        this.includeRoles = includeRoles;
    }

    public boolean isIncludeOrgs() {
        return includeOrgs;
    }

    public void setIncludeOrgs(boolean includeOrgs) {
        this.includeOrgs = includeOrgs;
    }

	public boolean isIncludeServices() {
		return includeServices;
	}

	public void setIncludeServices(boolean includeServices) {
		this.includeServices = includeServices;
	}

	public boolean isEnabledItemsOnly() {
        return enabledItemsOnly;
    }

    public void setEnabledItemsOnly(boolean enabledItemsOnly) {
        this.enabledItemsOnly = enabledItemsOnly;
    }
}
