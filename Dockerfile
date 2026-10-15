# One image: Angular is built, bundled into the Spring Boot jar as static files, and served from
# the same origin as the API (no CORS, one thing to deploy).

# 1) Frontend
FROM node:24-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build --configuration production

# 2) Backend (+ frontend as static resources)
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build/backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /build/frontend/dist/frontend/browser/ ./src/main/resources/static/
# Tests run in CI/locally; the image build only packages.
RUN mvn -B -q package -DskipTests

# 3) Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app && mkdir -p /data && chown app:app /data
USER app
COPY --from=backend /build/backend/target/salary-management-*.jar app.jar
ENV SALARY_DB_PATH=/data/salary.db \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
