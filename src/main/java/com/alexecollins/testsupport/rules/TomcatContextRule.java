package com.alexecollins.testsupport.rules;

import org.apache.catalina.UserDatabase;
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
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.sql.Driver;
import java.sql.DriverManager;


/**
 * Creates an context for tests using an Apache Tomcat server.xml.
 *
 * https://blogs.oracle.com/randystuph/entry/injecting_jndi_datasources_for_junit
 *
 * @author alex.e.c@gmail.com
 */
public class TomcatContextRule implements TestRule {

    public static final Logger LOGGER = Logger.getLogger(TomcatContextRule.class);

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

    private final URI serverXml;

	public TomcatContextRule(File serverXml, Object target) {
		this(serverXml.toURI(), target);
	}

	public TomcatContextRule(URI serverXml, Object target) {
        if (serverXml == null) {throw new IllegalArgumentException();}
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

        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
	    final InputStream in = serverXml.toURL().openStream();
	    final Document document;
	    try {
		    document = documentBuilder.parse(in);
	    } finally {
		    in.close();
	    }

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
                    @SuppressWarnings("unchecked") // this mus be driver?
                    final Class<? extends Driver> driverClass = (Class<? extends Driver>) Class.forName(resource.getAttribute("driverClassName"));

                    DriverManager.registerDriver(driverClass.newInstance());

                    instance = new BasicDataSource();
                } else if (type.equals(UserDatabase.class)) {
                    @SuppressWarnings("unchecked")
                    final Class<UserDatabase> factory = (Class<UserDatabase>) Class.forName(resource.getAttribute("factory"));

                    instance = factory.newInstance();
                } else {
                    // not supported, yet...
                    throw new AssertionError("type " + type + " not supported");
                }

                // find all the bean attributes and set them use some reflection
                injectDependencies(resource, instance);

                LOGGER.info("binding " + name + " <" + instance + ">");

                createSubContexts(ic, name);

                ic.bind(name, instance);
            }
        }
    }

    private static void injectDependencies(Element resource, Object target) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        for (Method method : target.getClass().getMethods()) {

            if (!method.getName().matches("^set.*")) {continue;}

            final String name = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);

            if (!resource.hasAttribute(name)) {continue;}
            Class<?> type = method.getParameterTypes()[0]; // might be primitive

            if (type.isPrimitive()) {
                if (type.getName().equals("boolean")) type = Boolean.class;
                if (type.getName().equals("byte")) type = Byte.class;
                if (type.getName().equals("char")) type = Character.class;
                if (type.getName().equals("double")) type = Double.class;
                if (type.getName().equals("float")) type = Float.class;
                if (type.getName().equals("int")) type = Integer.class;
                if (type.getName().equals("long")) type = Long.class;
                if (type.getName().equals("short")) type = Short.class;
                if (type.getName().equals("void")) type = Void.class;
            }

            method.invoke(target, type.getConstructor(String.class).newInstance(resource.getAttribute(name)));
        }
    }

    private void destroyInitialContext() {
        System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
        System.clearProperty(Context.URL_PKG_PREFIXES);

        LOGGER.info("context destroyed");
    }
}