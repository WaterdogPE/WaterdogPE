/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.logger;

import org.fusesource.jansi.Ansi;
import pe.waterdog.WaterdogPE;
import pe.waterdog.command.CommandReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.*;

public class Logger extends Thread{

    private static Logger instance;

    private boolean shutdown = false;

    private String logPath;
    private boolean debug = false;

    private String prefix = null;

    protected final ConcurrentLinkedQueue<String> logBuffer = new ConcurrentLinkedQueue<>();
    private Color[] textFormats = Color.values();

    public Logger(String logFile){
        this(logFile, false);
    }

    public Logger(String logFile, boolean debug){
        if (instance != null){
            throw new RuntimeException("Logger has been already created");
        }

        instance = this;
        this.logPath = logFile;
        this.debug = debug;
        this.start();
    }

    public static Logger getLogger() {
        return instance;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void shutdown() {
        this.shutdown = true;
    }


    public void emergency(String message) {
        this.log(Color.RED + "[EMERGENCY]", message);
    }


    public void alert(String message) {
        this.log(Color.RED + "[ALERT]", message);
    }


    public void critical(String message) {
        this.log(Color.RED + "[CRITICAL]", message);
    }


    public void error(String message) {
        this.log(Color.DARK_RED + "[ERROR]", message);
    }

    public void error(String message, Throwable e){
        this.error(message);
        this.logException(e);
    }

    public void warning(String message) {
        this.log(Color.YELLOW + "[WARNING]", message);
    }


    public void notice(String message) {
        this.log(Color.AQUA + "[NOTICE]", message);
    }


    public void info(String message) {
        this.log(Color.WHITE + "[INFO]", message);
    }


    public void debug(String message) {
        if (!this.debug) return;
        this.log(Color.GRAY + "[DEBUG]", message);
    }

    private void log(String format, String message){
        this.send(format+(this.prefix == null? "" : " ["+this.prefix+"] ")+ message);
    }

    public void logException(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);

        this.alert(stringWriter.toString());
    }

    protected void send(String message) {
        this.logBuffer.add(message);
    }

    private String colorize(String message){
        if (message.indexOf(Color.ESCAPE) < 0) return message;

        for (Color color : this.textFormats){
            message = message.replaceAll(color.toString(), color.getAnsi().toString());
        }

        return message + Ansi.ansi().reset();
    }

    @Override
    public void run() {
        File logFile = this.setupLogFiles();
        if (logFile == null){
            throw new RuntimeException("Unable to open Log File. Maybe file permission problem?");
        }

        while (!this.shutdown){
            if (logBuffer.isEmpty()) {
                try {
                    /*synchronized (this) {
                        this.wait(2000);
                    }*/
                    Thread.sleep(5);
                } catch (InterruptedException ignore) {
                }
            }

            this.flushBuffer(logFile);
        }

        this.flushBuffer(logFile);
    }

    private File setupLogFiles(){
        File logFile = new File(this.logPath+".log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                return null;
            }
            return logFile;
        }

        long date = logFile.lastModified();

        String newName = new SimpleDateFormat("Y-M-d HH.mm.ss").format(new Date(date)) + ".log";
        File oldLogs = new File(WaterdogPE.DATA_PATH, "logs");

        if (!oldLogs.exists()) oldLogs.mkdirs();

        logFile.renameTo(new File(oldLogs, newName));
        logFile = new File(this.logPath+".log");

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                return null;
            }
        }

        return logFile;
    }

    private void flushBuffer(File logFile){
        try {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8), 1024);
            Date now = new Date();
            String consoleDateFormat = new SimpleDateFormat("HH:mm:ss ").format(now);
            String fileDateFormat = new SimpleDateFormat("Y-M-d HH:mm:ss ").format(now);

            while (!logBuffer.isEmpty()) {
                String message = logBuffer.poll();
                if (message == null) continue;

                writer.write(fileDateFormat);
                writer.write(Color.clean(message));
                writer.write("\r\n");
                //CommandReader.getInstance().stashLine();
                System.out.println(this.colorize(Color.WHITE + consoleDateFormat + Color.RESET + message + Color.RESET));
                //CommandReader.getInstance().unstashLine();
            }
            writer.flush();
            writer.close();
        } catch (Exception e) {
            this.logException(e);
        }
    }
}
