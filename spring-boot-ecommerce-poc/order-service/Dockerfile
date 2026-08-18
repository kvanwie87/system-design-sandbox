FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle
COPY common common
COPY order-service order-service
COPY cart-service/build.gradle cart-service/build.gradle
COPY product-service/build.gradle product-service/build.gradle
COPY search-service/build.gradle search-service/build.gradle
COPY inventory-service/build.gradle inventory-service/build.gradle
COPY payment-service/build.gradle payment-service/build.gradle
RUN mkdir -p cart-service/src product-service/src search-service/src inventory-service/src payment-service/src
RUN chmod +x gradlew && ./gradlew :order-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/order-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
