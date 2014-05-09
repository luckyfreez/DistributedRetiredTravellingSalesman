/*
// How to run it
//javac -cp '.:htmlunit-2.11/lib/*' Jparse2.java && javac ParseXML.java && java -cp '.:htmlunit-2.11/lib/*' Jparse2 >> data.txt
//javac -cp '.:htmlunit-2.11/lib/*' Jparse2.java && java -cp '.:htmlunit-2.11/lib/*' Jparse2
javac -cp '.:/usr/local/bin/htmlunit-2.11/lib/*:/usr/local/bin/selenium-server-standalone-2.39.0.jar' ITACrawler.java && java -cp '.:/usr/local/bin/htmlunit-2.11/lib/*:/usr/local/bin/selenium-server-standalone-2.39.0.jar' ITACrawler
*/

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;

import com.google.common.base.Function;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.*;


import java.io.*;
import java.util.Iterator;
import java.util.Vector;
import java.util.ArrayList;
import java.util.AbstractList;
import java.util.regex.*;
import java.net.URL;
//import java.util.*;


class ITACrawler {

	private WebClient webClient;
	private HtmlPage mainPage;

	private WebDriver webDriver;

	private Function<WebDriver,WebElement> presenceOfElementLocated(final By locator) throws Exception {
		return new Function<WebDriver, WebElement>() {
			@Override
			public WebElement apply(WebDriver driver) {
				return driver.findElement(locator);
			}
		};
	}

    public ITACrawler() {
      System.out.println("crawling started");
      webDriver = new FirefoxDriver(); 
      System.out.println("firefox opened");
  }

  public Object[] checkPrice(String from, String to, String depDate) throws Exception {
        // Need syncronization


      webDriver.get("http://matrix.itasoftware.com");
      (new WebDriverWait(webDriver, 48)).until(presenceOfElementLocated(By.id("searchFormsContainer"))); 

      WebElement oneWayToggle = webDriver.findElement(By.id("ita_layout_TabContainer_0_tablist_ita_form_SliceForm_1"));
      oneWayToggle.click();
      (new WebDriverWait(webDriver, 48)).until(presenceOfElementLocated(By.id("advanced_from2"))); 
      Thread.sleep(1000);
        /* // for round-trip
        WebElement fromInput = webDriver.findElement(By.id("advancedfrom1"));
        WebElement toInput = webDriver.findElement(By.id("advancedto1"));
        WebElement depDateInput = webDriver.findElement(By.id("advanced_rtDeparture"));
        WebElement retDateInput = webDriver.findElement(By.id("advanced_rtReturn"));
        */
        WebElement fromInput = webDriver.findElement(By.id("advanced_from2"));
        WebElement toInput = webDriver.findElement(By.id("advanced_to2"));
        WebElement depDateInput = webDriver.findElement(By.id("ita_form_date_DateTextBox_1"));
        WebElement searchButton = webDriver.findElement(By.id("advanced_searchSubmitButton"));

        fromInput.clear();
        fromInput.sendKeys(from);
        toInput.clear();
        toInput.sendKeys(to);
        depDateInput.clear();
        depDateInput.sendKeys(depDate);
        //retDateInput.sendKeys("05/26/2014");
        searchButton.submit();

            //(new WebDriverWait(webDriver, 48)).until(!presenceOfElementLocated(By.id("itaLoadingIcon"))); 
        (new WebDriverWait(webDriver, 88)).until(presenceOfElementLocated(By.id("sites_matrix_panels_flights_Row_0")));
        //WebElement cheapestLabel = webDriver.findElement(By.id("ita_form_button_LinkButton_0_label"));
        WebElement row0 = webDriver.findElement(By.id("sites_matrix_panels_flights_Row_0"));
        WebElement priceSpan = row0.findElement(By.className("itaPrice"));
        WebElement carrierSpan = row0.findElement(By.className("itaSolutionCarriers"));
        WebElement depTimeSpan = row0.findElements(By.className("itaSliceTimes")).get(0);
        WebElement arrTimeSpan = row0.findElements(By.className("itaSliceTimes")).get(1);
        WebElement durationTimeSpan = row0.findElement(By.className("itaSliceDuration"));

        Object[] result = new Object[5];
        result[0] = priceSpan.getText();
        /*
        System.out.println("Cheapest price is " + priceSpan.getText());
        System.out.println("Carrier: " + carrierSpan.getText());
        System.out.println("Departing at " + depTimeSpan.getText());
        System.out.println("Arriving at" + arrTimeSpan.getText());
        System.out.println("Duration" + durationTimeSpan.getText());
        */
        return result;
    }

    /*
    public static void main(String[] args) throws Exception{
    }
    */
}
