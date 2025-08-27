package com.capacitorjs.plugins.pushnotifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.getcapacitor.JSObject;

public class CallActionReceiver extends BroadcastReceiver {

  public static String CALL_ID_LABEL = "CALL_ID";
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    String callId = intent.getStringExtra(CALL_ID_LABEL);

    if (action == null || callId == null) {
      return;
    }

    context.stopService(new Intent(context, RingtoneService.class));

    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
    notificationManager.cancel(MessagingService.CALL_NOTIFICATION_ID);

    Intent closeIntent = new Intent("ACTION_CLOSE_INCOMING_CALL_ACTIVITY");
    context.sendBroadcast(closeIntent);

    if ("ACTION_ACCEPT_CALL".equals(action)) {
      Log.d(MessagingService.TAG, "Call accepted: " + callId);

      JSObject callAction = new JSObject();
      callAction.put("actionId", "acceptCall");
      callAction.put("callId", callId);

      PushNotificationsPlugin.lastCallAction = callAction;
      Intent mainActivityIntent = new Intent(context, getMainActivityClass(context));
      mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      mainActivityIntent.putExtra("voipAction", "acceptCall");
      mainActivityIntent.putExtra("callId", callId);
      context.startActivity(mainActivityIntent);

    } else if ("ACTION_DECLINE_CALL".equals(action)) {
      Log.d(MessagingService.TAG, "Call declined: " + callId);

      Intent declineVoipIntent = new Intent("ACTION_DECLINE_VOIP_CALL");
      declineVoipIntent.putExtra("callId", callId);
      context.sendBroadcast(declineVoipIntent);
    }
  }

  private Class getMainActivityClass(Context context) {
    String packageName = context.getPackageName();
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
    String className = launchIntent.getComponent().getClassName();
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return null;
    }
  }
}
