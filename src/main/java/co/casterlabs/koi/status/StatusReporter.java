package co.casterlabs.koi.status;

import com.google.gson.JsonObject;

public interface StatusReporter {

    public String getName();

    public void report(JsonObject json);

}
