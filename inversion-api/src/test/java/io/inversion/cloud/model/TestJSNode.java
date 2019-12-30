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
package io.inversion.cloud.model;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

import io.inversion.cloud.utils.Utils;
import junit.framework.TestCase;

public class TestJSNode extends TestCase
{

   @Test
   public void testJsonPath1()
   {
      assertEquals("**.book.[(@_length-1)]", JSNode.fromJsonPath("$..book[(@.length-1)]"));
      assertEquals("**.book.[0,1]", JSNode.fromJsonPath("$..book[0,1]"));
      assertEquals("book.0.asdf", JSNode.fromJsonPath("book[0].asdf"));
      assertEquals(".store.**.price", JSNode.fromJsonPath(".store..price"));

      JSNode doc = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testJsonPath1.json")));
      JSArray found1 = null;
      JSArray found2 = null;

      found1 = doc.findAll("$..book[?(@.author = 'Herman Melville')]");

      assertEquals(1, found1.size());
      assertEquals("Herman Melville", found1.find("0.author"));

      found1 = doc.findAll("$.store..price");
      assertEquals(5, found1.size());

      found2 = doc.findAll("**.store.**.price");
      assertEquals(0, CollectionUtils.disjunction(found1, found2).size());

      found1 = doc.findAll("$..book[2]");
      found2 = doc.findAll("**.book.2");

      assertEquals("Herman Melville", found1.find("0.author"));
      assertEquals(0, CollectionUtils.disjunction(found1, found2).size());

      found1 = doc.findAll("$..*");
      found2 = doc.findAll("**.*");
      assertEquals(0, CollectionUtils.disjunction(found1, found2).size());

      found1 = doc.findAll("$.store.book[*].author");
      found2 = doc.findAll("store.book.*.author");
      assertEquals(0, CollectionUtils.disjunction(found1, found2).size());

      assertEquals(0, CollectionUtils.disjunction(found1, doc.findAll("**.store.book.*.author")).size());

      assertEquals(0, doc.findAll("*.store.book.*.author").size());

      found1 = doc.findAll("$..book[?(@.price = 12.99)]");
      assertEquals(1, found1.size());

      found1 = doc.findAll("$..book[?(@.price >= 12.99)]");
      System.out.println(found1);
      assertEquals(2, found1.size());

      found1 = doc.findAll("$..book[?(@.price!=8.99)]");
      System.out.println(found1);
      assertEquals(3, found1.size());

      found1 = doc.findAll("$..book[?(@.price != 100)]");
      System.out.println(found1);
      assertEquals(4, found1.size());

      found1 = doc.findAll("**.bicycle");
      System.out.println(found1);
      assertEquals(1, found1.size());

      found1 = doc.findAll("**.*.[?(@.price != 100)]");
      System.out.println(found1);
      assertEquals(5, found1.size());

      found1 = doc.findAll("$..[?(@.price != 100)]");
      System.out.println(found1);
      assertEquals(5, found1.size());

      found1 = doc.findAll("$..[?(@.price != 100)]", 1);
      System.out.println(found1);
      assertEquals(1, found1.size());

      found1 = doc.findAll("$..[?(@.price != 100)]", 3);
      System.out.println(found1);
      assertEquals(3, found1.size());

      found1 = doc.findAll("$..[?(@.price!=100)]", 3);
      System.out.println(found1);
      assertEquals(3, found1.size());
      
      found1 = doc.findAll("$..book[?(@.isbn)]", 2);
      System.out.println(found1);
      assertEquals(2, found1.size());

   }

   @Test
   public void testWith()
   {
      JSArray arr = new JSArray(1, 2, 3, 4);
      JSNode node = new JSNode().with("name", "value", "name2", "value2", "arr", arr);
      assertEquals("value", node.find("name"));
      assertEquals("value2", node.find("name2"));
      assertEquals(arr, node.find("arr"));
   }

   @Test
   public void testCollectNodes1()
   {
      JSArray found = null;
      JSNode doc = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testCollectNodes1.json")));

      found = doc.collect("data.*.basket.lineItems.code");
      assertTrue(found.size() == 0);

      found = doc.collect("data.*.basket.lineItems.*.code");
      assertTrue(found.size() == 2);

      found = doc.collect("lineItems.*.code");
      assertTrue(found.size() == 0);

      found = doc.collect("lineItems.code");
      assertTrue(found.size() == 1);

      found = doc.collect("data.*.basket.*");
      assertTrue(found.size() == 3);

      found = doc.collect("**.lineItems.*.code");
      assertTrue(found.size() == 3);

      System.out.println(found);
   }

   @Test
   public void testDiff1()
   {
      List found = null;
      JSNode doc1 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff1.1.json")));
      JSNode doc2 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff1.2.json")));

      JSArray patches = doc2.diff(doc1);

      doc1.patch(patches);

      assertTrue(doc1.toString().equals(doc2.toString()));

      System.out.println(found);
   }

   /**
    * This test was developed for an error in diff/patch that could result in the same JSNode
    * appearing multiple times in the object graph and causing serialization problems.
    *
    * The fix was to copy the patches before applying or after computing inside to the JSNode methods
    */
   @Test
   public void testDiff3()
   {
      JSNode stateDoc = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff3.1.json")));
      JSNode apiEvent = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff3.2.json")));

      JSNode meta = stateDoc.getNode("meta");
      meta.put("events", new JSArray(apiEvent));

      JSNode body = apiEvent.getNode("body");

      if (!body.isArray())
      {
         body = new JSArray(body);
      }

      JSArray patches = body.diff(stateDoc.getNode("data"));
      if (patches.size() > 0)
      {
         apiEvent.put("patches", patches);
         stateDoc.getNode("data").patch(patches);
      }

      stateDoc = JSNode.parseJsonNode(stateDoc.toString());

      assertEquals("028000003647", stateDoc.findString("data.0.basket.lineItems.1.code"));
      assertEquals("028000003647", stateDoc.findString("meta.events.0.body.basket.lineItems.1.code"));

   }

   @Test
   public void testDiff4()
   {
      JSNode array1 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff4.1.json")));
      JSNode array2 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff4.2.json")));

      JSArray patches = array1.diff(array2);

      array2.patch(patches);

   }

   /**
    * This test was developed to test the patch replace behavior, which caused a replacement of
    * a value in a json package to be set to null instead of the new value
    */
   @Test
   public void testDiff5()
   {
      List found = null;
      JSNode doc1 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff5.1.json")));
      JSNode doc2 = JSNode.parseJsonNode(Utils.read(getClass().getResourceAsStream("testDiff5.2.json")));

      JSArray patches = doc2.diff(doc1);

      doc1.patch(patches);

      assertTrue(doc1.toString().equals(doc2.toString()));
   }

}
