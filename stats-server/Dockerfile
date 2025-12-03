FROM amazoncorretto:21

COPY service/target/service-*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]