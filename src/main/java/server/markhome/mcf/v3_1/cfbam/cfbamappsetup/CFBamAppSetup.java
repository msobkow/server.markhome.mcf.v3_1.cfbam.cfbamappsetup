
// Description: Java 25 Main for testing the CFBam schema database creation

/*
 *	server.markhome.mcf.CFBam
 *
 *	Copyright (c) 2016-2026 Mark Stephen Sobkow
 *	
 *	Mark's Code Fractal 3.1 CFBam - Business Application Model
 *	
 *	This file is part of Mark's Code Fractal CFBam.
 *	
 *	Mark's Code Fractal CFBam is available under dual commercial license from
 *	Mark Stephen Sobkow, or under the terms of the GNU General Public License,
 *	Version 3 or later.
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
 *	
 */

package server.markhome.mcf.v3_1.cfbam.cfbamappsetup;

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

import javafx.application.Application;

import ch.qos.logback.classic.LoggerContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.text.StringEscapeUtils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.ConfigurableEnvironment;

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
import server.markhome.mcf.v3_1.cfbam.cfbamappsetup.fx.CFBamAppSetupFxApplication;

@SpringBootApplication
@ComponentScan(basePackages = {
    "server.markhome.mcf.v3_1.cfbam.cfbamappsetup.fx",   // if you have service beans here
    "server.markhome.mcf.v3_1.cfbam.cfbamappsetup.spring"   // if you have service beans here
})
@EnableAutoConfiguration(exclude = {
})
public class CFBamAppSetup
{
    private static final AtomicReference<Properties> systemProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> applicationProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> userDefaultProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> userProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> mergedProperties = new AtomicReference<>(null);

	public static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(server.markhome.mcf.v3_1.cfbam.cfbamappsetup.CFBamAppSetup.class.getName());
	public static LoggerContext loggerContext = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
	public static String startingupMessage = CFBamAppSetup.class.getName() + " starting.";

    /**
     * Loads the application properties file from the application resources.
     */
    public static Properties getApplicationProperties() {
        if (applicationProperties.get() == null) {
            Properties props = new Properties();
            try (var in = CFBamAppSetup.class.getClassLoader().getResourceAsStream("application.properties")) {
                if (in != null) {
                    props.load(in);
                } else {
                    throw new RuntimeException(Inz.x("cfbamappsetup.ApplicationPropertiesNotFound"));
                }
            } catch (IOException e) {
                throw new RuntimeException(Inz.x("cfbamappsetup.CouldNotLoadApplicationProperties"), e);
            }
            applicationProperties.compareAndSet(null, props);
        }
        return applicationProperties.get();
    }

    /**
     * Loads the system properties, which hopefully haven't had the merge applied yet.
     */
    public static Properties getSystemProperties() {
        if (systemProperties.get() == null) {
            Properties props = new Properties();
            props.putAll(System.getProperties());
            systemProperties.compareAndSet(null, props);
        }
        return systemProperties.get();
    }
  
    /**
     * Loads the user default properties file from the application resources.
     */
    public static Properties getUserDefaultProperties() {
        if (userDefaultProperties.get() == null) {
            Properties props = new Properties();
            try (var in = CFBamAppSetup.class.getClassLoader().getResourceAsStream("user-default.properties")) {
                if (in != null) {
                    props.load(in);
                } else {
                    throw new RuntimeException(Inz.x("cfbamappsetup.UserDefaultPropertiesNotFound"));
                }
            } catch (IOException e) {
                throw new RuntimeException(Inz.x("cfbamappsetup.FailedToLoadUserDefaultProperties"), e);
            }
            userDefaultProperties.compareAndSet(null, props);
        }
        return userDefaultProperties.get();
    }

    /**
     * Make sure the user properties file doesn't exist yet
     */
    public static Properties getUserProperties() {
        if (userProperties.get() == null) {
            Properties props = new Properties();
            File userFile = new File(System.getProperty("user.home"), ".cfbamappsrv.properties");
            if (userFile.exists()) {
				throw new CFLibUsageException(CFBamAppSetup.class,
					"getUserProperties",
					String.format("%1$s already exists - application initialization previously completed", userFile.getPath ( ) ), String.format ( "%1$s already exists - application initialization previously completed", userFile.getPath()));
                
			}
			userProperties.compareAndSet(null, props);
        }
        return userProperties.get();
    }

    /**
     * Merges the System and User properties, giving preference to the User properties.
     */
    public static Properties getMergedProperties() {
        if (mergedProperties.get() == null) {
            Properties merged = new Properties();
            merged.putAll(getApplicationProperties());
            merged.putAll(getUserDefaultProperties());
            merged.putAll(getSystemProperties());
            merged.putAll(getUserProperties());
            mergedProperties.compareAndSet(null, merged);
        }
        return mergedProperties.get();
    }

    public static void main(String[] args) {
        Inz.addPathEntry(new InzPathEntry("/opt/mcf/v3_1/java" + "/server.markhome.mcf.v3_1.cfbam.cfbamappsetup/src/main/resources/" + "/server.markhome.mcf.v3_1.cfbam.cfbamappsetup".replace(".","/") + "/langs"));

        // This weird looking cadence ensures that all the sub-property lists are prepared before getMergedProperties() is invoked, ensuring that any errors and exceptions along the way are thrown first and in predictable order
        Properties mergedProperties = getApplicationProperties();
        mergedProperties = getUserDefaultProperties();
        mergedProperties = getSystemProperties();
        mergedProperties = getUserProperties();
        mergedProperties = getMergedProperties();
        System.getProperties().putAll(mergedProperties);

        SpringApplication app = new SpringApplication(CFBamAppSetup.class);
        app.addInitializers((applicationContext) -> {
            ConfigurableEnvironment env = applicationContext.getEnvironment();
            env.getPropertySources().addLast(new org.springframework.core.env.PropertiesPropertySource("userProperties", userProperties.get()));
        });
		int finalvalue = 0xf00000;
		CFSecBuffSchema buffCFSec = new CFSecBuffSchema();
		finalvalue = buffCFSec.initClassMapEntries(finalvalue);
		buffCFSec.setCFSecSchema(buffCFSec);
		CFIntBuffSchema buffCFInt = new CFIntBuffSchema();
		finalvalue = buffCFInt.initClassMapEntries(finalvalue);
		buffCFInt.setCFIntSchema(buffCFInt);
		CFBamBuffSchema buffCFBam = new CFBamBuffSchema();
		finalvalue = buffCFBam.initClassMapEntries(finalvalue);
		buffCFBam.setCFBamSchema(buffCFBam);
		logStartupInfo(args);
		logger.info("Runtime class codes are " + 0xf00000 + ".." + (finalvalue-1) + " (" + (finalvalue-0xf00000-1) + " tables in total)");
        Application.launch(CFBamAppSetupFxApplication.class, args);
    }

	public static void logStartupInfo(String[] args) {
		logger.info(startingupMessage);
		String argsString = "";
		if (args != null && args.length > 0) {
			StringBuilder sb = new StringBuilder(argsString);
			for (int i = 0; i < args.length; i++) {
				sb.append("arg ").append(i).append(" = >>>").append(args[i]).append("<<<");
			}
			argsString = sb.toString();
		}
		String message = "Spring boot Application args >>>" + argsString + "<<<";
		logger.info("The CFBam Application Setup is starting with args >>>{}<<<", message);
		String currentDirectory = new File(".").getAbsolutePath();
		message = "Application current Directory is: >>>" + currentDirectory + "<<<";
		logger.info(message);
	}

}


