package rest;

import eu.olympus.verifier.interfaces.PABCVerifier;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class VerifierServer {
    private Server server;
    private static VerifierServer myself = null;
    private PABCVerifier verifier;
    private VerifierServer(){
    }
    public PABCVerifier getVerifier() {
        return verifier;
    }
    public void setVerifier(PABCVerifier verifier) {
        this.verifier = verifier;
    }
    public static VerifierServer getInstance(){
        if(myself == null) {
            myself = new VerifierServer();
            return myself;
        } else {
            return myself;
        }
    }
    public void start(int port) throws Exception{
        System.out.println("STARTING REST Verifier:" +port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        if(verifier!=null)  // If manual setup was performed, e.g. for testing
            context.setAttribute("verifier", verifier);

        server = new Server();
        ServerConnector connector = new ServerConnector(server);

        connector.setPort(port);
        Connector[] connectors = new Connector[1];
        connectors[0] = connector;
        server.setConnectors(connectors);
        server.setHandler(context);
        ServletHolder jerseyServlet = context.addServlet(
                org.glassfish.jersey.servlet.ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);
        // Tells the Jersey Servlet which REST service/class to load.
        jerseyServlet.setInitParameter(
                "jersey.config.server.provider.classnames",
                VerifierServlet.class.getCanonicalName());
        server.start();
    }
    public void stop() throws Exception{
        System.out.println("STOPPING REST Verifier");
        server.stop();
    }
}
