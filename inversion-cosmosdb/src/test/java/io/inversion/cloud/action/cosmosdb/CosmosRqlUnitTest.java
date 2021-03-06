package io.inversion.cloud.action.cosmosdb;

import io.inversion.cloud.rql.AbstractRqlTest;
import io.inversion.cloud.rql.RqlValidationSuite;

public class CosmosRqlUnitTest extends AbstractRqlTest
{

   public CosmosRqlUnitTest()
   {
      queryClass = CosmosSqlQuery.class.getName();
      db = new CosmosDocumentDb();
   }

   /**
    * The majority of these should be postgres/h2 compatible.  Mysql and MsSQL 
    * will probably have to customize most of these.
    */
   @Override
   protected void customizeUnitTestSuite(RqlValidationSuite suite)
   {

      suite//
           .withResult("eq", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"orderID\\\"] = @orderID1 AND orders[\\\"shipCountry\\\"] = @shipCountry2 ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@orderID1\",\"value\":\"10248\"},{\"name\":\"@shipCountry2\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("ne", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE (NOT (orders[\\\"shipCountry\\\"] = @shipCountry1)) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("n", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE IS_NULL (orders[\\\"shipRegion\\\"]) ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("nn", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"shipRegion\\\"] <> null ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("emp", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE (orders[\\\"shipRegion\\\"] IS NULL OR orders[\\\"shipRegion\\\"] = '') ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("nemp", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE (orders[\\\"shipRegion\\\"] IS NOT NULL AND orders[\\\"shipRegion\\\"] != '') ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("likeMiddle", "The 'like' RQL operator for CosmosDb expects a single wildcard at the beginning OR the end of a value.  CosmosDb does not really support 'like' but compatible 'like' statements are turned into 'sw' or 'ew' statments that are supported.")//
           .withResult("likeStartsWith", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE STARTSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"Franc\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("likeEndsWith", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE ENDSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"ance\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("sw", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE STARTSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"Franc\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("ew", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE ENDSWITH (orders[\\\"shipCountry\\\"], @shipCountry1) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"nce\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("w", "CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.")//
           .withResult("wo", "CosmosDb supports 'sw' and 'ew' but not 'w' or 'wo' functions.")//
           .withResult("lt", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"freight\\\"] < @freight1 ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@freight1\",\"value\":10}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("le", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"freight\\\"] <= @freight1 ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@freight1\",\"value\":10}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("gt", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"freight\\\"] > @freight1 ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@freight1\",\"value\":3.67}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("ge", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"freight\\\"] >= @freight1 ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@freight1\",\"value\":3.67}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("in", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"shipCity\\\"] IN(@shipCity1, @shipCity2) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCity1\",\"value\":\"Reims\"},{\"name\":\"@shipCity2\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("out", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"shipCity\\\"] NOT IN(@shipCity1, @shipCity2) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCity1\",\"value\":\"Reims\"},{\"name\":\"@shipCity2\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("and", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE orders[\\\"orderID\\\"] = @orderID1 AND orders[\\\"shipCountry\\\"] = @shipCountry2 ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@orderID1\",\"value\":\"10248\"},{\"name\":\"@shipCountry2\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("or", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE (orders[\\\"shipCity\\\"] = @shipCity1 OR orders[\\\"shipCity\\\"] = @shipCity2) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCity1\",\"value\":\"Reims\"},{\"name\":\"@shipCity2\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("not", "SqlQuerySpec={\"query\":\"SELECT * FROM orders WHERE NOT ((orders[\\\"shipCity\\\"] = @shipCity1 OR orders[\\\"shipCity\\\"] = @shipCity2)) ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCity1\",\"value\":\"Reims\"},{\"name\":\"@shipCity2\",\"value\":\"Charleroi\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("as", "SqlQuerySpec={\"query\":\"SELECT *, orders[\\\"orderid\\\"] AS \\\"order_identifier\\\" FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("includes", "SqlQuerySpec={\"query\":\"SELECT orders[\\\"shipCountry\\\"], orders[\\\"shipCity\\\"] FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("distinct", "SqlQuerySpec={\"query\":\"SELECT DISTINCT orders[\\\"shipCountry\\\"] FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("count1", "SqlQuerySpec={\"query\":\"SELECT COUNT(*) FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("count2", "SqlQuerySpec={\"query\":\"SELECT COUNT(@null1) FROM orders ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@null1\",\"value\":\"1\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("count3", "SqlQuerySpec={\"query\":\"SELECT COUNT(orders[\\\"shipRegion\\\"]) FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("countAs", "SqlQuerySpec={\"query\":\"SELECT COUNT(*) AS \\\"countOrders\\\" FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("sum", "SqlQuerySpec={\"query\":\"SELECT SUM(orders[\\\"freight\\\"]) FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("sumAs", "SqlQuerySpec={\"query\":\"SELECT SUM(orders[\\\"freight\\\"]) AS \\\"Sum Freight\\\" FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("sumIf", "SqlQuerySpec={\"query\":\"SELECT SUM(CASE WHEN orders[\\\"shipCountry\\\"] = @shipCountry1 THEN 1 ELSE 0 END) AS \\\"French Orders\\\" FROM orders ORDER BY orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@shipCountry1\",\"value\":\"France\"}]} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("min", "SqlQuerySpec={\"query\":\"SELECT MIN(orders[\\\"freight\\\"]) FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("max", "SqlQuerySpec={\"query\":\"SELECT MAX(orders[\\\"freight\\\"]) FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("groupCount", "SqlQuerySpec={\"query\":\"SELECT orders[\\\"shipCountry\\\"], COUNT(*) AS \\\"countryCount\\\" FROM orders GROUP BY orders[\\\"shipCountry\\\"] ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("offset", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("limit", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("page", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("pageNum", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("after", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"id\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("sort", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"shipCountry\\\"] DESC, orders[\\\"shipCity\\\"] ASC\"} FeedOptions={enableCrossPartitionQuery=true}")//
           .withResult("order", "SqlQuerySpec={\"query\":\"SELECT * FROM orders ORDER BY orders[\\\"shipCountry\\\"] ASC, orders[\\\"shipCity\\\"] DESC\"} FeedOptions={enableCrossPartitionQuery=true}")//
      ;
   }
   
}
