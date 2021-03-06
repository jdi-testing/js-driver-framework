package com.jdiai.tools;

import com.jdiai.JDI;
import org.openqa.selenium.Alert;

/**
 * Created by Roman Iovlev on 06.05.2021
 * Email: roman.iovlev.jdi@gmail.com; Skype: roman.iovlev
 */
public class Alerts {
    /**
     * Accept alert
     */
    public static void acceptAlert() {
        alert().accept();
    }

    /**
     * Dismiss alert
     */
    public static void dismissAlert() {
        alert().dismiss();
    }

    /**
     * Get alert text
     * @return String text
     */
    public static String getAlertText() {
        return alert().getText();
    }
    /**
     * Input the specified text in the alert and accept it
     * @param text to compare
     */
    public static void inputAndAcceptAlert(String text) {
        alert().sendKeys(text);
        alert().accept();
    }

    private static Alert alert() {
        return JDI.driver().switchTo().alert();
    }
}
