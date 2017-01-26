package io.elastic.sailor

import io.elastic.api.JSON
import io.elastic.api.Message
import spock.lang.Specification

class ExecutionContextSpec extends Specification {


    def "should build default headers properly"() {
        given:
        def step = JSON.parseObject("{" +
                "\"id\":\"step_1\"," +
                "\"comp_id\":\"testcomponent\"," +
                "\"function\":\"test\"," +
                "\"snapshot\":{\"timestamp\":\"19700101\"}}")
        def originalHeaders = [
                execId: "_exec_01",
                taskId: "5559edd38968ec0736000003",
                userId: "010101"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                new Step(step), new Message.Builder().build(), originalHeaders);

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        print headers
        headers.compId == 'testcomponent'
        headers.function == 'test'
        headers.stepId == 'step_1'
        headers.start != null
        headers.taskId == '5559edd38968ec0736000003'
        headers.userId == '010101'
        headers.execId == '_exec_01'
    }
}
