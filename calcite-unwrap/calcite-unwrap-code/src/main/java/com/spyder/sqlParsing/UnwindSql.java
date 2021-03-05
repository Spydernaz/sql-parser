package com.spyder.sqlParsing;

// Data Types
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.*;

// Apache Calcite
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParser.ConfigBuilder;
import org.apache.calcite.util.JsonBuilder;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.dialect.ParaccelSqlDialect;

// Webserver
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;


public class UnwindSql {
    public static String parseQuery(String query) throws Exception {
        final String connectionstring = "PSSQL://somesqlserver.example.com:5432/database1";
        String testSimpleQuery = "SELECT a_id, firstname as FirstName from tbl where FirstName = 'Nate' ORDER BY 1";
        String testComplexQuery = "SELECT a.a_id as Academic_ID, a.fn as Academic_FirstName, \n"
                                + "s.s_id as Student_ID, s.fn as Student_FirstName \n"
                                + " FROM uni1.academic a JOIN uni1.student s on a.a_id = s.s_id";

        // Check if I want to use the sample queries
        String q;
        if ( query.toString() == "SIMPLE" ){
            System.out.println("ENTERS SIMPLE IF STATEMENT");
            q = testSimpleQuery;
        } else if ( query == "COMPLEX" ) {
            q = testComplexQuery;
        } else {
            System.out.println("HITS DEFAULT IF STATEMENT IF STATEMENT. QUERY: " + query);

            q = query;
        }
        System.out.println(q);

        final SqlParser parser = SqlParser.create(q);
        final SqlNode sqlNode = parser.parseQuery();
        // final SqlSelect sqlSelect = (SqlSelect) sqlNode;
        // final SqlJoin from = (SqlJoin) sqlSelect.getFrom();
        // JSONObject jsonObject = JsonBuilderFactory.buildObject();
        JSONObject jsonObject = new JSONObject();
        jsonObject.append("query", query);
        jsonObject.append("results", unwrap(sqlNode));

        return (jsonObject.toString());
        // System.out.println("pause");
    }

