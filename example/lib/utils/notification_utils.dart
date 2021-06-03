import 'package:flutter/material.dart' hide DateUtils;

import 'package:awesome_notifications/awesome_notifications.dart';
import 'package:awesome_notifications_fcm_example/routes.dart';

/* *********************************************
    LOCAL NOTIFICATIONS CREATION
************************************************ */

Future<bool> createLocalNotification({
  required BuildContext context,
  required int id,
  required String channelKey,
  String? largeIconUrl,
  String? bigPictureUrl,
  DateTime? dateTime
}) async {

  bool isAllowed = await requireUserNotificationPermissions(context, channelKey: channelKey);
  if(!isAllowed) return false;

  return AwesomeNotifications().createNotification(
      content: NotificationContent(
        id: id,
        channelKey: channelKey,
        title: 'Local alert',
        body:
          'This notification was created locally on '+
          DateUtils.parseDateToString(DateTime.now())!+
           (
              dateTime == null ? '' : (
                ' to be displayed at '+
                DateUtils.parseDateToString(dateTime)!
              )
           )
      ),
      actionButtons: [
        NotificationActionButton(
          key: 'ACCEPT',
          label: 'Accept'
        ),
        NotificationActionButton(
            key: 'DENY',
            label: 'Deny'
        )
      ],
      schedule:
        dateTime == null ? null :
        NotificationCalendar.fromDate(date: dateTime)
  );
}

/* *********************************************
    LIST SCHEDULED NOTIFICATIONS
************************************************ */

Future<void> listScheduledNotifications(BuildContext context) async {
  List<PushNotification> activeSchedules =
  await AwesomeNotifications().listScheduledNotifications();
  for (PushNotification schedule in activeSchedules) {
    debugPrint(
        'pending notification: ['
            'id: ${schedule.content!.id}, '
            'title: ${schedule.content!.titleWithoutHtml}, '
            'schedule: ${schedule.schedule.toString()}'
            ']');
  }
  return showDialog<void>(
    context: context,
    builder: (BuildContext context) {
      return AlertDialog(
        content: Text('${activeSchedules.length} schedules founded'),
        actions: [
          TextButton(
            child: Text('OK'),
            onPressed: () {
              Navigator.of(context).pop();
            },
          ),
        ],
      );
    },
  );
}

/* *********************************************
    TIME ZONE METHODS
************************************************ */

Future<String> getCurrentTimeZone(){
  return AwesomeNotifications().getLocalTimeZoneIdentifier();
}

Future<String> getUtcTimeZone(){
  return AwesomeNotifications().getUtcTimeZoneIdentifier();
}

/* *********************************************
    BADGE NOTIFICATIONS
************************************************ */

Future<int> getBadgeIndicator() async {
  int amount = await AwesomeNotifications().getGlobalBadgeCounter();
  return amount;
}

Future<void> setBadgeIndicator(int amount) async {
  await AwesomeNotifications().setGlobalBadgeCounter(amount);
}

Future<void> resetBadgeIndicator() async {
  await AwesomeNotifications().resetGlobalBadge();
}

/* *********************************************
    CANCEL METHODS
************************************************ */

void cancelNotification(int id){
  AwesomeNotifications().cancel(id);
}

void dismissAllNotifications(){
  AwesomeNotifications().dismissAllNotifications();
}

void cancelAllSchedules(){
  AwesomeNotifications().cancelAllSchedules();
}

void cancelAllNotifications(){
  AwesomeNotifications().cancelAll();
}

/* *********************************************
    ACTION METHODS
************************************************ */

void processDefaultActionReceived(BuildContext context, ReceivedAction receivedNotification) {
  String targetPage = PAGE_NOTIFICATION_DETAILS;

  // Avoid to open the notification details page over another details page already opened
  Navigator.pushNamedAndRemoveUntil(context, targetPage,
          (route) => (route.settings.name != targetPage) || route.isFirst,
      arguments: receivedNotification);
}

Future<bool> requireUserNotificationPermissions(BuildContext context, {String? channelKey}) async {
  bool isAllowed = await AwesomeNotifications().isNotificationAllowed(channelKey: channelKey);
  if(!isAllowed){
    await showRequestUserPermissionDialog(context, channelKey: channelKey);
    isAllowed = await AwesomeNotifications().isNotificationAllowed(channelKey: channelKey);
  }
  return isAllowed;
}

Future<void> showPermissionPage() async {
  await AwesomeNotifications().showNotificationConfigPage();
}

Future<void> showNotificationConfigPage() async {
  AwesomeNotifications().showNotificationConfigPage();
}

Future<void> showRequestUserPermissionDialog(BuildContext context, {String? channelKey}) async {
  return showDialog(
    context: context,
    builder: (_) => AlertDialog(
      backgroundColor: Color(0xfffbfbfb),
      title: Text('Get Notified!',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 22.0, fontWeight: FontWeight.w600)),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Image.asset(
            'assets/images/animated-bell.gif',
            height: 200,
            fit: BoxFit.fitWidth,
          ),
          Text(
            'Allow Awesome Notifications to send you beautiful notifications!',
            textAlign: TextAlign.center,
          ),
        ],
      ),
      actions: [
        TextButton(
          style: TextButton.styleFrom(backgroundColor: Colors.grey),
          onPressed: () async {
            Navigator.of(context).pop();
          },
          child: Text('Later', style: TextStyle(color: Colors.white)),
        ),
        TextButton(
          style: TextButton.styleFrom(backgroundColor: Colors.deepPurple),
          onPressed: () async {
            await AwesomeNotifications()
                .requestPermissionToSendNotifications(channelKey: channelKey);
            Navigator.of(context).pop();
          },
          child: Text('Allow', style: TextStyle(color: Colors.white)),
        )
      ],
    ),
  );
}