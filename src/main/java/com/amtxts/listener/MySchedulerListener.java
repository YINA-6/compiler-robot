package com.amtxts.listener;

import org.quartz.*;

import javax.swing.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class MySchedulerListener implements SchedulerListener {
    private HashMap<String,Object> params;
    public MySchedulerListener(HashMap<String,Object> param){
        params = param;
    }

    @Override
    public void jobScheduled(Trigger trigger) {

    }

    @Override
    public void jobUnscheduled(TriggerKey triggerKey) {

    }

    @Override
    public void triggerFinalized(Trigger trigger) {

    }

    @Override
    public void triggerPaused(TriggerKey triggerKey) {

    }

    @Override
    public void triggersPaused(String triggerGroup) {

    }

    @Override
    public void triggerResumed(TriggerKey triggerKey) {

    }

    @Override
    public void triggersResumed(String triggerGroup) {

    }

    @Override
    public void jobAdded(JobDetail jobDetail) {

    }

    @Override
    public void jobDeleted(JobKey jobKey) {

    }

    @Override
    public void jobPaused(JobKey jobKey) {

    }

    @Override
    public void jobsPaused(String jobGroup) {

    }

    @Override
    public void jobResumed(JobKey jobKey) {

    }

    @Override
    public void jobsResumed(String jobGroup) {

    }

    @Override
    public void schedulerError(String msg, SchedulerException cause) {

    }

    @Override
    public void schedulerInStandbyMode() {

    }

    @Override
    public void schedulerStarted() {
    }

    @Override
    public void schedulerStarting() {
        try {
            JTextArea infoBar2 = (JTextArea) params.get("infoBar2");
            infoBar2.setText("");
            String expression = (String) params.get("expression");
            infoBar2.append("最近3次运行时间：\n");
            CronExpression cronExpression = new CronExpression(expression);
            long currentTimeMillis = System.currentTimeMillis();
            for (int i = 0; i < 3; i++) {
                Date nextValidTime = cronExpression.getNextValidTimeAfter(new Date(currentTimeMillis));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                infoBar2.append(sdf.format(nextValidTime) + "\n");
                currentTimeMillis = nextValidTime.getTime() + 1;

            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void schedulerShutdown() {

    }

    @Override
    public void schedulerShuttingdown() {

    }

    @Override
    public void schedulingDataCleared() {

    }
}
