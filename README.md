# SQL Tokeniser #

## Description ##

This project attempts to be tokenise SQL statements, allow a person to parse TSQL DB logs quickly and generate retrospective lineage.

## Running the code ##

Requirements: Internet and Maven

### Build the JAR ###

```sh
cd calcite-unwrap/calcite-unwrap-code
mvn clean package
```

### Running the Code ###

You can either run through Maven or from the jar (N/A, @TODO: create a fat jar).

```sh
mvn exec:java
```

### Using the API ###

You can go to `http://localhost:8500/query?query=<QUERY_STRING>` in your browser or use cURL and add the query to the URL where <QUERY_STRING> is in order to test the unwrap function. If the QUERY_STRING is invalid, you will recieve a HTTP 500 error. For any other issues, please raise an issue in this repository.
