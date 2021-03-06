/**
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.List;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.util.FocusTabVisibleBehavior;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class AbstractRoleMainPanel<R extends AbstractRoleType> extends FocusMainPanel<R> {
	private static final long serialVersionUID = 1L;
	
	private LoadableModel<List<AssignmentEditorDto>> inducementsModel;

	public AbstractRoleMainPanel(String id, LoadableModel<ObjectWrapper<R>> objectModel, 
			LoadableModel<List<AssignmentEditorDto>> assignmentsModel, 
			LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel, 
			LoadableModel<List<AssignmentEditorDto>> inducementsModel, PageAdminFocus<R> parentPage) {
		super(id, objectModel, assignmentsModel, projectionModel, parentPage);
		this.inducementsModel = inducementsModel;
	}

	@Override
	protected List<ITab> createTabs(final PageAdminObjectDetails<R> parentPage) {
		List<ITab> tabs = super.createTabs(parentPage);

		FocusTabVisibleBehavior authorization = new FocusTabVisibleBehavior(unwrapModel(),
				ComponentConstants.UI_FOCUS_TAB_INDUCEMENTS_URL);
		tabs.add(new CountablePanelTab(parentPage.createStringResource("FocusType.inducement"), authorization)
		{
			private static final long serialVersionUID = 1L;
			
			@Override
			public WebMarkupContainer createPanel(String panelId) {
				return new AssignmentTablePanel<R>(panelId, parentPage.createStringResource("FocusType.inducement"), inducementsModel) {
					private static final long serialVersionUID = 1L;

					@Override
					public List<AssignmentType> getAssignmentTypeList() {
						return getObject().asObjectable().getInducement();
					}

					@Override
					public String getExcludeOid() {
						return getObject().getOid();
					}
				};
			}
			
			@Override
			public String getCount() {
				return Integer.toString(inducementsModel.getObject() == null ? 0 : inducementsModel.getObject().size());
			}
		});
		
		return tabs;
	}
}
