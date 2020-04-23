import pytest
from model.sql_statement import *


class TestOrderByTokensSingle:
    def test_has_ordered_true_single(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID
                ORDER BY ProductID;"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.orderby.ordered == True

    def test_has_ordered_el_single(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID
                ORDER BY ProductID;"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.orderby.order_by_keys[0].element_name == "ProductID"

    def test_has_ordered_desc_single(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID
                ORDER BY ProductID DESC;"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.orderby.direction == "DESC"

    def test_has_ordered_asc_single(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID
                ORDER BY ProductID ASC;"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.orderby.direction == "ASC"

    def test_has_ordered_no_direction_single(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID
                ORDER BY ProductID;"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.orderby.direction == "ASC"

    def test_has_ordered_true_multiple(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID
                ORDER BY ProductID, SpecialOfferID;"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.orderby.ordered == True

    def test_has_ordered_el_multiple(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID
                ORDER BY ProductID, SpecialOfferID;"""
        tokenised_sql = tsql_select_statement(q)
        assert len(tokenised_sql.orderby.order_by_keys) == 2

    def test_has_ordered_desc_multiple(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID
                ORDER BY ProductID, SpecialOfferID DESC;"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.orderby.direction == "DESC"

    def test_has_ordered_asc_multiple(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID
                ORDER BY ProductID, SpecialOfferID ASC;"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.orderby.direction == "ASC"

    def test_has_ordered_no_direction_multiple(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID
                ORDER BY ProductID, SpecialOfferID;"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.orderby.direction == "ASC"

