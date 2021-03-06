/*
 * Copyright (c) 2015-2019 Rocket Partners, LLC
 * https://github.com/inversion-api
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.inversion.cloud.rql;

import java.util.List;

import io.inversion.cloud.model.ApiException;
import io.inversion.cloud.model.Index;
import io.inversion.cloud.model.Status;
import io.inversion.cloud.utils.Rows.Row;

public class Where<T extends Where, P extends Query> extends Builder<T, P>
{

   public Where(P query)
   {
      super(query);
      withFunctions("_key", "and", "or", "not", "eq", "ne", "n", "nn", "like", "sw", "ew", "lt", "le", "gt", "ge", "in", "out", "if", "w", "wo", "emp", "nemp");
   }

   protected boolean addTerm(String token, Term term)
   {
      if (functions.contains(token))
      {
         term = transform(term);
         if (term.getParent() == null && term.hasToken("and"))//"unwrap" root and terms as redundant
         {
            for (Term t : term.getTerms())
            {
               t.withParent(null);
               super.addTerm(t.getToken(), t);
            }
         }
         else
         {
            super.addTerm(term.getToken(), term);
         }
         return true;
      }
      else
      {
         return super.addTerm(token, term);
      }

   }

   protected Term transform(Term parent)
   {
      Term transformed = parent;

      for (Term child : parent.getTerms())
      {
         if (!child.isLeaf())
         {
            if (!functions.contains(child.getToken()))
               throw new ApiException(Status.SC_400_BAD_REQUEST, "Invalid where function token '" + child.getToken() + "' : " + parent);
            transform(child);
         }
      }

      if (parent.hasToken("_key"))
      {
         String indexName = parent.getToken(0);

         Index index = getParent().getCollection().getIndex(indexName);
         if (index == null)
            throw new ApiException(Status.SC_400_BAD_REQUEST, "You can't use the _key() function unless your table has a unique index");

         if (index.size() == 1)
         {
            Term t = Term.term(null, "in", index.getColumn(0).getColumnName());
            List<Term> children = parent.getTerms();
            for (int i = 1; i < children.size(); i++)
            {
               Term child = children.get(i);
               t.withTerm(child);
            }
            if (t.getNumTerms() == 2)
               t.withToken("eq");

            transformed = t;
         }
         else
         {
            //collection/valCol1~valCol2,valCol1~valCol2,valCol1~valCol2
            //keys(valCol1~valCol2,valCol1~valCol2,valCol1~valCol2)

            //or( and(eq(col1,val),eq(col2,val)), and(eq(col1,val),eq(col2,val)), and(eq(col1val), eq(col2,val)) 
            Term or = Term.term(null, "or");
            List<Term> children = parent.getTerms();
            transformed = or;

            for (int i = 1; i < children.size(); i++)
            {
               Term child = children.get(i);
               if (!child.isLeaf())
                  throw new ApiException(Status.SC_400_BAD_REQUEST, "Entity key value is not a leaf node: " + child);

               Row keyParts = getParent().getCollection().decodeKey(index, child.getToken());
               Term and = Term.term(or, "and");
               for (String key : keyParts.keySet())
               {
                  and.withTerm(Term.term(and, "eq", key, keyParts.get(key).toString()));
               }
            }
            if (or.getNumTerms() == 1)
            {
               transformed = or.getTerm(0);
               transformed.withParent(null);
            }
         }
      }

      if (parent.getParent() != null && transformed != parent)
         parent.getParent().replaceTerm(parent, transformed);

      return transformed;
   }

   public List<Term> getFilters()
   {
      return getTerms();
   }
}
