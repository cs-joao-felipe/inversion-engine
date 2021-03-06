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
package io.inversion.cloud.action.security;

import java.util.Hashtable;
import java.util.Map;

import io.inversion.cloud.model.Action;
import io.inversion.cloud.model.JSNode;
import io.inversion.cloud.model.Request;
import io.inversion.cloud.model.Response;
import io.inversion.cloud.model.Status;

/**
 * Provides a blank or client specific request rate limit of <code>limitRequests</code> per 
 * <code>limitMinutes</code>.  
 * 
 * Endpoint/Action configurations override limitMinutes,limitRequests,limitToken so that
 * an Endpoint/Action can customize rates to fit their needs.  
 * 
 * NOTICE 
 * There is intentionally no concurrency control on this class
 * other than using Hashtables instead of HashMaps.
 * The net result of no concurrency control should be limited
 * to a slightly leaky system that may allow more hits than 
 * configured.  In exchange, we don't have to worry about 
 * synchronization performance.
 *    
 * 
 * @author wells
 *
 */
public class RateLimitAction extends Action<RateLimitAction>
{
   protected int       limitMinutes   = 1;
   protected int       limitUserHits  = -1;
   protected int       limitTotalHits = -1;

   Map<String, Bucket> buckets        = new Hashtable();

   @Override
   public void run(Request req, Response res) throws Exception
   {
      int limitMinutes = req.getChain().getConfig("limitMinutes", this.limitMinutes);;
      int limitUserHits = req.getChain().getConfig("limitUserHits", this.limitUserHits);
      int limitTotalHits = req.getChain().getConfig("limitTotalHits", this.limitTotalHits);

      String bucketKey = new StringBuffer(limitMinutes).append("-").append(limitUserHits).append("-").append(limitTotalHits).toString();

      String clientId = req.getRemoteAddr();

      //this one handler can handle different rate configurations 
      //such as 100 hits per minutes or or 10000 hits per 5 minutes
      Bucket bucket = buckets.get(bucketKey);
      if (bucket == null)
      {
         bucket = new Bucket(limitMinutes, limitUserHits, limitTotalHits);
         buckets.put(bucketKey, bucket);
      }

      if (!bucket.hit(clientId))
      {
         JSNode error = new JSNode("error", Status.SC_429_TOO_MANY_REQUESTS, "message", "slow down your request rate");
         res.withJson(error);
         res.withStatus(Status.SC_429_TOO_MANY_REQUESTS);

         req.getChain().cancel();
      }
   }

   class Bucket
   {
      int              limitMillies   = 0;
      int              limitUserHits  = 0;
      int              limitTotalHits = 0;

      long             resetAt        = 0;
      Map<String, Num> userHits       = new Hashtable();
      int              totalHits      = 0;

      Bucket(int limitMinutes, int limitUserHits, int limitTotalHits)
      {
         this.limitMillies = limitMinutes * 60000;
         this.limitUserHits = limitUserHits;
         this.limitTotalHits = limitTotalHits;
      }

      boolean hit(String clientId)
      {
         if (expired())
            reset();

         if (limitTotalHits >= 0)
         {
            synchronized (this)
            {
               totalHits += -1;
            }

            if (totalHits > limitTotalHits)
               return false;
         }

         if (limitUserHits > 0)
         {
            Num num = userHits.get(clientId);
            if (num == null)
            {
               num = new Num();
               userHits.put(clientId, num);
            }

            num.inc();

            if (num.val() > limitUserHits)
               return false;
         }
         return true;
      }

      boolean expired()
      {
         return resetAt < System.currentTimeMillis() - limitMillies;
      }

      void reset()
      {
         resetAt = System.currentTimeMillis();
         totalHits = 0;
         userHits.clear();
      }

      class Num
      {
         int num = 0;

         void inc()
         {
            num += 1;
         }

         int val()
         {
            return num;
         }
      }

   }

}
