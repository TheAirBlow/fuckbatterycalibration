package fuck.battery.calibration;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

import com.judemanutd.autostarter.AutoStartPermissionHelper;

public class MainActivity extends AppCompatActivity {
    private static PowerManager.WakeLock wakeLock = null;
    public static Activity activity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activity = this;

        Button button = findViewById(R.id.allowAutoStart);
        AutoStartPermissionHelper helper = AutoStartPermissionHelper.Companion.getInstance();
        button.setEnabled(helper.isAutoStartPermissionAvailable(getApplicationContext(), true));
    }

    @Override
    protected void onDestroy() {
        if (wakeLock != null)
            wakeLock.release();        activity = null;
        super.onDestroy();
    }

    public void resetMaxVoltage(View v) {
        SucklessApplication.cachedMaxVoltage = new Double(-1);
        SucklessApplication.writeMaxVoltage();
    }

    public void setMaxVoltage(View v) {
        SucklessApplication.cachedMaxVoltage = BackgroundService.currVoltage;
        SucklessApplication.writeMaxVoltage();
    }

    public void disableOptimisation(View v) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 1002);
    }

    public void askForAutoStart(View v) {
        AutoStartPermissionHelper helper = AutoStartPermissionHelper.Companion.getInstance();
        helper.getAutoStartPermission(getApplicationContext(), true, true);
    }

    public void switchWakeLock(View v) {
        if (wakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "FuckBatteryCalibration::WakeLock");
            wakeLock.acquire();
        } else {
            wakeLock.release();
            wakeLock = null;
        }
    }
}