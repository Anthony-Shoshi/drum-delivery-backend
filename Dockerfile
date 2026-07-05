FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first to leverage Docker cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application (skip tests and test compilation)
RUN mvn package -Dmaven.test.skip=true

# Production image
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built JAR file
COPY --from=build /app/target/*.jar app.jar

# Create document storage directory
RUN mkdir -p /app/storage/documents
VOLUME /app/storage/documents

# Expose the port
EXPOSE 8080

# Set entrypoint
ENTRYPOINT ["java","-jar","/app/app.jar"]