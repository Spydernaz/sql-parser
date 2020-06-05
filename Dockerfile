FROM openjdk:8

RUN apt update
RUN apt install git maven -y
RUN mkdir /sql-parser && git clone https://github.com/Spydernaz/sql-parser /sql-parser

WORKDIR /sql-parser/calcite-unwrap/calcite-unwrap-code

RUN mvn dependency:go-offline
RUN mvn package

EXPOSE 8500
CMD [ "mvn", "exec:java" ]