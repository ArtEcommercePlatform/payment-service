# Use the lightweight OpenJDK 17 Alpine base image
FROM openjdk:17-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the application JAR file into the container
COPY target/payment-service-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8086

# Set the default command to run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
