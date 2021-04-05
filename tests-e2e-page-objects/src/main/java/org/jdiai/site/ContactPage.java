package org.jdiai.site;

import org.jdiai.JS;
import org.jdiai.WebPage;
import org.jdiai.annotations.UI;

public class ContactPage extends WebPage {
    @UI(".lname-res") public JS lastNameInResult;
    @UI(".descr-res") public JS descriptionInResult;
    @UI(".sal-res") public JS weatherInResult;
    public JS contactForm;

    @UI("#contact-form") public ContactForm contacts;
}
