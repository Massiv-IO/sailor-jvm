package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.elastic.api.EventEmitter;
import io.elastic.sailor.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SailorModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(SailorModule.class.getName());

    @Override
    protected void configure() {
        bind(AMQPWrapperInterface.class).to(AMQPWrapper.class);
        bind(MessageProcessor.class).to(MessageProcessorImpl.class);

        bind(ApiClient.class).to(ApiClientImpl.class);

        install(new FactoryModuleBuilder()
                .implement(
                        CountingCallback.class,
                        Names.named(Constants.NAME_CALLBACK_DATA),
                        DataCallback.class)
                .implement(
                        CountingCallback.class,
                        Names.named(Constants.NAME_CALLBACK_ERROR),
                        ErrorCallback.class)
                .implement(
                        CountingCallback.class,
                        Names.named(Constants.NAME_CALLBACK_SNAPSHOT),
                        SnapshotCallback.class)
                .implement(
                        CountingCallback.class,
                        Names.named(Constants.NAME_CALLBACK_REBOUND),
                        ReboundCallback.class)
                .implement(
                        EventEmitter.Callback.class,
                        Names.named(Constants.NAME_CALLBACK_UPDATE_KEYS),
                        UpdateKeysCallback.class)
                .build(EmitterCallbackFactory.class));
    }


    @Provides
    @Named(Constants.NAME_STEP_JSON)
    Step provideTask(
            ApiClient apiClient,
            @Named(Constants.ENV_VAR_TASK_ID) String taskId,
            @Named(Constants.ENV_VAR_STEP_ID) String stepId) {

        return apiClient.retrieveTaskStep(taskId, stepId);
    }
}
