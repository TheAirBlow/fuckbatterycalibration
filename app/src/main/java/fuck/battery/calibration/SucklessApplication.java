package fuck.battery.calibration;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SucklessApplication extends Application {
    private static Timer timer = new Timer();
    private static int secondsPassed;
    public static File maxVoltageFile;
    public static Double cachedMaxVoltage;
    private NotificationManager manager = null;

    @Override
    public void onCreate() {
        super.onCreate();

        maxVoltageFile = new File(getFilesDir(), "maxVoltage");
        if (!maxVoltageFile.exists()) {
            try {
                maxVoltageFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cachedMaxVoltage = readMaxVoltage();

        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel nc = new NotificationChannel("FBC", "Battery Information",
                NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(nc);

        startForegroundService(new Intent(this, BackgroundService.class));
    }

    public static double readMaxVoltage() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(maxVoltageFile));
            String line = reader.readLine();
            return Double.parseDouble(line);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static void writeMaxVoltage() {
        try {
            maxVoltageFile.delete();
            maxVoltageFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(maxVoltageFile));
            String str = Double.toString(cachedMaxVoltage);
            writer.write(str);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getSecondsPassed() {
        return secondsPassed;
    }

    public static void resetTimer() {
        timer.cancel();
        timer = new Timer();
        secondsPassed = 0;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                secondsPassed++;
            }
        }, 0, 1000);
    }
}
