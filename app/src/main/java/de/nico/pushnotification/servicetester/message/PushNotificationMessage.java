package de.nico.pushnotification.servicetester.message;

import android.util.Base64;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class PushNotificationMessage implements Message {
    private static final String TITLE_KEY = "title";
    private static final String CONTENT_KEY = "content";
    private static final String ICON_KEY = "icon";
    private static final String URI_KEY = "uri";
    private static final String RECEIVER_KEY = "receiver";
    private String mTitle;
    private String mContent;
    private byte[] mIcon;
    private String mUri;
    private String mReceiverPackage;

    public PushNotificationMessage(String title, String content, String receiverPackage,
                                   @Nullable String icon, @Nullable String uri) {
        mTitle = title;
        mContent = content;
        mReceiverPackage = receiverPackage;
        mIcon = Base64.decode(icon, Base64.DEFAULT);
        mUri = uri;
    }

    public static PushNotificationMessage parse(String message) throws JSONException {
        JSONObject json = new JSONObject(message);
        return new PushNotificationMessage(
                json.getString(TITLE_KEY),
                json.getString(CONTENT_KEY),
                json.getString(RECEIVER_KEY),
                json.has(ICON_KEY) ? json.getString(ICON_KEY) : null,
                json.has(URI_KEY) ? json.getString(URI_KEY) : null
        );
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public byte[] getIcon() {
        return mIcon;
    }

    public void setIcon(byte[] icon) {
        mIcon = icon;
    }

    public String getUri() {
        return mUri;
    }

    public void setUri(String uri) {
        mUri = uri;
    }

    public String getReceiverPackage() {
        return mReceiverPackage;
    }

    public void setReceiverPackage(String receiverPackage) {
        mReceiverPackage = receiverPackage;
    }

    @Override
    public String create() {
        try {
            JSONObject ret = new JSONObject()
                    .put(TITLE_KEY, mTitle)
                    .put(CONTENT_KEY, mContent)
                    .put(RECEIVER_KEY, mReceiverPackage);
            if (mIcon != null) {
                ret.put(ICON_KEY, Base64.encodeToString(mIcon, Base64.DEFAULT));
            }
            if (mUri != null) {
                ret.put(URI_KEY, mUri);
            }
            return ret.toString();
        } catch (JSONException e) {
            return null;
        }
    }
}
