package me.carda.awesome_notifications_fcm.services;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import io.flutter.embedding.engine.FlutterShellArgs;

public class DartBackgroundService extends JobIntentService {
    private static final String TAG = "DartBackgroundService";

    private static final List<Intent> intentQueue =
            Collections.synchronizedList(new LinkedList<Intent>());

    /** Background Dart execution context. */
    public static DartBackgroundExecutor dartBackgroundExecutor;
    public static Context applicationContext;

    /**
     * Enqueue the silent data to be future handle by the {@link DartBackgroundExecutor}.
     */
    public static void enqueueSilentDataProcessing(Context context, Intent intent) {
        applicationContext = context;
        if(DartBackgroundExecutor.getSilentCallbackDispatcher(applicationContext) == 0L){
            Log.e(TAG, "There is no valid callback to handle silent data.");
            return;
        }

        enqueueWork(
            context,
            DartBackgroundService.class,
            42,
            intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (dartBackgroundExecutor == null) {
            dartBackgroundExecutor = new DartBackgroundExecutor();
        }
        dartBackgroundExecutor.startBackgroundIsolate();
    }

    /**
     * Executes a Dart callback, as specified within the incoming {@code intent}.
     *
     * <p>Invoked by our {@link JobIntentService} superclass after a call to {@link
     * JobIntentService#enqueueWork(Context, Class, int, Intent);}.
     *
     * <p>If there are no pre-existing callback execution requests, other than the incoming {@code
     * intent}, then the desired Dart callback is invoked immediately.
     *
     * <p>If there are any pre-existing callback requests that have yet to be executed, the incoming
     * {@code intent} is added to the {@link #intentQueue} to be invoked later, after all
     * pre-existing callbacks have been executed.
     */
    @Override
    protected void onHandleWork(@NonNull final Intent intent) {
        if (!dartBackgroundExecutor.isDartBackgroundHandlerRegistered()) {
            Log.w(
                TAG,
                "A background message could not be handled in Dart" +
                        " because there is no onSilentData handler registered.");
            return;
        }

        // If we're in the middle of processing queued messages, add the incoming
        // intent to the queue and return.
        synchronized (intentQueue) {
            if (dartBackgroundExecutor.isNotRunning()) {
                Log.i(TAG, "Service has not yet started, intent will be queued.");
                intentQueue.add(intent);
                return;
            }
        }

        // There were no pre-existing callback requests. Execute the callback
        // specified by the incoming intent.
        final CountDownLatch latch = new CountDownLatch(1);
        new Handler(getMainLooper())
                .post(new Runnable() {
                    @Override
                    public void run() {
                        dartBackgroundExecutor.executeDartCallbackInBackgroundIsolate(intent, latch);
                    }
                });

        try {
            latch.await();
        } catch (InterruptedException ex) {
            Log.i(TAG, "Exception waiting to execute Dart callback", ex);
        }
    }

    /**
     * Starts the background isolate for the {@link FlutterFirebaseMessagingBackgroundService}.
     *
     * <p>Preconditions:
     *
     * <ul>
     *   <li>The given {@code callbackHandle} must correspond to a registered Dart callback. If the
     *       handle does not resolve to a Dart callback then this method does nothing.
     *   <li>A static {@link #pluginRegistrantCallback} must exist, otherwise a {@link
     *       PluginRegistrantException} will be thrown.
     * </ul>
     */
    @SuppressWarnings("JavadocReference")
    public static void startBackgroundIsolate(long callbackHandle, FlutterShellArgs shellArgs) {
        if (dartBackgroundExecutor != null) {
            Log.w(TAG, "Attempted to start a duplicate background isolate. Returning...");
            return;
        }
        dartBackgroundExecutor = new DartBackgroundExecutor();
        dartBackgroundExecutor.startBackgroundIsolate(callbackHandle, shellArgs);
    }

    /**
     * Called once the Dart isolate ({@code flutterBackgroundExecutor}) has finished initializing.
     *
     * <p>Invoked by AwesomeNotificationsFcm initialization when it receives the {@code
     * FirebaseMessaging.initialized} message. Processes all messaging events that came in while the
     * isolate was starting.
     */
    /* package */
    static void startExecutions() {
        Log.i(TAG, "Silent background executions ready to go");
        synchronized (intentQueue) {
            // Handle all the message events received before the Dart isolate was
            // initialized, then clear the queue.
            for (Intent intent : intentQueue) {
                dartBackgroundExecutor.executeDartCallbackInBackgroundIsolate(intent, null);
            }
            intentQueue.clear();
        }
    }

    /**
     * Sets the {@link io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback} used to
     * register the plugins used by an application with the newly spawned background isolate.
     *
     * <p>This should be invoked in {@link MainApplication.onCreate} with {@link
     * GeneratedPluginRegistrant} in applications using the V1 embedding API in order to use other
     * plugins in the background isolate. For applications using the V2 embedding API, it is not
     * necessary to set a {@link io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback} as
     * plugins are registered automatically.
     */
    @SuppressWarnings({"deprecation", "JavadocReference"})
    public static void setPluginRegistrant(
            io.flutter.plugin.common.PluginRegistry.PluginRegistrantCallback callback) {
        // Indirectly set in FlutterFirebaseMessagingBackgroundExecutor for backwards compatibility.
        DartBackgroundExecutor.setPluginRegistrant(callback);
    }
}