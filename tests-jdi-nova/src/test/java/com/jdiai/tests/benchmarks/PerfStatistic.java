package com.jdiai.tests.benchmarks;

import com.epam.jdi.tools.Timer;
import com.epam.jdi.tools.func.JAction;
import com.epam.jdi.tools.func.JFunc;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.LongStream;

import static com.epam.jdi.tools.PropertyReader.getProperties;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;

public class PerfStatistic {
    public static <T> String testScenario(String testName, JFunc<T> seleniumAction, JFunc<T> jdiAction, int count) {
        return testScenario(testName, () -> {}, "Selenium", seleniumAction, "JDI Nova", jdiAction, count);
    }
    public static <T> String testScenario(String testName, JAction precondition, String framework1, JFunc<T> seleniumAction,
              String framework2, JFunc<T> jdiAction, int count) {
        List<Long> seleniumStats = new ArrayList<>();
        List<Long> jsStats = new ArrayList<>();
        int executionCount = parseBoolean(getProperties("/../../target/classes/test.properties")
            .getProperty("run.performance")) ? count : 1;
        for (int i = 0; i < executionCount; i++) {
            System.out.println("RUN#"+ i);
            precondition.execute();
            Timer t = new Timer();
            T seleniumResult = seleniumAction.execute();
            long seleniumTime = t.timePassedInMSec();
            seleniumStats.add(seleniumTime);

            precondition.execute();
            t = new Timer();
            T jdiResult = jdiAction.execute();
            long jsTime = t.timePassedInMSec();
            jsStats.add(jsTime);
            System.out.println(testName + " Ratio: " + df2((double)seleniumTime/jsTime));
            assertEquals(seleniumResult, jdiResult);
        }
        double avSelenium = getAverage(seleniumStats);
        double avJdi = getAverage(jsStats);
        System.out.println("Average " + framework1 + ": " + df2(avSelenium));
        System.out.println("Average " + framework2 + ": " + df2(avJdi));
        String avRatio = df2(avSelenium/avJdi);
        System.out.println("Average Ratio: " + avRatio);
        String min = df2(min(seleniumStats, jsStats));
        String max = df2(max(seleniumStats, jsStats));
        System.out.println("Min [" + min + "] and Max [" + max + "]");
        return format(framework1 + "[average:%s];" + framework2 + "[average:%s];Ratio[average:%s;min:%s;max:%s]",
            df2(avSelenium), df2(avJdi), avRatio, min, max);
    }
    private static String df2(double value) {
        return new DecimalFormat("#.##").format(value);
    }

    private static double getAverage(Collection<Long> times) {
        return toLong(times).average().orElse(-1);
    }
    private static double min(List<Long> selenium, List<Long> js) {
        if (selenium.size() == 1) {
            return (double) selenium.get(0) / js.get(0);
        }
        double min = Double.MAX_VALUE;
        double prev = Double.MAX_VALUE;
        for (int i = 0; i < js.size(); i++) {
            double ratio = (double) selenium.get(i)/js.get(i);
            if (min > ratio) {
                prev = min;
                min = ratio;
            }
        }
        return prev == Double.MAX_VALUE ? min : prev;
    }
    private static double max(List<Long> selenium, List<Long> js) {
        if (selenium.size() == 1) {
            return (double) selenium.get(0) / js.get(0);
        }
        double max = -1;
        double prev = -1;
        for (int i = 0; i < js.size(); i++) {
            double ratio = (double) selenium.get(i) / js.get(i);
            if (max < ratio) {
                prev = max;
                max = ratio;
            }
        }
        return prev == -1 ? max : prev;
    }
    private static LongStream toLong(Collection<Long> collection) {
        return collection.stream().mapToLong(a -> a);
    }
}
