package kr.inuappcenterportal.inuportal.service;

import jakarta.annotation.PostConstruct;
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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CafeteriaService {
    private final RedisService redisService;
    private final String url = "https://www.inu.ac.kr/inu/643/subview.do";
    @Value("${installPath}")
    private String installPath;

    @PostConstruct
    public void initCafeteria() throws InterruptedException {
        crawlingCafeteria();
    }

    @Scheduled(cron = "0 5 0 * * ?")
    public void getCafeteria() throws InterruptedException {
        crawlingCafeteria();
    }


    public void crawlingCafeteria() throws InterruptedException{
        System.setProperty("webdriver.chrome.driver",installPath);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        WebDriver webDriver = new ChromeDriver(options);

        try{
            webDriver.get(url);
            Thread.sleep(1500);
            int index;
            String menu;

            List<WebElement> foods = webDriver.findElements(By.className("con"));
            if(!foods.isEmpty()) {
                redisService.storeMeal("학생식당", 1, foods.get(1).getText());
                redisService.storeMeal("학생식당", 2, foods.get(2).getText());
                menu = foods.get(3).getText();
                menu = menu.replace("\"","");
                redisService.storeMeal("학생식당", 3, menu);
            }
            else{
                weekend("학생식당");
            }

            WebElement linkElement = webDriver.findElement(By.xpath("//a[span[text()='제1기숙사식당']]"));
            linkElement.click();
            Thread.sleep(1500);

            foods = webDriver.findElements(By.className("con"));
            if(!foods.isEmpty()) {
                menu = foods.get(0).getText();
                index = menu.indexOf("*");
                if(index!=-1){
                    index++;
                    menu = menu.substring(index);
                    index = menu.indexOf("*");
                    index = index+2;
                    menu = menu.substring(index);
                }
                redisService.storeMeal("제1기숙사식당", 1, menu);
                menu = foods.get(1).getText();
                index = menu.indexOf("*");
                if(index!=-1){
                    index++;
                    menu = menu.substring(index);
                    index = menu.indexOf("*");
                    index = index+2;
                    menu = menu.substring(index);
                }
                index = menu.indexOf("=");
                if(index!=-1){
                    menu = menu.substring(0,index);
                }
                redisService.storeMeal("제1기숙사식당", 2, menu);
                menu = foods.get(2).getText();
                index = menu.indexOf("*");
                if(index!=-1){
                    index++;
                    menu = menu.substring(index);
                    index = menu.indexOf("*");
                    index = index+2;
                    menu = menu.substring(index);
                }
                index = menu.indexOf("=");
                if(index!=-1){
                    menu = menu.substring(0,index);
                }
                redisService.storeMeal("제1기숙사식당", 3, menu);
            }else{
                weekend("제1기숙사식당");
            }

            linkElement = webDriver.findElement(By.xpath("//a[span[text()='2호관(교직원)식당']]"));
            linkElement.click();
            Thread.sleep(1500);

            foods = webDriver.findElements(By.className("con"));
            if(!foods.isEmpty()) {
                redisService.storeMeal("2호관(교직원)식당", 1, "-");
                menu = foods.get(0).getText();
                index =  menu.indexOf('-');
                if(index!=-1){
                    menu = menu.substring(0,index);
                }
                redisService.storeMeal("2호관(교직원)식당", 2, menu);
                menu = foods.get(1).getText();
                index =  menu.indexOf('-');
                if(index!=-1){
                    menu = menu.substring(0,index);
                }
                redisService.storeMeal("2호관(교직원)식당", 3, menu);
            }else{
                weekend("2호관(교직원)식당");
            }

            linkElement = webDriver.findElement(By.xpath("//a[span[text()='27호관식당']]"));
            linkElement.click();
            Thread.sleep(1500);

            foods = webDriver.findElements(By.className("con"));
            if(!foods.isEmpty()) {
                redisService.storeMeal("27호관식당", 1, foods.get(0).getText());
                redisService.storeMeal("27호관식당", 2, foods.get(1).getText());
                redisService.storeMeal("27호관식당", 3, foods.get(2).getText());
            }else{
                weekend("27호관식당");
            }

            linkElement = webDriver.findElement(By.xpath("//a[span[text()='사범대식당']]"));
            linkElement.click();
            Thread.sleep(1500);

            foods = webDriver.findElements(By.className("con"));
            if(!foods.isEmpty()) {
                redisService.storeMeal("사범대식당", 1, "-");
                menu = foods.get(0).getText();
                index =  menu.indexOf('-');
                if(index!=-1){
                    menu = menu.substring(0,index);
                }
                redisService.storeMeal("사범대식당", 2, menu);

                menu = foods.get(1).getText();
                index =  menu.indexOf('-');
                if(index!=-1){
                    menu = menu.substring(0,index);
                }
                redisService.storeMeal("사범대식당", 3, menu);
            }else{
                weekend("사범대식당");
            }

        } finally {
            webDriver.quit();
        }
    }

    public List<String> getCafeteria(String cafeteria){
        List<String> menu = new ArrayList<>();
        for(int i = 1 ; i<4;i++){
            menu.add(redisService.getMeal(cafeteria,i));
        }
        return menu;
    }

    public void weekend(String cafeteria){
        redisService.storeMeal(cafeteria,1,"-");
        redisService.storeMeal(cafeteria,2,"-");
        redisService.storeMeal(cafeteria,3,"-");
    }
}
