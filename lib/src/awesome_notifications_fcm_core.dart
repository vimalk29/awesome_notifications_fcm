import 'dart:async';
import 'dart:ui';

import 'package:awesome_notifications_fcm/src/silent_data/dart_callback_handle.dart';
import 'package:awesome_notifications_fcm/src/silent_data/silent_data_model.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

import 'package:awesome_notifications_fcm/src/definitions.dart';

import 'exceptions/exceptions.dart';

typedef Future<void> SilentDataHandler(SilentData silentData);

class AwesomeNotificationsFcm {

  /// STREAM CREATION METHODS *********************************************

  /// Streams are created so that app can respond to notification-related events since the plugin is initialised in the `main` function
  final StreamController<String> _tokenStreamController = StreamController<String>();

  static bool _isInitialized = false;
  static get isInitialized => _isInitialized;

  /// STREAM METHODS *********************************************

  /// Stream to capture all FCM token updates. Could be changed at any time.
  Stream<String> get fcmTokenStream {
    return _tokenStreamController.stream;
  }

  /// SINK METHODS *********************************************

  /// Sink to dispose the stream, if you don't need it anymore.
  Sink get fcmTokenSink {
    return _tokenStreamController.sink;
  }

  /// CLOSE STREAM METHODS *********************************************

  /// Closes definitely all the streams.
  dispose() {
    _tokenStreamController.close();
  }

  /// SINGLETON METHODS *********************************************

  final MethodChannel _channel;

  factory AwesomeNotificationsFcm() => _instance;

  @visibleForTesting
  AwesomeNotificationsFcm.private(MethodChannel channel) : _channel = channel;

  SilentDataHandler? _silentDataHandler;

  static final AwesomeNotificationsFcm _instance =
    AwesomeNotificationsFcm.private(const MethodChannel(CHANNEL_FLUTTER_PLUGIN));

  /// INITIALIZING METHODS *********************************************

  /// Initializes the plugin, creating a default icon and the initial channels. Only needs
  /// to be called at main.dart once.
  /// [debug]: enables the console log prints
  Future<bool> initialize({
    required SilentDataHandler onSilentDataHandle,
    bool debug = false
  }) async {
    WidgetsFlutterBinding.ensureInitialized();

    final dartCallbackReference = PluginUtilities.getCallbackHandle(dartCallbackHandle);
    final userCallbackReference = PluginUtilities.getCallbackHandle(onSilentDataHandle);

    _channel.setMethodCallHandler(_handleMethod);
    _isInitialized = await _channel.invokeMethod(CHANNEL_METHOD_INITIALIZE, {
      DEBUG_MODE: debug,
      DART_BG_HANDLE: dartCallbackReference!.toRawHandle(),
      SILENT_HANDLE: userCallbackReference?.toRawHandle()
    });

    if(userCallbackReference == null){
      print(
          'Callback onSilentDataHandle is not defined or is invalid.'
          '\nPlease, ensure to create a valid global method to handle it.');
    }

    return _isInitialized;
  }

  Future<dynamic> _handleMethod(MethodCall call) async {

    switch (call.method) {

      case CHANNEL_METHOD_NEW_FCM_TOKEN:
        final String token = call.arguments;
        _tokenStreamController.add(token);
        return;

      case CHANNEL_METHOD_SILENCED_CALLBACK:
        try {
            if(!await receiveSilentData((call.arguments as Map).cast<String, dynamic>())){
              throw AwesomeNotificationsFcmException('Silent data could not be recovered');
            }
        } on DartCallbackException {
          print('Fatal: could not find silent callback');
        } catch (e) {
          print("Awesome Notifications FCM: An error occurred in your silent data handler:");
          print(e);
        }
        return;

      default:
        throw UnsupportedError('Unrecognized JSON message');
    }
  }

  /// FIREBASE METHODS *********************************************

  /// Gets the firebase cloud messaging token
  Future<String> get firebaseAppToken async {
    final String token =
      await _channel.invokeMethod(CHANNEL_METHOD_GET_FCM_TOKEN);
    return token;
  }

  /// Check if firebase is fully available on the project
  Future<bool> get isFirebaseAvailable async {
    final bool isAvailable =
      await _channel.invokeMethod(CHANNEL_METHOD_IS_FCM_AVAILABLE);
    return isAvailable;
  }

  Future<void> subscribeToTopic(String topic) async {
    await _channel.invokeMethod(CHANNEL_METHOD_SUBSCRIBE_TOPIC, {
      NOTIFICATION_TOPIC: topic
    });
  }

  Future<void> unsubscribeToTopic(String topic) async {
    await _channel.invokeMethod(CHANNEL_METHOD_UNSUBSCRIBE_TOPIC, {
      NOTIFICATION_TOPIC: topic
    });
  }
}
