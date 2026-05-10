# syntax=docker/dockerfile:1
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache tzdata && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

COPY target/immune-sentinel.jar /app/app.jar

ENV JAVA_OPTS="-Xms256m -Xmx768m -XX:+UseSerialGC -Duser.timezone=Asia/Shanghai"
EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
