package com.jdiai;

import com.epam.jdi.tools.Safe;
import com.epam.jdi.tools.Timer;
import com.epam.jdi.tools.map.MapArray;
import com.epam.jdi.tools.pairs.Pair;
import com.google.gson.JsonObject;
import com.jdiai.annotations.UI;
import com.jdiai.asserts.DisplayedTypes;
import com.jdiai.interfaces.HasCore;
import com.jdiai.jsbuilder.IJSBuilder;
import com.jdiai.jsdriver.JDINovaException;
import com.jdiai.jsdriver.JSDriver;
import com.jdiai.jsdriver.JSDriverUtils;
import com.jdiai.jsproducer.Json;
import com.jdiai.jswraper.JSSmart;
import com.jdiai.scripts.Whammy;
import com.jdiai.tools.*;
import com.jdiai.visual.Direction;
import com.jdiai.visual.ImageTypes;
import com.jdiai.visual.OfElement;
import com.jdiai.visual.StreamToImageVideo;
import org.apache.commons.lang3.NotImplementedException;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;

import java.io.File;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.epam.jdi.tools.EnumUtils.getEnumValue;
import static com.epam.jdi.tools.LinqUtils.*;
import static com.epam.jdi.tools.PrintUtils.print;
import static com.epam.jdi.tools.ReflectionUtils.*;
import static com.jdiai.JDI.conditions;
import static com.jdiai.jsbuilder.GetTypes.dataType;
import static com.jdiai.jsdriver.JSDriverUtils.*;
import static com.jdiai.jswraper.JSWrappersUtils.*;
import static com.jdiai.page.objects.PageFactoryUtils.getLocatorFromField;
import static com.jdiai.tools.FilterConditions.textEquals;
import static com.jdiai.tools.GetTextTypes.INNER_TEXT;
import static com.jdiai.tools.Keyboard.pasteText;
import static com.jdiai.tools.VisualSettings.*;
import static com.jdiai.visual.Direction.VECTOR_SIMILARITY;
import static com.jdiai.visual.ImageTypes.VIDEO_WEBM;
import static com.jdiai.visual.RelationsManager.*;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.openqa.selenium.OutputType.*;

public class JSLight implements JS {
    public JSSmart js;
    protected Supplier<WebDriver> driver;
    protected Safe<Actions> actions;
    protected String name = "";
    protected Object parent = null;
    protected JSImages imagesData;
    public int renderTimeout = 5000;
    protected String objectMap;

    public JSLight() {
        this(JDI::driver, new ArrayList<>());
    }

    public JSLight(Supplier<WebDriver> driver, List<By> locators) {
        this.driver = driver;
        this.js = new JSSmart(driver, locators);
        // this.js.multiSearch();
        this.actions = new Safe<>(() -> new Actions(driver()));
    }

    public JSLight(WebDriver driver, List<By> locators) {
        this(() -> driver, locators);
    }

    public JSLight(Supplier<WebDriver> driver, By... locators) {
        this(driver, newList(locators));
    }

    public JSLight(WebDriver driver, By... locators) {
        this(() -> driver, locators);
    }

    public JSLight(JSLight parent, By locator) {
        this(parent::driver, locator, parent);
    }

    public JSLight(WebDriver driver, By locator, Object parent) {
        this(() -> driver, locator, parent);
    }

    public JSLight(Supplier<WebDriver> driver, By locator, Object parent) {
        this(driver, JSUtils.getLocators(locator, parent));
        this.parent = parent;
        if (parent != null && isInterface(parent.getClass(), HasCore.class)) {
            this.js.updateDriver(((HasCore) parent).core().jsDriver().jsDriver());
        }
    }

    public WebDriver driver() {
        return this.driver.get();
    }

    public JavascriptExecutor js() {
        return (JavascriptExecutor) driver();
    }

    public JS core() { return this; }

    public void setCore(JS core) {
        if (!isClass(core.getClass(), JSLight.class)) {
            return;
        }
        JSLight jsLight = (JSLight) core;
        this.js = jsLight.js;
        this.driver = jsLight.driver;
        this.actions = jsLight.actions;
        this.name = jsLight.name;
        this.parent = jsLight.parent;
        this.imagesData = jsLight.imagesData;
        this.renderTimeout = jsLight.renderTimeout;
        this.objectMap = jsLight.objectMap;
    }
    // public void setCore(JS core) {
    //     List<Field> coreFields = getFieldsDeep(core);
    //     for (Field field : coreFields) {
    //         try {
    //             Field thisField = getClass().getField(field.getName());
    //             thisField.set(this, getValueField(field, core));
    //         } catch (Exception ignore) { }
    //     }
    // }

