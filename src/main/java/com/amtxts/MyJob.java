package com.amtxts;

import com.amtxts.tools.CTool;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.swing.*;
import java.io.File;

public class MyJob implements Job {
    private String[] commands;
    private File outputFile, cmdsFile;
    private JTextArea infoBar;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        this.commands = (String[]) jobDataMap.get("commands");
        this.outputFile = (File) jobDataMap.get("outputFile");
        this.cmdsFile = (File) jobDataMap.get("cmdsFile");
        this.infoBar = (JTextArea) jobDataMap.get("infoBar");

        commands = CTool.parseFileContext(cmdsFile);
        CTool.appendText(infoBar, "定时任务开始执行...");
        CTool.doCmdInWorker(infoBar, outputFile, commands);

    }

}
