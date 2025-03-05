package kr.inuappcenterportal.inuportal.domain.cafeteria.service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CafeteriaService {
    private final RedisService redisService;
    private final String url = "https://www.inu.ac.kr/inu/643/subview.do";
    @Value("${installPath}")
    private String installPath;

   /*@PostConstruct
    @Transactional
    public void initCafeteria() throws InterruptedException {
        crawlCafeteria();
    }*/

    @Scheduled(cron = "0 10 0 ? * MON-SAT")
    @Transactional
    public void jobCafeteria() throws InterruptedException {
        crawlCafeteria();
    }



    public List<String> getCafeteria(String cafeteria,int day){
        if(day==0){
            LocalDate today = LocalDate.now();
            DayOfWeek dayOfWeek = today.getDayOfWeek();
            day = dayOfWeek.getValue();
        }
        List<String> menu = new ArrayList<>();
        for(int i = 1 ; i<4;i++){
            menu.add(redisService.getMeal(cafeteria,day,i));
        }
        return menu;
    }


    public void crawlCafeteria() throws InterruptedException {
        System.setProperty("webdriver.chrome.driver", installPath);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        WebDriver webDriver = new ChromeDriver(options);

        try {
            webDriver.get(url);
            Thread.sleep(1500);

            WebElement linkElement = webDriver.findElement(By.xpath("//*[@id=\"menu643_obj4031\"]/div[2]/form/div[2]/div/a[2]"));
            linkElement.click();
            Thread.sleep(1500);
            storeStudentCafeteria(webDriver.findElements(By.className("wrap-week")));
            log.info("학생식당 저장 완료");

            linkElement = webDriver.findElement(By.xpath("//a[span[text()='제1기숙사식당']]"));
            linkElement.click();
            Thread.sleep(1500);
            storeDormitoryCafeteria(webDriver.findElements(By.className("wrap-week")));
            log.info("제1기숙사식당 저장 완료");

            linkElement = webDriver.findElement(By.xpath("//a[span[text()='2호관(교직원)식당']]"));
            linkElement.click();
            Thread.sleep(1500);
            storeEmployeeCafeteria(webDriver.findElements(By.className("wrap-week")));
            log.info("2호관(교직원)식당 저장 완료");

            linkElement = webDriver.findElement(By.xpath("//a[span[text()='27호관식당']]"));
            linkElement.click();
            Thread.sleep(1500);
            store27Cafeteria(webDriver.findElements(By.className("wrap-week")));
            log.info("27호관식당 저장 완료");

            linkElement = webDriver.findElement(By.xpath("//a[span[text()='사범대식당']]"));
            linkElement.click();
            Thread.sleep(1500);
            storeTeacherCafeteria(webDriver.findElements(By.className("wrap-week")));
            log.info("사범대식당 저장 완료");

        } finally {
            webDriver.quit();
        }
    }


    public void storeStudentCafeteria(List<WebElement> wrapWeekDivs){
        int day = 1 ;
        for (WebElement wrapWeekDiv : wrapWeekDivs) {
            WebElement tbody = wrapWeekDiv.findElement(By.tagName("tbody"));
            List<WebElement> rows = tbody.findElements(By.tagName("tr"));
            for (int i = 1; i < 4; i++) {
                List<WebElement> foods = rows.get(i).findElements(By.tagName("td"));
                if(i==2){
                    String menu = foods.get(0).getText().equals("")?"오늘은 쉽니다":foods.get(0).getText();
                    menu = menu.replace("\\", "");
                    menu = menu.replace("\"", "");
                    redisService.storeMeal("학생식당", day, i,menu);
                }
                else if(i==3){
                    String menu = foods.get(0).getText();
                    if(menu.equals("")){
                        menu = "오늘은 쉽니다";
                    }
                    menu = menu.replace("\"", "");
                    redisService.storeMeal("학생식당", day, i,menu);
                }
                else {
                    redisService.storeMeal("학생식당", day, i, foods.get(0).getText().equals("")?"오늘은 쉽니다":foods.get(0).getText());
                }
            }
            day++;
        }
    }

    public void storeDormitoryCafeteria(List<WebElement> wrapWeekDivs){
        int day = 1 ;
        for (WebElement wrapWeekDiv : wrapWeekDivs) {
            WebElement tbody = wrapWeekDiv.findElement(By.tagName("tbody"));
            List<WebElement> rows = tbody.findElements(By.tagName("tr"));

            for(int i = 0 ; i < 3 ; i++){
                List<WebElement> foods = rows.get(i).findElements(By.tagName("td"));
                String menu = foods.get(0).getText();
                int index = menu.indexOf("*");
                if(index!=-1){
                    index++;
                    menu = menu.substring(index);
                    index = menu.indexOf("*");
                    index = index+2;
                    menu = menu.substring(index);
                }
                redisService.storeMeal("제1기숙사식당", day,i+1,menu);
            }
            day++;
        }
    }

    public void storeEmployeeCafeteria(List<WebElement> wrapWeekDivs){
        int day = 1 ;
        for (WebElement wrapWeekDiv : wrapWeekDivs) {
            WebElement tbody = wrapWeekDiv.findElement(By.tagName("tbody"));
            List<WebElement> rows = tbody.findElements(By.tagName("tr"));
            redisService.storeMeal("2호관(교직원)식당", day, 1,"-");
            for(int i = 0 ; i < 2; i++){
                List<WebElement> foods = rows.get(i).findElements(By.tagName("td"));
                String menu = foods.get(0).getText();
                int index =  menu.indexOf('-');
                if(index!=-1){
                    menu = menu.substring(0,index);
                }
                redisService.storeMeal("2호관(교직원)식당", day,i+2, menu);
            }
            day++;
        }
    }


    public void store27Cafeteria(List<WebElement> wrapWeekDivs){
        int day = 1 ;
        for (WebElement wrapWeekDiv : wrapWeekDivs) {
            WebElement tbody = wrapWeekDiv.findElement(By.tagName("tbody"));
            List<WebElement> rows = tbody.findElements(By.tagName("tr"));

            for(int i = 0 ; i < 3 ; i++){
                List<WebElement> foods = rows.get(i).findElements(By.tagName("td"));
                redisService.storeMeal("27호관식당", day,i+1, foods.get(0).getText());
            }
            day++;
        }
    }

    public void storeTeacherCafeteria(List<WebElement> wrapWeekDivs){
        int day = 1 ;
        for (WebElement wrapWeekDiv : wrapWeekDivs) {
            WebElement tbody = wrapWeekDiv.findElement(By.tagName("tbody"));
            List<WebElement> rows = tbody.findElements(By.tagName("tr"));
            redisService.storeMeal("사범대식당", day, 1,"-");
            for(int i = 0 ; i < 2 ; i++){
                List<WebElement> foods = rows.get(i).findElements(By.tagName("td"));
                String menu = foods.get(0).getText();
                if(menu.equals("")){
                    menu="오늘은 쉽니다";
                }
                else {
                    int index = menu.indexOf('-');
                    if (index != -1) {
                        menu = menu.substring(0, index);
                    }
                }
                redisService.storeMeal("사범대식당", day,i+2, menu);
            }
            day++;
        }
    }



}
