package ch.axonivy.fintech.mortgage.mockutil;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.ws.rs.client.WebTarget;

import org.apache.commons.lang3.StringUtils;
import org.junit.rules.TemporaryFolder;
import org.primefaces.context.RequestContext;

import ch.axonivy.fintech.guiframework.base.DialogContextHolder;
import ch.axonivy.fintech.guiframework.bean.CommandButton;
import ch.axonivy.fintech.guiframework.bean.CommonDialogBean;
import ch.axonivy.fintech.guiframework.bean.ComponentContext;
import ch.axonivy.fintech.guiframework.bean.GuiFrameworkManagedBean;
import ch.axonivy.fintech.guiframework.bean.PageContext;
import ch.axonivy.fintech.guiframework.component.UIComponentMock;
import ch.axonivy.fintech.guiframework.component.UIViewRootMock;
import ch.axonivy.fintech.guiframework.context.FacesContextMock;
import ch.axonivy.fintech.guiframework.context.RequestContextMock;
import ch.axonivy.fintech.guiframework.core.GuiFrameworkControllerConfig;
import ch.axonivy.fintech.guiframework.enums.GuiArgument;
import ch.axonivy.fintech.guiframework.enums.GuiLevel;
import ch.axonivy.fintech.guiframework.exception.GuiFrameworkException;
import ch.axonivy.fintech.guiframework.mock.SecurityManagerMock;
import ch.axonivy.fintech.guiframework.util.GuiFrameworkUtil;
import ch.axonivy.fintech.guiframework.util.ManagedBeanUtil;
import ch.axonivy.fintech.guiframework.util.UIComponentUtil;
import ch.axonivy.fintech.guiframework.workflow.BaseGuiWorkflow;
import ch.axonivy.fintech.standard.core.exception.BusinessException;
import ch.axonivy.fintech.standard.log.BaseLogOrigin;
import ch.axonivy.fintech.standard.log.LogOrigin;
import ch.axonivy.fintech.standard.log.LoggerFactory;
import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.bpm.error.BpmPublicErrorBuilder;
import ch.ivyteam.ivy.cm.IContentManagementSystem;
import ch.ivyteam.ivy.data.cache.IDataCache;
import ch.ivyteam.ivy.data.cache.IDataCacheContext;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.globalvars.IGlobalVariableContext;
import ch.ivyteam.ivy.rest.client.IRestClientContext;
import ch.ivyteam.ivy.scripting.objects.CompositeObject;
import ch.ivyteam.ivy.security.ISecurityContext;
import ch.ivyteam.ivy.security.ISecurityManager;
import ch.ivyteam.ivy.security.IUser;
import ch.ivyteam.ivy.security.SecurityManagerFactory;
import ch.ivyteam.ivy.workflow.IWorkflowContext;
import ch.ivyteam.ivy.workflow.IWorkflowSession;

public final class MortgageTestPrepareUtil {

	private MortgageTestPrepareUtil() {}
	
	public IContentManagementSystem prepareMockIvyCms() {
		IContentManagementSystem cms = Mockito.mock(IContentManagementSystem.class);
		Mockito.when(Ivy.cms()).thenReturn(cms);
		Mockito.when(cms.co(Mockito.any(String.class))).thenReturn("");
		return cms;
	}

	public IContentManagementSystem prepareMockIvyCmsWithContent(String content) {
		IContentManagementSystem cms = Mockito.mock(IContentManagementSystem.class);
		Mockito.when(Ivy.cms()).thenReturn(cms);
		Mockito.when(cms.co(Mockito.any(String.class))).thenReturn(content);
		return cms;
	}
	
	public void prepareMockStaticIvy() {
		PowerMockito.mockStatic(Ivy.class);
	}
	
