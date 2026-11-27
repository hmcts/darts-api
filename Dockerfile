# ARG must be before all "FROM"s
# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.10

# Prepare the native tools in a stage with shell utilities.
FROM mcr.microsoft.com/openjdk/jdk:25-azurelinux AS build

WORKDIR /usr/local/bin
# Linux Static Builds (http://www.ffmpeg.org/download.html#build-linux)
# https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz
ADD lib/ffmpeg-release-amd64-static.tar.xz /usr/local/bin
RUN cp -p ffmpeg*/ffmpeg /usr/bin

ADD lib/azcopy_linux_amd64_10.24.0.tar.gz /usr/local/bin
RUN cp -p azcopy*/azcopy /usr/bin
RUN chmod 777 /usr/bin/azcopy

# Only the selected native tools are copied into the HMCTS runtime image.
# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
FROM hmctsprod.azurecr.io/base/java:25-distroless
COPY --from=build /usr/bin/ffmpeg /usr/bin
COPY --from=build /usr/bin/azcopy /usr/bin

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/darts-api.jar /opt/app/

EXPOSE 4550
# Native access for Netty/Tomcat classpath consumers; see README.md.
ENTRYPOINT ["java","--enable-native-access=ALL-UNNAMED","-Duser.timezone=UTC","-jar","/opt/app/darts-api.jar"]
