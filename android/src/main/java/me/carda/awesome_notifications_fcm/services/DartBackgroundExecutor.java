package me.carda.awesome_notifications_fcm.services;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import android.content.res.AssetManager;
import android.os.Looper;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterShellArgs;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.embedding.engine.dart.DartExecutor.DartCallback;
import io.flutter.embedding.engine.plugins.shim.ShimPluginRegistry;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.view.FlutterCallbackInformation;
import me.carda.awesome_notifications.notifications.NotificationBuilder;
import me.carda.awesome_notifications.notifications.models.returnedData.ActionReceived;
import me.carda.awesome_notifications_fcm.AwesomeNotificationsFcmPlugin;
import me.carda.awesome_notifications_fcm.FcmDefinitions;
import me.carda.awesome_notifications_fcm.managers.FcmDefaultsManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An background execution abstraction which handles initializing a background isolate running a
 * callback dispatcher, used to invoke Dart callbacks while backgrounded.
 */
public class DartBackgroundExecutor implements MethodCallHandler {
    private static final String TAG = "DartBackgroundExec";

    public static final NotificationBuilder notificationBuilder = new NotificationBuilder();

    public static Context applicationContext;

    private static io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback
            pluginRegistrantCallback;

    private final AtomicBoolean isCallbackDispatcherReady = new AtomicBoolean(false);
    /**
     * The {@link MethodChannel} that connects the Android side of this plugin with the background
     * Dart isolate that was created by this plugin.
     */
    private MethodChannel backgroundChannel;

    private FlutterEngine backgroundFlutterEngine;

    final Handler mainHandler = new Handler(Looper.getMainLooper());
    Runnable myRunnable;

    /**
     * Sets the {@code io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback} used to
     * register plugins with the newly spawned isolate.
     *
     * <p>Note: this is only necessary for applications using the V1 engine embedding API as plugins
     * are automatically registered via reflection in the V2 engine embedding API. If not set,
     * background message callbacks will not be able to utilize functionality from other plugins.
     */
    public static void setPluginRegistrant(
            io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback callback) {
        pluginRegistrantCallback = callback;
    }

    public static long getSilentCallbackDispatcher(Context context){
        return FcmDefaultsManager.getSilentCallbackDispatcher(context);
    }

    public static long getDartCallbackDispatcher(Context context){
        return FcmDefaultsManager.getDartCallbackDispatcher(context);
    }

    public static void setApplicationContext(Context context) {
        applicationContext = context.getApplicationContext();
    }

    /**
     * Returns true when the background isolate has started and is ready to handle background
     * messages.
     */
    public boolean isNotRunning() {
        return !isCallbackDispatcherReady.get();
    }

    private void startExecutions() {
        isCallbackDispatcherReady.set(true);
        DartBackgroundService.startExecutions();
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        String method = call.method;
        try {
            if (method.equals(FcmDefinitions.CHANNEL_METHOD_INITIALIZE)) {
                // This message is sent by the background method channel as soon as the background isolate
                // is running. From this point forward, the Android side of this plugin can send
                // callback handles through the background method channel, and the Dart side will execute
                // the Dart methods corresponding to those callback handles.
                startExecutions();
                result.success(true);
            } else {
                result.notImplemented();
            }
        } catch (Exception e) {
            result.error("error", "Dart background error: " + e.getMessage(), null);
        }
    }

    /**
     * Starts running a background Dart isolate within a new {@link FlutterEngine} using a previously
     * used entrypoint.
     */
    public void startBackgroundIsolate() {
        if (isNotRunning()) {
            long dartCallbackHandle = getDartCallbackDispatcher(applicationContext);
            if (dartCallbackHandle != 0) {
                startBackgroundIsolate(dartCallbackHandle, null);
            }
        }
    }

