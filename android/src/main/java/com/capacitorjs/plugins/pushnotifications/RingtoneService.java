package com.capacitorjs.plugins.pushnotifications;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class RingtoneService extends Service {

  private Ringtone ringtone;
  private Vibrator vibrator;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    // Get the default phone ringtone.
    Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    this.ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
    this.vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    // Set it to loop indefinitely.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      ringtone.setLooping(true);
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (ringtone != null && !ringtone.isPlaying()) {
      ringtone.play();
    }

    long[] pattern = {0, 1000, 1000};

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
    } else {
      vibrator.vibrate(pattern, 0);
    }

    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (ringtone != null && ringtone.isPlaying()) {
      ringtone.stop();
    }

    if (vibrator != null) {
      vibrator.cancel();
    }
  }
}
