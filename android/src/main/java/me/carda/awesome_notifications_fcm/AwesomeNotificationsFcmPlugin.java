package me.carda.awesome_notifications_fcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import me.carda.awesome_notifications.AwesomeNotificationsPlugin;
import me.carda.awesome_notifications.notifications.NotificationBuilder;
import me.carda.awesome_notifications.notifications.models.returnedData.ActionReceived;
import me.carda.awesome_notifications.utils.DateUtils;
import me.carda.awesome_notifications.utils.MapUtils;
import me.carda.awesome_notifications_fcm.exceptions.AwesomeNotificationFcmException;
import me.carda.awesome_notifications_fcm.managers.FcmDefaultsManager;
import me.carda.awesome_notifications_fcm.models.FcmDefaultsModel;
import me.carda.awesome_notifications_fcm.services.DartBackgroundExecutor;
import me.carda.awesome_notifications_fcm.services.DartBackgroundService;

/**
 * AwesomeNotificationsFcmPlugin
 */
public class AwesomeNotificationsFcmPlugin extends BroadcastReceiver
        implements
            FlutterPlugin,
            MethodCallHandler {

    private static MethodChannel pluginChannel;

    private static final String TAG = "AwesomeNotificationsFcmPlugin";

    public static boolean debug = false;
    public static boolean firebaseEnabled = false;
    private static Context applicationContext;

    public static boolean isInitialized = false;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {

        AttachAwesomeNotificationsFCMPlugin(
            flutterPluginBinding.getApplicationContext(),
            new MethodChannel(
                flutterPluginBinding.getBinaryMessenger(),
                "awesome_notifications_fcm"
            )
        );
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        detachAwesomeNotificationsFCMPlugin(
                binding.getApplicationContext());
    }

    private void AttachAwesomeNotificationsFCMPlugin(Context context, MethodChannel channel) {

        applicationContext = context;

        pluginChannel = channel;
        pluginChannel.setMethodCallHandler(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FcmDefinitions.BROADCAST_FCM_TOKEN);
        intentFilter.addAction(FcmDefinitions.BROADCAST_SILENT_DATA);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
        manager.registerReceiver(this, intentFilter);

        try {
            enableFirebase(context);
        } catch (AwesomeNotificationFcmException e) {
            firebaseEnabled = false;
            e.printStackTrace();
        }

        if (AwesomeNotificationsPlugin.debug)
            Log.d(TAG, "Awesome Notifications FCM attached for Android " + Build.VERSION.SDK_INT);

    }

    private void detachAwesomeNotificationsFCMPlugin(Context context) {

        applicationContext = null;

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
        manager.unregisterReceiver(this);

        if (AwesomeNotificationsPlugin.debug)
            Log.d(TAG, "Awesome Notifications FCM detached from Android " + Build.VERSION.SDK_INT);
    }

    private void enableFirebase(Context context) throws AwesomeNotificationFcmException {

        if (AwesomeNotificationsFcmPlugin.debug)
            Log.d(TAG, "Enabling firebase messaging...");

        FirebaseApp firebaseApp = FirebaseApp.initializeApp(context);
        firebaseEnabled = firebaseApp != null;

        if (AwesomeNotificationsFcmPlugin.debug)
            Log.d(TAG, "Firebase "+(firebaseEnabled ? "enabled" : "not enabled"));

        if (!firebaseEnabled)
            throw new AwesomeNotificationFcmException("Firebase not enabled.");
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

        try {

            switch (call.method) {

                case FcmDefinitions.CHANNEL_METHOD_INITIALIZE:
                    channelMethodInitialize(call, result);
                    return;

                case FcmDefinitions.CHANNEL_METHOD_GET_FCM_TOKEN:
                    channelMethodGetFcmToken(call, result);
                    return;

                case FcmDefinitions.CHANNEL_METHOD_IS_FCM_AVAILABLE:
                    channelMethodIsFcmAvailable(call, result);
                    return;

                case FcmDefinitions.CHANNEL_METHOD_SUBSCRIBE_TOPIC:
                    channelMethodSubscribeToTopic(call, result);
                    return;

                case FcmDefinitions.CHANNEL_METHOD_UNSUBSCRIBE_TOPIC:
                    channelMethodUnsubscribeFromTopic(call, result);
                    return;

                default:
                    result.notImplemented();
            }

        } catch (Exception e) {
            if (AwesomeNotificationsFcmPlugin.debug)
                Log.d(TAG, String.format("%s", e.getMessage()));

            result.error(call.method, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void channelMethodInitialize(MethodCall call, Result result) throws AwesomeNotificationFcmException {

        if(isInitialized) {
            result.success(true);
            return;
        }

        Map<String, Object> platformParameters = call.arguments();
        if(platformParameters == null){
            throw new AwesomeNotificationFcmException("Invalid parameters on initialization");
        }

        Object callbackSilentObj = platformParameters.get(FcmDefinitions.SILENT_HANDLE);
        Object callbackDartObj = platformParameters.get(FcmDefinitions.DART_BG_HANDLE);
        Object object = platformParameters.get(FcmDefinitions.DEBUG_MODE);

        debug = object != null && (boolean) object;
        long silentCallback = callbackSilentObj == null ? 0L : (Long) callbackSilentObj;
        long dartCallback = callbackDartObj == null ? 0L :(Long) callbackDartObj;

        DartBackgroundExecutor.setApplicationContext(applicationContext);
        FcmDefaultsManager.saveDefault(applicationContext, new FcmDefaultsModel(
            firebaseEnabled, dartCallback, silentCallback
        ));

        isInitialized = true;
        result.success(firebaseEnabled);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        AwesomeNotificationsPlugin.getApplicationLifeCycle();

        String action = intent.getAction();
        switch (action) {
            case FcmDefinitions.BROADCAST_FCM_TOKEN:
                onBroadcastNewFcmToken(intent);
                break;

            case FcmDefinitions.BROADCAST_SILENT_DATA:
                onBroadcastForegroundSilentData(intent);
                break;
        }
    }

    private void onBroadcastNewFcmToken(Intent intent) {
        String token = intent.getStringExtra(FcmDefinitions.EXTRA_BROADCAST_FCM_TOKEN);
        pluginChannel.invokeMethod(FcmDefinitions.CHANNEL_METHOD_NEW_FCM_TOKEN, token);
    }

    private void onBroadcastForegroundSilentData(Intent intent) {
        ActionReceived actionReceived = NotificationBuilder.buildNotificationActionFromIntent(applicationContext, intent, true);

        if (actionReceived != null) {

            actionReceived.actionDate = DateUtils.getUTCDate();
            actionReceived.actionLifeCycle = AwesomeNotificationsPlugin.getApplicationLifeCycle();
            actionReceived.createdSource = me.carda.awesome_notifications.notifications.enumerators.NotificationSource.Firebase;
            actionReceived.displayedDate = actionReceived.createdDate;
            actionReceived.displayedLifeCycle = actionReceived.createdLifeCycle;

            final Map<String, Object> actionData = actionReceived.toMap();

            pluginChannel.invokeMethod(
                FcmDefinitions.CHANNEL_METHOD_SILENCED_CALLBACK,
                new HashMap<String, Object>() {
                    {
                        put(FcmDefinitions.SILENT_HANDLE, DartBackgroundExecutor.getSilentCallbackDispatcher(applicationContext));
                        put(FcmDefinitions.NOTIFICATION_SILENT_DATA, actionData);
                    }
                });
        }
    }

    private void channelMethodSubscribeToTopic(MethodCall call, Result result) throws AwesomeNotificationFcmException {
        String topicReference = null;

        if (!firebaseEnabled) {
            throw new AwesomeNotificationFcmException("Firebase service not available (check if you have google-services.json file)");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = MapUtils.extractArgument(call.arguments(), Map.class).orNull();
        if (data != null)
            topicReference = (String) data.get(FcmDefinitions.NOTIFICATION_TOPIC);

        if (topicReference == null)
            throw new AwesomeNotificationFcmException("topic name is required");

        FirebaseMessaging
                .getInstance().subscribeToTopic(topicReference);

        result.success(true);
    }

    private void channelMethodUnsubscribeFromTopic(MethodCall call, Result result) throws AwesomeNotificationFcmException {
        String topicReference = null;

        if (!firebaseEnabled) {
            throw new AwesomeNotificationFcmException("Firebase service not available (check if you have google-services.json file)");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = MapUtils.extractArgument(call.arguments(), Map.class).orNull();
        if (data != null)
            topicReference = (String) data.get(FcmDefinitions.NOTIFICATION_TOPIC);

        if (topicReference == null)
            throw new AwesomeNotificationFcmException("topic name is required");

        FirebaseMessaging
                .getInstance().subscribeToTopic(topicReference);

        result.success(true);
    }

    private void channelMethodIsFcmAvailable(MethodCall call, Result result) throws AwesomeNotificationFcmException {
        try {
            result.success(firebaseEnabled);
        } catch (Exception e) {
            Log.w(TAG, "FCM could not enabled for this project.", e);
            result.success(false);
        }
    }

    private void channelMethodGetFcmToken(MethodCall call, final Result result) throws AwesomeNotificationFcmException {

        if (!firebaseEnabled) {
            throw new AwesomeNotificationFcmException("Firebase service not available (check if you have google-services.json file)");
        }

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {

                        if (!task.isSuccessful()) {
                            Exception exception = task.getException();
                            Log.w(TAG, "Fetching FCM registration token failed", exception);
                            result.error(exception.getMessage(), "Fetching FCM registration token failed", exception);
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();
                        result.success(token);
                    }
                });
    }
}