    /**
     * Starts running a background Dart isolate within a new {@link FlutterEngine} using a previously
     * used entrypoint.
     */
    public void closeBackgroundIsolate(Runnable myRunnable) {
        if (!isNotRunning()) {
            isCallbackDispatcherReady.set(false);
        }
        mainHandler.removeCallbacks(myRunnable);
        backgroundFlutterEngine.destroy();
        backgroundFlutterEngine = null;
    }

    /**
     * Starts running a background Dart isolate within a new {@link FlutterEngine}.
     *
     * <p>The isolate is configured as follows:
     *
     * <ul>
     *   <li>Bundle Path: {@code io.flutter.view.FlutterMain.findAppBundlePath(context)}.
     *   <li>Entrypoint: The Dart method represented by {@code callbackHandle}.
     *   <li>Run args: none.
     * </ul>
     *
     * <p>Preconditions:
     *
     * <ul>
     *   <li>The given {@code callbackHandle} must correspond to a registered Dart callback. If the
     *       handle does not resolve to a Dart callback then this method does nothing.
     *   <li>A static {@link #pluginRegistrantCallback} must exist, otherwise a {@link
     *       Exception} will be thrown.
     * </ul>
     */
    public void startBackgroundIsolate(final long callbackHandle, final FlutterShellArgs shellArgs) {
        if (backgroundFlutterEngine != null) {
            Log.e(TAG, "Background isolate already started.");
            return;
        }

        myRunnable =
            new Runnable() {
                @Override
                public void run() {
                    io.flutter.view.FlutterMain.startInitialization(applicationContext);
                    io.flutter.view.FlutterMain.ensureInitializationCompleteAsync(
                            applicationContext,
                            null,
                            mainHandler,
                            new Runnable() {
                                @Override
                                public void run() {
                                    String appBundlePath = io.flutter.view.FlutterMain.findAppBundlePath();
                                    AssetManager assets = applicationContext.getAssets();
                                    if (isNotRunning()) {
                                        if (shellArgs != null) {
                                            Log.i(
                                                    TAG,
                                                    "Creating background FlutterEngine instance, with args: "
                                                            + Arrays.toString(shellArgs.toArray()));
                                            backgroundFlutterEngine =
                                                    new FlutterEngine(
                                                            applicationContext, shellArgs.toArray());
                                        } else {
                                            Log.i(TAG, "Creating background FlutterEngine instance.");
                                            backgroundFlutterEngine =
                                                    new FlutterEngine(applicationContext);
                                        }

                                        // We need to create an instance of `FlutterEngine` before looking up the
                                        // callback. If we don't, the callback cache won't be initialized and the
                                        // lookup will fail.
                                        FlutterCallbackInformation flutterCallback =
                                                FlutterCallbackInformation.lookupCallbackInformation(callbackHandle);

                                        DartExecutor executor = backgroundFlutterEngine.getDartExecutor();
                                        initializeMethodChannel(executor);
                                        DartCallback dartCallback =
                                                new DartCallback(assets, appBundlePath, flutterCallback);

                                        executor.executeDartCallback(dartCallback);

                                        // The pluginRegistrantCallback should only be set in the V1 embedding as
                                        // plugin registration is done via reflection in the V2 embedding.
                                        if (pluginRegistrantCallback != null) {
                                            pluginRegistrantCallback.registerWith(
                                                    new ShimPluginRegistry(backgroundFlutterEngine));
                                        }
                                    }
                                }
                            }
                    );
                }
            };

        mainHandler.post(myRunnable);
    }

    private void shutdownDart(){
        closeBackgroundIsolate(myRunnable);
        backgroundChannel.invokeMethod(
                FcmDefinitions.CHANNEL_METHOD_SHUTDOWN_DART, null);
    }

    boolean isDartBackgroundHandlerRegistered() {
        return getSilentCallbackDispatcher(applicationContext) != 0L;
    }

