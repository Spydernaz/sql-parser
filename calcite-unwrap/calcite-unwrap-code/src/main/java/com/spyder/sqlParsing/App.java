package com.spyder.sqlParsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.calcite.sql.*;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParser.ConfigBuilder;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.dialect.ParaccelSqlDialect;

/* Hello world!
 *
 */
public class App {

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

    public static void main(String[] args) throws SqlParseException {       
        final String query = "SELECT e.first_name AS FirstName, s.salary AS Salary from employee AS e join salary AS s on e.emp_id=s.emp_id and e.id=s.id where e.organization = 'Tesla' and s.organization = 'Tesla'";
        final SqlParser parser = SqlParser.create(query);
        final SqlNode sqlNode = parser.parseQuery();
        final SqlSelect sqlSelect = (SqlSelect) sqlNode;
        final SqlJoin from = (SqlJoin) sqlSelect.getFrom();

        // Extract table names/data sets, For above SQL query : [e, s]
        final List<String> tables = extractTableNames(from);

        // Extract where clauses, For above SQL query : [e.organization -> 'Tesla', s.organization -> 'Tesla']
        final Map<String, String> whereClauses = extractWhereClauses(sqlSelect);

        System.out.println("done?");
    }

    public static void main2(String[] args) throws Exception {
        String q = "SELECT    t1.col2 AS col1 \n" + "    , t1.col6 AS col2 \n" + "    , t2.col8 AS col3 \n"
                + "    , COALESCE(t3.col4, 'unknown') AS col4 \n" + "    , SUM(t1.col5) AS col5 \n"
                + "FROM    table1 t1 \n" + "    INNER JOIN table2 t2 \n" + "        ON t1.id = t2.id \n"
                + "    LEFT JOIN ( \n" + "        SELECT    t3.id, t3.col4 \n" + "        FROM    table3 t3 \n"
                + "        GROUP BY \n" + "            t3.id, t3.col4 \n" + "    ) t3 \n"
                + "        ON t1.id = t3.id \n" + "WHERE    (t1.col3 = 1 AND t2.col4 = 2) OR \n"
                + "    (t1.col3 = 2 AND t2.col4 = 5) \n" + "GROUP BY \n" + "    t1.col2 \n" + "    , t1.col6 \n"
                + "    , t2.col8 \n" + "    , COALESCE(t3.col4, 'unknown') \n" + "ORDER BY t1.col2";
        String selectQuery = "SELECT B.A as COL FROM B where c > 15 order by d asc";
        // SqlParser parser = SqlParser.create(q, SqlParser.configBuilder().build());
        SqlParser parser = SqlParser.create(selectQuery, SqlParser.configBuilder().build());
        SqlNode node = parser.parseStmt();
        // parser.getMetadata();
        
        visit((SqlCall) node);

        System.out.println("done");

    }

    public static void visit(SqlCall call) throws Exception {
        final SqlKind kind = call.getKind();
        System.out.println("Kind is: " + kind);

        final List<SqlNode> operators = call.getOperandList();
        while (operators.remove(null)) {}

        for (SqlNode op:  operators){
            System.out.println(op.getClass());
        }

    }
    public static void blank(SqlCall call) {
        final SqlKind kind = call.getKind();
        System.out.println("Kind is: " + kind);
        final List<SqlNode> operators = call.getOperandList();

        for (int i = 0; i < operators.size(); i++) {
            
            System.out.println(call.getOperandList().get(i));
        }
    }

}
