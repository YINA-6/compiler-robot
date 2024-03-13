package com.amtxts;

import com.amtxts.listener.MyJobListener;
import com.amtxts.listener.MySchedulerListener;
import com.amtxts.tools.CTool;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class AppWindow {
    private File outputFile, cmdsFile; // 默认为桌面路径
    private String[] commands;
    private final Image dukeIcon;
    private final JTextArea infoBar;
    private final JTextArea infoBar2;
    private final JTextField cronTable;
    private final JButton startTaskButton;
    private final JButton endTaskButton;
    private Scheduler scheduler;
    private TrayIcon trayIcon;

    public AppWindow() {
        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // 系统风格
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel"); // 经典风格
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        infoBar = new JTextArea(12, 50);
        infoBar2 = new JTextArea(5, 50);
        cronTable = new JTextField("0 * * * * ?", 50); // 默认每分钟
        startTaskButton = new JButton("开启定时任务");
        endTaskButton = new JButton("关闭定时任务");
        endTaskButton.setEnabled(false);

        // 获取当前运行的jar包所在的目录
        String jarPath = AppWindow.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String jarDirectory = new File(jarPath).getParent();

        // 创建输出文件的路径
        Path outputPath = Paths.get(jarDirectory, "output.txt");
        Path cmdsPath = Paths.get(jarDirectory, "commands.txt");

        outputFile = outputPath.toFile();
        cmdsFile = cmdsPath.toFile();

        // 确保文件存在，如果不存在则创建
        createFileIfNotExists(outputFile);
        createFileIfNotExists(cmdsFile);

        startTaskButton.addActionListener(e -> timedTask());
        endTaskButton.addActionListener(e -> closeTimedTask());
        dukeIcon = Toolkit.getDefaultToolkit().
                getImage(Application.class.getResource("/com/sun/java/swing/plaf/windows/icons/JavaCup32.png"));

    }


    public void buildWindow() {
        // 创建Swing窗口
        JFrame frame = new JFrame("Compiler Robot");
        frame.setSize(570, 463);
        frame.setLayout(new BorderLayout());

        // 上下分割
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(1.0); // 设置上下布局的分割比例

        // 上部分
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(infoScrollPane(), BorderLayout.CENTER);

        // 下部分
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel cronPanel = new JPanel(new BorderLayout());
        JLabel cronLabel = createLinkLabel("CRON表达式(Quartz)：", "https://tool.lu/crontab/");
        cronLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        cronPanel.add(cronLabel, BorderLayout.WEST);
        cronPanel.add(cronTableScrollPane(), BorderLayout.EAST);
        bottomPanel.add(cronPanel, BorderLayout.NORTH);
        bottomPanel.add(info2ScrollPane(), BorderLayout.CENTER);

        // 将上下部分加入上下分割面板
        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(bottomPanel);
        frame.add(splitPane, BorderLayout.CENTER);

        // 右侧按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        addVerticalSpace(buttonPanel, 10);
        addButton(buttonPanel, chooseLogFileButton());
        addVerticalSpace(buttonPanel, 10);
        addButton(buttonPanel, chooseCmdFileButton());
        addVerticalSpace(buttonPanel, 10);
        addButton(buttonPanel, startButton());
        addVerticalSpace(buttonPanel, 10);
        addButton(buttonPanel, startTaskButton);
        addVerticalSpace(buttonPanel, 10);
        addButton(buttonPanel, endTaskButton);
        addVerticalSpace(buttonPanel, 10);

        frame.add(buttonPanel, BorderLayout.EAST);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                minimizeToTray(frame);
            }
        });
        frame.setResizable(false);
        frame.setVisible(true);
    }

    private void executeCommands() {
        commands = CTool.parseFileContext(cmdsFile);
        CTool.appendText(infoBar, "开始执行...");
        CTool.doCmdInWorker(infoBar, outputFile, commands);
    }

    private Scheduler initializeScheduler() throws SchedulerException {
        HashMap<String, Object> param = new HashMap<>();
        param.put("expression", cronTable.getText());
        param.put("cronTable", cronTable);
        param.put("infoBar", infoBar);
        param.put("infoBar2", infoBar2);

        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.getListenerManager().addJobListener(new MyJobListener(param));
        scheduler.getListenerManager().addSchedulerListener(new MySchedulerListener(param));
        scheduler.start();
        return scheduler;
    }

    private void timedTask() {
        try {
            String expression = cronTable.getText();
            if (expression == null || !CronExpression.isValidExpression(expression)) {
                CTool.appendText(infoBar, "Cron表达式有误");
                return;
            }
            scheduler = initializeScheduler();

            HashMap<Object, Object> map = new HashMap<>();
            map.put("commands", commands);
            map.put("outputFile", outputFile);
            map.put("cmdsFile", cmdsFile);
            map.put("infoBar", infoBar);

            JobDataMap dataMap = new JobDataMap(map);
            JobDetail job = JobBuilder.newJob(MyJob.class)
                    .withIdentity("job1", "group1")
                    .usingJobData(dataMap)
                    .build();

            CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1", "group1")
                    .withSchedule(CronScheduleBuilder
//                            .cronSchedule("0 * * * * ?") // 每分钟0秒
//                            .cronSchedule("0 0 0 * * ?") // 每天0点
                                    .cronSchedule(expression)
                                    .withMisfireHandlingInstructionFireAndProceed()
                    )
                    .build();


            scheduler.scheduleJob(job, cronTrigger);
            CTool.appendText(infoBar, "定时任务开启");

            startTaskButton.setEnabled(false);
            endTaskButton.setEnabled(true);
            cronTable.setEditable(false);
        } catch (Exception e) {
            try {
                scheduler.shutdown();
            } catch (SchedulerException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }

    private void closeTimedTask() {
        try {
            scheduler.shutdown();
            startTaskButton.setEnabled(true);
            endTaskButton.setEnabled(false);
            cronTable.setEditable(true);
            infoBar2.setText("");
            CTool.appendText(infoBar, "定时任务关闭");
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }


    private void createFileIfNotExists(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 创建包含URL链接的JLabel方法
    private static JLabel createLinkLabel(String text, String url) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.BLUE); // 设置文字颜色为蓝色，以表示链接
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 设置鼠标光标为手形，表示可点击
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    openWebpage(new URI(url));
                } catch (URISyntaxException | IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        return label;
    }

    // 打开链接的方法
    private static void openWebpage(URI uri) throws IOException {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(uri);
        }
    }

    private void minimizeToTray(JFrame frame) {
        if (SystemTray.isSupported()) {
            try {
                SystemTray systemTray = SystemTray.getSystemTray();
                PopupMenu popupMenu = new PopupMenu();
                // 创建最小化到托盘的菜单项
                MenuItem restoreItem = new MenuItem("还原");
                restoreItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        systemTray.remove(trayIcon);
                        frame.setVisible(true);
                    }
                });

                MenuItem exitItem = new MenuItem("退出");
                exitItem.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.exit(0);
                    }
                });

                popupMenu.add(restoreItem);
                popupMenu.add(exitItem);
                // 设置 JFrame 的默认图标


                trayIcon = new TrayIcon(dukeIcon, "监控机器人", popupMenu);
                trayIcon.setImageAutoSize(true);

                // 添加双击事件，实现从托盘还原窗口
                trayIcon.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        systemTray.remove(trayIcon);
                        frame.setVisible(true);
                    }
                });

                systemTray.add(trayIcon);
                frame.setVisible(false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            CTool.appendText(infoBar, "SystemTray is not supported");
        }
    }


    private JButton chooseLogFileButton() {
        JButton button = new JButton("选择日志文件");
        button.addActionListener(e -> chooseLogFile());
        return button;
    }

    private JButton chooseCmdFileButton() {
        JButton button = new JButton("选择命令文件");
        button.addActionListener(e -> chooseCmdFile());
        return button;
    }

    private JButton startButton() {
        JButton button = new JButton("开始执行任务");
        button.addActionListener(e -> executeCommands());
        return button;
    }

    private JScrollPane infoScrollPane() {
        infoBar.setEditable(false);
        return new JScrollPane(infoBar);
    }

    private JScrollPane info2ScrollPane() {
        infoBar2.setEditable(false);
        return new JScrollPane(infoBar2);
    }

    private JScrollPane cronTableScrollPane() {
        return new JScrollPane(cronTable);
    }

    private void chooseLogFile() {
        File file = CTool.chooseFile("选择日志文件");
        if (file != null) {
            outputFile = file;
            CTool.appendText(infoBar, "加载日志文件：" + file.getAbsolutePath());
        }
    }

    private void chooseCmdFile() {
        File file = CTool.chooseFile("选择命令文件");
        if (file != null) {
            cmdsFile = file;
            CTool.appendText(infoBar, "加载命令文件：" + file.getAbsolutePath());
            commands = CTool.parseFileContext(cmdsFile);
        }
    }

    // 向容器中添加垂直间距
    private void addVerticalSpace(Container container, int height) {
        container.add(Box.createRigidArea(new Dimension(0, height)));
    }

    // 向容器中添加按钮
    private void addButton(Container container, JButton button) {
        button.setAlignmentX(Component.CENTER_ALIGNMENT); // 将按钮的水平对齐方式设置为居中对齐
        container.add(button);
    }

}
