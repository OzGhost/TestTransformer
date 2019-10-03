package ch.axonivy.fintech.crdhway.mockutil;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.primefaces.context.RequestContext;

import ch.axonivy.fintech.crdhway.CrdhwayDossier;
import ch.axonivy.fintech.crdhway.business.AlertCockpitCheckManualUpdater;
import ch.axonivy.fintech.crdhway.document.CrdhwayDocumentType;
import ch.axonivy.fintech.crdhway.document.enums.CrdhwayDefaultDocumentType;
import ch.axonivy.fintech.crdhway.dossier.process.CrdhwayConfiguration;
import ch.axonivy.fintech.crdhway.dossier.process.DocumentConfiguration;
import ch.axonivy.fintech.guiframework.bean.CommonDialogBean;
import ch.axonivy.fintech.guiframework.bean.ComponentContext;
import ch.axonivy.fintech.guiframework.bean.GlobalVariableContext;
import ch.axonivy.fintech.guiframework.bean.GuiFrameworkManagedBean;
import ch.axonivy.fintech.guiframework.bean.PageContext;
import ch.axonivy.fintech.guiframework.enums.ButtonType;
import ch.axonivy.fintech.guiframework.enums.DialogType;
import ch.axonivy.fintech.guiframework.rulesengine.RuleParamVO;
import ch.axonivy.fintech.guiframework.rulesengine.RuleParamVO.RuleParamVOBuilder;
import ch.axonivy.fintech.guiframework.util.GlobalVariable;
import ch.axonivy.fintech.guiframework.util.GuiFrameworkUtil;
import ch.axonivy.fintech.guiframework.util.ManagedBeanUtil;
import ch.axonivy.fintech.guiframework.util.UIComponentUtil;
import ch.axonivy.fintech.standard.core.bean.EmailServerConfig;
import ch.axonivy.fintech.standard.core.util.IvyEngineUtil;
import ch.axonivy.fintech.standard.core.util.UserUtil;
import ch.axonivy.fintech.standard.document.service.DocumentViewerPageService;
import ch.axonivy.fintech.standard.log.BaseLogOrigin;
import ch.axonivy.fintech.standard.log.LogOrigin;
import ch.axonivy.fintech.standard.log.LoggerFactory;
import ch.ivy.addon.portalkit.util.UserUtils;
import ch.ivyteam.ivy.business.data.store.BusinessDataRepository;
import ch.ivyteam.ivy.cm.IContentManagementSystem;
import ch.ivyteam.ivy.cm.IContentObjectValue;
import ch.ivyteam.ivy.data.cache.IDataCache;
import ch.ivyteam.ivy.data.cache.IDataCacheContext;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.globalvars.IGlobalVariableContext;
import ch.ivyteam.ivy.htmldialog.IHtmlDialogContext;
import ch.ivyteam.ivy.security.ISecurityContext;
import ch.ivyteam.ivy.security.ISecurityManager;
import ch.ivyteam.ivy.security.IUser;
import ch.ivyteam.ivy.security.SecurityManagerFactory;
import ch.ivyteam.ivy.workflow.IWorkflowContext;
import ch.ivyteam.ivy.workflow.IWorkflowSession;
//import ch.ivyteam.log.Logger;

public class CrdhwayTestPrepareUtil {
	public static final String SESSION_USER_NAME = "admin";
	public static final String SERVER_MAIL = "Server Mail";
	
	private CrdhwayTestPrepareUtil() {}
	
	public static IContentManagementSystem prepareMockIvyCms() {
		IContentManagementSystem cms = Mockito.mock(IContentManagementSystem.class);
		PowerMockito.when(Ivy.cms()).thenReturn(cms);
		when(cms.co(Mockito.any(String.class))).thenReturn("");
		IContentObjectValue iContentObjectValue = Mockito.mock(IContentObjectValue.class);
		when(cms.findContentObjectValue(Mockito.anyString(), Mockito.any(Locale.class))).thenReturn(iContentObjectValue);
		when(iContentObjectValue.getContentAsString()).thenReturn("");
		return cms;
	}
	
	public static IGlobalVariableContext prepareMockIvyGlobalVariable() {
		IGlobalVariableContext globalVariable = Mockito.mock(IGlobalVariableContext.class);
		PowerMockito.when(Ivy.var()).thenReturn(globalVariable);
		return globalVariable;
	}
	