    @Override
    public String getElement(String valueFunc) {
        return js.getValue(valueFunc);
    }

    @Override
    public List<String> getList(String valueFunc) {
        return js.getValues(valueFunc);
    }

    @Override
    public String filterElements(String valueFunc) {
        return js.firstValue(valueFunc);
    }

    @Override
    public String getJSResult(String action) {
        return js.getAttribute(action);
    }

    @Override
    public void set(String action) {
        doAction(action);
    }

    @Override
    public void setOption(String option) {
        if (option == null) {
            return;
        }
        doAction("option.value = " + option + ";\nelement.dispatchEvent(new Event('change'));");
    }

    @Override
    public void selectByName(String name) {
        if (name == null) {
            return;
        }
        doAction("dispatchEvent(new Event('change'));\n" +
            "element.selectedIndex = [...element.options]" +
            ".findIndex(option => option.text === '" + name + "');\n" +
            "element.dispatchEvent(new Event('change'));");
    }

    @Override
    public boolean selectedByValueOption(String value) {
        return core().getJSResult("selectedOptions[0].value").trim().equals(value);
    }

    @Override
    public boolean selectedOption(String value) {
        return core().getJSResult("selectedOptions[0].innerText").trim().equals(value);
    }

    @Override
    public void doAction(String action) {
        js.doAction(action);
    }

    public WebElement we() {
        if (isEmpty(locators())) {
            throw new JDINovaException("Failed to use we() because element has no locators");
        }
        SearchContext ctx = driver();
        for (By locator : locators()) {
            ctx = ctx.findElement(locator);
        }
        return (WebElement) ctx;
    }

    @Override
    public void actionsWithElement(BiFunction<Actions, WebElement, Actions> action) {
        action.apply(actions.get().moveToElement(this), this).build().perform();
    }

    @Override
    public void actions(BiFunction<Actions, WebElement, Actions> action) {
        action.apply(actions.get(), this).build().perform();
    }

    public String getName() {
        return isNotBlank(name)
            ? name
            : print(locators(), by -> JSDriverUtils.getByType(by) + ":" + JSDriverUtils.getByLocator(by), " > ");
    }

    @Override
    public JSLight setName(String name) {
        this.name = name;
        return this;
    }

    public Object parent() {
        return this.parent;
    }

    public JS setParent(Object parent) {
        this.parent = parent;
        return this;
    }

    public void click() {
        doAction("click();");
    }
    @Override
    public void clickCenter() {
        doAction("let rect = element.getBoundingClientRect();" +
            "let x = rect.x + rect.width / 2;" +
            "let y = rect.y + rect.height / 2;" +
            "document.elementFromPoint(x, y).click();");
    }

    @Override
    public void click(int x, int y) {
        js.jsExecute("document.elementFromPoint(" + x + ", " + y + ").click();");
    }

    @Override
    public void select() { click(); }

    @Override
    public void select(String value) {
        if (value == null || isEmpty(locators())) {
            return;
        }
        By lastLocator = last(locators());
        if (lastLocator.toString().contains("%s")) {
            List<By> locators = locators().size() == 1
                ? new ArrayList<>()
                : locators().subList(0, locators().size() - 2);
            locators.add(fillByTemplate(lastLocator, value));
            new JSLight(driver, locators).click();
        } else {
            findFirst(textEquals(value)).click();
        }
    }
    @Override
    public void selectSubList(String value) {
        if (value == null || isEmpty(locators())) {
            return;
        }
        find(format(SELECT_FIND_TEXT_LOCATOR, value)).click();
    }

    public static String SELECT_FIND_TEXT_LOCATOR = ".//*[text()='%s']";

    public String selectFindTextLocator = SELECT_FIND_TEXT_LOCATOR;

    protected String selectFindTextLocator() {
        return selectFindTextLocator;
    }

    @Override
    public JS setFindTextLocator(String locator) {
        selectFindTextLocator = locator;
        return this;
    }

