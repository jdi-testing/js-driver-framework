package com.jdiai;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import static com.jdiai.JDI.domain;
import static com.jdiai.jsbuilder.QueryLogger.ALL;
import static com.jdiai.jsbuilder.QueryLogger.LOG_QUERY;
import static com.jdiai.jswraper.driver.DriverManager.killDrivers;
import static com.jdiai.jswraper.driver.JDIDriver.DRIVER_OPTIONS;

public interface TestInit {

    @BeforeSuite(alwaysRun = true)
    default void setUp() {
        killDrivers();
        DRIVER_OPTIONS.chrome = cap -> cap.addArguments("--headless");
        LOG_QUERY = ALL;
        domain = "https://jdi-testing.github.io/jdi-light";
    }

    @AfterSuite(alwaysRun = true)
    default void tearDown() {
        killDrivers();
    }

}
