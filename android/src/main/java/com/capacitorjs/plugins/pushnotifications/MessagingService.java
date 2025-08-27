package com.capacitorjs.plugins.pushnotifications;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessagingService extends FirebaseMessagingService {

  public static String TAG = "MessagingService";
  public static final int CALL_NOTIFICATION_ID = 120;

  @Override
  public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    super.onMessageReceived(remoteMessage);

    Map<String, String> data = remoteMessage.getData();
    if (data.isEmpty()) return;

    String type = data.get("type");
    if (type == null) return;

    switch (type) {
      case "message":
        handleMessagePayload(remoteMessage, data);
        break;
      case "alert":
        handleAlertPayload(data);
        break;
      case "incoming_call":
        handleIncomingCallPayload(data);
        break;
      case "cancel_call":
        handleCancelCallPayload(data);
        break;
      default:
        Log.w("PushNotification", "Unknown message type: " + type);
        break;
    }
  }

  private void handleMessagePayload(RemoteMessage remoteMessage, Map<String, String> payload) {
    String roomType = payload.get("roomType");
    if (!"one-to-one".equals(roomType)) return;

    String activeDirectChatId = PushNotificationsPlugin.chatRoutingInfo.getActiveDirectChatId();
    String receiver = payload.get("receiver");
    String sender = payload.get("sender");

    if (receiver == null || sender == null) return;

    String incomingDirectChatId = "dm_" + receiver + ":" + sender;

    if (!incomingDirectChatId.equals(activeDirectChatId)) {
      PushNotificationsPlugin.sendRemoteMessage(remoteMessage);
    }
  }

  private void handleAlertPayload(Map<String, String> payload) {
    String channelId = getString(R.string.DEFAULT_CHANNEL);
    showAlert(channelId, payload);
  }

  @Override
  public void onNewToken(@NonNull String s) {
    super.onNewToken(s);
    PushNotificationsPlugin.onNewToken(s);
  }

  public void showAlert(String channelId, Map<String, String> alert) {
    String notificationId = alert.get("_id");

    alert.put("notificationId", notificationId);
    alert.put("receivedAt", new Date().toString());
    alert.put("channelId", channelId);

    Map<String, RemoteViews> layouts = this.getAlertLayouts(alert);
    NotificationCompat.Builder builder = this.buildAlertNotification(channelId, alert, layouts);

    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      // TODO: Consider calling
      //    ActivityCompat#requestPermissions
      // here to request the missing permissions, and then overriding
      //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
      //                                          int[] grantResults)
      // to handle the case where the user grants the permission. See the documentation
      // for ActivityCompat#requestPermissions for more details.
      return;
    }

    assert notificationId != null;
    notificationManager.notify(Integer.parseInt(notificationId, 16), builder.build());
  }

  public Map<String, RemoteViews> getAlertLayouts(Map<String, String> alert) {
    RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.small_notification_layout);
    RemoteViews notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.large_notification_layout);

    String title = alert.get("title");
    String sender = alert.get("sender");
    String message = alert.get("body");
    String receivedAt = alert.get("receivedAt");

    if (sender == null) {
      throw new AssertionError();
    }

    /* Normal Layout */
    notificationLayout.setTextViewText(R.id.notification_title, title);
    notificationLayout.setTextViewText(R.id.notification_body, "Message: " + message);
    notificationLayout.setTextViewText(R.id.notification_sender, "Envoyé par: " + sender);

    /* Expanded layout */
    notificationLayoutExpanded.setTextViewText(R.id.notification_sender, "Envoyé par: ".concat(sender));
    notificationLayoutExpanded.setTextViewText(R.id.notification_title, title);
    notificationLayoutExpanded.setTextViewText(R.id.notification_body, "Message: " + message);
    notificationLayoutExpanded.setTextViewText(R.id.notification_date, receivedAt);

    Map<String, RemoteViews> layouts = new HashMap<>();

    layouts.put("notificationLayout", notificationLayout);
    layouts.put("notificationLayoutExpanded", notificationLayoutExpanded);

    return layouts;
  }

  public NotificationCompat.Builder buildAlertNotification(String channelId, Map<String, String> alert, Map<String, RemoteViews> layouts) {
    Boolean isDeviceLocked = isDeviceLocked();

    Intent intent = createIntent(alert, isDeviceLocked);
    Intent mainIntent = createIntent(alert, false);

    // Use different request codes for different intents
    PendingIntent pendingIntent = PendingIntent.getActivity(
      this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    PendingIntent mainPendingIntent = PendingIntent.getActivity(
      this, 1, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );

    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
      .setContentTitle(alert.get("title"))
      .setContentText(alert.get("message"))
      .setSmallIcon(R.drawable.icon)
      .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
      .setContentIntent(mainPendingIntent)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setGroup(getPackageName())
      .setColor(Color.RED)
      .setColorized(true)
      .setOngoing(true)
      .setAutoCancel(true);

    NotificationCompat.DecoratedCustomViewStyle style = new NotificationCompat.DecoratedCustomViewStyle();
    builder.setStyle(style);

    if (isDeviceLocked) {
      builder.setFullScreenIntent(pendingIntent, true);
    }

    if (layouts != null) {
      RemoteViews contentView = layouts.get("notificationLayout");
      RemoteViews expandedView = layouts.get("notificationLayoutExpanded");

      if (contentView != null) {
        builder.setCustomContentView(contentView);
      }
      if (expandedView != null) {
        builder.setCustomBigContentView(expandedView);
      }
    }

    return builder;
  }

  public Intent createIntent(Map<String, String> alert, Boolean isDeviceLocked) {
    String packageName = getPackageName();
    String className;

    try {
      className = getString(isDeviceLocked ? R.string.fullscreen_notification_class_name : R.string.main_activity_class_name);

      Log.d(TAG, className);

      String notificationFullClassName = packageName + "." + className;

      Log.d(TAG, notificationFullClassName);

      Class<?> targetActivity = Class.forName(notificationFullClassName);

      Intent intent = new Intent(this, targetActivity);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      intent.setAction("ACTION_STRING" + System.currentTimeMillis());

      if (alert != null) {
        intent.putExtra("notificationId", alert.get("notificationId"));
        intent.putExtra("receivedAt", alert.get("receivedAt"));
      }

      return intent;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  private boolean isDeviceLocked() {
    KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
    return keyguardManager != null && keyguardManager.isKeyguardLocked();
  }

  private void handleIncomingCallPayload(Map<String, String> payload) {
    String CALL_CHANNEL_ID = getString(R.string.DEFAULT_CHANNEL);

    String callId = payload.get("callId");
    String callerName = payload.get("callerName");

    if (callId == null || callerName == null) {
      Log.e(TAG, "Incoming call payload is missing required data.");
      return;
    }

    Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
    fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    fullScreenIntent.putExtra("CALL_ID", callId);
    fullScreenIntent.putExtra("CALLER_NAME", callerName);

    PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    Intent declineIntent = new Intent(this, CallActionReceiver.class);
    declineIntent.setAction("ACTION_DECLINE_CALL");
    declineIntent.putExtra("CALL_ID", callId);
//    PendingIntent declinePendingIntent = PendingIntent.getBroadcast(this, 1, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    Intent acceptIntent = new Intent(this, CallActionReceiver.class);
    acceptIntent.setAction("ACTION_ACCEPT_CALL");
    acceptIntent.putExtra("CALL_ID", callId);
    PendingIntent acceptPendingIntent = PendingIntent.getBroadcast(this, 2, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    SpannableString acceptTitle = new SpannableString("Accept");
    acceptTitle.setSpan(new ForegroundColorSpan(Color.parseColor("#82e0aa")), 0, acceptTitle.length(), 0);

//    SpannableString declineTitle = new SpannableString("Decline");
//    declineTitle.setSpan(new ForegroundColorSpan(Color.parseColor("#f1948a")), 0, declineTitle.length(), 0);

    Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

    @SuppressLint("NotificationTrampoline") NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CALL_CHANNEL_ID)
      .setSmallIcon(R.drawable.icon)
      .setContentTitle("Incoming Call")
      .setContentText(callerName)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setAutoCancel(false)
      .setFullScreenIntent(fullScreenPendingIntent, true)
      .addAction(R.drawable.ic_call_accept, acceptTitle, acceptPendingIntent);

    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      Log.e(TAG, "Missing POST_NOTIFICATIONS permission.");
      return;
    }

    notificationManager.notify(CALL_NOTIFICATION_ID, builder.build());
    startService(new Intent(this, RingtoneService.class));
  }

  private void handleCancelCallPayload(Map<String, String> payload) {
    Log.d(TAG, "Cancelling incoming call notification.");
    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
    notificationManager.cancel(CALL_NOTIFICATION_ID);

    // Also, send a broadcast to close the activity if it's open
    Intent closeIntent = new Intent("ACTION_CLOSE_INCOMING_CALL_ACTIVITY");
    sendBroadcast(closeIntent);
    stopService(new Intent(this, RingtoneService.class));
  }
}
