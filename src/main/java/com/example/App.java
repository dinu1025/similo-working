package com.example;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * 
 *
 */
public class App 
{
    public static String DEFAULT_PROPERTIES_FILE_PATH = "metadata.json";
    public static void main( String[] args )
    {

        WidgetLocator wl = new WidgetLocator(args);
        ChromeOptions options = new ChromeOptions();
		options.addArguments("--disable-web-security");
		options.addArguments("--allow-running-insecure-content");

		WebDriverManager.chromedriver().setup();
        WebDriver webDriver = new ChromeDriver(options);
        webDriver.manage().window().maximize();
		webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10000));

        // // Old Website
        // webDriver.get("https://web.archive.org/web/20190801/https://www.youtube.com/");
        // wl.readMetadata_ofLocator("//h2[contains(text(),'Home')]", webDriver);
        // Locator l = wl.readMetadata_ofLocator("//li/a/span[contains(text(),'Trending')]", webDriver);
        // wl.readMetadata_ofLocator("//a[@title='History']", webDriver);

       
        

        // New Website
        webDriver.get("https://www.youtube.com/");
        String s[] = {"//h2[contains(text(),'Home')]"};
        Locator locc =  new Locator();
        // for (String string : s) {
            locc.loadPropertiesFromFile(DEFAULT_PROPERTIES_FILE_PATH, s[0]);
            
            List<Locator> locator = wl.getAllLocators(locc, webDriver);
            locator.get(0).getProperties().entrySet().stream().forEach(System.out::println);
            // Locator locator_adv = wl.getLocatorForElement(locator.get(0).getMetadata("xpath"), webDriver);
            Locator locatorAll = wl.getAllLocatorsForElement(locator.get(0).getMetadata("idxpath"), webDriver);
            // if (locatorAll != null) {
            System.out.println("ide :"+locatorAll.getMetadata("ide") + "  robula :"+locatorAll.getMetadata("robula")+ "  montoto :"+locatorAll.getMetadata("montoto"));
            // }
            // System.out.println("Robula locator for : "+locator_adv.getMetadata("robula"));
            // System.out.println("Montoto locator for : "+locator_adv.getMetadata("montoto"));
            // System.out.println("Xpath locator for : "+locator_adv.getMetadata("xpath"));
        // }
        webDriver.quit();

    }
}
