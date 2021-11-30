package rest;

import eu.olympus.client.PabcIdPRESTConnection;
import eu.olympus.verifier.VerificationResult;
import eu.olympus.verifier.interfaces.PABCVerifier;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@Path("/verifier")
public class VerifierServlet {
    private static final byte[] seed="randomSeedNotNeeded".getBytes(StandardCharsets.UTF_8);
    private static final String bearer="NotNeededBearerToken";
    @Context
    ServletContext context;

    @Path(VerifierEndpoints.SETUP)
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public void setup(SetupModel request) throws Exception {
        System.out.println("Got a setup request");
        List<PabcIdPRESTConnection> connections = new ArrayList<>();
        for (String s : request.getUrls()) {
            PabcIdPRESTConnection pabcIdPRESTConnection = new PabcIdPRESTConnection(s, bearer, 0,1000);
            connections.add(pabcIdPRESTConnection);
        }
    }
    @Path(VerifierEndpoints.VERIFY)
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public boolean verify(VerificationModel request) throws Exception {
        System.out.println("Got a verify request");
        PABCVerifier verifier= (PABCVerifier) context.getAttribute("verifier");
        VerificationResult result=verifier.verifyPresentationToken(request.getToken(),request.getPolicy());
        return result==VerificationResult.VALID;
    }

}


