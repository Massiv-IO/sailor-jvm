package io.elastic.sailor;


import javax.json.JsonObject;

public interface ApiClient {

    Step retrieveFlowStep(String taskId, String stepId);

    JsonObject updateAccount(String accountId, JsonObject body);
}
