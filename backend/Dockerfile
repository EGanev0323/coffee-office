# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Всички библиотеки и плъгини се теглят тук, преди кода, и се пазят в BuildKit кеш (/root/.m2) между билдовете:
# при промяна на pom.xml се теглят само новите, а ако връзката към Maven Central прекъсне,
# следващият опит продължава с вече изтегленото. dependency:go-offline не тегли всичко
# (напр. micrometer-observation), затова е resolve + resolve-plugins.
RUN --mount=type=cache,target=/root/.m2 \
    for i in 1 2 3 4 5; do \
      mvn -B -q dependency:resolve dependency:resolve-plugins && exit 0; \
      echo "Тегленето на библиотеките не успя (опит $i от 5)."; \
      [ "$i" -lt 5 ] && sleep 15; \
    done; \
    echo "Няма връзка с Maven Central. Провери мрежата и пусни билда пак - изтегленото досега се пази."; \
    exit 1
COPY src ./src
# Офлайн (-o): тук вече не се тегли нищо, така че мрежата не може да счупи билда.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -o package -DskipTests

# ---- run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/coffee-app.jar app.jar
USER app
ENV JAVA_OPTS="-Xms128m -Xmx384m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
