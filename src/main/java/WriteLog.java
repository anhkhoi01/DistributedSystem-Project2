import java.io.FileWriter;
import java.io.IOException;

public class WriteLog {
    private static final String logFile = "logServer.txt";

    void writeLogAppend(String str) {
        DateTime now = new DateTime();
        try (FileWriter logfile = new FileWriter(logFile,true)) {
            logfile.write(now.getTime() + ": " + str + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void writeNewLog(String str) {
        DateTime now = new DateTime();
        try (FileWriter logfile = new FileWriter(logFile)) {
            if (str.equals("")) {
                logfile.write("");
                return;
            }
            logfile.write(now.getTime() + ": " + str + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
