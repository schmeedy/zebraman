FROM openjdk:8-jre-alpine

ADD target/scala-2.12/zebraman.jar /app/zebraman.jar
ENTRYPOINT ["java", "-jar", "/app/zebraman.jar"]