# We will use OpenJDK 20
#FROM ghcr.io/graalvm/jdk-community:20.0.1
FROM amazoncorretto:20-alpine-jdk

# 8090 is the port number the application will use
EXPOSE 8090

# Install dependencies
# freetype is required for the font rendering in PlantUML
RUN apk add --no-cache freetype

# Set up a volume for the data directory
VOLUME /data

# copy jar
COPY ./app/target/app-*.jar /usr/local/lib/talkforgeai.jar

# startup command
CMD java -DTALKFORGE_DATADIR=/data -jar /usr/local/lib/talkforgeai.jar --spring.config.additional-location=/usr/local/talkforgeai/talkforgeai.properties
