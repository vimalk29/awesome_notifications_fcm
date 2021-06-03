package me.carda.awesome_notifications_fcm;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import me.carda.awesome_notifications.AwesomeNotificationsPlugin;
import me.carda.awesome_notifications.Definitions;
import me.carda.awesome_notifications.notifications.enumerators.NotificationLifeCycle;
import me.carda.awesome_notifications.notifications.models.PushNotification;
import me.carda.awesome_notifications_fcm.services.DartBackgroundExecutor;
import me.carda.awesome_notifications_fcm.services.DartBackgroundService;

public class BroadcastFCMSender {

    private static final String TAG = "BroadcastFCMSender";

    public static boolean SendBroadcastNewFcmToken(Context context, String token){

        Intent intent = new Intent(Definitions.BROADCAST_FCM_TOKEN);
        intent.putExtra(Definitions.EXTRA_BROADCAST_FCM_TOKEN, token);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
        return broadcastManager.sendBroadcast(intent);
    }

    public static boolean SendBroadcastSilentData(Context context, NotificationLifeCycle initialLifeCycle, PushNotification pushNotification){

        boolean isApplicationNotKilled = initialLifeCycle != NotificationLifeCycle.AppKilled;

        if (isApplicationNotKilled) {
            Intent intent =
                DartBackgroundExecutor.notificationBuilder.buildNotificationIntentFromModel(
                        context,
                        FcmDefinitions.BROADCAST_SILENT_DATA,
                        pushNotification,
                        AwesomeNotificationsFcmPlugin.class);

            if(intent == null) return false;

            LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
            return broadcastManager.sendBroadcast(intent);
        }
        else {
            Intent intent =
                DartBackgroundExecutor.notificationBuilder.buildNotificationIntentFromModel(
                        context,
                        FcmDefinitions.BROADCAST_SILENT_DATA,
                        pushNotification,
                        DartBackgroundService.class);

            if(intent == null) return false;

            DartBackgroundService.enqueueSilentDataProcessing(context, intent);
            return true;
        }
    }
}
