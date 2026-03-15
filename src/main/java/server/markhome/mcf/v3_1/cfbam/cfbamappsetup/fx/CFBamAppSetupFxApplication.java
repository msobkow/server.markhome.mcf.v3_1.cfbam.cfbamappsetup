
// Description: JavaFX Application Setup entry point

/*
 *	server.markhome.mcf.CFBam
 *
 *	Copyright (c) 2016-2026 Mark Stephen Sobkow
 *	
 *	Mark's Code Fractal CFBam 3.1 Business Application Model
 *	
 *	Copyright 2016-2026 Mark Stephen Sobkow
 *	
 *	This file is part of Mark's Code Fractal CFBam.
 *	
 *	Mark's Code Fractal CFBam is available under dual commercial license from
 *	Mark Stephen Sobkow, or under the terms of the GNU General Public License,
 *	Version 3 or later with classpath and static linking exceptions.
 *	
 *	As a special exception, Mark Sobkow gives you permission to link this library
 *	with independent modules to produce an executable, provided that none of them
 *	conflict with the intent of the GPLv3; that is, you are not allowed to invoke
 *	the methods of this library from non-GPLv3-compatibly licensed code. You may not
 *	implement an LPGLv3 "wedge" to try to bypass this restriction. That said, code which
 *	does not rely on this library is free to specify whatever license its authors decide
 *	to use. Mark Sobkow specifically rejects the infectious nature of the GPLv3, and
 *	considers the mere act of including GPLv3 modules in an executable to be perfectly
 *	reasonable given tools like modern Java's single-jar deployment options.
 *	
 *	Mark's Code Fractal CFBam is free software: you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *	
 *	Mark's Code Fractal CFBam is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *	
 *	You should have received a copy of the GNU General Public License
 *	along with Mark's Code Fractal CFBam.  If not, see <https://www.gnu.org/licenses/>.
 *	
 *	If you wish to modify and use this code without publishing your changes,
 *	or integrate it with proprietary code, please contact Mark Stephen Sobkow
 *	for a commercial license at mark.sobkow@gmail.com
 */

package server.markhome.mcf.v3_1.cfbam.cfbamappsetup.fx;

import java.lang.reflect.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.rmi.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.text.StringEscapeUtils;

import javafx.application.Application;
import javafx.application.Application.Parameters;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import server.markhome.mcf.v3_1.cfsec.cfsecappsetup.CFSecAppSetup;

import server.markhome.mcf.v3_1.cflib.*;
import server.markhome.mcf.v3_1.cflib.inz.Inz;
import server.markhome.mcf.v3_1.cflib.inz.InzPathEntry;
import server.markhome.mcf.v3_1.cflib.dbutil.*;
import server.markhome.mcf.v3_1.cfsec.cfsec.*;
import server.markhome.mcf.v3_1.cfint.cfint.*;
import server.markhome.mcf.v3_1.cfbam.cfbam.*;
import server.markhome.mcf.v3_1.cfsec.cfsec.buff.*;
import server.markhome.mcf.v3_1.cfint.cfint.buff.*;
import server.markhome.mcf.v3_1.cfbam.cfbam.buff.*;
import server.markhome.mcf.v3_1.cfbam.cfbamappsetup.CFBamAppSetup;

@Component
public class CFBamAppSetupFxApplication extends Application {

	public static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(server.markhome.mcf.v3_1.cfbam.cfbamappsetup.CFBamAppSetup.class.getName());

	public static String INIT_LOG_MESSAGE1 = "The CFBamAppSetupFxApplication init method is called";

	private static CFBamAppSetupFxApplication instance = null;

	private Parent rootNode = null;

	private Scene scene;

	private Stage stage = null;

	@Override
	public void init() throws Exception {
		//Only initialize once
		if (getInstance() != null ) {
			return;

		}
		logMessage(INIT_LOG_MESSAGE1);

		ApplicationContextInitializer<GenericApplicationContext> initializer = ac -> {
			ac.registerBean(Application.class, () -> CFBamAppSetupFxApplication.this);
			ac.registerBean(Parameters.class, () -> getParameters());
			ac.registerBean(HostServices.class, () -> getHostServices());
		};

		this.context = new SpringApplicationBuilder().sources(CFBamAppSetup.class).initializers(initializer)
				.run(getParameters().getRaw().toArray(new String[0]));

		// provide a handle for the testing framework to inspect
		setInstance(this);
	}

