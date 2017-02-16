package io.elastic.sailor;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

public final class Step {

    private final String id;
    private final String compId;
    private final String function;
    private final JsonObject cfg;
    private final JsonObject snapshot;

    public Step(final JsonObject data) {
        this.id = getAsRequiredString(data, Constants.STEP_PROPERTY_ID);
        this.compId = getAsRequiredString(data, Constants.STEP_PROPERTY_COMP_ID);
        this.function = getAsRequiredString(data, Constants.STEP_PROPERTY_FUNCTION);
        this.cfg = getAsNullSafeObject(data, Constants.STEP_PROPERTY_CFG);
        this.snapshot = getAsNullSafeObject(data, Constants.STEP_PROPERTY_SNAPSHOT);
    }

    public String getId() {
        return this.id;
    }

    public String getCompId() {
        return this.compId;
    }

    public String getFunction() {
        return this.function;
    }

    public JsonObject getCfg() {
        return this.cfg;
    }

    public JsonObject getSnapshot() {
        return this.snapshot;
    }

    private static String getAsRequiredString(
            final JsonObject data, final String name) {

        final JsonString value = data.getJsonString(name);

        if (value == null) {
            throw new IllegalArgumentException(
                    String.format("Step's %s is required", name));
        }

        return value.getString();
    }

    private static JsonObject getAsNullSafeObject(
            final JsonObject data, final String name) {

        final JsonObject value = data.getJsonObject(name);

        if (value != null) {
            return value;
        } else {
            return Json.createObjectBuilder().build();
        }
    }
}
