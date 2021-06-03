package me.carda.awesome_notifications_fcm.services;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import io.flutter.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.carda.awesome_notifications.AwesomeNotificationsPlugin;
import me.carda.awesome_notifications.notifications.NotificationScheduler;
import me.carda.awesome_notifications.notifications.exceptions.AwesomeNotificationException;
import me.carda.awesome_notifications.notifications.models.PushNotification;
import me.carda.awesome_notifications.notifications.enumerators.NotificationLifeCycle;
import me.carda.awesome_notifications.notifications.enumerators.NotificationSource;
import me.carda.awesome_notifications.notifications.NotificationSender;
import me.carda.awesome_notifications.Definitions;
import me.carda.awesome_notifications.notifications.models.returnedData.NotificationReceived;
import me.carda.awesome_notifications.utils.DateUtils;
import me.carda.awesome_notifications.utils.JsonUtils;
import me.carda.awesome_notifications.utils.ListUtils;
import me.carda.awesome_notifications.utils.MapUtils;
import me.carda.awesome_notifications.utils.StringUtils;
import me.carda.awesome_notifications_fcm.AwesomeNotificationsFcmPlugin;
import me.carda.awesome_notifications_fcm.BroadcastFCMSender;
import me.carda.awesome_notifications_fcm.FcmDefinitions;
import me.carda.awesome_notifications_fcm.exceptions.AwesomeNotificationFcmException;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static Context applicationContext;

    /** Background Dart execution context. */
    private static DartBackgroundExecutor dartBackgroundExecutor;

    private static NotificationLifeCycle initialLifeCycle = NotificationLifeCycle.AppKilled;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();
        DartBackgroundExecutor.setApplicationContext(applicationContext);
    }

    /// Called when a new token for the default Firebase project is generated.
    @Override
    public void onNewToken(String token) {
        BroadcastFCMSender.SendBroadcastNewFcmToken(applicationContext, token);
    }

    @Override
    public void onMessageSent(String s) {
        super.onMessageSent(s);
        Log.d(TAG, "onMessageSent: upstream message");
    }

    private void printIntentExtras(Intent intent){
        Bundle bundle;
        if ((bundle = intent.getExtras()) != null) {
            for (String key : bundle.keySet()) {
                System.out.println(key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL"));
            }
        }
    }

    @Override
    // Thank you Google, for that brilliant idea to treat notification message and notification data
    // differently on Android, depending of what app life cycle is. Because of that, all the developers
    // are doing "workarounds", using data to send push notifications, and that's not what you planned for.
    // Let the developers decide what to do on their apps and always deliver the notification
    // to "onMessageReceived" method. Its simple, is freedom and its what the creative ones need.
    public void handleIntent(Intent intent){

        initialLifeCycle = AwesomeNotificationsPlugin.getApplicationLifeCycle();

        String messageId = intent.getExtras().getString("google.message_id");
        boolean isSilentData
                  = StringUtils.isNullOrEmpty(
                        intent.getExtras().getString("gcm.notification.title"),
                        false
                    ) &&
                    StringUtils.isNullOrEmpty(
                        intent.getExtras().getString("gcm.notification.body"),
                        false
                    );

        if(!StringUtils.isNullOrEmpty(messageId)){

            if(AwesomeNotificationsFcmPlugin.debug)
                Log.d(TAG, "Received Firebase message id: "+messageId);

            intent.removeExtra("gcm.notification.e");
            intent.removeExtra("gcm.notification.title");
            intent.removeExtra("gcm.notification.body");
            intent.removeExtra("google.c.a.e");
            intent.removeExtra("collapse_key");

            intent.putExtra(FcmDefinitions.FIREBASE_FLAG_IS_SILENT_DATA, isSilentData);

            intent.putExtra("gcm.notification.mutable_content", true);
            intent.putExtra("gcm.notification.content_available", true);

            //printIntentExtras(intent);
        }

        super.handleIntent(intent);
    }

    ///
    ///  Called when message is received.
    ///
    ///  @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
    ///
    @Override
    public void onMessageReceived(@NonNull final RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        boolean isSilentData = remoteMessage.toIntent().getBooleanExtra(FcmDefinitions.FIREBASE_FLAG_IS_SILENT_DATA, true);
        if (remoteMessage.getData().size() == 0) return;

        try {
            // FCM frame is discarded, only data is processed
            Map<String, String> remoteData = remoteMessage.getData();

            Map<String, Object> parsedNotificationContent = extractNotificationData(Definitions.PUSH_NOTIFICATION_CONTENT, remoteData);
            if(MapUtils.isNullOrEmpty(parsedNotificationContent)){
                Log.d(TAG, "Invalid notification content");
                return;
            }

            Map<String, Object> parsedSchedule = extractNotificationData(Definitions.PUSH_NOTIFICATION_SCHEDULE, remoteData);
            List<Map<String, Object>> parsedActionButtons = extractNotificationDataList(Definitions.PUSH_NOTIFICATION_BUTTONS, remoteData);

            HashMap<String, Object> parsedRemoteMessage = new HashMap<>();
            parsedRemoteMessage.put(Definitions.PUSH_NOTIFICATION_CONTENT, parsedNotificationContent);

            if(!MapUtils.isNullOrEmpty(parsedSchedule))
                parsedRemoteMessage.put(Definitions.PUSH_NOTIFICATION_SCHEDULE, parsedSchedule);

            if(!ListUtils.isNullOrEmpty(parsedActionButtons))
                parsedRemoteMessage.put(Definitions.PUSH_NOTIFICATION_BUTTONS, parsedActionButtons);

            PushNotification pushNotification = new PushNotification().fromMap(parsedRemoteMessage);

            if (isSilentData)
                pushNotification.content.generateNextRandomId();

            pushNotification.validate(applicationContext);

            if (isSilentData)
                receiveSilentDataContent(pushNotification);
            else
                receiveNotificationContent(pushNotification);

        } catch (Exception e) {
            Log.d(TAG, "Invalid push notification content");
            e.printStackTrace();
        }
    }

    private void receiveSilentDataContent(PushNotification pushNotification) throws AwesomeNotificationException {

        if(AwesomeNotificationsPlugin.debug)
            io.flutter.Log.d(TAG, "Silent notification received");

        BroadcastFCMSender.SendBroadcastSilentData(applicationContext, initialLifeCycle, pushNotification);
    }

    private void receiveNotificationContent(PushNotification pushNotification) throws AwesomeNotificationException {

        if(AwesomeNotificationsPlugin.debug)
            io.flutter.Log.d(TAG, "Push notification received");

        if(pushNotification.schedule == null){

            NotificationSender.send(
                applicationContext,
                NotificationSource.Firebase,
                pushNotification);
        }
        else {

            NotificationScheduler.schedule(
                applicationContext,
                NotificationSource.Firebase,
                pushNotification);
        }
    }

    private Map<String, Object> extractNotificationData(String reference, Map<String, String> remoteData) throws AwesomeNotificationFcmException {
        String jsonData = remoteData.get(reference);
        Map<String, Object> notification = null;
        try {
            if (jsonData != null) {
                notification = JsonUtils.fromJson(jsonData);
            }
        } catch (Exception e) {
            throw new AwesomeNotificationFcmException("Invalid Firebase notification "+reference);
        }
        return notification;
    }

    private List<Map<String, Object>> extractNotificationDataList(String reference, Map<String, String> remoteData) throws AwesomeNotificationFcmException {
        String jsonData = remoteData.get(reference);
        List<Map<String, Object>> list = null;
        try {
            if (jsonData != null) {
                Type mapType = new TypeToken<List<Map<String, Object>>>(){}.getType();
                list = new Gson().fromJson(jsonData, mapType);
            }
        } catch (Exception e) {
            throw new AwesomeNotificationFcmException("Invalid Firebase notification "+reference);
        }
        return list;
    }
}