    public static Object unwrap(SqlNode node) throws Exception {
        // If order by comes in the query.
        // @TODO: check for what type of query
        String temp = node.getKind().name();

        switch (node.getKind()) {
            case ORDER_BY:
                // Returns JSON formatted: '{"query": <JSON>, "orderedBy": <STRING>}'
                JSONObject order_by = new JSONObject();
                JSONArray rorderList = new JSONArray();
                order_by.put("type", "ORDER_BY");

                order_by.put("query", unwrap((SqlNode) ((SqlOrderBy) node).query));
                SqlNodeList orderList = ((SqlOrderBy) node).orderList;
                for (SqlNode el : orderList) {
                    rorderList.put(unwrap(el));
                }
                order_by.put("orderedBy", rorderList);
                return order_by;

            case JOIN:
                // Returns JSON formatted: '{"query": <JSON>, "orderedBy": <STRING>}'
                JSONObject join = new JSONObject();
                join.put("type", "JOIN");
                join.put("joinType", ((SqlJoin) node).getJoinType().toString());
                join.put("joinCondition", ((SqlJoin) node).getConditionType().toString());
                join.put("joinLeft", unwrap(((SqlJoin) node).getLeft()));
                join.put("joinRight", unwrap(((SqlJoin) node).getRight()));
                return join;

            case SELECT:
                // Returns JSON formatted: '{"selectList": <ARRAY>, "from": <LIST>}'
                System.out.println("CREATE SELECT STATEMENT");
                JSONObject select = new JSONObject();
                select.put("type", "SELECT");

                // select list
                System.out.println("CREATE SELECT LIST");
                JSONArray rselectList = new JSONArray();
                SqlNodeList selectList = ((SqlSelect) node).getSelectList();
                for (SqlNode el : selectList) {
                    rselectList.put(unwrap(el));
                }
                select.put("selectList", rselectList);


                // from statement
                System.out.println("CREATE FROM FOR QUERY");
                SqlNode from = (SqlNode) ((SqlSelect) node).getFrom();
                if (from != null){
                    select.put("from", unwrap(from));
                }

                // Group By Logic
                System.out.println("CREATE GROUP BY FOR QUERY");
                JSONArray rgroupBy = new JSONArray();
                SqlNodeList groupByList = ((SqlSelect) node).getGroup();
                if (groupByList != null) {
                    for (SqlNode el : groupByList) {
                        rgroupBy.put(unwrap(el));
                    }
                    select.put("groupBy", rgroupBy);
                }
                
                System.out.println("CREATE WHERE CONDITION FOR QUERY");
                SqlNode where = ((SqlSelect) node).getWhere();
                if (where != null) {
                    select.put("where", unwrap(where));
                }

                // join.put("joinType", ((SqlJoin) node).getJoinType().toString());
                // join.put("joinCondition", ((SqlJoin) node).getConditionType().toString());
                // join.put("joinLeft", unwrap(((SqlJoin) node).getLeft()));
                // join.put("joinRight", unwrap(((SqlJoin) node).getRight()));

                return (select);
            case AS:
                // Return a JSON in the format: '{"actual": <STRING>, "alias": <STRING>}'
                JSONObject as = new JSONObject();
                // String actualId = (String) unwrap((SqlNode) ((SqlBasicCall)
                // node).operand(0));
                // String alias = (String) unwrap(((SqlBasicCall) node).operand(1));
                as.put("type", "AS");
                as.put("actual", unwrap((SqlNode) ((SqlBasicCall) node).operand(0)));
                as.put("alias", unwrap((SqlNode) ((SqlBasicCall) node).operand(1)));
                return (as);

            case IDENTIFIER:
                JSONObject identifier = new JSONObject();
                identifier.put("type", "IDENTIFIER");
                identifier.put("name", (String) ((SqlIdentifier) node).toString());
                return (identifier);
            case OTHER_FUNCTION:
                JSONObject other_function = new JSONObject();
                other_function.put("type", "OTHER_FUNCTION");
                // other_function.put("name", (String) ((SqlFunction) ((SqlBasicCall)
                // node)).getName();
                other_function.put("string", (String) node.toString());
                return (other_function);

            case AND:
            case OR:
                JSONObject logic = new JSONObject();
                logic.put("type", "LOGICAL_CONDITION");
                logic.put("condition_type", node.getKind().toString().toLowerCase());
                logic.put("left", unwrap((SqlNode) ((SqlBasicCall) node).operand(0)));
                logic.put("right", unwrap((SqlNode) ((SqlBasicCall) node).operand(1)));

                return (logic);

            case EQUALS:
            case NOT_EQUALS:
            case GREATER_THAN:
            case GREATER_THAN_OR_EQUAL:
            case LESS_THAN:
            case LESS_THAN_OR_EQUAL:
            case LIKE:
                JSONObject comparison = new JSONObject();
                comparison.put("type", "comparison");
                comparison.put("comparison_type", node.getKind().toString().toLowerCase());
                comparison.put("left", unwrap((SqlNode) ((SqlBasicCall) node).operand(0)));
                comparison.put("right", unwrap((SqlNode) ((SqlBasicCall) node).operand(1)));

                return (comparison);
            case LITERAL:
                System.out.println("Literal :: " + node.getKind().name());
                JSONObject literal = new JSONObject();
                literal.put("type", "LITERAL");
                literal.put("value", ((SqlLiteral) node).getValue());
                
                return (literal);
            default:
                System.out.println("some other type :: " + node.getKind().name());
                String defaultString = ((SqlNode) node).toString();
                return (defaultString);
        }

        // return objects back to a hashmap
        // return results;

    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8500), 0);
        HttpContext context = server.createContext("/query");
        context.setHandler(UnwindSql::handleRequest);
        server.start();
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        URI requestURI = exchange.getRequestURI();
        Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
        String query = params.get("query");
        query = java.net.URLDecoder.decode(query, StandardCharsets.UTF_8.name());
        String response;
        try {
            response = parseQuery(query);
            exchange.sendResponseHeaders(200, response.getBytes().length);


        } catch (Exception e) {
            // TODO Auto-generated catch block

            response = "error 500";
            e.printStackTrace();
            exchange.sendResponseHeaders(500, response.getBytes().length);


        }
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();

    }

    /**
   * returns the url parameters in a map
   * @param query
   * @return map
   */
  public static Map<String, String> queryToMap(String query){
    Map<String, String> result = new HashMap<String, String>();
    for (String param : query.split("&")) {
        String pair[] = param.split("=");
        if (pair.length>1) {
            result.put(pair[0], pair[1]);
        }else{
            result.put(pair[0], "");
        }
    }
    return result;
  }
}
