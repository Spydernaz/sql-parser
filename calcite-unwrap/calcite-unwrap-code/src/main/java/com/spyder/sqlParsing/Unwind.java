package com.spyder.sqlParsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParser.ConfigBuilder;
import org.apache.calcite.util.JsonBuilder;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.dialect.ParaccelSqlDialect;
import org.json.*;

/* Hello world!
 *
 */
public class Unwind {

    private static List<String> extractTableNames(SqlNode node) {
        final List<String> tables = new ArrayList<>();

        // If order by comes in the query.
        if (node.getKind().equals(SqlKind.ORDER_BY)) {
            // Retrieve exact select.
            node = ((SqlSelect) ((SqlOrderBy) node).query).getFrom();
        } else {
            node = ((SqlSelect) node).getFrom();

        }

        if (node == null) {
            return tables;
        }

        // Case when only 1 data set in the query.
        if (node.getKind().equals(SqlKind.AS)) {
            tables.add(((SqlBasicCall) node).operand(1).toString());
            return tables;
        }

        // Case when there are more than 1 data sets in the query.
        if (node.getKind().equals(SqlKind.JOIN)) {
            final SqlJoin from = (SqlJoin) node;

            // Case when only 2 data sets are in the query.
            if (from.getLeft().getKind().equals(SqlKind.AS)) {
                tables.add(((SqlBasicCall) from.getLeft()).operand(1).toString());
            } else {
                // Case when more than 2 data sets are in the query.
                SqlJoin left = (SqlJoin) from.getLeft();

                // Traverse until we get a AS.
                while (!left.getLeft().getKind().equals(SqlKind.AS)) {
                    tables.add(((SqlBasicCall) left.getRight()).operand(1).toString());
                    left = (SqlJoin) left.getLeft();
                }

                tables.add(((SqlBasicCall) left.getLeft()).operand(1).toString());
                tables.add(((SqlBasicCall) left.getRight()).operand(1).toString());
            }

            tables.add(((SqlBasicCall) from.getRight()).operand(1).toString());
            return tables;
        }

        return tables;
    }

    private static Map<String, String> extractWhereClauses(SqlNode node) {
        final Map<String, String> tableToPlaceHolder = new HashMap<>();

        // If order by comes in the query.
        if (node.getKind().equals(SqlKind.ORDER_BY)) {
            // Retrieve exact select.
            node = ((SqlOrderBy) node).query;
        }

        if (node == null) {
            return tableToPlaceHolder;
        }

        final SqlBasicCall where = (SqlBasicCall) ((SqlSelect) node).getWhere();

        if (where != null) {
            // Case when there is only 1 where clause
            if (where.operand(0).getKind().equals(SqlKind.IDENTIFIER)
                    && where.operand(1).getKind().equals(SqlKind.LITERAL)) {
                tableToPlaceHolder.put(where.operand(0).toString(), 
                        where.operand(1).toString()); 
                return tableToPlaceHolder;
            }

            final SqlBasicCall sqlBasicCallRight = where.operand(1);
            SqlBasicCall sqlBasicCallLeft = where.operand(0);

            // Iterate over left until we get a pair of identifier and literal.
            while (!sqlBasicCallLeft.operand(0).getKind().equals(SqlKind.IDENTIFIER)
                    && !sqlBasicCallLeft.operand(1).getKind().equals(SqlKind.LITERAL)) {
                tableToPlaceHolder.put(((SqlBasicCall) sqlBasicCallLeft.operand(1)).operand(0).toString(), 
                        ((SqlBasicCall) sqlBasicCallLeft.operand(1)).operand(1).toString()); 
                sqlBasicCallLeft = sqlBasicCallLeft.operand(0); // Move to next where condition.
            }

            tableToPlaceHolder.put(sqlBasicCallLeft.operand(0).toString(), 
                    sqlBasicCallLeft.operand(1).toString()); 
            tableToPlaceHolder.put(sqlBasicCallRight.operand(0).toString(), 
                    sqlBasicCallRight.operand(1).toString()); 
            return tableToPlaceHolder;
        }

        return tableToPlaceHolder;
    }

    public static void main(String[] args) throws Exception {       
        final String query = "SELECT e.first_name AS FirstName, s.salary AS Salary from employee AS e join salary AS s on e.emp_id=s.emp_id and e.id=s.id where (e.organization = 'Tesla' and s.organization = 'Tesla') or e.id = 5 order by e.id";
        final String query2 = "SELECT tbl1.col1 as COL1, tbl2.col2, tbl3.col3 as COL_THREE FROM tbl1, tb2, tb3 where COL1 > 1 order by tbl.col2";
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

        System.out.println(jsonObject.toString());
        // System.out.println("pause");
    }

    public static Object unwrap(SqlNode node) throws Exception {
        // If order by comes in the query.
        //@TODO: check for what type of quer
        String temp = node.getKind().name();
        
        switch (node.getKind()) {
            case ORDER_BY:
            // Returns JSON formatted: '{"query": <JSON>, "orderedBy": <STRING>}'
                JSONObject order_by = new JSONObject();
                JSONArray rorderList = new JSONArray();
                order_by.put("type", "ORDER_BY");

                order_by.put("query", unwrap((SqlNode)((SqlOrderBy) node).query));
                SqlNodeList orderList = ((SqlOrderBy) node).orderList;
                for (SqlNode el: orderList){
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
                for (SqlNode el: selectList){
                    rselectList.put(unwrap(el));
                }
                select.put("selectList", rselectList);
                // from statement
                SqlNode from  = (SqlNode)((SqlSelect) node).getFrom();
                select.put("from", unwrap(from));

                // Group By Logic
                JSONArray rgroupBy = new JSONArray();
                SqlNodeList groupByList = ((SqlSelect) node).getGroup();
                if (groupByList != null) {
                    for (SqlNode el: groupByList){
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
                // String actualId = (String) unwrap((SqlNode) ((SqlBasicCall) node).operand(0));
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
                // other_function.put("name", (String) ((SqlFunction) ((SqlBasicCall) node)).getName();
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

}
