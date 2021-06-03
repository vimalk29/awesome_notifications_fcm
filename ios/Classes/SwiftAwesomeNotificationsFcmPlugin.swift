import Flutter
import UIKit

public class SwiftAwesomeNotificationsFcmPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "awesome_notifications_fcm", binaryMessenger: registrar.messenger())
    let instance = SwiftAwesomeNotificationsFcmPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    result("iOS " + UIDevice.current.systemVersion)
  }
}
