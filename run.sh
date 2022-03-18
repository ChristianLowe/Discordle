mvn clean package spring-boot:repackage \
  && docker build -t discordle . \
  && docker run -d --env DISCORDLE_TOKEN discordle:latest
