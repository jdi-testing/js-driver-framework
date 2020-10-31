package org.jdiai;

import org.jdiai.interfaces.IBuilderActions;
import org.jdiai.interfaces.IJSBuilder;
import org.openqa.selenium.By;

import java.text.MessageFormat;

import static java.lang.String.format;
import static org.jdiai.GetTypes.dataType;
import static org.jdiai.JSSmartTemplates.*;

public class SmartBuilderActions implements IBuilderActions {
    private final IJSBuilder builder;
    public SmartBuilderActions(IJSBuilder builder) {
        this.builder = builder;
    }
    public String oneToOne(String ctx, By locator) {
        return builder.registerVariable("element") + format(ONE_TO_ONE, MessageFormat.format(dataType(locator).get, ctx, builder.selector(locator)));
    }
    public String oneToList(String ctx, By locator) {
        builder.registerVariable("list");
        builder.registerFunction("filter", FILTER_FUNC);
        return builder.registerVariable("elements") + format(ONE_TO_LIST, MessageFormat.format(dataType(locator).getAll, ctx, builder.selectorAll(locator)));
    }
    public String listToOne(By locator) {
        builder.registerVariables("found", "i", "element", "first");
        builder.registerFunction("filter", FILTER_FUNC);
        return format(LIST_TO_ONE, MessageFormat.format(dataType(locator).get, "elements[i]", builder.selector(locator)));
    }
    public String listToList(By locator) {
        GetData data = dataType(locator);
        builder.registerVariables("list", "first");
        builder.registerFunction("filter", FILTER_FUNC);
        return format(LIST_TO_LIST, MessageFormat.format(data.getAll, "element", builder.selectorAll(locator)));
    }
    public String getResult(String collector) {
        return format(ONE_TO_RESULT, collector);
    }
    public String getResultList(String collector) {
        return format(LIST_TO_RESULT, collector);
    }
}