package com.jdiai.tests.benchmarks;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.google.gson.JsonObject;
import com.jdiai.TestInit;
import com.jdiai.jsproducer.Json;
import com.jdiai.testng.TestNGListener;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.jdiai.JDI.$;
import static com.jdiai.JDI.driver;
import static com.jdiai.Pages.PERFORMANCE_PAGE;
import static com.jdiai.states.States.loggedInAt;
import static com.jdiai.states.States.logout;
import static com.jdiai.tests.benchmarks.PerfStatistic.testScenario;
import static com.jdiai.tools.FilterConditions.textEquals;
import static java.util.Arrays.asList;

@Listeners(TestNGListener.class)
public class BenchmarkTests implements TestInit {
    final String atTop = "Zachary Hendrix";
    final String atBottom = "Yen Stevenson";
    final int index = 385;
    final int repeat = 1;
    String totalResult = "";
    final int lightTestsCount = 500;
    final int heavyTestsCount = 30;

    @BeforeMethod
    public void before() {
        loggedInAt(PERFORMANCE_PAGE);;
    }

    @Test(invocationCount = repeat)
    public void getText() {
        totalResult += testScenario("getText: ",
            () -> driver().findElement(By.cssSelector("#users-table tr>th")).getText(),
            () -> $("#users-table tr>th").getText(),
        lightTestsCount) + "\n";
    }

