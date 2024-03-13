package com.amtxts.tools;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.DefaultCaret;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CTool {

    public static File chooseFile(String dialogTitle) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    public static void writeToFile(File file, String content) {
        try {
            String contentWithTimestamp = getWithTimestamp(content) + "\n";
            Files.write(file.toPath(), contentWithTimestamp.getBytes(Charset.forName("GBK")), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getWithTimestamp(String content) {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = currentTime.format(formatter);

        return "[" + timestamp + "] " + content;
    }

    public static void appendText(JTextArea component, String text) {
        if (component == null) {
            System.out.println("component is null");
        }
        SwingUtilities.invokeLater(() -> component.append(getWithTimestamp(text) + "\n"));
    }

    public static String getDesktopPath() {
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        return fileSystemView.getHomeDirectory().getAbsolutePath();
    }

    public static String getLocalPath() {
        return CTool.class.getResource("").getPath();
    }


    /**
     * 执行命令集，并将信息存储到指定日志文件
     * @param infoBar    信息显示组件
     * @param outputFile 日志文件
     * @param commands   CMD命令集合
     */
    public static void doCmdInWorker(JTextArea infoBar, File outputFile, String[] commands) {
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            private long startTime;

            @Override
            protected Void doInBackground() throws Exception {
                if (commands == null || commands.length == 0) {
                    CTool.appendText(infoBar, "未读取到执行命令");
                    return null;
                }
                startTime = System.currentTimeMillis();
                for (String command : commands) {
                    if (isCancelled()) {
                        break;  // 如果任务已被取消，立即退出循环
                    }
                    if (command == null || "".equals(command)) {
                        continue;
                    }
                    if (processCmd(outputFile, command) != 0) {
                        CTool.appendText(infoBar, "执行出错：" + command);
                        cancel(true);
                    } else {
                        CTool.appendText(infoBar, command);
                    }
                    scrollToBottom(infoBar);
                }
                return null;
            }

            @Override
            protected void done() {
                if (commands != null) {
                    long endTime = System.currentTimeMillis();
                    long executionTimeInMillis = endTime - startTime;
                    double executionTimeInSeconds = executionTimeInMillis / 1000.0;
                    CTool.appendText(infoBar, "任务执行耗时：" + executionTimeInSeconds);

                    if (isCancelled()) {
                        CTool.writeToFile(outputFile, "任务被中断");
                    } else {
                        CTool.writeToFile(outputFile, "任务执行完成");
                    }
                }
            }
        };

        worker.execute();
    }


    /**
     * 执行CMD命令，并将信息存储到指定日志文件
     *
     * @param outputFile 日志文件
     * @param command    CMD命令
     * @return 0正常执行
     */
    public static int processCmd(File outputFile, String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("GBK")));
            String result = reader.readLine();

            int exitID = process.waitFor();
            if (exitID != 0) {
                CTool.writeToFile(outputFile, result); // line 出错原因
            }
            return exitID;
        } catch (IOException | InterruptedException e) {
//            String info = e.getMessage();
//            CTool.appendText(info);
//            FileOperations.writeToFile(outputFile, info);  // 异常信息
            return 1;
        }
    }

    // 滚动到文本域底部
    private static void scrollToBottom(JTextArea textArea) {
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    public static String[] parseFileContext(File file) {
        StringBuilder stringBuilder = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file.getAbsolutePath());
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                // 过滤空字符串或空行
                if (!line.trim().isEmpty()) {
                    if (stringBuilder.length() > 0) {
                        stringBuilder.append(",");
                    }
                    stringBuilder.append(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (stringBuilder.length() == 0) {
            return null;
        }
        return stringBuilder.toString().split(",");
    }

}
