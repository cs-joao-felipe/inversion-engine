/*
 * Copyright (c) 2015-2018 Rocket Partners, LLC
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
package io.inversion.cloud.action.sql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Collection;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.rql.Term;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;
import io.inversion.cloud.utils.SqlUtils;
import io.inversion.cloud.utils.Utils;

public class SqlSuggestAction extends Action<SqlSuggestAction>
{
   protected HashSet<String> whitelist    = new HashSet();

   protected String          propertyProp = "property";
   protected String          searchProp   = "value";
   protected String          tenantCol    = "tenantId";

   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
            String propertyProp = chain.getConfig("propertyProp", this.propertyProp);
            String searchProp = chain.getConfig("searchProp", this.searchProp);
            String tenantCol = chain.getConfig("tenantCol", this.tenantCol);
      
            String whitelistStr = chain.getConfig("whitelist", null);
            Set<String> whitelist = this.whitelist;
            if (whitelistStr != null)
            {
               whitelist = new HashSet(Utils.explode(",", whitelistStr.toLowerCase()));
            }
      
            String properties = req.removeParam(propertyProp);
      
            if (Utils.empty(properties))
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Missing query param '" + propertyProp + "' which should be a comma separated list of collection.property names to query");
      
            if (!properties.contains("."))
               throw new ApiException(SC.SC_400_BAD_REQUEST, "Query param '" + propertyProp + "' must be in the format '{collection}.{property}'");
      
            String value = req.removeParam(searchProp);
            if (Utils.empty(value))
            {
               value = "";
            }
            else
            {
               value = value.trim();
               value = value.replace("`", "");
               value = value.replace("\'", "");
               value = value.replace("\"", "");
            }
      
            List<String> propertyList = Utils.explode(",", properties);
            String firstProp = propertyList.get(0);
            String collectionKey = firstProp.substring(0, firstProp.indexOf("."));
      
            Collection collection = api.getCollection(collectionKey);//getApi().getCollection(collectionKey, SqlDb.class);
            if (collection == null)
               throw new ApiException(SC.SC_404_NOT_FOUND, "Collection '" + collectionKey + "' could not be found");
            
            String sql = "";
            sql += " SELECT DISTINCT " + searchProp;
            sql += " \r\n FROM (";
      
            for (int i = 0; i < propertyList.size(); i++)
            {
               String prop = propertyList.get(i);
      
               if (!whitelist.contains(prop.toLowerCase()))
                  throw new ApiException(SC.SC_400_BAD_REQUEST, "One of the properties you requested is not in the SuggestHandler whitelist, please edit your query or your config and try again");
      
               if (prop.indexOf(".") < 0)
                  throw new ApiException(SC.SC_400_BAD_REQUEST, "Query param '" + propertyProp + "' must be of the form '" + propertyProp + "=collection.property[,collection.property...]");
      
               collectionKey = prop.substring(0, prop.indexOf("."));
      
               String tableName = SqlUtils.check(collection.getEntity().getTable().getName());
               String column = SqlUtils.check(prop.substring(prop.indexOf(".") + 1, prop.length()));
      
               sql += " \r\nSELECT DISTINCT " + column + " AS " + searchProp + " FROM " + tableName + " WHERE " + column + " LIKE '%" + SqlUtils.check(value) + "%' AND " + column + " != ''";
      
               if (api.isMultiTenant() && api.findTable(tableName).getColumn(tenantCol) != null)
                  sql += " AND " + tenantCol + "=" + Chain.peek().getUser().getTenantId();
      
               if (i + 1 < propertyList.size())
                  sql += " \r\nUNION ";
            }
            sql += " \r\n ) as v ";
            sql += " \r\n ORDER BY CASE WHEN " + searchProp + " LIKE '" + SqlUtils.check(value) + "%' THEN 0 ELSE 1 END, " + searchProp;
      
            // removing the tenantId here so the Get Handler won't add an additional where clause to the sql we are sending it
            req.removeParam("tenantId");
      
            SqlDb db = (SqlDb) collection.getDb();
            chain.put("db", db.getName());
            chain.put("select", sql);
   }

   public List<String> getWhitelist()
   {
      return new ArrayList<String>(whitelist);
   }

   public void setWhitelist(java.util.Collection<String> whitelist)
   {
      this.whitelist.clear();
      for (String entry : whitelist)
      {
         String lowercaseEntry = entry.toLowerCase();
         this.whitelist.add(lowercaseEntry);
      }
   }
   
   public SqlSuggestAction withWhitelist(java.util.Collection<String> whitelist)
   {
      setWhitelist(whitelist);
      return this;
   }
   
   public SqlSuggestAction withWhitelist(String... whitelist)
   {
      this.whitelist.clear();
      for (String whitelistEntry : Utils.explode(",", whitelist))
      {
         this.whitelist.add(whitelistEntry.toLowerCase());
      }

      return this;
   }

}
