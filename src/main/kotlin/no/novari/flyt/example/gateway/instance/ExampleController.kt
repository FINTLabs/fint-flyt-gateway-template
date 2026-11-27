package no.novari.flyt.example.gateway.instance

import no.novari.flyt.example.gateway.instance.model.AdvancedExample
import no.novari.flyt.example.gateway.instance.model.SimpleExample
import no.novari.flyt.example.gateway.instance.model.SimpleExampleStatus
import no.novari.flyt.gateway.instance.InstanceProcessor
import no.novari.flyt.gateway.instance.MultipartInstanceProcessor
import no.novari.flyt.security.UrlPaths.EXTERNAL_API
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartHttpServletRequest

@RestController
@RequestMapping("$EXTERNAL_API/example/instances")
class ExampleController(
    @param:Qualifier("simpleProcessor")
    private val simpleExampleProcessor: InstanceProcessor<SimpleExample>,
    @param:Qualifier("advancedProcessor")
    private val advancedExampleProcessor: InstanceProcessor<AdvancedExample>,
    @param:Qualifier("advancedMultipartProcessor")
    private val advancedMultipartExampleProcessor: MultipartInstanceProcessor<AdvancedExample>,
) {
    @GetMapping("/simple/{instanceId}/status")
    fun getSimpleStatus(
        @PathVariable instanceId: String,
    ): SimpleExampleStatus {
        return SimpleExampleStatus(instanceId)
    }

    @PostMapping("/simple")
    fun createSimpleExample(
        @RequestBody simpleExample: SimpleExample,
    ): ResponseEntity<Void> {
        return simpleExampleProcessor.processInstance(simpleExample)
    }

    @PostMapping(
        "/advanced",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun createAdvancedExample(
        @RequestBody advancedExample: AdvancedExample,
    ): ResponseEntity<Void> {
        return advancedExampleProcessor.processInstance(advancedExample)
    }

    @PostMapping(
        "/advanced",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    fun createAdvancedMultipartExample(
        @RequestPart(INSTANCE_PART_NAME) advancedExample: AdvancedExample,
        multipartRequest: MultipartHttpServletRequest,
    ): ResponseEntity<Void> {
        return advancedMultipartExampleProcessor.processInstance(
            incomingInstance = advancedExample,
            multipartFiles =
                multipartRequest.multiFileMap
                    .filterKeys { it != INSTANCE_PART_NAME }
                    .values
                    .flatten(),
        )
    }

    private companion object {
        private const val INSTANCE_PART_NAME = "instance"
    }
}
