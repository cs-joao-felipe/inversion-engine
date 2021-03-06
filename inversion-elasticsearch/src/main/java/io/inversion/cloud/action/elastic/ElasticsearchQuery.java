/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.action.elastic;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.inversion.cloud.jdbc.db.JdbcDb;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.rql.Group;
import io.inversion.cloud.rql.Order;
import io.inversion.cloud.rql.Page;
import io.inversion.cloud.rql.Query;
import io.inversion.cloud.rql.Select;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.rql.Where;

/**
 * @author kfrankic
 * @see https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html
 */
// 
public class ElasticsearchQuery extends Query<ElasticsearchQuery, JdbcDb, Select<Select<Select, ElasticsearchQuery>, ElasticsearchQuery>, Where<Where<Where, ElasticsearchQuery>, ElasticsearchQuery>, Group<Group<Group, ElasticsearchQuery>, ElasticsearchQuery>, Order<Order<Order, ElasticsearchQuery>, ElasticsearchQuery>, Page<Page<Page, ElasticsearchQuery>, ElasticsearchQuery>>
{
   // identifies a nested path
   @JsonIgnore
   protected String nestedPath;

   public String getNestedPath()
   {
      return nestedPath;
   }

   //   private List<String> searchAfter;
   //
   //   private String       prevStart;
   //
   //   private List<String> source;
   //
   //   private List<String> excludes;

   public ElasticsearchQuery(Collection collection, Object terms)
   {
      super(collection, terms);
   }

   protected ElasticsearchPage createPage()
   {
      return new ElasticsearchPage(this);
   }

   protected void push(List<JSNode> stack, JSNode child)
   {

   }

   public JSNode getJson()
   {
      JSNode root = new JSNode();
      for (Term term : getTerms())
      {
         JSNode child = toJson(null, term);
      }

      return root;
   }

   public JSNode toJson(Term parent, Term child)
   {
      JSNode query = null;

      String token = child.getToken().toLowerCase();
      String field = child.getToken(0);

      Object value = child.getTerm(1).isLeaf() ? child.getToken(1) : toJson(child, child.getTerm(1));

      switch (token)
      {
         case "gt":
         case "ge":
         case "lt":
         case "le":
            //       "range" : {
            //            "age" : {
            //                "gte" : 10,
            //                "lte" : 20,
            //            }
            //        }
            query = new JSNode("range", new JSNode(field, new JSNode(token, value)));
            break;
         case "eq": // equal
         case "ne": // not equal
            if (value instanceof String && ((String) value).contains("*"))
            {
               //               {
               //                  "query": {
               //                      "wildcard" : { "user" : "ki*y" }
               //                  }
               //              }               
               query = new JSNode("wildcard", new JSNode(field, value));
            }
            else
            {
               //               {
               //                  "query": {
               //                    "term" : { "user" : "Kimchy" } 
               //                  }
               //                }
               query = new JSNode("term", new JSNode(field, value));
            }

            if ("ne".equals(token))
            {
               //               "bool" : {
               //                  "must_not" : {
               //                    "range" : {
               //                      "age" : { "gte" : 10, "lte" : 20 }
               //                    }
               //                  }
               //                }
               query = new JSNode("bool", new JSNode("must_not", query));
            }
            break;
         //         case "and":
         //            elastic = new BoolQuery();
         //            ((BoolQuery) elastic).divvyElasticList(elasticList);
         //            break;
         //         case "or":
         //            elastic = new BoolQuery();
         //            // add everything in the list to the 'should' to 'or' the list together.
         //            for (ElasticQuery eq : elasticList)
         //               ((BoolQuery) elastic).addShould(eq);
         //            break;
         case "sw":
            query = new JSNode("wildcard", new JSNode(field, value + "*"));
            break;
         case "ew":
            query = new JSNode("wildcard", new JSNode(field, "*" + value));
            break;
         case "w":
            query = new JSNode("wildcard", new JSNode(field, "*" + value + "*"));
            break;
         case "wo":

            //        "bool" : {
            //            "must_not" : {
            //              "range" : {
            //                "age" : { "gte" : 10, "lte" : 20 }
            //              }
            //            }
            //         }
            break;
         //         case "emp": // checks for empty strings AND null values
         //            elastic = new BoolQuery();
         //            ((BoolQuery) elastic).addShould(new Term(pred.getTerms().get(0).token, "", "emp"));
         //            mustNotBool = new BoolQuery();
         //            mustNotBool.addMustNot(new ExistsQuery(pred.getTerms().get(0).token));
         //            ((BoolQuery) elastic).addShould(mustNotBool);
         //            break;
         //         case "nemp": // checks for empty strings AND null values
         //            elastic = new BoolQuery();
         //            mustNotBool = new BoolQuery(); // 'mustNotBool' is used even-though it should be named 'mustBool'
         //            mustNotBool.addMustNot(new Term(pred.getTerms().get(0).token, "", "nemp"));
         //            ((BoolQuery) elastic).addMust(mustNotBool);
         //            BoolQuery mustBool = new BoolQuery();
         //            mustBool.addMust(new ExistsQuery(pred.getTerms().get(0).token));
         //            ((BoolQuery) elastic).addMust(mustBool);
         //            break;
         //         case "nn": // NOT NULL
         //            query = new ObjectNode("exists", new ObjectNode("field", field));
         //            break;
         //         case "n": // NULL
         //            elastic = new BoolQuery();
         //            ((BoolQuery) elastic).addMustNot(new ExistsQuery(pred.getTerms().get(0).token));
         //            break;
         //         case "in":
         //            termsList = new ArrayList<Term>(pred.terms);
         //            elastic = new Term(termsList.remove(0).token, pred.token);
         //            for (Term pTerm : termsList)
         //               ((Term) elastic).addValue(Parser.dequote(pTerm.token));
         //            break;
         //         case "out":
         //            elastic = new BoolQuery();
         //            termsList = new ArrayList<Term>(pred.terms);
         //            Term term = new Term(termsList.remove(0).token, pred.token);
         //            for (Term pTerm : termsList)
         //               term.addValue(Parser.dequote(pTerm.token));
         //            ((BoolQuery) elastic).addMustNot(term);
         //            break;
         case "search":
            //            "query": {
            //               "fuzzy" : { "user" : "ki" }
            //            }
            query = new JSNode("fuzzy", new JSNode(field, value));
            break;
         default :
            throw new RuntimeException("unexpected rql token: " + token);

      }
      return query;

   }

}