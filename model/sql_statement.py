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

import re

class tsql_statement(object):
    """
    A class that represents a T-SQL Select Statement
    """

    def __init__(self, query):
        """ Get the query and choose a parser  """
        self.old_query = query
        # strip all newline characters 
        # assumes UNIX line endings (LF a.k.a. "\n")
        self.query = query.replace("\n"," ")
        self.query = f" {self.query} "
        self.query = self.query.replace(";", " ; ")

    def __initial_tokenisation__(self):
        pass


class tsql_select_statement(tsql_statement):
    """
    A class that represents a T-SQL Select Statement
    """

    def __init__(self, query):
        """ Get the query and begin parsing
        ASSUMTION: SEMANTICLY AND SYNTACTICLY CORRECT T-SQL QUERY
        """
        super().__init__(query)
        # super(tsql_select_statement, self).__init__(query)
        self.action = "SELECT"
        
        # Set the possible Clause Phrases
        self.clause_phrases = {}
        self.clause_phrases["WITH"] = None
        self.clause_phrases["SELECT"] = None
        self.clause_phrases["FROM"] = None
        self.clause_phrases["WHERE"] = None
        self.clause_phrases["GROUP BY"] = None
        self.clause_phrases["HAVING"] = None
        self.clause_phrases["ORDER BY"] = None
        self.clause_phrases["OPTIONS"] = None

        self.fields = []
        self.tables = []
        self.databases = []
        self.join_keys = []

        self.__initial_tokenisation__()

        self.orderby = tokenised_order_by(self.clause_phrases["ORDER BY"])
        self.groupby = tokenised_group_by(self.clause_phrases["GROUP BY"])
        
    def __initial_tokenisation__(self):
        TOKENS = ["WITH", "SELECT", "FROM", "WHERE",
                    "GROUP BY", "HAVING", "ORDER BY", "OPTIONS"]

        # SELECT_CLAUSE_TOKENS = ["TOP", "ALL", "DISTINCT", "AS", "INTO"]
        # FROM_CLAUSE_TOKENS = ["INNER", "OUTER", "LEFT", "RIGHT", "JOIN", "ON"]
        # WHERE_CLAUSE_TOKENS = ["AND", "OR"]
        # ORDER_BY_TOKENS = ["ASC", "DESC"]

        clauses = {}
        previous_token = None
        for token in TOKENS:
            loc = self.query.upper().find(f" {token} ")
            if loc != -1:
                clauses[token] = {}
                clauses[token]["start"] = loc
                if previous_token:
                    clauses[previous_token]["end"] = loc
                previous_token = token

        clauses[previous_token]["end"] = len(self.query)
        for c in clauses:
            self.clause_phrases[c] = f"{self.query[clauses[c]['start']:clauses[c]['end']]} "

class tokenised_select_elements(object):
    def __init__(self, name):
        self.element_name = name


class tokenised_order_by(object):
    def __init__(self, clause):
        ORDER_BY_TOKENS = ["ASC", "DESC"]
        if clause:
            self.ordered = True
            self.direction = "ASC"
            self.order_by_keys = []

            # DROP ORDER BY RESERVED WORDS AND CHECK FOR DIRECTION
            self.clause = clause
            self.old_clause = clause
            self.clause = self.clause.replace(" ORDER BY ", '')
            loc = self.clause.upper().find(" DESC ")
            if loc != -1:
                self.clause = self.direction = "DESC"
                self.clause = self.clause.replace(" DESC ", '')
            else:    
                self.clause = self.clause.replace(" ASC ", '')

            self.clause = self.clause.replace(";", '')
            self.clause = self.clause.replace(" ", '')
            element_keys = re.split(r',\s*(?![^()]*\))', self.clause)
            for el in element_keys:
                self.order_by_keys.append(tokenised_select_elements(el))
        else:
            self.ordered = False
            self.direction = None
            self.order_by_keys = None

class tokenised_group_by(object):
    def __init__(self, clause):
        if clause:
            self.grouped = True
            self.group_by_keys = []

            # DROP ORDER BY RESERVED WORDS AND CHECK FOR DIRECTION
            self.clause = clause
            self.old_clause = clause
            self.clause = self.clause.replace(" GROUP BY ", '')

            self.clause = self.clause.replace(";", '')
            self.clause = self.clause.replace(" ", '')
            element_keys = re.split(r',\s*(?![^()]*\))', self.clause)
            for el in element_keys:
                self.group_by_keys.append(tokenised_select_elements(el))
        else:
            self.grouped = False
            self.group_by_keys = None
