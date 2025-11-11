# ✅ 1. Use stable OpenJDK base
FROM eclipse-temurin:17-jdk-jammy

# ✅ 2. Set working directory
WORKDIR /app

# ✅ 3. Install Maven
RUN apt-get update && apt-get install -y maven

# ✅ 4. Copy project files
COPY . .

# ✅ 5. Give permission to Maven wrapper (if present)
RUN chmod +x mvnw || true

# ✅ 6. Build JAR file
RUN mvn clean package -DskipTests

# ✅ 7. Expose port (Render uses PORT env)
EXPOSE 8080

# ✅ 8. Run the application
CMD ["java", "-jar", "target/cdaxVideo-0.0.1-SNAPSHOT.jar"]