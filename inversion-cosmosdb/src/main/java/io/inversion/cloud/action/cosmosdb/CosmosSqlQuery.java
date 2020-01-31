package io.inversion.cloud.action.cosmosdb;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.KeyValue;

import com.microsoft.azure.documentdb.Document;
import com.microsoft.azure.documentdb.DocumentClient;
import com.microsoft.azure.documentdb.FeedOptions;
import com.microsoft.azure.documentdb.FeedResponse;
import com.microsoft.azure.documentdb.SqlParameter;
import com.microsoft.azure.documentdb.SqlParameterCollection;
import com.microsoft.azure.documentdb.SqlQuerySpec;

import io.inversion.cloud.action.sql.SqlQuery;
import io.inversion.cloud.action.sql.SqlQuery.Parts;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Results;
import io.inversion.cloud.model.Table;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.rql.Order.Sort;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.utils.Rows.Row;

/**
 * @see https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-getting-started
 * @author wells
 */
public class CosmosSqlQuery extends SqlQuery<CosmosDocumentDb>
{
   public CosmosSqlQuery()
   {

   }

   public CosmosSqlQuery(CosmosDocumentDb db, Table table, List<Term> terms)
   {
      super(table, terms);
      super.withDb(db);
   }

   public Results<Row> doSelect() throws Exception
   {
      Results results = new Results(this);
      CosmosDocumentDb db = getDb();

      String collectionUri = db.getCollectionUri(table);

      String sql = getPreparedStmt();
      sql = sql.replaceAll("\r", "");
      sql = sql.replaceAll("\n", " ");

      SqlParameterCollection params = new SqlParameterCollection();
      for (int i = 0; i < values.size(); i++)
      {
         KeyValue kv = values.get(i);
         String varName = asVariableName(i);
         params.add(new SqlParameter(varName, kv.getValue()));
      }

      SqlQuerySpec querySpec = new SqlQuerySpec(sql, params);
      FeedOptions options = new FeedOptions();

      boolean enableCrossPartitionQuery = true;

      Index partKey = table.getIndex("PartitionKey");
      if (partKey != null)
      {
         String partKeyCol = partKey.getColumn(0).getName();
         //-- the only way to turn cross partition querying off is to 
         //-- have a single partition key identified in your query.
         //-- If we have a pk term but it is nested in an expression
         //-- the we can't be sure the cosmos query planner can use it.
         Term partKeyTerm = findTerm(partKeyCol, "eq");
         enableCrossPartitionQuery = partKeyTerm == null || partKeyTerm.getParent() != null;
      }

      options.setEnableCrossPartitionQuery(enableCrossPartitionQuery);

      //-- for test cases and query explain
      String debug = "CosmosDb: SqlQuerySpec=" + querySpec.toJson() + " FeedOptions={enableCrossPartitionQuery=" + enableCrossPartitionQuery + "}";
      debug = debug.replaceAll("\r", "");
      debug = debug.replaceAll("\n", " ");
      debug = debug.replaceAll(" +", " ");
      Chain.debug(debug);
      results.withTestQuery(debug);
      //-- end test case debug stuff

      System.out.println();

      if (!isDryRun())
      {
         DocumentClient cosmos = db.getDocumentClient();
         FeedResponse<Document> queryResults = null;
         try
         {
            queryResults = cosmos.queryDocuments(collectionUri, querySpec, options);
         }
         catch (Exception ex)
         {
            System.err.println(ex.getMessage());
            System.err.println(debug);

            throw ex;
         }

         for (Document doc : queryResults.getQueryIterable())
         {
            String json = doc.toJson();
            JSNode node = JSNode.parseJsonNode(json);

            //-- removes all cosmos applied system keys that start with "_"
            //-- TODO: might want to make this a configuration option and/or
            //-- specifically blacklist known cosmos keys as this algorithm
            //-- will delete any _ prefixed property even if it was supplied
            //-- by the user
            for (String key : node.keySet())
            {
               if (key.startsWith("_"))
                  node.remove(key);
            }
            //-- the JSON returned from cosmos looks crazy, keys are all jumbled up.
            node.sortKeys();
            results.withRow(node);

         }
      }

      return results;
   }

   /**
    * Makes a few blanked tweaks to the sql created by the 
    * SqlQuery superclass to make it Cosmos compliant
    * 
    * Replaces: 
    * <li>SELECT "table".* FROM "table" -> SELECT * FROM table
    * <li>"table"."column"              -> table["column"]
    * 
    * @see https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-select
    * @see https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-select#quoted-property-accessor
    */
   protected String toSql(boolean preparedStmt)
   {
      String sql = super.toSql(preparedStmt);

      sql = sql.replace(columnQuote + table.getName() + columnQuote + ".*", "*");

      String regex = columnQuote + table.getName() + columnQuote + "\\." + columnQuote + "([^" + columnQuote + "]*)" + columnQuote;
      sql = sql.replaceAll(regex, table.getName() + "[\"$1\"]");

      sql = sql.replace(columnQuote + table.getName() + columnQuote, table.getName());

      return sql;
   }

   /**
    * The inversion configured primary index should contain at least
    * the document identifier and the partition key.  If you don't supply
    * a sort key on the query string that would default to adding two 
    * fields to the sort.  If you did not configure cosmos to have a compound
    * search index, that would fail...simply solution...if you did not supply
    * a sort on the query string, just search by the "id" field. 
    */
   protected List<Sort> getDefaultSorts(Parts parts)
   {
      return Arrays.asList(new Sort("id", true));
   }

   /**
    * Both offset and limit are required per cosmos spec.
    * 
    * @see https://docs.microsoft.com/en-us/azure/cosmos-db/sql-query-offset-limit
    */
   protected String printLimitClause(Parts parts, int offset, int limit)
   {
      if (offset < 0)
         offset = 0;

      if (limit <= 0)
         limit = 100;

      return "OFFSET " + offset + " LIMIT " + limit;
   }

   /**
    * Cosmos does not use "?" ansii sql style prepared statement vars, it uses
    * named variables prefixed with '@'. 
    */
   protected String asVariableName(int valuesPairIdx)
   {
      KeyValue kv = values.get(valuesPairIdx);
      return "@" + kv.getKey() + (valuesPairIdx + 1);
   }

}
