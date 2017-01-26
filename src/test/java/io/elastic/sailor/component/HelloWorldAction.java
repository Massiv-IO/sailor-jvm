package io.elastic.sailor.component;

import io.elastic.api.Component;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;

import javax.json.Json;
import javax.json.JsonObject;

public class HelloWorldAction extends Component {

    public HelloWorldAction(EventEmitter eventEmitter) {
        super(eventEmitter);
    }

    public void execute(ExecutionParameters parameters) {

        final JsonObject body = Json.createObjectBuilder()
                .add("de", "Hallo, Welt!")
                .add("en", "Hello, world!")
                .build();

        new Message.Builder().body(body).build();

        this.getEventEmitter().emitData(parameters.getMessage());
    }
}
