#!/usr/bin/env python3

# -*- coding: utf-8 -*-

"""
###########################################################################
#
#   Project Name:   SQL Tokeniser
#   Project Date:   Mar, 2020
#   Purpose:        Take a SQL Query and Tokenise it such that
#                   we can return back the objects in it
#
###########################################################################
"""

__author__ = ["Nathaniel Vala"]
__copyright__ = "Copyright 2020, Spydernaz"
__credits__ = ["Nathaniel Vala"]
__license__ = "GPL"
__version__ = "0.0.1"
__maintainer__ = ["Nathaniel Vala"]
__status__ = "Dev"



from model.sql_statement import *
q = """SELECT ProductID, SpecialOfferID, AVG(UnitPrice) AS [Average Price], SUM(LineTotal) AS SubTotal
FROM Sales.SalesOrderDetail
GROUP BY ProductID, SpecialOfferID
ORDER BY ProductID DESC;"""
ts = tsql_select_statement(q)

print(ts.orderby.clause)
ts.orderby.ordered
ts.orderby.direction
ts.orderby.order_by_keys