	public static ch.ivyteam.log.Logger prepareMockIvyLog() {
		ch.ivyteam.log.Logger logger = Mockito.mock(ch.ivyteam.log.Logger.class);
		PowerMockito.when(Ivy.log()).thenReturn(logger);
		Mockito.doNothing().when(logger).debug(Mockito.anyString());
		Mockito.doNothing().when(logger).info(Mockito.anyString());
		Mockito.doNothing().when(logger).error(Mockito.anyString());
		return logger;
	}
	
	public static ch.axonivy.fintech.standard.log.Logger prepareMockStandardLoggerFactory() {
		ch.axonivy.fintech.standard.log.Logger logger = Mockito.mock(ch.axonivy.fintech.standard.log.Logger.class);
		PowerMockito.mockStatic(LoggerFactory.class);
		PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class))).thenReturn(logger);
		PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class))).thenReturn(logger);
		PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class), Mockito.any())).thenReturn(logger);
		PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any())).thenReturn(logger);
		PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(), Mockito.any())).thenReturn(logger);
		Mockito.doNothing().when(logger).debug(Mockito.anyString(), Mockito.anyString(), Mockito.any());
		Mockito.doNothing().when(logger).info(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(logger).error(Mockito.anyString(), Mockito.anyString());
		Mockito.doNothing().when(logger).error(Mockito.anyString(), Mockito.anyString(), Mockito.any());
		return logger;
	}
	
	public static void prepareMockStaticIvy() {
		PowerMockito.mockStatic(Ivy.class);
	}

	public static IWorkflowSession prepareMockIvySession() {
		IWorkflowSession session = Mockito.mock(IWorkflowSession.class);
		PowerMockito.when(Ivy.session()).thenReturn(session);
		when(session.getSessionUserName()).thenReturn("admin");
		
		when(session.getContentLocale()).thenReturn(Locale.ENGLISH);
		IWorkflowContext iWorkflowContext = Mockito.mock(IWorkflowContext.class);
		when(session.getWorkflowContext()).thenReturn(iWorkflowContext);
		ISecurityContext iSecurityContext = Mockito.mock(ISecurityContext.class);
		when(iWorkflowContext.getSecurityContext()).thenReturn(iSecurityContext);
		when(session.getSecurityContext()).thenReturn(iSecurityContext);
		ArrayList<IUser> arrayList = new ArrayList<>();
		IUser user = Mockito.mock(IUser.class);
		when(session.getSessionUser()).thenReturn(user);
		when(user.getName()).thenReturn("admin");
		arrayList.add(user);
		when(iSecurityContext.getUsers()).thenReturn(arrayList);
		return session;
	}
	
	public static GuiFrameworkManagedBean prepareGuiFrameworkManagedBean() {
		PowerMockito.mockStatic(GuiFrameworkUtil.class);
		GuiFrameworkManagedBean gfManagedBean = Mockito.mock(GuiFrameworkManagedBean.class);
		Mockito.when(GuiFrameworkUtil.getGuiFrameworkManagedBean()).thenReturn(gfManagedBean);
		return gfManagedBean;
	}
	
	public static PageContext preparePageContext() {
		PageContext pageContext = Mockito.mock(PageContext.class);
		GuiFrameworkManagedBean gfManagedBean = prepareGuiFrameworkManagedBean();
		Mockito.when(gfManagedBean.getPageContext()).thenReturn(pageContext);
		return pageContext;
	}
	
	public static Map<String, ComponentContext> prepareComponentContexts() {
		Map<String, ComponentContext> componentContextMap = new HashMap<>();
		PageContext pageContext = preparePageContext();
		Mockito.when(pageContext.getComponentContexts()).thenReturn(componentContextMap);
		return componentContextMap;
	}
	
	public static <T> void mockManagedBeanUtil(Class<T> clazz, T mockService) {
		PowerMockito.mockStatic(ManagedBeanUtil.class);
		Mockito.when(ManagedBeanUtil.getServiceFromPool(clazz)).thenReturn(mockService);
	}

	public static void prepareMockGuiFrameworkUtil() {
		PowerMockito.mockStatic(GuiFrameworkUtil.class);
		CommonDialogBean commonDialog = Mockito.mock(CommonDialogBean.class);
		PowerMockito.when(GuiFrameworkUtil.getCommonDialog(Mockito.any(DialogType.class), Mockito.any(ButtonType.class), Mockito.anyString())).thenReturn(commonDialog);
		PowerMockito.doNothing().when(commonDialog).show();
	}
	
	public static GuiFrameworkManagedBean prepareMockGuiFrameworkUtilGetRootDataModel(CrdhwayDossier dossier) {
		GuiFrameworkManagedBean gfManagedBean = prepareGuiFrameworkManagedBean();
		Mockito.when(gfManagedBean.getRootDataModel()).thenReturn(dossier);
		return gfManagedBean;
	}
	
	public static CrdhwayConfiguration prepareConfiguration() {
		CrdhwayConfiguration crdhwayConfiguration = new CrdhwayConfiguration();
		DocumentConfiguration documentConfig = new DocumentConfiguration();
		documentConfig.setDocumentTypes(buildDocumentTypes());
		crdhwayConfiguration.setDocumentConfig(documentConfig);
		return crdhwayConfiguration;
	}

	public static IUser prepareMockIUserFindByUserUtil() {
		IUser user = Mockito.mock(IUser.class);
		PowerMockito.mockStatic(UserUtil.class);
		when(UserUtil.findUser(Mockito.anyString())).thenReturn(user);
		return user;
	}

	public static ISecurityManager prepareMockSecurityManager() {
		PowerMockito.mockStatic(SecurityManagerFactory.class);
		ISecurityManager mockISecurityManager = PowerMockito.mock(ISecurityManager.class);
		when(SecurityManagerFactory.getSecurityManager()).thenReturn(mockISecurityManager);
		return mockISecurityManager;
	}

	public static EmailServerConfig prepareMockEmailServerConfig() throws Exception {
		PowerMockito.mockStatic(IvyEngineUtil.class);
		EmailServerConfig mockEmailServerConfig = Mockito.mock(EmailServerConfig.class);
		when(IvyEngineUtil.getEmailServerConfig()).thenReturn(mockEmailServerConfig);
		when(mockEmailServerConfig.getMailAddress()).thenReturn("Server Mail");
		return mockEmailServerConfig;
	}

	public static IDataCache prepareMockDataCache() {
		IDataCacheContext dataCacheContext = Mockito.mock(IDataCacheContext.class);
		IDataCache dataCache = Mockito.mock(IDataCache.class);
		
		Mockito.when(Ivy.datacache()).thenReturn(dataCacheContext);
		Mockito.when(Ivy.datacache().getAppCache()).thenReturn(dataCache);
		
		return dataCache;
	}
	
	public static BusinessDataRepository prepareMockIvyRepo() {
		BusinessDataRepository businessDataRepository = Mockito.mock(BusinessDataRepository.class);
		Mockito.when(Ivy.repo()).thenReturn(businessDataRepository);
		return businessDataRepository;
	}
	
	public static void prepareMockUIComponentUtil() {
		PowerMockito.mockStatic(UIComponentUtil.class);
	}
	
	public static FacesContext prepareMockFacesContext() {
		PowerMockito.mockStatic(FacesContext.class);
		FacesContext facesContext = mock(FacesContext.class);
		when(FacesContext.getCurrentInstance()).thenReturn(facesContext);
		return facesContext;
	}
	
	public static ExternalContext mockExternalContext() {
		FacesContext mockFacesContext = prepareMockFacesContext();
		ExternalContext mockExternalContext = Mockito.mock(ExternalContext.class);
		Mockito.when(mockFacesContext.getExternalContext()).thenReturn(mockExternalContext);
		Mockito.when(mockExternalContext.getSessionMap()).thenReturn(new HashMap<>());
		return mockExternalContext;
	}
	
	public static void prepareMockUserUtils() {
		PowerMockito.mockStatic(UserUtils.class);
		when(UserUtils.getSessionUserName()).thenReturn("");
	}
	
	public static RequestContext prepareMockRequestContext() {
		RequestContext requestContext = Mockito.mock(RequestContext.class);
		PowerMockito.mockStatic(RequestContext.class);
		when(RequestContext.getCurrentInstance()).thenReturn(requestContext);
		return requestContext;
	}
	
	public static void prepareMockGlobalVariable() {
		PowerMockito.mockStatic(GlobalVariable.class);
		when(GlobalVariable.get(Mockito.anyString())).thenReturn("");
		when(GlobalVariable.getGlobalVariableValueAsBoolean(Mockito.anyString())).thenReturn(false);
		when(GlobalVariable.getGlobalVariableValueAsNumber(Mockito.anyString())).thenReturn(0);
		when(GlobalVariable.getGlobalVariableValueAsNumber(Mockito.anyString(), Mockito.anyInt())).thenReturn(0);
	}
	
	public static IHtmlDialogContext prepareMockHtml() {
		IHtmlDialogContext html = Mockito.mock(IHtmlDialogContext.class);
		PowerMockito.when(Ivy.html()).thenReturn(html);
		Mockito.when(html.startref(Mockito.anyString())).thenReturn("");
		return html;
	}
	
	public static DocumentViewerPageService mockDocumentViewerPageService() throws IOException {
		DocumentViewerPageService documentViewerPageService = PowerMockito.mock(DocumentViewerPageService.class);
		PowerMockito.mockStatic(DocumentViewerPageService.class);
		Mockito.when(DocumentViewerPageService.createInstance(Mockito.any())).thenReturn(documentViewerPageService);
		Mockito.when(documentViewerPageService.withTitleForBrowserTab(Mockito.anyString())).thenReturn(documentViewerPageService);
		Mockito.when(documentViewerPageService.withDefaultDownloadFileName(Mockito.anyString())).thenReturn(documentViewerPageService);
		Mockito.doNothing().when(documentViewerPageService).execute();
		return documentViewerPageService;
	}
	
	/**
	 * Remember to add {@link RuleParamVOBuilder} and {@link RuleParamVO} into @PrepareForTest
	 */
	public static RuleParamVO prepareMockRuleParamVO() {
		PowerMockito.mockStatic(RuleParamVOBuilder.class);
		RuleParamVOBuilder ruleParamVOBuilder = Mockito.mock(RuleParamVOBuilder.class);
		Mockito.when(RuleParamVOBuilder.createBuilder()).thenReturn(ruleParamVOBuilder);
		Mockito.when(RuleParamVOBuilder.createBuilder().setCollectionMetadata(Mockito.anyList())).thenReturn(ruleParamVOBuilder);
		Mockito.when(RuleParamVOBuilder.createBuilder().setContextVariables(Mockito.anyMap())).thenReturn(ruleParamVOBuilder);
		Mockito.when(RuleParamVOBuilder.createBuilder().setGlobalVarContext(Mockito.any(GlobalVariableContext.class))).thenReturn(ruleParamVOBuilder);
		Mockito.when(RuleParamVOBuilder.createBuilder().setGuiFrameworkBean(Mockito.any(GuiFrameworkManagedBean.class))).thenReturn(ruleParamVOBuilder);
		Mockito.when(RuleParamVOBuilder.createBuilder().setIgnoreContextVariable(Mockito.anyBoolean())).thenReturn(ruleParamVOBuilder);
		Mockito.when(RuleParamVOBuilder.createBuilder().setRequireNamespaces(Mockito.anyList())).thenReturn(ruleParamVOBuilder);
		RuleParamVO ruleParamVO = PowerMockito.mock(RuleParamVO.class);
		Mockito.when(ruleParamVOBuilder.build()).thenReturn(ruleParamVO);
		return ruleParamVO;
	}
	
	public static AlertCockpitCheckManualUpdater prepareMockAlertCockpitCheckManualUpdater() {
		AlertCockpitCheckManualUpdater alertUpdater = Mockito.mock(AlertCockpitCheckManualUpdater.class);
		PowerMockito.mockStatic(AlertCockpitCheckManualUpdater.class);
		Mockito.when(AlertCockpitCheckManualUpdater.createInstance(Mockito.anyBoolean())).thenReturn(alertUpdater);
		Mockito.doNothing().when(alertUpdater).execute(Mockito.any(RuleParamVO.class));
		return alertUpdater;
	}
	
	private static Set<CrdhwayDocumentType> buildDocumentTypes() {
		List<CrdhwayDefaultDocumentType> crdhwayDefaultDocumentTypes = CrdhwayDefaultDocumentType.DEFAULT_DOCUMENT_TYPE;
		Set<CrdhwayDocumentType> documentTypes = new LinkedHashSet<>();
		for (CrdhwayDefaultDocumentType documentType : crdhwayDefaultDocumentTypes) {
			documentTypes.add(documentType.getCrdhwayDocumentType());
		}
		return documentTypes;
	}
}
