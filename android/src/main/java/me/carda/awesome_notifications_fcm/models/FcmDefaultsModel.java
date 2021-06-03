package me.carda.awesome_notifications_fcm.models;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import me.carda.awesome_notifications.Definitions;
import me.carda.awesome_notifications.notifications.exceptions.AwesomeNotificationException;
import me.carda.awesome_notifications.notifications.models.Model;
import me.carda.awesome_notifications_fcm.FcmDefinitions;

public class FcmDefaultsModel extends Model {

    public Long silentDataCallback;
    public Long reverseDartCallback;
    public Boolean firebaseEnabled;

    public FcmDefaultsModel(){}

    public FcmDefaultsModel(boolean firebaseEnabled, Long reverseDartCallback, Long silentDataCallback){
        this.firebaseEnabled = firebaseEnabled;
        this.silentDataCallback = silentDataCallback;
        this.reverseDartCallback = reverseDartCallback;
    }

    @Override
    public Model fromMap(Map<String, Object> arguments) {
        silentDataCallback = getValueOrDefault(arguments, FcmDefinitions.SILENT_HANDLE, Long.class);
        reverseDartCallback = getValueOrDefault(arguments, FcmDefinitions.DART_BG_HANDLE, Long.class);
        firebaseEnabled  = getValueOrDefault(arguments, FcmDefinitions.FIREBASE_ENABLED, Boolean.class);

        return this;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> returnedObject = new HashMap<>();

        returnedObject.put(FcmDefinitions.SILENT_HANDLE, silentDataCallback);
        returnedObject.put(FcmDefinitions.DART_BG_HANDLE, reverseDartCallback);
        returnedObject.put(FcmDefinitions.FIREBASE_ENABLED, firebaseEnabled);
        return returnedObject;
    }

    @Override
    public String toJson() {
        return templateToJson();
    }

    @Override
    public FcmDefaultsModel fromJson(String json){
        return (FcmDefaultsModel) super.templateFromJson(json);
    }

    @Override
    public void validate(Context context) throws AwesomeNotificationException {

    }
}
