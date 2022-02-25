FROM openjdk:17-jdk-slim
COPY target/Discordle-*.jar /app.jar
CMD ["/usr/local/openjdk-17/bin/java", "-jar", "/app.jar"]