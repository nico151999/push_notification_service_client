package de.nico.pushnotification.servicetester.message;

import org.json.JSONException;
import org.json.JSONObject;

public class ApplicationSubscriptionMessage implements Message {
    private static final String MESSAGE = "application_subscription";
    private static final String PACKAGE_KEY = "package";
    private String mPackage;

    public ApplicationSubscriptionMessage(String pkg) {
        mPackage = pkg;
    }

    public static ApplicationSubscriptionMessage parse(String message) throws JSONException {
        return new ApplicationSubscriptionMessage(
                new JSONObject(message).getJSONObject(MESSAGE).getString(PACKAGE_KEY)
        );
    }

    public String getPackage() {
        return mPackage;
    }

    public void setPackage(String pkg) {
        mPackage = pkg;
    }

    @Override
    public String create() {
        try {
            return new JSONObject().put(
                    MESSAGE,
                    new JSONObject().put(PACKAGE_KEY, mPackage)
            ).toString();
        } catch (JSONException e) {
            return null;
        }
    }
}