    @Override
    public void select(String... values) {
        if (isEmpty(values) || isEmpty(locators())) {
            return;
        }
        By locator = last(locators());
        IJSBuilder builder = getByLocator(locator).contains("%s")
            ? getTemplateScriptForSelect(locator, values)
            : getScriptForSelect(values);
        builder.executeQuery();
    }

    private IJSBuilder getTemplateScriptForSelect(By locator, String... values) {
        IJSBuilder builder;
        String ctx;
        if (locators().size() == 1) {
            builder = js.jsDriver().builder();
            ctx = "document";
        } else {
            builder = new JSDriver(js.jsDriver().driver(), listCopyUntil(locators(), locators().size() - 1))
                .buildOne();
            ctx = "element";
        }
        builder.registerVariable("option");
        builder.setElementName("option");
        for (String value : values) {
            By by = fillByTemplate(locator, value);
            builder.oneToOne(ctx, by).doAction("option.click();\n");
        }
        return builder;
    }

    private IJSBuilder getScriptForSelect(String... values) {
        IJSBuilder builder = js.jsDriver().buildOne();
        builder.registerVariable("option");
        builder.setElementName("option");
        for (String value : values) {
            By by = defineLocator(format(selectFindTextLocator(), value));
            builder.oneToOne("element", by).doAction("option.click();\n");
        }
        return builder;
    }

    @Override
    public <TEnum extends Enum<?>> void select(TEnum name) {
        select(getEnumValue(name));
    }

    @Override
    public void check(boolean condition) {
        doAction("checked=" + condition + ";");
    }

    @Override
    public void check() {
        check(true);
    }

    @Override
    public void uncheck() {
        check(false);
    }

    @Override
    public void rightClick() {
        actionsWithElement(Actions::contextClick);
    }

    @Override
    public void doubleClick() {
        actionsWithElement(Actions::doubleClick);
    }

    @Override
    public void hover() {
        actions(Actions::moveToElement);
    }

    @Override
    public void dragAndDropTo(WebElement to) {
        dragAndDropTo(to.getLocation().x, to.getLocation().y);
    }

    @Override
    public void dragAndDropTo(int x, int y) {
        actions((a,e) -> a.dragAndDropBy(e, x, y));
    }

    public void submit() {
        doAction("submit()");
    }

    private String charToString(CharSequence... value) {
        return value.length == 1 ? value[0].toString() : "";
    }

    public void sendKeys(CharSequence... value) {
        if (value == null) {
            return;
        }
        set("value+='" + charToString(value) + "';\nelement.dispatchEvent(new Event('input'));");
    }

    @Override
    public void input(CharSequence... value) {
        if (value == null) {
            return;
        }
        set("value='" + charToString(value) + "';\nelement.dispatchEvent(new Event('input'));");
    }

    @Override
    public void slide(String value) {
        throw new NotImplementedException();
        // TODO
        //Actions a = new Actions(DRIVER.get());
        //a.dragAndDropBy(DRIVER.get().findElement(By.xpath("[aria-labelledby='range-slider'][data-index='0']")),20, 0)
        //  .build().perform();
        //js.jsDriver().builder().oneToOne("document", locators.get(0))
        //  .addJSCode("element.value='" + value + "';\n")
        //  .trigger("mousedown")
        //  .trigger("mousemove", "which: 1, pageX: 460");
        //.trigger("mousedown")
        //  .trigger("mousemove", { which: 1, pageX: 460 })
    }

    public void clear() {
        doAction("value = ''");
    }

    public String getTagName() {
        return getJSResult("tagName").toLowerCase();
    }

    @Override
    public String tag() {
        return getTagName();
    }

    public String getAttribute(String attrName) {
        return getJSResult("getAttribute('" + attrName + "')");
    }

    @Override
    public String getProperty(String property) {
        return getJSResult(property);
    }

    @Override
    public Json getJson(String valueFunc) {
        return js.getMap(valueFunc);
    }

    public String attr(String attrName) {
        return getAttribute(attrName);
    }

    @Override
    public List<String> getAttributesAsList(String attr) {
        return js.getAttributeList(attr);
    }
    public List<String> attrList(String attr) {
        return getAttributesAsList(attr);
    }
    public List<Json> getAttributesAsList(String... attr) {
        return js.getMultiAttributes(attr);
    }
    public List<Json> attrList(String... attr) {
        return getAttributesAsList(attr);
    }

