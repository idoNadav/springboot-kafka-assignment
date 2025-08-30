FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
ARG MODULE=order-service
RUN mvn -B -DskipTests -pl ${MODULE} -am clean package

FROM eclipse-temurin:21-jre
ENV APP_USER=spring
RUN useradd -ms /bin/bash ${APP_USER}
USER ${APP_USER}
WORKDIR /app
ARG MODULE=order-service
COPY --from=build /workspace/${MODULE}/target/*.jar /app/app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]