FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/cedal-server/ ./
ENV PORT=8080
EXPOSE 8080
CMD ["./bin/cedal-server"]
