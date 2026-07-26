# Stage 1: Build the React frontend with Node.
FROM node:22-bookworm AS frontend-build
WORKDIR /app/frontend-react
COPY frontend-react/package.json frontend-react/package-lock.json ./
RUN npm ci
COPY frontend-react/ ./
RUN npm run build

# Stage 2: Build the Spring Boot jar with the frontend bundle already available.
FROM eclipse-temurin:17-jdk AS backend-build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
COPY src/ src/
COPY --from=frontend-build /app/frontend-react/dist frontend-react/dist
ENV SKIP_FRONTEND_BUILD=true
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

# Stage 3: Runtime.
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
COPY --from=backend-build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
