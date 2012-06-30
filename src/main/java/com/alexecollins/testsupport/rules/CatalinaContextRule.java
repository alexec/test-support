package com.alexecollins.testsupport.rules;

import org.apache.log4j.Logger;
import org.apache.tomcat.dbcp.dbcp.BasicDataSource;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.DriverManager;


/**
 * Creates an context for tests using an Apache Tomcat server.xml.
 *
 * https://blogs.oracle.com/randystuph/entry/injecting_jndi_datasources_for_junit
 *
 * @author alex.e.c@gmail.com
 */
public class CatalinaContextRule implements TestRule {

    public static final Logger LOGGER = Logger.getLogger(CatalinaContextRule.class);

    /**
     * Creates all the sub-contexts for a name.
     */
    public static void createSubContexts(Context ctx, String name) {
        String subContext = "";
        for (String x : name.substring(0, name.lastIndexOf('/')).split("/")) {
            subContext += x;
            try {
                ctx.createSubcontext(subContext);
            } catch (NamingException e) {
                // nop
            }
            subContext += '/';
        }
    }

    private final File serverXml;

    public CatalinaContextRule(File serverXml, Object target) {
        if (serverXml == null || !serverXml.isFile()) {throw new IllegalArgumentException();}
        if (target == null) {throw new IllegalArgumentException();}
        this.serverXml = serverXml;
    }

    public Statement apply(final Statement statement, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                createInitialContext();
                try {
                    statement.evaluate();
                } finally {
                    destroyInitialContext();
                }
            }
        };
    }

    private void createInitialContext() throws Exception {

        LOGGER.info("creating context");

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, org.apache.naming.java.javaURLContextFactory.class.getName());
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");

        final InitialContext ic = new InitialContext();

        createSubContexts(ic, "java:/comp/env");

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document document = builder.parse(serverXml);

        // create Environment
        {
            final NodeList envs = document.getElementsByTagName("Environment");
            for (int i = 0; i < envs.getLength(); i++) {
                final Element env = (Element)envs.item(i); // must be Element
                final String name = "java:comp/env/" + env.getAttribute("name");
                final Object instance = Class.forName(env.getAttribute("type")).getConstructor(String.class)
                        .newInstance(env.getAttribute("value"));

                LOGGER.info("binding " + name + " <" + instance + ">");

                createSubContexts(ic, name);

                ic.bind(name, instance);
            }
        }

        // Resource
        {
            final NodeList resources = document.getElementsByTagName("Resource");
            for (int i = 0; i < resources.getLength(); i++) {
                final Element resource = (Element)resources.item(i); // must be Element
                final String name = "java:comp/env/" + resource.getAttribute("name");
                final Class<?> type = Class.forName(resource.getAttribute("type"));

                final Object instance;
                if (type.equals(DataSource.class)) {
                    {
                        @SuppressWarnings("unchecked") // this mus be driver?
                        final Class<? extends Driver> driverClass = (Class<? extends Driver>) Class.forName(resource.getAttribute("driverClassName"));

                        DriverManager.registerDriver(driverClass.newInstance());
                    }

                    final BasicDataSource dataSource = new BasicDataSource();
                    // find all the bean attributes and set them use some reflection
                    for (Method method : dataSource.getClass().getMethods()) {

                        if (!method.getName().matches("^set.*")) {continue;}

                        final String x = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);

                        if (!resource.hasAttribute(x)) {continue;}
                        Class<?> y = method.getParameterTypes()[0]; // might be primitive

                        if (y.isPrimitive()) {
                            if (y.getName().equals("boolean")) y = Boolean.class;
                            if (y.getName().equals("byte")) y = Byte.class;
                            if (y.getName().equals("char")) y = Character.class;
                            if (y.getName().equals("double")) y = Double.class;
                            if (y.getName().equals("float")) y = Float.class;
                            if (y.getName().equals("int")) y = Integer.class;
                            if (y.getName().equals("long")) y = Long.class;
                            if (y.getName().equals("short")) y = Short.class;
                            if (y.getName().equals("void")) y = Void.class;
                        }

                        method.invoke(dataSource, y.getConstructor(String.class).newInstance(resource.getAttribute(x)));
                    }

                    instance = dataSource;
                } else {
                    // not supported, yet...
                    throw new AssertionError("type " + type + " not supported");
                }

                LOGGER.info("binding " + name + " <" + instance + ">");

                createSubContexts(ic, name);

                ic.bind(name, instance);
            }
        }
    }

    private void destroyInitialContext() {
        System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
        System.clearProperty(Context.URL_PKG_PREFIXES);

        LOGGER.info("context destroyed");
    }
}