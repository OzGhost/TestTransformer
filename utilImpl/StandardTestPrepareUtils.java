package ch.axonivy.fintech.standard.core.mock;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import ch.axonivy.fintech.guiframework.mock.SecurityManagerMock;
import ch.axonivy.fintech.standard.log.BaseLogOrigin;
import ch.axonivy.fintech.standard.log.LogOrigin;
import ch.axonivy.fintech.standard.log.LoggerFactory;
import ch.ivyteam.ivy.application.IApplication;
import ch.ivyteam.ivy.application.restricted.IEnvironment;
import ch.ivyteam.ivy.cm.IContentManagementSystem;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.globalvars.IGlobalVariableContext;
import ch.ivyteam.ivy.rest.client.IRestClientContext;
import ch.ivyteam.ivy.security.ISecurityManager;
import ch.ivyteam.ivy.security.SecurityManagerFactory;
import ch.ivyteam.ivy.workflow.IWorkflowContext;
import ch.ivyteam.ivy.workflow.IWorkflowSession;
//import ch.ivyteam.log.Logger;

public final class StandardTestPrepareUtils {

	private StandardTestPrepareUtils() {}
	
	public static void prepareIvy(){
		PowerMockito.mockStatic(Ivy.class);
	}
	
	public static void prepareGlobalVar(){
		IGlobalVariableContext globalVariableContext = Mockito.mock(IGlobalVariableContext.class);
		PowerMockito.when(Ivy.var()).thenReturn(globalVariableContext);
	}
	
	public static void prepareIvyLogger(){
		ch.ivyteam.log.Logger ivyLogger = Mockito.mock(ch.ivyteam.log.Logger.class);
		PowerMockito.when(Ivy.log()).thenReturn(ivyLogger);
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
	
	public static void prepareIvyCms(){
		IContentManagementSystem cms = Mockito.mock(IContentManagementSystem.class);
		PowerMockito.when(Ivy.cms()).thenReturn(cms);
	}
	
	public static void prepareIvyRestService(){
		IRestClientContext restClient = Mockito.mock(IRestClientContext.class);
		PowerMockito.when(Ivy.rest()).thenReturn(restClient);
	}
	
	public static void prepareIvySession(){
		IWorkflowSession workflowSession = Mockito.mock(IWorkflowSession.class);
		PowerMockito.when(Ivy.session()).thenReturn(workflowSession);
	}
	
	public static IEnvironment mockIvyEnvironment() {
		IWorkflowContext workflowContext = Mockito.mock(IWorkflowContext.class);
		PowerMockito.when(Ivy.wf()).thenReturn(workflowContext);
		
		IApplication application = Mockito.mock(IApplication.class);
		Mockito.when(workflowContext.getApplication()).thenReturn(application);
		
		IEnvironment environment = Mockito.mock(IEnvironment.class);
		Mockito.when(application.getActualEnvironment()).thenReturn(environment);
		
		return environment; 
	}
	
	public static ISecurityManager mockSecurityManager() {
		PowerMockito.mockStatic(SecurityManagerFactory.class);
		SecurityManagerMock securityManagerMock = new SecurityManagerMock();
		PowerMockito.when(SecurityManagerFactory.getSecurityManager()).thenReturn(securityManagerMock);
		return securityManagerMock;
	}
	
}
