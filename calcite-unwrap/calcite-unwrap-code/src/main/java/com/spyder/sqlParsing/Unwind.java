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

/* Hello world!
 *
 */
public class Unwind {
    public static String parseQuery(String query) throws Exception {
        final String query2 = "SELECT tbl1.col1 as COL1, tbl2.col2, tbl3.col3 as COL_THREE FROM tbl1, tbl2, tb3l where COL1 > 1 and COL1 <= 25 order by tbl.col2";
        String q = "SELECT    t1.col2 AS col1 \n" + "    , t1.col6 AS col2 \n" + "    , t2.col8 AS col3 \n"
                + "    , COALESCE(t3.col4, 'unknown') AS col4 \n" + "    , SUM(t1.col5) AS col5 \n"
                + "FROM    table1 t1 \n" + "    INNER JOIN table2 t2 \n" + "        ON t1.id = t2.id \n"
                + "    LEFT JOIN ( \n" + "        SELECT    t3.id, t3.col4 \n" + "        FROM    table3 t3 \n"
                + "        GROUP BY \n" + "            t3.id, t3.col4 \n" + "    ) t3 \n"
                + "        ON t1.id = t3.id \n" + "WHERE    (t1.col3 = 1 AND t2.col4 = 2) OR \n"
                + "    (t1.col3 = 2 AND t2.col4 = 5) \n" + "GROUP BY \n" + "    col1 \n" + "    , t1.col6 \n"
                + "    , t2.col8 \n" + "    , COALESCE(t3.col4, 'unknown') \n" + "ORDER BY t1.col2, col2";

        final SqlParser parser = SqlParser.create(query);
        final SqlNode sqlNode = parser.parseQuery();
        // final SqlSelect sqlSelect = (SqlSelect) sqlNode;
        // final SqlJoin from = (SqlJoin) sqlSelect.getFrom();

        // JSONObject jsonObject = JsonBuilderFactory.buildObject();
        JSONObject jsonObject = new JSONObject();
        jsonObject.append("results", unwrap(sqlNode));

        return (jsonObject.toString());
        // System.out.println("pause");
    }

    public static Object unwrap(SqlNode node) throws Exception {
        // If order by comes in the query.
        // @TODO: check for what type of quer
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
                JSONObject select = new JSONObject();
                select.put("type", "SELECT");

                // select list
                JSONArray rselectList = new JSONArray();
                SqlNodeList selectList = ((SqlSelect) node).getSelectList();
                for (SqlNode el : selectList) {
                    rselectList.put(unwrap(el));
                }
                select.put("selectList", rselectList);
                // from statement
                SqlNode from = (SqlNode) ((SqlSelect) node).getFrom();
                select.put("from", unwrap(from));

                // Group By Logic
                JSONArray rgroupBy = new JSONArray();
                SqlNodeList groupByList = ((SqlSelect) node).getGroup();
                if (groupByList != null) {
                    for (SqlNode el : groupByList) {
                        rgroupBy.put(unwrap(el));
                    }
                    select.put("groupBy", rgroupBy);
                }

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
        context.setHandler(Unwind::handleRequest);
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
