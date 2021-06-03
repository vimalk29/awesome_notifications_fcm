package me.carda.awesome_notifications_fcm.managers;

import android.content.Context;

import me.carda.awesome_notifications_fcm.AwesomeNotificationsFcmPlugin;
import me.carda.awesome_notifications_fcm.FcmDefinitions;
import me.carda.awesome_notifications_fcm.models.FcmDefaultsModel;
import me.carda.awesome_notifications.notifications.managers.SharedManager;

public class FcmDefaultsManager {

    private static final SharedManager<FcmDefaultsModel> shared = new SharedManager<>("FcmDefaultsManager", FcmDefaultsModel.class);

    public static Boolean removeDefault(Context context) {
        return shared.remove(context, FcmDefinitions.SHARED_FCM_DEFAULTS, "Defaults");
    }

    public static void saveDefault(Context context, FcmDefaultsModel defaults) {
        shared.set(context, FcmDefinitions.SHARED_FCM_DEFAULTS, "Defaults", defaults);
    }

    public static FcmDefaultsModel getDefaultByKey(Context context){
        return shared.get(context, FcmDefinitions.SHARED_FCM_DEFAULTS, "Defaults");
    }

    public static long getSilentCallbackDispatcher(Context context) {
        FcmDefaultsModel defaults = shared.get(context, FcmDefinitions.SHARED_FCM_DEFAULTS, "Defaults");
        return (defaults != null) ? defaults.silentDataCallback : 0L;
    }

    public static long getDartCallbackDispatcher(Context context) {
        FcmDefaultsModel defaults = shared.get(context, FcmDefinitions.SHARED_FCM_DEFAULTS, "Defaults");
        return (defaults != null) ? defaults.reverseDartCallback : 0L;
    }

    public static void commitChanges(Context context){
        shared.commit(context);
    }
}
