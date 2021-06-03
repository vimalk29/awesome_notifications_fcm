import 'dart:io';
import 'dart:ui';

import 'package:awesome_notifications/awesome_notifications.dart' hide CHANNEL_METHOD_INITIALIZE;
import 'package:awesome_notifications_fcm/awesome_notifications_fcm.dart';
import 'package:awesome_notifications_fcm/src/definitions.dart';
import 'package:awesome_notifications_fcm/src/exceptions/exceptions.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

Future<bool> receiveSilentData(Map<String, dynamic> arguments) async {

  final CallbackHandle silentCallbackHandle = CallbackHandle.fromRawHandle(arguments[SILENT_HANDLE]);

  // PluginUtilities.getCallbackFromHandle performs a lookup based on the
  // callback handle and returns a tear-off of the original callback.
  final SilentDataHandler? onSilentDataHandle = PluginUtilities.getCallbackFromHandle(silentCallbackHandle) as SilentDataHandler?;

  if (onSilentDataHandle == null) {
    throw DartCallbackException('could not find silent callback');
  }

  Map<String, dynamic> silentMap = Map<String, dynamic>.from(arguments[NOTIFICATION_SILENT_DATA]);
  final SilentData silentData = SilentData().fromMap(silentMap);
  await onSilentDataHandle(silentData);

  return true;
}

void dartCallbackHandle() {

  // Initialize state necessary for MethodChannels.
  WidgetsFlutterBinding.ensureInitialized();

  const MethodChannel _channel = MethodChannel(DART_REVERSE_CHANNEL);

  // This is where we handle background events from the native portion of the plugin.
  _channel.setMethodCallHandler((MethodCall call) async {

    switch (call.method) {

      case CHANNEL_METHOD_SILENCED_CALLBACK:
        try {
          if (!await receiveSilentData(
              (call.arguments as Map).cast<String, dynamic>())) {
            throw AwesomeNotificationsFcmException(
                'Silent data could not be recovered');
          }
        } on DartCallbackException {
          print('Fatal: could not find silent callback');
          exit(-1);
        } catch (e) {
          print(
              "Awesome Notifications FCM: An error occurred in your background messaging handler:");
          print(e);
        }
        break;

      case CHANNEL_METHOD_SHUTDOWN_DART:
        exit(0);

      default:
        throw UnimplementedError("${call.method} has not been implemented");
    }
  });

  // Once we've finished initializing, let the native portion of the plugin
  // know that it can start scheduling alarms.
  _channel.invokeMethod<void>(CHANNEL_METHOD_INITIALIZE);
}