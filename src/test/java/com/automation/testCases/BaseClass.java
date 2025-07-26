package com.automation.testCases;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.Markup;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.json.JSONObject;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.v136.page.model.Screenshot;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class BaseClass {

    protected static Properties prop;
    private String environment;
    protected static WebDriver driver;
    public static String currentReportFolderName;
    public static ExtentReports reports;
    public static ExtentTest loggerTest;
    private String suiteName;

    private void init_properties(){

         prop = new Properties();

         try {

             BufferedReader reader = new BufferedReader(new FileReader(new File("./src/test/java/com/automation/utilities/config.json")));
             StringBuilder builder = new StringBuilder();
             String line = null;
             while ((line = reader.readLine()) != null){
                 builder.append(line);
             }
             JSONObject mainJson = new JSONObject(builder.toString());
             HashMap<String,String> propMap = new HashMap<>();

             List<String> otherKeys = Arrays.asList("browser","accessCode");
             for (String key : mainJson.keySet()){
                 if (key != null && !key.isEmpty()){

                     if (key.equalsIgnoreCase(environment)){
                         String backUrl = mainJson.getJSONObject(key).getJSONObject("backend").getString("url");
                         propMap.put("backendUrl",backUrl);

                         String userName = mainJson.getJSONObject(key).getJSONObject("backend").getString("username");
                         propMap.put("username",userName);

                         String password = mainJson.getJSONObject(key).getJSONObject("backend").getString("password");
                         propMap.put("password",password);

                         String envName = mainJson.getJSONObject(key).getString("environmentName");
                         propMap.put("getEnv",envName);

                     } else if (otherKeys.contains(key)) {
                         propMap.put(key, mainJson.getString(key));
                     }
                 }
             }
             prop.putAll(propMap);
             System.out.println("Prop = "+ prop);

         } catch (FileNotFoundException e) {
             System.out.println("Config file not found!");
             e.printStackTrace();
         }catch (IOException e) {
             System.out.println("Failed to load properties file!");
             e.printStackTrace();
         }
    }

    public WebDriver setUp(){
        String browser = prop.getProperty("browser");

        if (browser.equalsIgnoreCase("chrome")){

            driver = new ChromeDriver();

        } else if (browser.equalsIgnoreCase("firefox")) {

            driver = new FirefoxDriver();

        } else if (browser.equalsIgnoreCase("edge")) {

            driver = new EdgeDriver();

        } else if (browser.equalsIgnoreCase("safari")) {
            driver = new SafariDriver();

        }else {
            System.out.println("Please provide proper browser name...");
        }

        return driver;
    }

    public static String dateTimeFolder(Date date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH-mm-ss-SSS");

        return dateFormat.format(date) + "/" + timeFormat.format(date);

    }

    @BeforeSuite(alwaysRun = true )
    public void beforeSuiteMethod()  {

        init_properties();

        Date d = new Date();
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy'T'HH-mm-ss-SSS'Z'");

        String fileName = "Automation Report_" + format.format(d) + ".html";

        currentReportFolderName = dateTimeFolder(d);

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(new File(System.getProperty("user.dir") + "/Reports/" + currentReportFolderName +"/" + fileName));
        sparkReporter.config().enableOfflineMode(true);
        sparkReporter.config().setTheme(Theme.DARK);
        sparkReporter.config().setDocumentTitle(fileName);
        sparkReporter.config().setEncoding("utf-8");
        sparkReporter.config().setReportName(fileName);
        sparkReporter.config().setTimelineEnabled(false);

        reports = new ExtentReports();
        reports.attachReporter(sparkReporter);

        reports.setSystemInfo("Organization","Automation Test Solutions");
        reports.setSystemInfo("Project","Selenium With Java");
        reports.setSystemInfo("Environment", prop.getProperty("getEnv"));
        reports.setSystemInfo("Test-Suite",suiteName);
        reports.setSystemInfo("Url", prop.getProperty("backendUrl"));

    }

    public static String extentReportScreenshot(WebDriver driver, String screenshotName) throws IOException {
        String dateName = new SimpleDateFormat("yyyy-MM-dd_hh_mm_ss").format(new Date());
        TakesScreenshot ts = (TakesScreenshot) driver;

        File source = ts.getScreenshotAs(OutputType.FILE);
        File userDir = new File(System.getProperty("user.dir"));
        String destinationFolder = userDir.getAbsolutePath() + "/Reports/" + currentReportFolderName + "/Screenshots/";
        String destination = destinationFolder + screenshotName + dateName + ".png";
        File destinationFolderFile = new File(destinationFolder);

        if (!destinationFolderFile.exists()){
            destinationFolderFile.mkdir();
        }
        File finalDestinationFile = new File(destination);
        Files.copy(source.toPath(), finalDestinationFile.toPath());
        System.out.println("Fail Screenshot Captured.");
        return "./Screenshots" + screenshotName + dateName + ".png";
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown_AM(ITestResult result){

        try {
            if (result.getStatus() == ITestResult.FAILURE){

                loggerTest.log(Status.FAIL,"Test Case Failed Is " + result.getThrowable());
                try {
                    String screenshotPath = BaseClass.extentReportScreenshot(driver,result.getName());
                    loggerTest.log(Status.FAIL, ("<b>" + "<font colour=" + "red>" + "Screenshot of failure" + "</font" + "</b>"));
                    loggerTest.addScreenCaptureFromPath(screenshotPath);
                } catch (IOException e) {

                }
                String methodName = result.getMethod().getMethodName();
                String failedText = "Test Case Failed:- " + methodName;
                Markup F = MarkupHelper.createLabel(failedText, ExtentColor.RED);
                loggerTest.log(Status.FAIL,F);

            } else if (result.getStatus() == ITestResult.SUCCESS) {

                String methodName = result.getMethod().getMethodName();
                String failedText = "Test Case:- " + methodName + "Passed.";
                Markup F = MarkupHelper.createLabel(failedText, ExtentColor.GREEN);
                loggerTest.log(Status.PASS,F);

            } else if (result.getStatus() == ITestResult.SKIP) {

                String methodName = result.getMethod().getMethodName();
                String failedText = "Test Case:- " + methodName + "Skipped.";
                Markup F = MarkupHelper.createLabel(failedText, ExtentColor.GREY);
                loggerTest.log(Status.SKIP,F);
            }

            reports.flush();

        } catch (Throwable e) {
            if(loggerTest != null){
                throw new RuntimeException(e);
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown(){

        if(driver != null){
            driver.quit();
        }
    }

    public void printLogs(String log){
        System.out.println(log);
        loggerTest.pass(log);
    }
}
