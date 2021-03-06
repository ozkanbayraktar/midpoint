package com.evolveum.midpoint.model.api;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;

public enum ModelAuthorizationAction implements DisplayableValue<String> {

	READ("read", "Read", "READ_HELP"),
	ADD("add", "Add", "ADD_HELP"),
	MODIFY("modify", "Modify", "MODIFY_HELP"),
	DELETE("delete", "Delete", "DELETE_HELP"),
	RECOMPUTE("recompute", "Recompute", "RECOMPUTE_HELP"),
	TEST("test", "Test resource", "TEST_RESOURCE_HELP"),
	
	/**
	 * Import objects from file or a stream. This means importing any type of
	 * object (e.g. user, configuration, resource, object templates, ...
	 */
	IMPORT_OBJECTS("importObjects", "Import Objects", "IMPORT_OBJECTS_HELP"),
	
	/**
	 * Import resource objects from resource. This means import of accounts, entitlements
	 * or other objects from a resource. The import creates shadows.
	 */
	IMPORT_FROM_RESOURCE("importFromResource", "Import from Resource", "IMPORT_FROM_RESOURCE_HELP"),
	
	DISCOVER_CONNECTORS("discoverConnectors", "Discover Connectors", "DISCOVER_CONNECTORS_HELP"), 
	
	ASSIGN("assign", "Assign", "ASSIGN_HELP"),
	UNASSIGN("unassign", "Unassign", "UNASSIGN_HELP"),
    EXECUTE_SCRIPT("executeScript", "Execute script", "EXECUTE_SCRIPT_HELP"),
    CHANGE_CREDENTIALS("changeCredentials", "Change credentials", "CHANGE_CREDENTIALS_HELP"),

	SUSPEND_TASK("suspendTask", "Suspend task", "SUSPEND_TASK_HELP"),
	RESUME_TASK("resumeTask", "Resume task", "RESUME_TASK_HELP"),
	RUN_TASK_IMMEDIATELY("runTaskImmediately", "Run task immediately", "RUN_TASK_IMMEDIATELY_HELP"),
	STOP_SERVICE_THREADS("stopServiceThreads", "Stop service threads", "STOP_SERVICE_THREADS_HELP"),
	START_SERVICE_THREADS("startServiceThreads", "Start service threads", "START_SERVICE_THREADS_HELP"),
	SYNCHRONIZE_TASKS("synchronizeTasks", "Synchronize tasks", "SYNCHRONIZE_TASKS_HELP"),
	SYNCHRONIZE_WORKFLOW_REQUESTS("synchronizeWorkflowRequests", "Synchronize workflow requests", "SYNCHRONIZE_WORKFLOW_REQUESTS_HELP"),
	STOP_TASK_SCHEDULER("stopTaskScheduler", "Stop task scheduler", "STOP_TASK_SCHEDULER_HELP"),
	START_TASK_SCHEDULER("startTaskScheduler", "Start task scheduler", "START_TASK_SCHEDULER_HELP"),

	CREATE_CERTIFICATION_CAMPAIGN("createCertificationCampaign", "Create a certification campaign", "CREATE_CERTIFICATION_CAMPAIGN_HELP"),
	OPEN_CERTIFICATION_CAMPAIGN_REVIEW_STAGE("openCertificationCampaignReviewStage", "Open access certification campaign review stage", "OPEN_CERTIFICATION_CAMPAIGN_REVIEW_STAGE_HELP"),
	CLOSE_CERTIFICATION_CAMPAIGN_REVIEW_STAGE("closeCertificationCampaignReviewStage", "Close access certification campaign review stage", "CLOSE_CERTIFICATION_CAMPAIGN_REVIEW_STAGE_HELP"),
	START_CERTIFICATION_REMEDIATION("startCertificationRemediation", "Start certification campaign results remediation", "START_CERTIFICATION_REMEDIATION_HELP"),
	CLOSE_CERTIFICATION_CAMPAIGN("closeCertificationCampaign", "Close certification campaign", "CLOSE_CERTIFICATION_CAMPAIGN_HELP"),
	READ_OWN_CERTIFICATION_DECISIONS("readOwnCertificationDecisions", "Read own access certification decisions", "READ_OWN_CERTIFICATION_DECISIONS_HELP"),
	RECORD_CERTIFICATION_DECISION("recordCertificationDecision", "Record access certification decision", "RECORD_CERTIFICATION_DECISION_HELP"),

	COMPLETE_ALL_WORK_ITEMS("completeAllWorkItems", "Complete all work items", "COMPLETE_ALL_WORK_ITEMS_HELP"),
	READ_ALL_WORK_ITEMS("readAllWorkItems", "Read all work items", "READ_ALL_WORK_ITEMS_HELP"),		// currently not implemented seriously
	STOP_APPROVAL_PROCESS_INSTANCE("stopApprovalProcessInstance", "Stop approval process instance", "STOP_APPROVAL_PROCESS_INSTANCE_HELP")
	;
	
	private String url;
	private String label;
	private String description;
	
	private ModelAuthorizationAction(String urlLocalPart, String label, String desc) {
		this.url = QNameUtil.qNameToUri(new QName(ModelService.AUTZ_NAMESPACE, urlLocalPart));
		this.label = label;
		this.description = desc;
	}
	
	public String getUrl() {
		return url;
	}
	
	@Override
	public String getValue() {
		return url;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
}
