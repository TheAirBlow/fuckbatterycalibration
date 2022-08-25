package fuck.battery.calibration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;

public class StorageManager {
    private static File file;

    public class Data {
        public Double maxObservedVoltage;
        public LocalDateTime lastMaxVoltageCheck;
    }

    public static void loadStorage(String filesDir) {
        file = new File(filesDir, "maxVoltage");
        try {
            if (!file.exists())
                file.createNewFile();
            BufferedReader reader = new BufferedReader(new FileReader(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
