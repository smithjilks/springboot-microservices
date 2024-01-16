plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
include("product-service")
include("review-service")
include("recommendation-service")
include("product-composite-service")
include("api")
include("util")
include(":spring-cloud:eureka-server")
include(":spring-cloud:gateway")
include(":spring-cloud:authorization-server")
