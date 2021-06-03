import 'package:awesome_notifications_fcm_example/routes.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';

import 'package:awesome_notifications/awesome_notifications.dart';
import 'package:awesome_notifications_fcm/awesome_notifications_fcm.dart';
import 'package:fluttertoast/fluttertoast.dart';

main() async {
  await initializeAllNotificationsPlugins();
  runApp(MyApp());
}

Future<void> mySilentDataHandle(SilentData silentData) async {
  Fluttertoast.showToast(
      msg: 'Silent data received',
      backgroundColor: Colors.blueAccent,
      textColor: Colors.white,
      fontSize: 16
  );
  debugPrint('"SilentData": ${silentData.toString()}');
}

Future<void> initializeAllNotificationsPlugins() async {
  await AwesomeNotifications().initialize(
      'resource://drawable/res_app_icon',
      [
        NotificationChannel(
            channelKey: 'alerts',
            channelName: 'Alerts',
            channelDescription: 'Notification alerts',
            importance: NotificationImportance.High,
            defaultColor: Color(0xFF9D50DD),
            ledColor: Colors.white,
            groupKey: 'alerts',
            channelShowBadge: true
        )
      ],
      debug: true
  );

  await Firebase.initializeApp();
  await AwesomeNotificationsFcm().initialize(
      onSilentDataHandle: mySilentDataHandle,
      debug: true
  );
}

class MyApp extends StatefulWidget {

  static final Color mainColor = Color(0xFF9D50DD);

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    AwesomeNotifications().dispose();
    AwesomeNotificationsFcm().dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Awesome Notifications FCM',
      color: MyApp.mainColor,
      theme: ThemeData(
        primaryColor: MyApp.mainColor,
        appBarTheme: AppBarTheme(
          brightness: Brightness.light,
          backgroundColor: Colors.white,
          iconTheme: IconThemeData(
            color: MyApp.mainColor
          ),
          textTheme: TextTheme(
            headline6: TextStyle(
              color: MyApp.mainColor,
              fontWeight: FontWeight.bold
            )
          ),
        )
      ),
      initialRoute: PAGE_HOME,
      routes: materialRoutes
    );
  }
}