    public List<String> allClasses() {
        String cl = attr("class");
        return cl.length() > 0
            ? newList(cl.split(" "))
            : new ArrayList<>();
    }

    public boolean hasClass(String className) {
        return allClasses().contains(className);
    }

    public boolean hasAttribute(String attrName) {
        return getJSResult("hasAttribute('" + attrName + "')").equals("true");
    }

    @Override
    public Json allAttributes() {
        return js.getMap("return '{'+[...element.attributes].map((attr)=> `'${attr.name}'='${attr.value}'`).join()+'}'");
    }

    public String printHtml() {
        return MessageFormat.format("<{0} {1}>{2}</{0}>", getTagName().toLowerCase(),
            print(allAttributes(), el -> format("%s='%s'", el.key, el.value), " "),
            getJSResult("innerHTML"));
    }

    public JS show() {
        if (isDisplayed() && !isInView()) {
            doAction("scrollIntoView({behavior:'auto',block:'center',inline:'center'})");
        }
        return this;
    }

    @Override
    public void highlight(String color) {
        show();
        set("styles.border='3px dashed "+color+"'");
    }

    public void highlight() {
        highlight("red");
    }

    @Override
    public String cssStyle(String style) {
        return js.getStyle(style);
    }
    @Override
    public Json cssStyles(String... style) {
        return js.getStyles(style);
    }
    @Override
    public Json allCssStyles() {
        return js.getAllStyles();
    }

    public boolean isSelected() {
        return getProperty("checked").equals("true");
    }

    @Override
    public boolean isDeselected() {
        return !isSelected();
    }

    public boolean isEnabled() {
        return hasAttribute("enabled");
    }
    @Override
    public JS setTextType(GetTextTypes textType) {
        this.textType = textType; return this;
    }

    public GetTextTypes textType = INNER_TEXT;
    public String getText() {
        return getText(textType);
    }
    @Override
    public String getText(GetTextTypes textType) {
        return getJSResult(textType.value);
    }

    public List<WebElement> findElements(By by) {
        return we().findElements(by);
    }

    public WebElement findElement(By by) {
        return we().findElement(by);
    }

    public boolean isDisplayed() {
        return getElement(conditions.isDisplayed).equalsIgnoreCase("true");
    }

    public boolean isVisible() {
        return getElement(DisplayedTypes.isVisible).equalsIgnoreCase("true");
    }

    public boolean isInView() {
        if (isHidden()) {
            return false;
        }
        Dimension visibleRect = getSize();
        return visibleRect.height > 0 && visibleRect.width > 0;
    }

    public boolean isExist() {
        return js.jsDriver().getSize() > 0;
    }

    public Point getLocation() {
        ClientRect rect = getClientRect();
        int x, y;
        if (inVision(rect))
            return new Point(-1, -1);
        int left = max(rect.left, 0);
        int top = max(rect.top, 0);
        x = left + getWidth(rect) / 2;
        y = top + getHeight(rect) / 2;
        return new Point(x, y);
    }

    protected boolean inVision(ClientRect rect) {
        return rect.x >= rect.windowWidth || rect.y >= rect.windowHeight || rect.bottom < 0 || rect.right < 0;
    }

    public Dimension getSize() {
        ClientRect rect = getClientRect();
        int width, height;
        if (inVision(rect))
            return new Dimension(0, 0);
        width = getWidth(rect);
        height = getHeight(rect);
        return new Dimension(width, height);
    }

    private int getWidth(ClientRect rect) {
        int left = max(rect.left, 0);
        int right = min(rect.right, rect.windowWidth);
        return right - left;
    }

    private int getHeight(ClientRect rect) {
        int top = max(rect.top, 0);
        int bottom = min(rect.bottom, rect.windowHeight);
        return bottom - top;
    }

    public Rectangle getRect() {
        ClientRect rect = getClientRect();
        return inVision(rect)
            ? new Rectangle(0, 0, 0, 0)
            : new Rectangle(rect.x, rect.y, getHeight(rect), getWidth(rect));
    }

    @Override
    public ClientRect getClientRect() {
        return new ClientRect(js.getJson("let rect = element.getBoundingClientRect();\n" +
            "return { x: rect.x, y: rect.y, top: rect.top, bottom: rect.bottom, left: rect.left, right: rect.right, " +
            "wWidth: window.innerWidth, wHeight: window.innerHeight };"));
    }

