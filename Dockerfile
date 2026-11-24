# Runtime image for any module. Build the jars first (./scripts/build.sh), then:
#   docker build --build-arg MODULE=order-service -t ecommerce/order-service:1.0.0 .
FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY ${MODULE}/target/${MODULE}-1.0.0.jar app.jar
ENV LOG_DIR=/logs JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