	public IWorkflowSession prepareMockIvySession() {
		IWorkflowSession session = Mockito.mock(IWorkflowSession.class);
		Mockito.when(Ivy.session()).thenReturn(session);
		Mockito.when(session.getSessionUserName()).thenReturn("admin");
		
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
	
	public IWorkflowSession prepareMockIvySession(String username) {
		IWorkflowSession session = Mockito.mock(IWorkflowSession.class);
		Mockito.when(Ivy.session()).thenReturn(session);
		Mockito.when(session.getSessionUserName()).thenReturn(username);
		
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
	
	public IWorkflowSession mockLocale(String countryCode) {
		IWorkflowSession session = Mockito.mock(IWorkflowSession.class);
		Mockito.when(Ivy.session()).thenReturn(session);
		Mockito.when(session.getContentLocale()).thenReturn(new Locale(countryCode));
		return session;
	}
	
	public ch.ivyteam.log.Logger prepareMockIvyLog() {
		ch.ivyteam.log.Logger logger = Mockito.mock(ch.ivyteam.log.Logger.class);
		PowerMockito.when(Ivy.log()).thenReturn(logger);
		Mockito.doNothing().when(logger).debug(Mockito.anyString());
		Mockito.doNothing().when(logger).info(Mockito.anyString());
		Mockito.doNothing().when(logger).error(Mockito.anyString());
		return logger;
	}
	
	public ch.axonivy.fintech.standard.log.Logger prepareMockStandardLoggerFactory() {
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
	
	public BpmPublicErrorBuilder mockBpmPublicErrorBuilder() {
		PowerMockito.mockStatic(BpmError.class);
		BpmError bpmError = Mockito.mock(BpmError.class);
		BpmPublicErrorBuilder bpmPublicErrorBuilder = Mockito.mock(BpmPublicErrorBuilder.class);
		PowerMockito.when(BpmError.create(Mockito.anyString())).thenReturn(bpmPublicErrorBuilder);
		Mockito.when(bpmPublicErrorBuilder.withMessage(Mockito.anyString())).thenReturn(bpmPublicErrorBuilder);
		Mockito.when(bpmPublicErrorBuilder.withAttribute(Mockito.anyString(), Mockito.any())).thenReturn(bpmPublicErrorBuilder);
		Mockito.when(bpmPublicErrorBuilder.withCause(Mockito.any())).thenReturn(bpmPublicErrorBuilder);
		Mockito.when(bpmPublicErrorBuilder.build()).thenReturn(bpmError);
		return bpmPublicErrorBuilder;
	}
	
	public GuiFrameworkManagedBean prepareGuiFrameworkManagedBean() {
		GuiFrameworkManagedBean gfManagedBean = Mockito.mock(GuiFrameworkManagedBean.class);
		Mockito.when(GuiFrameworkUtil.getGuiFrameworkManagedBean()).thenReturn(gfManagedBean);
		return gfManagedBean;
	}

	public PageContext preparePageContext() {
		GuiFrameworkManagedBean gfManagedBean = Mockito.mock(GuiFrameworkManagedBean.class);
		Mockito.when(GuiFrameworkUtil.getGuiFrameworkManagedBean()).thenReturn(gfManagedBean);
		PageContext pageContext = Mockito.mock(PageContext.class);
		Mockito.when(gfManagedBean.getPageContext()).thenReturn(pageContext);
		return pageContext;
	}
	
	public void prepareMockGuiFrameworkUtil() {
		PowerMockito.mockStatic(GuiFrameworkUtil.class);
	}
	
	public IGlobalVariableContext prepareMockIvyGlobalVariable() {
		IGlobalVariableContext globalVariable = Mockito.mock(IGlobalVariableContext.class);
		Mockito.when(Ivy.var()).thenReturn(globalVariable);
		return globalVariable;
	}

	public IDataCache prepareIvyDataCache() {
		IDataCacheContext dataCacheContext = Mockito.mock(IDataCacheContext.class);
		Mockito.when(Ivy.datacache()).thenReturn(dataCacheContext);
		IDataCache dataCache = Mockito.mock(IDataCache.class);
		Mockito.when(dataCacheContext.getAppCache()).thenReturn(dataCache);
		return dataCache;
	}

	public WebTarget prepareIvyRestClientContext(String path) {
		IRestClientContext restContext = Mockito.mock(IRestClientContext.class);
		WebTarget webTarget = Mockito.mock(WebTarget.class);
		Mockito.when(Ivy.rest()).thenReturn(restContext);
		Mockito.when(restContext.client(path)).thenReturn(webTarget);
		return webTarget;
	}
	
	public CommonDialogBean mockDialog() {
		mockStatic(Ivy.class);
		IContentManagementSystem cms = mock(IContentManagementSystem.class);
		when(Ivy.cms()).thenReturn(cms);
		when(cms.co(ArgumentMatchers.anyString())).thenReturn("");
		mockStatic(GuiFrameworkUtil.class);
		CommonDialogBean dialog = mock(CommonDialogBean.class);
		when(GuiFrameworkUtil.getCommonDialog(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(dialog);
		CommandButton btn = mock(CommandButton.class);
		when(dialog.getYesButton()).thenReturn(btn);
		when(dialog.getOkButton()).thenReturn(btn);
		when(dialog.getNoButton()).thenReturn(btn);
		when(dialog.getCancelButton()).thenReturn(btn);
		doNothing().when(btn).setOnClickMethod(ArgumentMatchers.anyString());
		doNothing().when(btn).setActionListener(ArgumentMatchers.any());
		doNothing().when(btn).setButtonSubmitType(ArgumentMatchers.any());
		
		doNothing().when(dialog).show();
		return dialog;
	}
	
	public CommonDialogBean mockPureDialog() {
		mockStatic(Ivy.class);
		IContentManagementSystem cms = mock(IContentManagementSystem.class);
		when(Ivy.cms()).thenReturn(cms);
		when(cms.co(ArgumentMatchers.anyString())).thenReturn("");
		mockStatic(GuiFrameworkUtil.class);
		return mock(CommonDialogBean.class);
	}
	
	public IContentManagementSystem mockCms(String content) {
		mockStatic(Ivy.class);
		IContentManagementSystem cms = mock(IContentManagementSystem.class);
		when(Ivy.cms()).thenReturn(cms);
		when(cms.co(ArgumentMatchers.anyString())).thenReturn(content);
		return cms;
	}
	

	public void mockManagedBeanUtil(String beanName, Object mockService) throws GuiFrameworkException, BusinessException {
		PowerMockito.mockStatic(ManagedBeanUtil.class);
		Mockito.when(ManagedBeanUtil.getSessionManagedBean(Mockito.any(), Mockito.eq(beanName))).thenReturn(mockService);
	}
	
	public <T> void mockManagedBeanUtil(Class<T> clazz, T mockService) {
		PowerMockito.mockStatic(ManagedBeanUtil.class);
		Mockito.when(ManagedBeanUtil.getServiceFromPool(clazz)).thenReturn(mockService);
	}
	
	public RequestContext mockRequestContext(){
		RequestContextMock requestMock = PowerMockito.spy(new RequestContextMock());
		PowerMockito.mockStatic(RequestContext.class);
		Mockito.when(RequestContext.getCurrentInstance()).thenReturn(requestMock);
		return requestMock;
	}
	
	public RequestContext doNothing_requestContext_update(){
		RequestContextMock requestMock = PowerMockito.spy(new RequestContextMock());
		PowerMockito.mockStatic(RequestContext.class);
		Mockito.when(RequestContext.getCurrentInstance()).thenReturn(requestMock);

		RequestContext requestContext = requestMock;
		PowerMockito.doNothing().when(requestContext).update(Mockito.anyString());
		return requestContext;
	}
	
	public void mockUIComponentUtil_findComponentOnRootView(UIComponent mockComponent){
		mockStatic(UIComponentUtil.class);
		when(UIComponentUtil.findComponentOnRootView(ArgumentMatchers.any(), ArgumentMatchers.anyString())).thenReturn(mockComponent);
	}
	
	public FacesContext mockFacesContext(){
		mockStatic(FacesContext.class);
		FacesContextMock fc = Mockito.spy(new FacesContextMock());
		when(FacesContext.getCurrentInstance()).thenReturn(fc);
		return fc;
	}
	
	public FacesContextMock mockFacesContext_getViewRoot(){
		mockStatic(FacesContext.class);
		UIViewRootMock viewRoot = new UIViewRootMock();
		List<UIComponent> nodes = new ArrayList<>();
		UIComponentMock node = new UIComponentMock();
		node.setId("dummyComponent");
		node.setClientId("form:dummyComponent");
		nodes.add(node);
		viewRoot.setFacetsAndChildren(nodes.iterator());
		FacesContextMock fcm = new FacesContextMock(new HashMap<>());
		fcm.setViewRoot(viewRoot);
		when(FacesContext.getCurrentInstance()).thenReturn(fcm);
		return fcm;
	}

	public IContentManagementSystem mockCms(String uri, String content) {
		mockStatic(Ivy.class);
		IContentManagementSystem cms = mock(IContentManagementSystem.class);
		when(Ivy.cms()).thenReturn(cms);
		when(cms.co(uri)).thenReturn(content);
		return cms;
	}

	public <T extends CompositeObject> GuiFrameworkControllerConfig<T> mockGuiFrameworkControllerConfig() {
        @SuppressWarnings("unchecked")
        GuiFrameworkControllerConfig<T> guiConfig = Mockito.mock(GuiFrameworkControllerConfig.class);
		Mockito.when(guiConfig.getLevel()).thenReturn(GuiLevel.MIDDLE_LEVEL);
		Map<GuiArgument, Supplier<Object>> argumentGetters = new HashMap<>();
		argumentGetters.put(GuiArgument.COMPONENT_CONTEXT, ComponentContext::new);
		Mockito.when(guiConfig.getGuiArgumentGetters()).thenReturn(argumentGetters);
		Map<GuiArgument, Consumer<Object>> argumentSetters = new HashMap<>();
		argumentSetters.put(GuiArgument.DATAMODEL, dataModel -> dataModel.toString());
		Mockito.when(guiConfig.getGuiArgumentSetters()).thenReturn(argumentSetters);
		return guiConfig;
	}
	
	private Map<GuiArgument, Supplier<Object>> createMockedArgumentGetters() {
		Map<GuiArgument, Supplier<Object>> argumentGetters = new HashMap<>();
		argumentGetters.put(GuiArgument.COMPONENT_CONTEXT, ComponentContext::new);
		return argumentGetters;
	}
	
	private Map<GuiArgument, Consumer<Object>> createMockedArgumentSetters() {
		Map<GuiArgument, Consumer<Object>> argumentSetters = new HashMap<>();
		argumentSetters.put(GuiArgument.DATAMODEL, dataModel -> dataModel.toString());
		return argumentSetters;
	}
	
	public ISecurityManager mockSecurityManager() {
		ISecurityManager securityManager = new SecurityManagerMock();
		PowerMockito.mockStatic(SecurityManagerFactory.class);
		PowerMockito.when(SecurityManagerFactory.getSecurityManager()).thenReturn(securityManager);
		return securityManager;
	}
	
	public BaseGuiWorkflow mockBaseGuiWorkflow() {
		BaseGuiWorkflow baseGuiWorkflow = Mockito.mock(BaseGuiWorkflow.class);
		PowerMockito.mockStatic(BaseGuiWorkflow.class);
		PowerMockito.when(BaseGuiWorkflow.getInstance()).thenReturn(baseGuiWorkflow);
		return baseGuiWorkflow;
	}
	
	
	public void mockReturnWorkflowFromDialogContextHolder(BaseGuiWorkflow mockedBaseGuiWorkflow, Class<?> dialogDataClass) {
		PowerMockito.mockStatic(DialogContextHolder.class);
		DialogContextHolder dialogContextHolder = Mockito.mock(DialogContextHolder.class);
		Mockito.when(DialogContextHolder.getInstance()).thenReturn(dialogContextHolder);
		Mockito.when(dialogContextHolder.getWorkflow(Mockito.any(dialogDataClass))).thenReturn(mockedBaseGuiWorkflow);
	}
	
	public ch.ivyteam.ivy.scripting.objects.File mockIvyFile(TemporaryFolder tmpFolder, String fileName) throws Exception {
		PowerMockito.mockStatic(ch.ivyteam.ivy.scripting.objects.File.class);
        ch.ivyteam.ivy.scripting.objects.File file = Mockito.mock(ch.ivyteam.ivy.scripting.objects.File.class);
        PowerMockito.whenNew(ch.ivyteam.ivy.scripting.objects.File.class).withAnyArguments().thenReturn(file);
        File outputFile = tmpFolder.newFile(fileName);
        Mockito.when(file.getAbsolutePath()).thenReturn(outputFile.getAbsolutePath());
        Mockito.when(file.getJavaFile()).thenReturn(outputFile);
        return file;
	}
}