    public String getCssValue(String style) {
        return js.getStyle(style);
    }

    public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException {
        StreamToImageVideo screen = makeScreenshot(DEFAULT_IMAGE_TYPE);
        if (outputType == BASE64) {
            return (X) screen.asBase64();
        }
        if (outputType == BYTES) {
            return (X) screen.asByteStream();
        }
        if (outputType == FILE) {
            return (X) screen.asFile(IMAGE_TEMPLATE.apply("", this));
        }
        throw new JDINovaException("Failed to get screenshot - unknown type: " + outputType);
    }

    private String canvas2Image(ImageTypes imageType) {
        return "toDataURL('" + imageType.value + "')";
    }

    private String element2Image(ImageTypes imageType) {
        return "html2canvas(element).then((canvas) => canvas."+canvas2Image(imageType)+")";
    }

    public StreamToImageVideo makeScreenshot() {
        return makeScreenshot(DEFAULT_IMAGE_TYPE);
    }

    @Override
    public File makeScreenshot(String tag) {
        show();
        File imageFile = makeScreenshot().asFile(getScreenshotName(tag));
        imagesData().images.update(tag, imageFile.getPath());
        imagesData().imageFile = imageFile;
        return imageFile;
    }

    protected String getScreenshotName(String tag) {
        return IMAGE_TEMPLATE.apply(tag, this);
    }

    public StreamToImageVideo makeScreenshot(ImageTypes imageType) {
        String stream = getElement("if (element.toDataURL) { return element."+canvas2Image(imageType)+"; }\n"
            + "try { return " + element2Image(imageType) + "; } catch {\n"
            + "return await import(`https://html2canvas.hertzen.com/dist/html2canvas.min.js`).then("
            + "() => " + element2Image(imageType) + ") }"
        );
        return new StreamToImageVideo(stream, imageType);
    }

    @Override
    public void startRecording() {
        startRecording(VIDEO_WEBM);
    }

    @Override
    public void startRecording(ImageTypes imageType) {
        String value = getElement("let blobs = [];\n" +
            "const recorder = new MediaRecorder(element.captureStream(), { mimeType: '" + imageType.value + "' });\n" +
            "recorder.ondataavailable = (e) => {\n" +
            "  if (e.data && e.data.size > 0) { blobs.push(e.data); }\n}\n" +
            "recorder.onstop = () => {\n" +
            "  const blob = new Blob(blobs, { type: '" + imageType.value + "' });\n" +
            "  let reader = new FileReader();\n" +
            "  reader.readAsDataURL(blob);\n" +
            "  reader.onloadend = () => window.jdiVideoBase64 = reader.result;\n" +
            "}\n" +
            "recorder.start();\n" +
            "window.jdiRecorder = recorder;\n" +
            "return 'start recording'");
        if (!value.equals("start recording")) {
            throw new JDINovaException(value);
        }
    }

    @Override
    public StreamToImageVideo stopRecordingAndSave(ImageTypes imageType) {
        js.jsExecute("window.jdiRecorder.stop();");
        String stream = "";
        Timer timer = new Timer(renderTimeout);
        while (stream.length() < 10 && timer.isRunning()) {
            stream = js.jsExecute("return window.jdiVideoBase64;");
        }
        return new StreamToImageVideo(stream, imageType);
    }

    @Override
    public StreamToImageVideo stopRecordingAndSave() {
        return stopRecordingAndSave(VIDEO_WEBM);
    }

    @Override
    public StreamToImageVideo recordCanvasVideo(int sec) {
        return recordCanvasVideo(VIDEO_WEBM, sec);
    }

    @Override
    public StreamToImageVideo recordCanvasVideo(ImageTypes imageType, int sec) {
        startRecording(imageType);
        Timer.sleep((sec+1) * 1000L);
        return stopRecordingAndSave(imageType);
    }

    // Experimental record video for any element
    @Override
    public StreamToImageVideo recordVideo(int sec) {
        js.jsExecute("await import(`https://html2canvas.hertzen.com/dist/html2canvas.min.js`)");
        getElement(Whammy.script);
        Timer.sleep((sec+5) * 1000L);
        js.jsExecute("jdi.recording = false; jdi.compile();");
        String stream = "";
        Timer timer = new Timer(renderTimeout);
        while (stream.length() < 10 && timer.isRunning()) {
            stream = js.jsExecute("return jdi.videoBase64");
        }
        return new StreamToImageVideo(stream, VIDEO_WEBM);
    }

