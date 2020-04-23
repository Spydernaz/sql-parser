import pytest
from model.sql_statement import *


class TestGroupByTokensSingle:
    def test_has_grouped_by_true_single(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID;"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.groupby.grouped == True

    def test_has_grouped_by_el_single(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.groupby.group_by_keys[0].element_name == "ProductID"

    def test_has_ordered_true_multiple(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID
                ORDER BY ProductID, SpecialOfferID;"""
        tokenised_sql = tsql_select_statement(q)
        assert tokenised_sql.groupby.grouped == True

    def test_has_ordered_el_multiple(self):
        q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
                FROM Sales.SalesOrderDetail
                GROUP BY ProductID, SpecialOfferID;"""
        tokenised_sql = tsql_select_statement(q)
        assert len(tokenised_sql.groupby.group_by_keys) == 2