	//This method exists so that the correct behavior and existence of the logger can be tested separately from within a junit test
	// because this class CFBamAppSetupFxApplication extends Application the failure of the logger may be more likely than POJOs
	public static void logMessage(String message) {
		log.info(message);
	}

	private void setUpParameterOverride() {
		Map<String, String> parameterMap;
		Parameters javaFxParameters = getParameters();
		if (javaFxParameters != null) {
			parameterMap = javaFxParameters.getNamed();
			for (String key : parameterMap.keySet()) {
				switch (key) {
				case "interpretation" -> javaFxResource
						.setInterpretation(StreamInterpretation.valueOf(parameterMap.get("interpretation")));
				case "defaultResource" -> javaFxResource.setDefaultResource(parameterMap.get("defaultResource"));
				default -> {}
				}
			}
		}
	}

	@Override
	public void start(Stage stage) {
		String logRootPropertyString = System.getProperty("LOG_ROOT");
		String logRootPropertyMessage ="The CFBamAppSetupFxApplication sees System Property LOG_ROOT: >>>"+logRootPropertyString+"<<<";
		logMessage(logRootPropertyMessage);
		// Save the stage
		setStage(stage);
		// Set the stage to not always on top
		stage.setAlwaysOnTop(false);
		// Set the title of the stage
		stage.setTitle("CFBam Application Setup");
		// Make the stage available through javaFxResource
		javaFxResource.setStage(getStage());
		double sceneMargin = 5; // some margin at the edge of the scene
		String message = "The CFBamAppSetupFxApplication start method is called";
		log.info( message);
//		getLoader().setControllerFactory(context::getBean);
		setUpParameterOverride();
		rootNode = getRoot();

		double defaultWidth = 100;
		double defaultHeight = 100;
		if (rootNode instanceof Region) {
			defaultWidth = ((Region) rootNode).getPrefWidth() + sceneMargin;
			defaultHeight = ((Region) rootNode).getPrefHeight() + sceneMargin;
		}
		// Override the preferences if there is a resource setting
		double fxResourceWidth = javaFxResource.getRootSceneWidth();
		double fxResourceHeight = javaFxResource.getRootSceneHeight();
		defaultWidth = (fxResourceWidth != 0.0) ? defaultWidth : fxResourceWidth;
		defaultHeight = (fxResourceHeight != 0.0) ? defaultHeight : fxResourceHeight;
		scene = new Scene(rootNode, defaultWidth, defaultHeight);
		stage.setScene(scene);
		stage.show();
	}

	@Override
	public void stop() throws Exception {
		stage.close();
		setInstance(null);
		context.close();
		System.gc();
		System.runFinalization();
		Platform.exit();
	}

	public Parent getRoot() {
		if (javaFxResource != null) {
		logMessage("getRoot javaFxResource is not null");
		} else {
			logMessage("getRoot javaFxResource is null");
		}
		return getRoot(javaFxResource.getDefaultResourceStream());
	}

	public static Parent getRoot(InputStream inputStream) {
		if (inputStream == null) {
			String message = "The input stream is null.";
			throw new MmsRuntimeException(message);
		}
		Parent result = null;
		try {
			FXMLLoader aLoader = getInstance().getLoader();
			result = aLoader.getRoot();
			if (JavaFxResource.locationUrl != null) {
				URL aLocation = JavaFxResource.locationUrl;
				aLoader.setLocation(aLocation);
			}
			if (result == null) {
				Parent layout = (Parent) aLoader.load(inputStream);
				result = layout;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new MmsRuntimeException(e.getMessage());
		}
		return result;
	}

	public static CFBamAppSetupFxApplication getInstance() {
		return instance;
	}

	public static void setInstance(CFBamAppSetupFxApplication instance) {
		CFBamAppSetupFxApplication.instance = instance;
	}

	public void main(String[] args) {
		SpringApplication.run(CFBamAppSetupFxApplication.class, args);
	}
}