    @Override
    public JS setObjectMapping(String objectMap, Class<?> cl) {
        this.objectMap = objectMap;
        this.js.setupEntity(cl);
        return this;
    }

    @Override
    public JsonObject getJSObject(String json) {
        return js.getJson(json);
    }

    @Override
    public <T> T getEntity(Class<T> cl) {
        return getEntity(GET_OBJECT_MAP.apply(cl), cl);
    }

    @Override
    public <T> T getEntity() {
        return js.getEntity(objectMap);
    }

    @Override
    public void setEntity() {
        js.setEntity(objectMap);
    }
    @Override
    public <T> T getEntity(String objectMap, Class<?> cl) {
        js.setupEntity(cl);
        return js.getEntity(objectMap);
    }

    @Override
    public void setEntity(String objectMap) {
        js.setEntity(objectMap);
    }

    @Override
    public JS find(String by) {
        return find(NAME_TO_LOCATOR.apply(by));
    }

    @Override
    public JS find(By by) {
        return new JSLight(this, by);
    }
    @Override
    public JS children() {
        return find("*");
    }
    @Override
    public JS ancestor() {
        return find("/..");
    }

    @Override
    public List<String> values(GetTextTypes getTextType) {
        return js.getAttributeList(getTextType.value);
    }

    @Override
    public List<String> values() {
        return values(textType);
    }

    @Override
    public int size() {
        return js.getSize();
    }

    @Override
    public List<JsonObject> getObjectList(String json) {
        return js.getJsonList(json);
    }

    @Override
    public <T> List<T> getEntityList() {
        return js.getEntityList(objectMap);
    }

    @Override
    public void setEntityList() {
        js.setEntity(objectMap);
    }

    public static Function<Field, String> GET_COMPLEX_VALUE = field -> {
        if (!field.isAnnotationPresent(FindBy.class) && !field.isAnnotationPresent(UI.class)) {
            return null;
        }
        By locator = getLocatorFromField(field);
        if (locator != null) {
            String element = MessageFormat.format(dataType(locator).get, "element", getByLocator(locator));
            return format("'%s': %s", field.getName(), getValueType(field, element));
        }
        return null;
    };

    public static BiFunction<Field, Object, String> SET_COMPLEX_VALUE = (field, value)-> {
        if (!field.isAnnotationPresent(FindBy.class) && !field.isAnnotationPresent(UI.class))
            return null;
        By locator = getLocatorFromField(field);
        if (locator == null) {
            return null;
        }
        String element = MessageFormat.format(dataType(locator).get, "element", getByLocator(locator));
        return setValueType(field, element, value);
    };

    public static Function<Class<?>, String> GET_OBJECT_MAP = cl -> {
        Field[] allFields = cl.getDeclaredFields();
        List<String> mapList = new ArrayList<>();
        for (Field field : allFields) {
            String value = GET_COMPLEX_VALUE.apply(field);
            if (value != null) {
                mapList.add(value);
            }
        }
        return "{ " + print(mapList, ", ") + " }";
    };

    @Override
    public <T> List<T> getEntityList(Class<T> cl) {
        return getEntityList(GET_OBJECT_MAP.apply(cl), cl);
    }

    @Override
    public void fill(Object obj) {
        setEntity(obj);
    }

    @Override
    public void submit(Object obj, String locator) {
        setEntity(obj);
        find(locator).click();
    }

    @Override
    public void submit(Object obj) {
        submit(obj, SUBMIT_LOCATOR);
    }

    @Override
    public void loginAs(Object obj, String locator) {
        submit(obj, locator);
    }

    @Override
    public void loginAs(Object obj) {
        submit(obj);
    }

    public static String SUBMIT_LOCATOR = "[type=submit]";

    @Override
    public JS setEntity(Object obj) {
        Field[] allFields = obj.getClass().getDeclaredFields();
        List<String> mapList = new ArrayList<>();
        for (Field field : allFields) {
            Object fieldValue = getValueField(field, obj);
            if (fieldValue == null) {
                continue;
            }
            String value = SET_COMPLEX_VALUE.apply(field, fieldValue);
            if (value != null) {
                mapList.add(value);
            }
        }
        setEntity(print(mapList, ";\n") + ";\nreturn ''");
        return this;
    }

