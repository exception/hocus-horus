FROM openjdk:11-jdk

RUN mkdir /app
WORKDIR /app
COPY target/hocus-1.0-SNAPSHOT.jar hocus.jar

RUN wget -O /bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.2.2/dumb-init_1.2.2_amd64
RUN chmod +x /bin/dumb-init
ENTRYPOINT ["/bin/dumb-init", "--"]

CMD java -jar hocus.jar