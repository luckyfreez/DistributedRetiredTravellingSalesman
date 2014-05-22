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

    private boolean idle;

	private Function<WebDriver,WebElement> presenceOfElementLocated(final By locator) throws Exception {
		return new Function<WebDriver, WebElement>() {
			@Override
			public WebElement apply(WebDriver driver) {
				return driver.findElement(locator);
			}
		};
	}

    public ITACrawler() {
      idle = false;
      System.out.println("crawling started");
      webDriver = new FirefoxDriver(); 
      System.out.println("firefox opened");
      idle = true;
  }

  private synchronized Object[] checkPriceHelper(String from, String to, String depDate) throws Exception {
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
        System.out.println("getting depDateInputs");
        /*
        ArrayList<WebElement> depDateInputs = (ArrayList<WebElement>) webDriver.findElements(
            By.xpath("//*[starts-with(@id, \"ita_form_date_DateTextBox_\")]"));
            //By.xpath("//*[@id=\"ita_form_date_DateTextBox_1\"]"));
            //By.xpath("//*[@id=\"ita_form_date_DateTextBox_1\"]"));
            //By.xpath("//input[matches(@id,'ita_form_date_DateTextBox_\\d+')]"));
        //*[@id="ita_form_date_DateTextBox_6"]
        System.out.println(depDateInputs.size());
        */

        WebElement searchButton = webDriver.findElement(By.id("advanced_searchSubmitButton"));

        fromInput.click();
        fromInput.sendKeys(from);
        fromInput.sendKeys(Keys.RETURN);
        toInput.click();
        toInput.sendKeys(to);
        toInput.sendKeys(Keys.RETURN);
                        depDateInput.click();
                depDateInput.sendKeys(depDate);

                /*
        for (WebElement depDateInput : depDateInputs) {
            if (depDateInput.isDisplayed()) {
                System.out.println("id = " + depDateInput.getAttribute("id"));
                depDateInput.clear();
                depDateInput.sendKeys(depDate);
                depDateInput.sendKeys(Keys.RETURN);
            }
        }
        */
        //retDateInput.sendKeys("05/26/2014");
        searchButton.submit();

        System.out.println("submitted the search button");
            //(new WebDriverWait(webDriver, 48)).until(!presenceOfElementLocated(By.id("itaLoadingIcon"))); 
        (new WebDriverWait(webDriver, 88)).until(presenceOfElementLocated(By.id("sites_matrix_panels_flights_Row_0")));
        //WebElement cheapestLabel = webDriver.findElement(By.id("ita_form_button_LinkButton_0_label"));
        System.out.println("found sites_matrix_panels_flights_Row_0");

        WebElement row0 = webDriver.findElement(By.id("sites_matrix_panels_flights_Row_0"));
        WebElement priceSpan = row0.findElement(By.className("itaPrice"));
        WebElement carrierSpan = row0.findElement(By.className("itaSolutionCarriers"));
        WebElement depTimeSpan = row0.findElements(By.className("itaSliceTimes")).get(0);
        WebElement arrTimeSpan = row0.findElements(By.className("itaSliceTimes")).get(1);
        WebElement durationTimeSpan = row0.findElement(By.className("itaSliceDuration"));

        System.out.println("found all the elements");

        // This is the default result to return (when query fails).
        Object[] result = new Object[] { false, -1, "", "", "" };
        result[0] = true;   // Indicates that the search succeeded.

        // Get rid of the "$" at the beginning of the priceSpan
        result[1] = priceSpan.getText().substring(1).replaceAll(",", "");
        /*
        System.out.println("Cheapest price is " + priceSpan.getText());
        System.out.println("Carrier: " + carrierSpan.getText());
        System.out.println("Departing at " + depTimeSpan.getText());
        System.out.println("Arriving at" + arrTimeSpan.getText());
        System.out.println("Duration" + durationTimeSpan.getText());
        */
       System.out.println("about to finish"); 
      webDriver.manage().deleteAllCookies();
        return result;
    }

    private synchronized boolean reserveCrawler() {
        if (idle) {
            idle = false;
            return true;
        } else {
            return false;
        }
    }

    public Object[] checkPrice(String from, String to, String depDate) {
        // This is the default result to return (when query fails).
        Object[] result = new Object[] { false, -1, "", "", "" };

        if (reserveCrawler()) {
            try {
                result = checkPriceHelper(from, to, depDate);
            } catch (Exception e) {
                System.err.println("ERROR: Crawler cannot check flight " +
                   from + " -> " + to + " at " + depDate +
                   "! Exception is " + e);
            }
        }
        idle = true;
        return result;
    }

    public static void main(String[] args) throws Exception{
        ITACrawler crawler = new ITACrawler();
        //System.out.println("Price = " + crawler.checkPrice("BOS", "NYC", "05/29/2014")[0]);

    }
}
