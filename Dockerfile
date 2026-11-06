# Stage 1: Build the JAR with Maven
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

# Copy Maven wrapper and POM first (to cache dependencies)
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal Runtime Image
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# Non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
RUN mkdir -p /app/uploads && chown -R spring:spring /app/uploads
USER spring:spring

# Copy built jar from builder
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
