package kr.inuappcenterportal.inuportal.domain.schedule.service;

import jakarta.annotation.PostConstruct;
import kr.inuappcenterportal.inuportal.domain.schedule.dto.ScheduleResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final String url = "https://www.inu.ac.kr/inu/651/subview.do";
    @Value("${installPath}")
    private String installPath;
    private static long id = 0L;


    @PostConstruct
    @Transactional
    public void initSchedule() throws InterruptedException {
        crawlingSchedule();
    }
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void renewalSchedule() throws InterruptedException {
        crawlingSchedule();
    }

    @Transactional
    public void crawlingSchedule() throws InterruptedException {
        id = 0;
        System.setProperty("webdriver.chrome.driver",installPath);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        WebDriver webDriver = new ChromeDriver(options);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        try {
            webDriver.get(url);
            Thread.sleep(1500);

            boolean isNoData = false;
            while(!isNoData){
                int month = 1;
                while(!isNoData&&month<13){
                    String xPath = "//*[@id=\"schdulWrap\"]/div[1]/div[2]/div/ul/li["+month+"]";
                    WebElement link = webDriver.findElement(By.xpath(xPath));
                    link.click();
                    Thread.sleep(1500);

                    List<WebElement> rows = webDriver.findElements(By.cssSelector(".applyList.calList tbody tr"));

                    for (WebElement row : rows) {
                        WebElement contentElement = row.findElement(By.tagName("td"));
                        String content = contentElement.getText();
                        if (content.equals("일정 데이터가 없습니다.")){
                            isNoData = true;
                            break;
                        }
                        else if(scheduleRepository.existsByContent(content)){
                            continue;
                        }
                        WebElement dateElement = row.findElement(By.tagName("th"));
                        String date = dateElement.getText();

                        String[] dates = date.split("~");
                        String startDate;
                        String endDate;
                        if(dates.length==2){
                            startDate = dates[0].substring(0,10);
                            endDate = dates[1].substring(1,11);
                        }
                        else{
                            startDate = date.substring(0,10);
                            endDate = date.substring(0,10);
                        }
                        LocalDate start = LocalDate.parse(startDate,formatter);
                        LocalDate end = LocalDate.parse(endDate,formatter);
                        Schedule schedule = Schedule.builder().id(++id).startDate(start).endDate(end).content(content).build();
                        scheduleRepository.save(schedule);
                    }
                    month++;
                }
                WebElement link = webDriver.findElement(By.xpath("//*[@id=\"schdulWrap\"]/div[1]/div[1]/a[2]"));
                link.click();
                Thread.sleep(1500);
            }
            log.info("학사일정 크롤링 완료");

        }
        finally {
            webDriver.quit();
        }
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponseDto> getScheduleByMonth(int year, int month){
        List<Schedule> schedules = scheduleRepository.findAllByStartDateOrEndDateMonth(year,month);
        return schedules.stream().map(ScheduleResponseDto::of).collect(Collectors.toList());
    }
}



