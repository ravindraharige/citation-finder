package cf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author Ravindra Harige
 */
class CFLogger {
    private FileWriter log = null;

    void initLog(String file) {
        if (!new File(Constants.rootDir).exists()) {
            System.out.println(".\\data directory does not exist!");
            System.exit(0);
        }
        File newTextFile = new File(file);
        try {
            log = new FileWriter(newTextFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void changeLog(String file) {
        closeLog();
        initLog(file);
    }

    void log(String str) {
        try {
            log.write(str);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void plog(String str) {
//        System.out.println(str);
        try {
            log.write(str + "\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    void closeLog() {
        try {
            log.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
