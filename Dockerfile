FROM harbor.codesystem.org/java/gradle:jdk19 AS build-stage-aagent

WORKDIR /source
ADD agent/src /source/src
ADD agent/build.gradle /source

RUN gradle clean fatJar


FROM harbor.codesystem.org/java/gradle:jdk19 AS build-stage-server

WORKDIR /source
ADD server/src /source/src
ADD server/build.gradle /source

COPY --from=build-stage-aagent /source/build/libs/Agent.jar /source/src/main/resources/agent/Agent.jar

RUN rm src/main/resources/application.properties
RUN rm -r src/test
RUN gradle clean bootJar


FROM harbor.codesystem.org/java/zulu-openjdk-alpine:19-latest

WORKDIR /jar
COPY --from=build-stage-server /source/build/libs/* /jar/backend.jar
ENV database.default-schema=taskflare

ENTRYPOINT ["java", "-jar", "backend.jar"]
