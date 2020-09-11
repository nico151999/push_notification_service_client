package de.nico.pushnotification.servicetester.message;

import org.json.JSONException;
import org.json.JSONObject;

public class ChannelSubscriptionMessage implements Message {
    private static final String PACKAGE_KEY = "package";
    private static final String CHANNEL_KEY = "channel";
    private String mPackage;
    private String mChannel;

    public ChannelSubscriptionMessage(String pkg, String channel) {
        mPackage = pkg;
        mChannel = channel;
    }

    public static ChannelSubscriptionMessage parse(String message) throws JSONException {
        JSONObject json = new JSONObject(message);
        return new ChannelSubscriptionMessage(json.getString(PACKAGE_KEY), json.getString(CHANNEL_KEY));
    }

    public String getPackage() {
        return mPackage;
    }

    public void setPackage(String pkg) {
        mPackage = pkg;
    }

    public String getChannel() {
        return mChannel;
    }

    public void setChannel(String channel) {
        mChannel = channel;
    }

    @Override
    public String create() {
        try {
            return new JSONObject()
                    .put(PACKAGE_KEY, mPackage)
                    .put(CHANNEL_KEY, mChannel)
                    .toString();
        } catch (JSONException e) {
            return null;
        }
    }
}
