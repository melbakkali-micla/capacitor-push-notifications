package com.capacitorjs.plugins.pushnotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class IncomingCallActivity extends AppCompatActivity {
  private BroadcastReceiver closeReceiver;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true);
      setTurnScreenOn(true);
    } else {
      getWindow().addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
      );
    }

    setContentView(R.layout.activity_incoming_call);

    WindowInsetsControllerCompat windowInsetsController =
    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
    // Hide the status bar
    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars());
    // Set the behavior to show transient bars by swiping
    windowInsetsController.setSystemBarsBehavior(
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    );

    TextView callerNameTextView = findViewById(R.id.callerName);
    ImageButton acceptButton = findViewById(R.id.acceptButton);
//    ImageButton declineButton = findViewById(R.id.declineButton);

    // 3. Retrieve all the data from the Intent
    String callerName = getIntent().getStringExtra("CALLER_NAME");
    String callId = getIntent().getStringExtra("CALL_ID");

    // 4. Set the caller's name
    if (callerName != null) {
      callerNameTextView.setText(callerName);
    }

    acceptButton.setOnClickListener(v -> {
      // we create an intent to open the main application activity
      Intent mainActivityIntent = new Intent(this, getMainActivityClass());
      mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      mainActivityIntent.putExtra("action", "acceptCall");
      mainActivityIntent.putExtra("callId", callId);
      startActivity(mainActivityIntent);

      // Also we send a broadcast to handle any other logic needed
      Intent broadcastIntent = new Intent(this, CallActionReceiver.class);
      broadcastIntent.setAction("ACTION_ACCEPT_CALL");
      broadcastIntent.putExtra("CALL_ID", callId);
      sendBroadcast(broadcastIntent);

      finish();
    });

//    declineButton.setOnClickListener(v -> {
//      Intent intent = new Intent(this, CallActionReceiver.class);
//      intent.setAction("ACTION_DECLINE_CALL");
//      intent.putExtra("CALL_ID", callId);
//      sendBroadcast(intent);
//      finish();
//    });

    closeReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if ("ACTION_CLOSE_INCOMING_CALL_ACTIVITY".equals(intent.getAction())) {
          finish();
        }
      }
    };


    registerReceiver(closeReceiver, new IntentFilter("ACTION_CLOSE_INCOMING_CALL_ACTIVITY"));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    stopService(new Intent(this, RingtoneService.class));
    if (closeReceiver != null) {
      unregisterReceiver(closeReceiver);
    }
  }

  private Class getMainActivityClass() {
    String packageName  = getPackageName();
    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
    String className = launchIntent.getComponent().getClassName();
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }
}
