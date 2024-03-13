package com.amtxts.listener;

import com.amtxts.tools.CTool;
import org.quartz.CronExpression;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import javax.swing.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MyJobListener implements JobListener {
    private HashMap<String, Object> params;

    public MyJobListener(HashMap<String, Object> map) {
        params = map;
    }


    @Override
    public String getName() {
        return "JOB";
    }

    // 在作业即将执行时调用
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        try {
            String expression = (String) params.get("expression");
            JTextField cronTable = (JTextField) params.get("cronTable");
            JTextArea infoBar2 = (JTextArea) params.get("infoBar2");
            if (params.get("cron") == null) {
                String oldText = cronTable.getText();
                params.put("cron", oldText);
            }

            infoBar2.setText("");
            CronExpression cronExpression = new CronExpression(expression);
            long currentTimeMillis = System.currentTimeMillis();

            infoBar2.append("最近3次执行时间：\n");
            // 计算未来三次的生效时间
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
    public void jobExecutionVetoed(JobExecutionContext context) {
    }



    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    }

}
