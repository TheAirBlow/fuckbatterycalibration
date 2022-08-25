package fuck.battery.calibration;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackgroundService extends Service
{
    private ExecutorService executorService = Executors.newFixedThreadPool(1);
    public static Double currVoltage = null;
    private NotificationManager notificationManager;
    private static double lastVoltage = -1;
    private static double maxRecordedVoltage = -1;
    private static boolean ignoreFurtherCharge = false;
    private static boolean shownWarning = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executorService.execute(this::mainThread);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        startForeground(1, createNotification(false));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @SuppressLint("SetTextI18n")
    private void mainThread() {
        try {
            while (true) {
                // Get battery information
                Intent batteryIntent = getApplicationContext().registerReceiver(null,
                        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

                boolean charging = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        -1) == BatteryManager.BATTERY_STATUS_CHARGING;

                int tmpVoltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                Double voltage = Double.parseDouble(insertString(Integer.toString(tmpVoltage), ".", 0));
                int tmpTemperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                double temperature = Double.parseDouble(insertString(Integer.toString(tmpTemperature), ".", 1));

                currVoltage = voltage;

                int normalPercentage = voltage > 3.2 ? (int)
                        (100 * (voltage - 3.2) / (SucklessApplication.cachedMaxVoltage - 3.2)) : 0;
                int cutoffPercentage = voltage <= 3.2 ? (int)
                        (100 * (voltage - 3.0) / (3.2 - 3.0)) : 100;

                // Show all of the info in the UI

                if (MainActivity.activity != null) {
                    Activity context = MainActivity.activity;
                    TextView voltageText = context.findViewById(R.id.currVoltage);
                    TextView temperatureText = context.findViewById(R.id.temperature);
                    TextView maxVoltageText = context.findViewById(R.id.maxVoltage);
                    TextView normalText = context.findViewById(R.id.normalLevel);
                    TextView cutoffText = context.findViewById(R.id.cutoffLevel);
                    voltageText.setText(voltage + "V");
                    temperatureText.setText(temperature + " °С");
                    if (SucklessApplication.cachedMaxVoltage == -1) {
                        maxVoltageText.setText("N/A");
                        normalText.setText("N/A");
                        cutoffText.setText("N/A");
                    } else {
                        maxVoltageText.setText(SucklessApplication.cachedMaxVoltage + "V");
                        normalText.setText(normalPercentage + "%");
                        cutoffText.setText(cutoffPercentage + "%");
                    }
                }

                if (charging) shownWarning = false;

                // Max battery voltage calculation :P
                if (!ignoreFurtherCharge && charging) {
                    if (voltage > lastVoltage && voltage > maxRecordedVoltage) {
                        maxRecordedVoltage = voltage;
                        SucklessApplication.resetTimer();
                    }

                    lastVoltage = voltage;

                    if (SucklessApplication.getSecondsPassed() > 600) {
                        ignoreFurtherCharge = true;
                        SucklessApplication.cachedMaxVoltage = voltage;
                        SucklessApplication.writeMaxVoltage();
                    } else updateTimePassed();
                } else {
                    if (MainActivity.activity != null) {
                        TextView timeText = MainActivity.activity.findViewById(R.id.voltageIncreaseTime);
                        timeText.setText("N/A");
                    }
                    ignoreFurtherCharge = false;
                    maxRecordedVoltage = -1;
                    lastVoltage = -1;
                }

                if (currVoltage < 3.2 && !shownWarning && !charging) {
                    if (MainActivity.activity == null) {
                        Intent newIntent = new Intent(getApplicationContext(), MainActivity.class);
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        getApplicationContext().startActivity(newIntent);
                    }
                    MainActivity.activity.runOnUiThread(() -> {
                        AlertDialog dialog = new AlertDialog.Builder(MainActivity.activity)
                                .setTitle("Critically low battery voltage")
                                .setMessage("Power off the device or connect it to a charger")
                                .create();
                        dialog.show();
                    });
                    shownWarning = true;
                }

                notificationManager.notify(1, createNotification(charging));
                Thread.sleep(500);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public Notification createNotification(boolean charging) {
        Intent batteryIntent = getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int tmpVoltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        Double voltage = Double.parseDouble(insertString(Integer.toString(tmpVoltage), ".", 0));
        currVoltage = voltage;

        int normalPercentage = currVoltage > 3.2 ? (int)
                (100 * (currVoltage - 3.2) / (SucklessApplication.cachedMaxVoltage - 3.2)) : 0;
        int cutoffPercentage = currVoltage <= 3.2 ? (int)
                (100 * (currVoltage - 3.0) / (3.2 - 3.0)) : 100;
        if (SucklessApplication.cachedMaxVoltage == -1)
            normalPercentage = 0;

        int chosenPercentage = currVoltage > 3.2 ? normalPercentage : cutoffPercentage;
        int iconPercentage = (int) (28 * (currVoltage - 3.2) / (SucklessApplication.cachedMaxVoltage - 3.2));
        if (currVoltage <= 3.2) iconPercentage += (int) (4 * (currVoltage - 3.0) / (3.2 - 3.0));
        else iconPercentage += 4;

        if (SucklessApplication.cachedMaxVoltage == -1) iconPercentage = -1;
        Notification.Builder builder = new Notification.Builder(
                getApplicationContext(), "FBC")
                .setSmallIcon(getIcon(iconPercentage))
                .setContentTitle(SucklessApplication.cachedMaxVoltage != -1
                        ? currVoltage > 3.2
                        ? "Battery is fine - not cutoff yet"
                        : "Battery is almost dead - cutoff voltage"
                        : charging ? "Calculating maximum voltage..."
                        : "Unable to calculate percentage!")
                .setContentText(SucklessApplication.cachedMaxVoltage != -1 ? currVoltage + "V | " + chosenPercentage + "%"
                        : charging ? getTimePassedText() : currVoltage + "V")
                .setOngoing(true).setAutoCancel(false);
        if (SucklessApplication.cachedMaxVoltage != -1)
            builder.setProgress(100, chosenPercentage, false);
        else if (charging) builder.setProgress(100, 100, true);
        else builder.setProgress(100, 100, false);
        return builder.build();
    }

    private int getIcon(int percentage) {
        if (percentage == -1) return R.drawable.unknown;

        try {
            return (int)R.drawable.class.getDeclaredField("battery_" + percentage).get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    private String getTimePassedText() {
        int s = SucklessApplication.getSecondsPassed();
        int m = (s / 60) % 60;
        if (s > 60) return m + "m " + s % 60 + "s";
        else return s % 60 + "s";
    }

    private void updateTimePassed() {
        if (MainActivity.activity == null) return;
        TextView timeText = MainActivity.activity.findViewById(R.id.voltageIncreaseTime);
        timeText.setText(getTimePassedText());
    }

    private static String insertString(
            String originalString,
            String stringToBeInserted,
            int index) {
        String newString = new String();

        for (int i = 0; i < originalString.length(); i++) {
            newString += originalString.charAt(i);

            if (i == index)
                newString += stringToBeInserted;
        }

        return newString;
    }
}
