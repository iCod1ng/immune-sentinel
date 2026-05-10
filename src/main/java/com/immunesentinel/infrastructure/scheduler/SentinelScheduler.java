package com.immunesentinel.infrastructure.scheduler;

import com.immunesentinel.application.checklist.ChecklistInstanceGenerator;
import com.immunesentinel.application.reminder.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SentinelScheduler {

    private final ChecklistInstanceGenerator generator;
    private final ReminderService reminderService;

    /** 每天 00:05 生成当日（及即将到来的相对事件）checklist 实例 */
    @Scheduled(cron = "0 5 0 * * *")
    @SchedulerLock(name = "daily_generate", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    public void generateDaily() {
        log.info("daily generate start");
        generator.generateFor(LocalDate.now());
    }

    /** 每天 07:30 推早晨 checklist */
    @Scheduled(cron = "0 30 7 * * *")
    @SchedulerLock(name = "remind_morning")
    public void remindMorning() {
        reminderService.pushReminderForTemplate("daily_morning");
    }

    /** 每天 21:00 推晚间 checklist + 症状日记 */
    @Scheduled(cron = "0 0 21 * * *")
    @SchedulerLock(name = "remind_evening")
    public void remindEvening() {
        reminderService.pushReminderForTemplate("daily_evening");
    }

    /** 每周一 08:00 推左腿监测 */
    @Scheduled(cron = "0 0 8 * * MON")
    @SchedulerLock(name = "remind_weekly_leg")
    public void remindWeeklyLeg() {
        reminderService.pushReminderForTemplate("weekly_leg");
    }

    /** 每月 1 号 09:00 推月度检查提示 */
    @Scheduled(cron = "0 0 9 1 * *")
    @SchedulerLock(name = "remind_monthly")
    public void remindMonthly() {
        reminderService.pushReminderForTemplate("monthly_infusion_lab");
    }

    /** 相对事件类模板由 generateDaily 生成实例，提醒则在当天早上 09:00 统一推 */
    @Scheduled(cron = "0 0 9 * * *")
    @SchedulerLock(name = "remind_relative_events")
    public void remindRelativeEvents() {
        reminderService.pushReminderForTemplate("infusion_t_minus_1d");
        reminderService.pushReminderForTemplate("infusion_day");
    }
}