    @Override
    public <T> List<T> getEntityList(String objectMap, Class<?> cl) {
        js.setupEntity(cl);
        return js.getEntityList(objectMap);
    }

    @Override
    public void setEntityList(String objectMap) {
        js.setEntity(objectMap);
    }

    @Override
    public JS findFirst(String by, Function<JS, String> condition) {
        return findFirst(NAME_TO_LOCATOR.apply(by), condition.apply(this));
    }

    @Override
    public JS findFirst(By by, Function<JS, String> condition) {
        return findFirst(by, condition.apply(this));
    }

    @Override
    public JS findFirst(String by, String condition) {
        return findFirst(NAME_TO_LOCATOR.apply(by), condition);
    }

    @Override
    public JS get(int index) {
        return listToOne("element = elements[" + index + "];\n")
            .setName(format("%s[%s]",getName(), index));
    }

    @Override
    public JS get(String by, int index) {
        return get(NAME_TO_LOCATOR.apply(by), index);
    }

    @Override
    public JS get(By by, int index) {
        String script = "element = elements.filter(e => "+
                MessageFormat.format(dataType(by).get, "e", selector(by, js.jsDriver().builder()))+
                ")[" + index + "];\n";
        return listToOne(script)
            .setName(format("%s[%s]",getName(), index)).core();
    }

    @Override
    public JSLight get(Function<JS, String> filter) {
        return findFirst(filter);
    }

    @Override
    public JSLight get(String value) {
        return get(textEquals(value))
            .setName(format("%s[%s]",getName(), value));
    }

    @Override
    public JSLight findFirst(Function<JS, String> condition) {
        return findFirst(condition.apply(this));
    }

    @Override
    public JSLight findFirst(String condition) {
        return listToOne("element = elements.find(e => e && " + handleCondition(condition, "e") + ");\n");
    }

    private String handleCondition(String condition, String elementName) {
        return condition.contains("#element#")
            ? condition.replace("#element#", elementName)
            : elementName + "." + condition;
    }

    @Override
    public JS findFirst(By by, String condition) {
        String script = "element = elements.find(e => { const fel = " +
            MessageFormat.format(dataType(by).get, "e", selector(by, js.jsDriver().builder())) + "; " +
            "return fel && " + handleCondition(condition, "fel") + "; });\n";
        return listToOne(script);
    }

    @Override
    public long indexOf(Function<JS, String> condition) {
        return js.jsDriver().indexOf(condition.apply(this));
    }

    private JSLight listToOne(String script) {
        JSLight result = new JSLight(driver);
        result.js.jsDriver().setScriptInElementContext(js.jsDriver(), script);
        js.jsDriver().builder().cleanup();
        return result;
    }

    // TODO
    // public WebList finds(@MarkupLocator String by) {
    //     return $$(by, this);
    // }
    // public WebList finds(@MarkupLocator By by) {
    //     return $$(by, this);
    // }

    public boolean isClickable() {
        Dimension dimension = getSize();
        if (dimension.getWidth() == 0) return false;
        return isClickable(dimension.getWidth() / 2, dimension.getHeight() / 2 - 1);
    }
    @Override
    public void uploadFile(String filePath) {
        we().click();
        String pathToPaste = new File(filePath).getAbsolutePath();
        pasteText(pathToPaste);
    }
    @Override
    public void press(Keys key) {
        Keyboard.press(key);
    }
    @Override
    public void keyboardCommands(String... commands) {
        Keyboard.commands(commands);
    }

    @Override
    public boolean isClickable(int xOffset, int yOffset) {
        return getElement("rect = element.getBoundingClientRect();\n" +
            "cx = rect.left + " + xOffset + ";\n" +
            "cy = rect.top + " + yOffset + ";\n" +
            "e = document.elementFromPoint(cx, cy);\n" +
            "for (; e; e = e.parentElement) {\n" +
            "  if (e === element)\n" +
            "    return true;\n" +
            "}\n" +
            "return false;").equals("true");
    }

    @Override
    public String fontColor() {
        return js.color();
    }

    @Override
    public String bgColor() {
        return js.bgColor();
    }

    @Override
    public String pseudo(String name, String value) {
        return js.pseudo(name, value);
    }

