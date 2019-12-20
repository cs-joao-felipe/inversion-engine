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
package io.inversion.cloud.action.elastic.v03x;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.Api;
import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Endpoint;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.SC;
import io.inversion.cloud.service.Chain;
import io.inversion.cloud.service.Engine;

/**
 * 
 * @author kfrankic
 *
 */
public class ElasticDbRestAction extends Action
{
   ElasticDbGetAction get = new ElasticDbGetAction();
   //   ElasticDbDeleteHandler delete = new ElasticDbDeleteHandler();
   //   ElasticDbPostHandler   post   = new ElasticDbPostHandler();

   @Override
   public void run(Engine engine, Api api, Endpoint endpoint, Chain chain, Request req, Response res) throws Exception
   {
      String method = req.getMethod();
      if ("GET".equalsIgnoreCase(method))
      {
         get.run(engine, api, endpoint, chain, req, res);
      }
      //      else if ("DELETE".equalsIgnoreCase(method))
      //      {
      //         delete.service(service, api, endpoint, action, chain, req, res);
      //      }
      //      else if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))
      //      {
      //         post.service(service, api, endpoint, action, chain, req, res);
      //      }
      else
      {
         throw new ApiException(SC.SC_400_BAD_REQUEST, "This handler only supports GET requests");
      }
   }

   public void setGet(ElasticDbGetAction get)
   {
      this.get = get;
   }

   //   public void setDelete(ElasticDbDeleteHandler delete)
   //   {
   //      this.delete = delete;
   //   }
   //
   //   public void setPost(ElasticDbPostHandler post)
   //   {
   //      this.post = post;
   //   }

}