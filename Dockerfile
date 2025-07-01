# ARG must be before all "FROM"s
# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.3

FROM openjdk:21-jdk-slim-bullseye AS build-env
WORKDIR /usr/local/bin
# Linux Static Builds (http://www.ffmpeg.org/download.html#build-linux)
# https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz
ADD lib/ffmpeg-release-amd64-static.tar.xz /usr/local/bin
RUN cp -p ffmpeg*/ffmpeg /usr/bin

ADD lib/azcopy_linux_amd64_10.24.0.tar.gz /usr/local/bin
RUN cp -p azcopy*/azcopy /usr/bin
RUN chmod 777 /usr/bin/azcopy

 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
FROM hmctspublic.azurecr.io/base/java:21-distroless
COPY --from=build-env /usr/bin/ffmpeg /usr/bin
COPY --from=build-env /usr/bin/azcopy /usr/bin

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/darts-api.jar /opt/app/

EXPOSE 4550
ENTRYPOINT ["java","-Duser.timezone=UTC","-jar","/opt/app/darts-api.jar"]
