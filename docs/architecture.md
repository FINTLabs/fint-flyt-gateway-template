# Architecture

The template is a FLYT gateway built on [flyt-gateway-starter](https://github.com/FINTLabs/fint-flyt-gateway-starter),
which does the processing; see its [architecture](https://github.com/FINTLabs/fint-flyt-gateway-starter/blob/main/docs/architecture.md).
The template adds the controller, processor configuration and mappers for one source application. Blue is the gateway's
own code, turquoise is the starter and other services.

## C4 level 1: System context

```mermaid
C4Context
    title Example gateway

    System_Ext(source, "Source application", "Sends instances.")
    System_Ext(nam, "NAM", "OAuth2 tokens.")
    System(gateway, "Example gateway", "Maps instances to Flyt.")
    System_Ext(kafka, "Kafka", "Instance and error events.")
    System_Ext(file, "file-service", "Stores attachments.")
    System_Ext(integration, "intgr-conf-service", "Integration status.")

    Rel(source, nam, "Fetches token")
    Rel(source, gateway, "HTTP")
    Rel(gateway, nam, "Validates, fetches token")
    Rel(gateway, kafka, "Publishes")
    Rel(gateway, file, "Uploads")
    Rel(gateway, integration, "Looks up")

    UpdateRelStyle(source, nam, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(source, gateway, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(gateway, nam, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateRelStyle(gateway, kafka, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(gateway, file, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(gateway, integration, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateElementStyle(source, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(nam, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(gateway, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(kafka, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(file, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(integration, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

flyt-security-starter accepts a source application token only on the routes in `novari.flyt.security.external.paths`
and only from the client ID of the configured `SourceApplication`. The template leaves both unset, so its routes are
denied until a gateway sets them. Internal calls use NAM client credentials.

## C4 level 3: Components

```mermaid
C4Component
    title Components of the example gateway

    Component_Ext(source, "Source application", "HTTP client", "JSON or multipart.")
    Component(controller, "ExampleController", "Spring MVC", "Simple and advanced routes.")
    Component_Ext(processors, "Processors", "flyt-gateway-starter", "Validate, check, publish.")
    Component(configs, "Processor configs", "Spring beans", "One processor per model.")
    Component(mappers, "Mappers", "Kotlin", "Model to InstanceObject.")
    Component_Ext(services, "Kafka and services", "flyt-gateway-starter", "Events, files, status.")

    Rel(source, controller, "Posts")
    Rel(controller, processors, "Delegates")
    Rel(configs, processors, "Creates")
    Rel(processors, mappers, "Maps")
    Rel(processors, services, "Publishes")

    UpdateRelStyle(source, controller, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(controller, processors, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateRelStyle(configs, processors, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(processors, mappers, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(processors, services, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="20")
    UpdateElementStyle(source, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(controller, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(processors, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(configs, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(mappers, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(services, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

- **ExampleController:** `/api/example/instances/simple` takes JSON; `/api/example/instances/advanced` takes JSON with
  Base64 files or multipart with an `instance` part and file parts.
- **Processor configs:** `SimpleProcessorConfiguration` and `AdvancedProcessorConfiguration` create the processors with
  `InstanceProcessorFactoryService`, a fixed integration ID and the instance ID from the payload.
- **Mappers:** `SimpleMappingService`, `AdvancedMappingService` and `AdvancedMultipartMappingService` map the models to
  `InstanceObject` and store attachments through the `persistFile` callback.
