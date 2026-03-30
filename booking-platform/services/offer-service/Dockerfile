FROM eclipse-temurin:17-jre-alpine

ARG SERVICE_PORT=8087
ENV SERVER_PORT=${SERVICE_PORT}

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY target/offer-service-*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE ${SERVICE_PORT}

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:${SERVICE_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
