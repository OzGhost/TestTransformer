package ch.axonivy.fintech.standard.document.action;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import ch.axonivy.fintech.standard.document.DefaultDocumentSection;
import ch.axonivy.fintech.standard.document.Document;
import ch.axonivy.fintech.standard.document.DocumentContent;
import ch.axonivy.fintech.standard.document.DocumentSection;
import ch.axonivy.fintech.standard.document.service.DocumentViewerPageService;
import ch.axonivy.fintech.standard.log.BaseLogOrigin;
import ch.axonivy.fintech.standard.log.LogOrigin;
import ch.axonivy.fintech.standard.log.LoggerFactory;

public final class DocumentSectionTestHelper {

	private DocumentSectionTestHelper() {
	}

	/**
	 * Invoke the specified method from the specified instance
	 * 
	 * @param instance to find and invoke method
	 * @param methodName method name
	 * @param argumentTypes argument types of this method
	 * @param arguments arguments to invoke
	 * @return the returned value of invoking method
	 * @throws NoSuchMethodException thrown if not found method
	 * @throws IllegalAccessException thrown if arguments is invalid
	 * @throws InvocationTargetException thrown if invoking method error
	 */
	public Object invokeInstanceMethod(
			Object instance, String methodName, Class<?>[] argumentTypes, Object[] arguments)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method method = null;
		Class<?> instanceClass = instance.getClass();
		while(Objects.isNull(method) && Objects.nonNull(instanceClass)) {
			List<Method> methodsList = new ArrayList<>();
			methodsList.addAll(Arrays.asList(instanceClass.getDeclaredMethods()));
			methodsList.addAll(Arrays.asList(instanceClass.getMethods()));

			List<Method> matchedMethods = methodsList
					.stream()
					.filter(med -> StringUtils.equalsAnyIgnoreCase(methodName, med.getName())
							&& ((med.getParameterCount() == 0 && ArrayUtils.isEmpty(argumentTypes))
									|| (ArrayUtils.isNotEmpty(argumentTypes) && med.getParameterCount() == argumentTypes.length)))
					.collect(Collectors.toList());
			if (CollectionUtils.isNotEmpty(matchedMethods) && ArrayUtils.isNotEmpty(argumentTypes)) {
				method = matchedMethods.stream()
                    .filter(med -> {
                        boolean matched = true;
                        Class<?>[] parameterTypes = med.getParameterTypes();
                        if (ArrayUtils.isNotEmpty(parameterTypes)) {
                            for(int i = 0; i < parameterTypes.length; i++) {
                                Class<?> parameterType = parameterTypes[ i ];
                                Class<?> argumentType = argumentTypes[ i ];
                                if (!parameterType.isAssignableFrom(argumentType)) {
                                    matched = false;
                                    break;
                                }
                            }
                        }
                        return matched;
                    })
					.findFirst().orElse(null);
			} else if (CollectionUtils.isNotEmpty(matchedMethods)) {
				method = matchedMethods.stream().findFirst().orElse(null);
			}
			instanceClass = instanceClass.getSuperclass();
		}
		boolean accessible = method.isAccessible();
		method.setAccessible(true);
		Object result = method.invoke(instance, arguments);
		method.setAccessible(accessible);
		return result;
	}
	private boolean matchMethodParameters(final Method method, Class<?>[] argumentTypes) {
		boolean matched = true;
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (ArrayUtils.isNotEmpty(parameterTypes)) {
			for(int i = 0; i < parameterTypes.length; i++) {
				Class<?> parameterType = parameterTypes[ i ];
				Class<?> argumentType = argumentTypes[ i ];
				if (!parameterType.isAssignableFrom(argumentType)) {
					matched = false;
					break;
				}
			}
		}
		return matched;
	}
	private Object invokeInstanceMethod(Object instance, Method method, Object[] arguments)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		boolean accessible = method.isAccessible();
		method.setAccessible(true);
		Object result = method.invoke(instance, arguments);
		method.setAccessible(accessible);
		return result;
	}

	public <T extends DocumentSectionHandler> Object invokeSectionHandlerMethod(
			T handlerInstance, String methodName, Class<?>[] argumentTypes, Object[] arguments)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Method method = null;
		Class<?> instanceClass = handlerInstance.getClass();
		while(Objects.isNull(method) && Objects.nonNull(instanceClass)) {
			List<Method> methodsList = new ArrayList<>();
			methodsList.addAll(Arrays.asList(instanceClass.getDeclaredMethods()));
			methodsList.addAll(Arrays.asList(instanceClass.getMethods()));

			List<Method> matchedMethods = methodsList
					.stream()
					.filter(med -> StringUtils.equalsAnyIgnoreCase(methodName, med.getName())
							&& ((med.getParameterCount() == 0 && ArrayUtils.isEmpty(argumentTypes))
									|| (ArrayUtils.isNotEmpty(argumentTypes) && med.getParameterCount() == argumentTypes.length)))
					.collect(Collectors.toList());
			if (CollectionUtils.isNotEmpty(matchedMethods) && ArrayUtils.isNotEmpty(argumentTypes)) {
				method = matchedMethods.stream()
                    .filter(med -> {
                        boolean matched = true;
                        Class<?>[] parameterTypes = med.getParameterTypes();
                        if (ArrayUtils.isNotEmpty(parameterTypes)) {
                            for(int i = 0; i < parameterTypes.length; i++) {
                                Class<?> parameterType = parameterTypes[ i ];
                                Class<?> argumentType = argumentTypes[ i ];
                                if (!parameterType.isAssignableFrom(argumentType)) {
                                    matched = false;
                                    break;
                                }
                            }
                        }
                        return matched;
                    })
					.findFirst().orElse(null);
			} else if (CollectionUtils.isNotEmpty(matchedMethods)) {
				method = matchedMethods.stream().findFirst().orElse(null);
			}
			instanceClass = instanceClass.getSuperclass();
		}
		boolean accessible = method.isAccessible();
		method.setAccessible(true);
		Object result = method.invoke(handlerInstance, arguments);
		method.setAccessible(accessible);
		return result;
	}

	public Document createDocumentWithContent(boolean initContent) {
		Document document = new Document();
		document.setId("DOCUMENT_ID");
		document.setTitle("DOCUMENT_TITLE");
		if (initContent) {
			document.setContent(new DocumentContent());
			document.getContent().setFileName("DOCUMENT_FILE_NAME");
			document.getContent().setPath("DOCUMENT_PATH_FILE");
		}
		return document;
	}

	public Document createDocumentWithContentAndSection(DocumentSection section) {
		Document document = new Document();
		document.setId("DOCUMENT_ID");
		document.setTitle("DOCUMENT_TITLE");
		if (true) {
			document.setContent(new DocumentContent());
			document.getContent().setFileName("DOCUMENT_FILE_NAME");
			document.getContent().setPath("DOCUMENT_PATH_FILE");
		}
		document.setDocumentSection(section);
		return document;
	}

	public void mockIgnoreLogger() {
		ch.ivyteam.log.Logger ivyLogger = Mockito.mock(ch.ivyteam.log.Logger.class, invocation -> null);
		PowerMockito.mockStatic(Ivy.class);
		PowerMockito.when(Ivy.log()).thenReturn(ivyLogger);

		ch.axonivy.fintech.standard.log.Logger stdLogger = Mockito.mock(ch.axonivy.fintech.standard.log.Logger.class, invocation -> null);
		PowerMockito.mockStatic(LoggerFactory.class);
		
		Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class))).thenReturn(stdLogger);
		Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class))).thenReturn(stdLogger);
		Mockito.when(LoggerFactory.getLoggerFor(Mockito.any())).thenReturn(stdLogger);
		
		PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class))).thenReturn(stdLogger);
		PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class))).thenReturn(stdLogger);
		PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any())).thenReturn(stdLogger);
		
		Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
		Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
		Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(), Mockito.anyString())).thenReturn(stdLogger);
		
		PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
		PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
		PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(), Mockito.anyString())).thenReturn(stdLogger);
	}

    public ch.ivyteam.log.Logger getMockIvyLogger() {
        ch.ivyteam.log.Logger ivyLogger = Mockito.mock(ch.ivyteam.log.Logger.class, invocation -> null);
        PowerMockito.mockStatic(Ivy.class);
        PowerMockito.when(Ivy.log()).thenReturn(ivyLogger);

        ch.axonivy.fintech.standard.log.Logger stdLogger = Mockito.mock(ch.axonivy.fintech.standard.log.Logger.class, invocation -> null);
        PowerMockito.mockStatic(LoggerFactory.class);

        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class))).thenReturn(stdLogger);
        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class))).thenReturn(stdLogger);
        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any())).thenReturn(stdLogger);

        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class))).thenReturn(stdLogger);
        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class))).thenReturn(stdLogger);
        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any())).thenReturn(stdLogger);

        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(), Mockito.anyString())).thenReturn(stdLogger);

        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(), Mockito.anyString())).thenReturn(stdLogger);
        return ivyLogger;
    }

    public ch.axonivy.fintech.standard.log.Logger getMockStandardLogger() {
        ch.ivyteam.log.Logger ivyLogger = Mockito.mock(ch.ivyteam.log.Logger.class, invocation -> null);
        PowerMockito.mockStatic(Ivy.class);
        PowerMockito.when(Ivy.log()).thenReturn(ivyLogger);

        ch.axonivy.fintech.standard.log.Logger stdLogger = Mockito.mock(ch.axonivy.fintech.standard.log.Logger.class, invocation -> null);
        PowerMockito.mockStatic(LoggerFactory.class);

        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class))).thenReturn(stdLogger);
        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class))).thenReturn(stdLogger);
        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any())).thenReturn(stdLogger);

        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class))).thenReturn(stdLogger);
        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class))).thenReturn(stdLogger);
        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any())).thenReturn(stdLogger);

        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
        Mockito.when(LoggerFactory.getLoggerFor(Mockito.any(), Mockito.anyString())).thenReturn(stdLogger);

        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(LogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(BaseLogOrigin.class), Mockito.anyString())).thenReturn(stdLogger);
        PowerMockito.when(LoggerFactory.getLoggerFor(Mockito.any(), Mockito.anyString())).thenReturn(stdLogger);
        return stdLogger;
    }

	public DocumentViewerPageService mockIgnoreViewerPage() throws IOException {
		PowerMockito.mockStatic(DocumentViewerPageService.class);
		DocumentViewerPageService viewerPageService = PowerMockito.mock(DocumentViewerPageService.class);
		PowerMockito.when(DocumentViewerPageService.createInstance(Mockito.any(File.class))).thenReturn(viewerPageService);
		PowerMockito.when(viewerPageService.withTitleForBrowserTab(Mockito.anyString())).thenReturn(viewerPageService);
		PowerMockito.when(viewerPageService.withDefaultDownloadFileName(Mockito.anyString())).thenReturn(viewerPageService);
		PowerMockito.doNothing().when(viewerPageService).execute();
		return viewerPageService;
	}

	public List<Document> createFullDocumentBySections() {
		List<Document> documents = new ArrayList<>();
        DefaultDocumentSection[] sections = new DefaultDocumentSection[]{
            DefaultDocumentSection.GENERAL_OBLIGATION_INFORMATION,
                DefaultDocumentSection.OPTIONAL_DOCUMENTS,
                DefaultDocumentSection.REQUIRED_VERIFICATIONS,
                DefaultDocumentSection.SIGNING_DOCUMENTS
        };
        for (DefaultDocumentSection s: sections) {
            Document document = new Document();
            document.setId("DOCUMENT_ID");
            document.setTitle("DOCUMENT_TITLE");
            if (true) {
                document.setContent(new DocumentContent());
                document.getContent().setFileName("DOCUMENT_FILE_NAME");
                document.getContent().setPath("DOCUMENT_PATH_FILE");
            }
            document.setDocumentSection(s);
            documents.add(document);
        }
		return documents;
	}

	public Provider<DocumentSectionConfig> createSectionConfigProvider(final DocumentSectionConfig sectionConfig) {
		return new Provider<DocumentSectionConfig>() {
			@Override
			public DocumentSectionConfig get() {
				return sectionConfig;
			}
		};
	}
	
	public Provider<DocumentSectionHandler> createSectionHandlerProvider(final DocumentSectionHandler sectionHandler) {
		return new Provider<DocumentSectionHandler>() {
			@Override
			public DocumentSectionHandler get() {
				return sectionHandler;
			}
		};
	}

	private Field findObjectFieldByName(Object instance, final String fieldName)
			throws IllegalArgumentException, IllegalAccessException {
		Field field = null;
		Class<?> instanceClass = instance.getClass();
		while(Objects.isNull(field) && Objects.nonNull(instanceClass)) {
			List<Field> fieldsList = new ArrayList<>();
			fieldsList.addAll(Arrays.asList(instanceClass.getDeclaredFields()));
			fieldsList.addAll(Arrays.asList(instanceClass.getFields()));

			field = fieldsList
					.stream()
					.filter(fld -> StringUtils.equalsIgnoreCase(fld.getName(), fieldName))
					.findFirst()
					.orElse(null);
			instanceClass = instanceClass.getSuperclass();
		}
		return field;
	}

	/**
	 * Get value of the specified field name from the specified instance
	 * 
	 * @param instance to find and invoke getting field value
	 * @param fieldName field name to filter
	 * 
	 * @throws IllegalAccessException thrown if could not access field
	 * @throws IllegalArgumentException  thrown if invalid arguments
	 */
	public Object invokeGetFieldValue(Object instance, final String fieldName)
			throws IllegalArgumentException, IllegalAccessException {
		Field field = null;
		Class<?> instanceClass = instance.getClass();
		while(Objects.isNull(field) && Objects.nonNull(instanceClass)) {
			List<Field> fieldsList = new ArrayList<>();
			fieldsList.addAll(Arrays.asList(instanceClass.getDeclaredFields()));
			fieldsList.addAll(Arrays.asList(instanceClass.getFields()));

			field = fieldsList
					.stream()
					.filter(fld -> StringUtils.equalsIgnoreCase(fld.getName(), fieldName))
					.findFirst()
					.orElse(null);
			instanceClass = instanceClass.getSuperclass();
		}

		boolean accessible = field.isAccessible();
		field.setAccessible(true);
		Object result = field.get(instance);
		field.setAccessible(accessible);
		return result;
	}
	private Object getFieldValue(Object instance, Field field)
			throws IllegalArgumentException, IllegalAccessException {
		boolean accessible = field.isAccessible();
		field.setAccessible(true);
		Object result = field.get(instance);
		field.setAccessible(accessible);
		return result;
	}

	/**
	 * Set value of the specified field name from the specified instance
	 * 
	 * @param instance to find and invoke setting field value
	 * @param fieldName field name to filter
	 * @param fieldValue value to set
	 * 
	 * @throws IllegalAccessException thrown if could not access field
	 * @throws IllegalArgumentException  thrown if invalid arguments
	 */
	public void invokeSetFieldValue(Object instance, final String fieldName, Object fieldValue)
			throws IllegalArgumentException, IllegalAccessException {
		Field field = null;
		Class<?> instanceClass = instance.getClass();
		while(Objects.isNull(field) && Objects.nonNull(instanceClass)) {
			List<Field> fieldsList = new ArrayList<>();
			fieldsList.addAll(Arrays.asList(instanceClass.getDeclaredFields()));
			fieldsList.addAll(Arrays.asList(instanceClass.getFields()));

			field = fieldsList
					.stream()
					.filter(fld -> StringUtils.equalsIgnoreCase(fld.getName(), fieldName))
					.findFirst()
					.orElse(null);
			instanceClass = instanceClass.getSuperclass();
		}

		boolean accessible = field.isAccessible();
		field.setAccessible(true);
		field.set(instance, fieldValue);
		field.setAccessible(accessible);
	}
	private void setFieldValue(Object instance, Field field, Object fieldValue)
			throws IllegalArgumentException, IllegalAccessException {
		boolean accessible = field.isAccessible();
		field.setAccessible(true);
		field.set(instance, fieldValue);
		field.setAccessible(accessible);
	}
}