    @Test(invocationCount = repeat)
    public void multiLocator() {
        totalResult += testScenario("multiLocator: ",
            () -> driver().findElement(By.cssSelector("#users-table"))
                .findElement(By.xpath(".//tr[2]"))
                .findElement(By.tagName("td")).getText(),
            () -> $("#users-table", ".//tr[2]", "td").getText(),
        lightTestsCount) + "\n";
    }
    @Test(invocationCount = repeat)
    public void getAllAttributes() {
        totalResult += testScenario("getAllAttributes: ", () -> {
            WebElement element = driver().findElement(By.cssSelector(".sidebar-tooltip"));
            String dt = element.getAttribute("data-toggle");
            String dp = element.getAttribute("data-placement");
            String title = element.getAttribute("title");
            String cl = element.getAttribute("class");
            return "data-toggle:"+dt+",data-placement:"+dp+",title:"+title+",class:"+cl;
        },
        () -> $(".sidebar-tooltip").allAttributes().toString(),
        lightTestsCount) + "\n";
    }
    @Test(invocationCount = repeat)
    public void getMultiData() {
        totalResult += testScenario("getMultiData: ", () -> {
            WebElement element = driver().findElement(By.cssSelector("#users-table"));
            String id = element.getAttribute("id");
            String ui = element.getAttribute("ui");
            String tag = element.getTagName().toUpperCase();
            String font = element.getCssValue("fontSize");
            String bgColor = element.getCssValue("backgroundColor");
            return "id:"+id+",ui:"+ui+",tag:"+tag+",font:"+font+",bg-color:"+bgColor;
        }, () -> {
            Json data = $("#users-table").getJson(
                "{ 'id': element.id, 'ui': element.getAttribute('ui'), 'tag': element.tagName, " +
                "'font': styles.fontSize, 'bg-color': styles.backgroundColor }");
            return data.toString();
        },
        lightTestsCount) + "\n";
    }
    @Test(invocationCount = repeat)
    public void multiElements() {
        totalResult += testScenario("multiElements: ", () -> {
            WebElement element = driver().findElement(By.xpath("//*[@id='users-table']//tr[2]"));
            List<String> result = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                result.add(element.findElement(By.xpath(".//td[" + i + "]")).getText());
            }
            return result;
        }, () -> {
            JsonObject jo = $("//*[@id='users-table']//tr[2]").getJSObject(
            "{ '1': xpath(element, 'td[1]').innerText, '2': xpath(element, 'td[2]').innerText, " +
                "'3': xpath(element, 'td[3]').innerText, '4': xpath(element, 'td[4]').innerText }");
                return asList(jo.get("1").getAsString(), jo.get("2").getAsString(), jo.get("3").getAsString(), jo.get("4").getAsString());
        },
        lightTestsCount) + "\n";
    }
    @Test(invocationCount = repeat)
    public void listGetValuesTest() {
        totalResult += testScenario("listGetValuesTest: ", () -> {
            List<WebElement> elements = driver().findElements(By.cssSelector("#users-table tr>td:first-child"));
            return elements.stream().map(WebElement::getText).collect(Collectors.toList());
        }, () -> $("#users-table tr>td:first-child").values(),
        heavyTestsCount) + "\n";
    }
    @Test(invocationCount = repeat)
    public void getValueByIndexTest() {
        totalResult += testScenario("getValueByIndexTest: ",
            () -> driver().findElements(By.cssSelector("#users-table tr"))
                .get(index).findElement(By.xpath(".//td[3]")).getText(),
            () -> $("#users-table tr").get(index).find("td").get(2).getText(),
        lightTestsCount) + "\n";
    }
    @Test(invocationCount = repeat)
    public void getValueByNameTopTest() {
        totalResult += testScenario("getValueByNameTopTest: ", () -> {
            List<WebElement> elements = driver().findElements(By.cssSelector("#users-table tr"));
            WebElement row = elements.stream().filter(el -> {
                WebElement td = null;
                boolean isExist = true;
                try {
                    td = el.findElement(By.cssSelector("td:first-child"));
                } catch (Exception ex) { isExist = false; }
                return isExist && td.getText().equals(atTop);
            }).findFirst().get();
            return row.findElement(By.xpath(".//td[3]")).getText();
        }, () -> $("#users-table tr").findFirst("td", textEquals(atTop)).find(".//td[3]").getText(),
        heavyTestsCount) + "\n";
    }


    @Test(invocationCount = repeat)
    public void getValueByNameBottomTest() {
        totalResult += testScenario("getValueByNameBottomTest: ", () -> {
            List<WebElement> elements = driver().findElements(By.cssSelector("#users-table tr"));
            WebElement row = elements.stream().filter(el -> {
                WebElement td = null;
                boolean isExist = true;
                try {
                    td = el.findElement(By.cssSelector("td:first-child"));
                } catch (Exception ex) { isExist = false; }
                return isExist && td.getText().equals(atBottom);
            }).findFirst().get();
            return row.findElement(By.xpath(".//td[3]")).getText();
        }, () -> $("#users-table tr").findFirst("td", textEquals(atBottom)).find(".//td[3]").getText(),
        heavyTestsCount) + "\n";
    }

    @Test(invocationCount = repeat)
    public void scenarioJdiSeleniumTest() {
        totalResult += testScenario("scenarioJdiSeleniumTest: ",
            () -> logout(),
            "Selenium",
            () -> { try { new SeleniumTests().simpleSearchTest(); return true; } catch (Exception ex) { return false; } },
            "JDI Nova",
            () -> { try { new JDINovaTests().simpleSearchTest(); return true; } catch (Exception ex) { return false; } },
            heavyTestsCount*2) + "\n";
    }

    @Test(invocationCount = repeat)
    public void scenarioJdiSelenideTest() {
        Configuration.headless = true;
        totalResult += testScenario("scenarioJdiSelenideTest: ",
            () -> { logout(); Selenide.clearBrowserCookies(); },
            "Selenide",
            () -> { try { new SelenideTests().simpleSearchTest(); return true; } catch (Exception ex) { return false; } },
            "JDI Nova",
            () -> { try { new JDINovaTests().simpleSearchTest(); return true; } catch (Exception ex) { return false; } },
            heavyTestsCount) + "\n";
    }
    @Test(invocationCount = repeat)
    public void scenarioAllJdiSeleniumTest() {
        Configuration.headless = true;
        totalResult += testScenario("scenarioAllJdiSeleniumTest: ",
            () -> logout(),
            "Selenium",
            () -> { try { new SeleniumTests().simpleAllSearchTest(); return true; } catch (Exception ex) { return false; } },
            "JDI Nova",
            () -> { try { new JDINovaTests().simpleAllOrderedSearchTest(); return true; } catch (Exception ex) { return false; } },
            heavyTestsCount) + "\n";
    }
    @Test(invocationCount = repeat)
    public void scenarioAllJdiSelenideTest() {
        Configuration.headless = true;
        totalResult += testScenario("scenarioAllJdiSelenideTest: ",
            () -> { logout(); Selenide.clearBrowserCookies(); },
            "Selenide",
            () -> { try { new SelenideTests().simpleAllSearchTest(); return true; } catch (Exception ex) { return false; } },
            "JDI Nova",
            () -> { try { new JDINovaTests().simpleAllOrderedSearchTest(); return true; } catch (Exception ex) { return false; } },
            heavyTestsCount) + "\n";
    }

    @AfterClass
    public void afterClass() {
        System.out.println(totalResult);
    }
}