    @Override
    public boolean focused() {
        return getElement("element === document.activeElement").equalsIgnoreCase("true");
    }

    @Override
    public List<By> locators() {
        return js.jsDriver().locators();
    }

    @Override
    public JSImages imagesData() {
        if (imagesData == null) {
            imagesData = new JSImages();
        }
        return imagesData;
    }

    @Override
    public File getImageFile() {
        return getImageFile("");
    }

    @Override
    public File getImageFile(String tag) {
        return imagesData().images.has(tag) ? new File(imagesData().images.get(tag)) : null;
    }

    @Override
    public void visualValidation() {
        visualValidation("");
    }

    @Override
    public void visualValidation(String tag) {
        VISUAL_VALIDATION.accept(tag, this);
    }

    @Override
    public void visualCompareWith(JS element) {
        COMPARE_IMAGES.apply(imagesData().imageFile, element.imagesData().imageFile);
    }

    public Direction getDirectionTo(WebElement element) {
        Rectangle elementCoordinates = getRect();
        Rectangle destinationCoordinates = element.getRect();
        Direction direction = new Direction(getCenter(elementCoordinates), getCenter(destinationCoordinates));
        if (isInterface(element.getClass(), HasCore.class)) {
            JS core = ((HasCore)element).core();
            if (relations == null) {
                relations = new MapArray<>(core.getFullName(), direction);
            } else {
                relations.update(core.getFullName(), direction);
            }
        }
        return direction;
    }

    @Override
    public boolean relativePosition(JS element, Direction expected) {
        return COMPARE_POSITIONS.apply(getDirectionTo(element), expected);
    }

    @Override
    public OfElement isOn(Function<Direction, Boolean> expected) {
        return new OfElement(expected, this);
    }

    @Override
    public boolean relativePosition(JS element, Function<Direction, Boolean> expected) {
        return expected.apply(getDirectionTo(element));
    }

    public MapArray<String, Direction> relations;

    @Override
    public void clearRelations() {
        relations = null;
    }

    @Override
    public MapArray<String, Direction> getRelativePositions(JS... elements) {
        relations = new MapArray<>();
        for (JS element : elements) {
            relations.update(element.getName(), getDirectionTo(element));
        }
        storeRelations(this, relations);
        return relations;
    }

    private boolean similar(Pair<String, Direction> relation, Direction expectedRelation) {
        return VECTOR_SIMILARITY.apply(relation.value, expectedRelation);
    }

    @Override
    public List<String> validateRelations() {
        MapArray<String, Direction> storedRelations = readRelations(this);
        if (isEmpty(storedRelations)) {
            return newList("No relations found in: " + RELATIONS_STORAGE);
        }
        List<String> failures = new ArrayList<>();
        if (isEmpty(relations)) {
            return newList("No element relations found: use getRelativePosition(...) first and save element relations");
        }
        MapArray<String, Direction> newRelations = getRelations(storedRelations, failures);
        if (isNotEmpty(newRelations)) {
            storeRelations(this, newRelations);
        }
        return failures;
    }

    private MapArray<String, Direction> getRelations(MapArray<String, Direction> storedRelations, List<String> failures) {
        MapArray<String, Direction> newRelations = new MapArray<>();
        for (Pair<String, Direction> relation : relations) {
            if (storedRelations.has(relation.key)) {
                checkRelations(storedRelations.get(relation.key), relation, failures);
            } else {
                newRelations.add(relation);
            }
        }
        return newRelations;
    }
    private void checkRelations(Direction expectedRelation,  Pair<String, Direction> relation, List<String> failures) {
        if (similar(relation, expectedRelation)) {
            return;
        }
        failures.add(format("Elements '%s' and '%s' are misplaced: angle: %s => %s; length: %s => %s",
            getFullName(), relation.key, relation.value.angle(), expectedRelation.angle(),
            relation.value.length(), expectedRelation.length()));
    }

    @Override
    public Point getCenter() {
        return getCenter(getRect());
    }

    protected Point getCenter(Rectangle rect) {
        int x = rect.x + rect.width / 2;
        int y = rect.y + rect.height / 2;
        return new Point(x, y);
    }

    @Override
    public JSSmart jsDriver() {
        return js;
    }

    @Override
    public String textType() {
        return textType.value;
    }
}
