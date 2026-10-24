FROM eclipse-temurin:25-jre
RUN useradd --system --no-create-home app
WORKDIR /app
COPY target/*.jar app.jar
USER app
EXPOSE 1122
ENTRYPOINT ["java", "-jar", "app.jar"]