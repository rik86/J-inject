package com.riccardonoviello.commons.jinject.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.riccardonoviello.commons.jinject.annotations.Inject;

public class ApplicationContext {

	private String[] scanPackages;
	private String[] propertiesFiles;

	private Map<Class<?>, Object> components = new HashMap<Class<?>, Object>();

	private Properties props = new Properties();
	
	private static ApplicationContext instance = null; // we only allow one instance at the time
	
	private static final Logger logger = Logger.getLogger(ApplicationContext.class.getName());
	
	
	/**
	 * 
	 * @param _scanPackage
	 * @param _propertiesFiles
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static ApplicationContext getInstance(String[] _scanPackages, String[] _propertiesFiles) throws IllegalArgumentException, IllegalAccessException, InstantiationException{
		if (instance == null) {
			instance = new ApplicationContext(_scanPackages, _propertiesFiles);		
		}		
		return instance;
	}
	
	
	/**
	 * Application Context constructor
	 * 
	 * @param _scanPackage
	 *            is the parent package that contains all components to be
	 *            scanned for loading dependencies
	 * @param _propertiesFiles
	 *            is the array of property files to be loaded in the current
	 *            context
	 * 
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private ApplicationContext(String[] _scanPackages, String[] _propertiesFiles) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		logger.info("*** Starting Application Context *** ");
		this.scanPackages = _scanPackages;
		this.propertiesFiles = _propertiesFiles;

		loadProperties();

		List<Class<?>> classes = new ArrayList<Class<?>>();
		for (String pkg : scanPackages){
			classes.addAll(ClassFinder.find(pkg));
		}
		logger.info("Loading "+classes.size()+" classes...");

		for (Class<?> c : classes) {
			
			logger.info("Examining class "+c.getName());

			// find all fields to inject
			for (Field field : c.getDeclaredFields()) {
				if (field.isAnnotationPresent(Inject.class)) {
					
					// make the field accessible
					field.setAccessible(true);

					// Inject properties if exist
					String prop = field.getAnnotation(Inject.class).property();

					Object valueToInject = null;

					if (prop.length() > 0) {
						// Set the property, as value to inject
						valueToInject = props.get(prop);
					} else {
						// Set a new instance of the Class, as value to inject
						valueToInject = getComponentByClassName(field.getType());
					}

					// need to inject the new instance of a field into the
					// component
					Object object = getComponentByClassName(c);
					field.set(object, valueToInject);
					
					logger.info("Injected field "+field);
				}
			}
		}
	}

	/**
	 * Load Property files
	 */
	private void loadProperties() {
		if (propertiesFiles != null) {
			for (String file : propertiesFiles) {
				InputStream input = null;

				try {
					// open the file
					input = new FileInputStream(file);
					// load a properties from the file
					props.load(input);

				} catch (IOException ex) {
					logger.severe("Error Loading property file "+file);
				} finally {
					if (input != null) {
						try {
							input.close();
						} catch (IOException e) {
							logger.severe("Error Closing property file "+file);
						}
					}
				}

			}
		}
	}

	/**
	 * Return the component or Builds a new instance of the component
	 * 
	 * @param type
	 * @return
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public <T>T getComponentByClassName(Class<T> type) throws IllegalAccessException, InstantiationException {
		
		return (T) ((components.get(type) != null) ? components.get(type) : addInstanceToMap(type));
	}

	/**
	 * 
	 * @param type
	 * @return
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public <T>T addInstanceToMap(Class<?> clazz) throws IllegalAccessException, InstantiationException {
		if (clazz != null){
			components.put(clazz, clazz.newInstance());
		}
		return (T) components.get(clazz);
	}

	/**
	 * 
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	public <T> T getComponent(Class<T> clazz) {
		return (T) this.components.get(clazz);
	}
	
	/**
	 * 
	 * @return
	 */
	public int getComponentsSize(){
		return this.components.size();
	}

}
