package de.nico.pushnotification.servicetester.message;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClientsMessage extends HashMap<String, List<String>> implements Message {
    private static final String TAG = ClientsMessage.class.getSimpleName();

    public static ClientsMessage parse(String message) throws JSONException {
        JSONObject json = new JSONObject(message);
        ClientsMessage clientsMessage = new ClientsMessage();
        json.keys().forEachRemaining((key) -> {
            JSONArray channels;
            try {
                channels = json.getJSONArray(key);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to get channels for " + key, e);
                return;
            }
            List<String> channelList = new ArrayList<>();
            try {
                for (int i = 0; i < channels.length(); i++) {
                        channelList.add(channels.getString(i));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to get a channel name", e);
                return;
            }
            clientsMessage.put(key, channelList);
        });
        return clientsMessage;
    }

    @Override
    public String create() {
        return new JSONObject(this).toString();
    }
}