    /**
     * Executes the desired Dart callback in a background Dart isolate.
     *
     * <p>The given {@code intent} should contain a {@code long} extra called "callbackHandle", which
     * corresponds to a callback registered with the Dart VM.
     */
    public void executeDartCallbackInBackgroundIsolate(Intent intent, final CountDownLatch latch) {
        if (backgroundFlutterEngine == null) {
            Log.i(
                TAG,
                "A background message could not be handled since dart callback handler has not been registered.");
            return;
        }

        Result result;
        if (latch != null) {
            result =
                    new Result() {
                        @Override
                        public void success(Object result) {
                            Long intentRemains = latch.getCount() - 1;
                            if(intentRemains == 0) {
                                if(AwesomeNotificationsFcmPlugin.debug)
                                    Log.e(TAG, "All silent intents fetched.");
                                shutdownDart();
                            }
                            else
                                if(AwesomeNotificationsFcmPlugin.debug)
                                    Log.e(TAG, "Remaining "+intentRemains.toString()+" silent intents to finish");

                            // If another thread is waiting, then wake that thread when the callback returns a result.
                            latch.countDown();
                        }

                        @Override
                        public void error(String errorCode, String errorMessage, Object errorDetails) {
                            Long intentRemains = latch.getCount() - 1;
                            if(intentRemains == 0) {
                                if (AwesomeNotificationsFcmPlugin.debug)
                                    Log.e(TAG, "All silent intents fetched.");
                                shutdownDart();
                            }

                            // If another thread is waiting, then wake that thread when the callback returns a result.
                            latch.countDown();
                        }

                        @Override
                        public void notImplemented() {
                            Long intentRemains = latch.getCount() - 1;
                            if(intentRemains == 0) {
                                if (AwesomeNotificationsFcmPlugin.debug)
                                    Log.e(TAG, "All silent intents fetched.");
                                shutdownDart();
                            }

                            // If another thread is waiting, then wake that thread when the callback returns a result.
                            latch.countDown();
                        }
                    };
        } else {
            result =
                    new Result() {
                        @Override
                        public void success(Object result) {
                            shutdownDart();
                        }

                        @Override
                        public void error(String errorCode, String errorMessage, Object errorDetails) {
                            shutdownDart();
                        }

                        @Override
                        public void notImplemented() {
                            shutdownDart();
                        }
                    };
        }

        // Handle the message event in Dart.
        ActionReceived actionReceived = NotificationBuilder.buildNotificationActionFromIntent(applicationContext, intent, true);
        if (actionReceived != null) {

            actionReceived.actionDate = me.carda.awesome_notifications.utils.DateUtils.getUTCDate();
            actionReceived.actionLifeCycle = me.carda.awesome_notifications.AwesomeNotificationsPlugin.getApplicationLifeCycle();

            actionReceived.createdSource = me.carda.awesome_notifications.notifications.enumerators.NotificationSource.Firebase;
            actionReceived.displayedDate = actionReceived.createdDate;
            actionReceived.displayedLifeCycle = actionReceived.createdLifeCycle;

            final Map<String, Object> actionData = actionReceived.toMap();

            backgroundChannel.invokeMethod(
                    FcmDefinitions.CHANNEL_METHOD_SILENCED_CALLBACK,
                    new HashMap<String, Object>() {
                        {
                            put(FcmDefinitions.SILENT_HANDLE, getSilentCallbackDispatcher(applicationContext));
                            put(FcmDefinitions.NOTIFICATION_SILENT_DATA, actionData);
                            put(FcmDefinitions.REMAINING_SILENT_DATA, latch == null ? 0 : latch.getCount() - 1);
                        }
                    },
                    result);
        } else {
            Log.e(TAG, "ActionReceived not found in Intent background.");
        }
    }

    private void initializeMethodChannel(BinaryMessenger isolate) {
        backgroundChannel = new MethodChannel(isolate, FcmDefinitions.DART_REVERSE_CHANNEL);
        backgroundChannel.setMethodCallHandler(this);
    }
}