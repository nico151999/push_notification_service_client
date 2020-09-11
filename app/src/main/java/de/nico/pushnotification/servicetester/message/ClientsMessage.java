package de.nico.pushnotification.servicetester.message;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ClientsMessage extends HashMap<String, List<String>> implements Message {
    private static final String MESSAGE = "clients";
    private static final String TAG = ClientsMessage.class.getSimpleName();

    public static ClientsMessage parse(String message) throws JSONException {
        JSONObject json = new JSONObject(message).getJSONObject(MESSAGE);
        ClientsMessage clientsMessage = new ClientsMessage();
        for (Iterator<String> it = json.keys(); it.hasNext(); ) {
            String key = it.next();
            JSONArray channels = json.getJSONArray(key);
            List<String> channelList = new ArrayList<>();
            for (int i = 0; i < channels.length(); i++) {
                channelList.add(channels.getString(i));
            }
            clientsMessage.put(key, channelList);
        }
        return clientsMessage;
    }

    @Override
    public String create() {
        try {
            return new JSONObject().put(MESSAGE, new JSONObject(this)).toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to put clients", e);
            return null;
        }
    }
}
