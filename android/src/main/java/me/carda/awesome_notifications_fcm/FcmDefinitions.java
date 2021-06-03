package me.carda.awesome_notifications_fcm;

public interface FcmDefinitions {

    String DART_REVERSE_CHANNEL = "awesomeNotificationsFCMReverse";

    String BROADCAST_FCM_TOKEN = "me.carda.awesome_notifications_fcm.services.firebase.TOKEN";
    String BROADCAST_SILENT_DATA = "me.carda.awesome_notifications_fcm.services.silentData";

    String SHARED_FCM_DEFAULTS = "fcmDefaults";

    String FIREBASE_FLAG_IS_SILENT_DATA = "isSilentData";
    String FIREBASE_ENABLED = "FIREBASE_ENABLED";

    String EXTRA_BROADCAST_FCM_TOKEN = "token";
    String EXTRA_SILENT_DATA = "silentData";

    String DEBUG_MODE = "debug";
    String SILENT_HANDLE = "awesomeSilentHandle";
    String DART_BG_HANDLE = "awesomeDartBGHandle";

    String NOTIFICATION_TOPIC = "topic";

    String REMAINING_SILENT_DATA = "remainingSilentData";
    String NOTIFICATION_SILENT_DATA = "notificationSilentData";

    String CHANNEL_METHOD_INITIALIZE = "initialize";
    String CHANNEL_METHOD_GET_FCM_TOKEN = "getFirebaseToken";
    String CHANNEL_METHOD_NEW_FCM_TOKEN = "newTokenReceived";
    String CHANNEL_METHOD_IS_FCM_AVAILABLE = "isFirebaseAvailable";
    String CHANNEL_METHOD_SUBSCRIBE_TOPIC = "subscribeTopic";
    String CHANNEL_METHOD_UNSUBSCRIBE_TOPIC = "unsubscribeTopic";
    String CHANNEL_METHOD_SILENCED_CALLBACK = "silentCallbackReference";
    String CHANNEL_METHOD_DART_CALLBACK = "dartCallbackReference";
    String CHANNEL_METHOD_SHUTDOWN_DART = "shutdown";
}
