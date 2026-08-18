FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle
COPY common common
COPY payment-service payment-service
COPY cart-service/build.gradle cart-service/build.gradle
COPY product-service/build.gradle product-service/build.gradle
COPY search-service/build.gradle search-service/build.gradle
COPY order-service/build.gradle order-service/build.gradle
COPY inventory-service/build.gradle inventory-service/build.gradle
RUN mkdir -p cart-service/src product-service/src search-service/src order-service/src inventory-service/src
RUN chmod +x gradlew && ./gradlew :payment-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/payment-service/build/libs/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
