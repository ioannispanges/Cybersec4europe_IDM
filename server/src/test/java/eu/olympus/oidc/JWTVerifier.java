package eu.olympus.oidc;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import eu.olympus.verifier.interfaces.Verifier;

import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import static org.junit.Assert.assertEquals;

/**
 * Simple class for verifying if a JSON Web Token
 * issued as part of TestFlow.
 *
 */
public class JWTVerifier implements Verifier {

	Algorithm algorithm;

	/**
	 * Instantiate the verifier
	 * @param pk The public key to use for verification
	 */
	public JWTVerifier(PublicKey pk){
		RSAKeyProvider provider = new RSAKeyProvider() {

			@Override
			public RSAPrivateKey getPrivateKey() {
				return null;
			}

			@Override
			public String getPrivateKeyId() {
				return null;
			}

			@Override
			public RSAPublicKey getPublicKeyById(String arg0) {
				return (RSAPublicKey)pk;
			}
		};

		this.algorithm = Algorithm.RSA256(provider);
	}


	@Override
	public boolean verify(String token) {
		com.auth0.jwt.JWTVerifier verifier = JWT.require(algorithm).build();
		boolean returnValue = false;
		try{
			DecodedJWT jwt = verifier.verify(token);

			// Claims may be printed for debugging:
			// DecodedJWT jwt = verifier.verify(token);

			 for(String s: jwt.getClaims().keySet()){
				if("iss".equals(s)) {
			    	assertEquals("https://olympus-vidp.com/issuer1", jwt.getClaim(s).asString());
			    }
				if("sub".equals(s)) {
					assertEquals("test", jwt.getClaim(s).asString());
				}
			 }
			 returnValue = (jwt.getClaim("exp").asDouble() >= (System.currentTimeMillis()/1000));
		}catch(SignatureVerificationException e) {
			e.printStackTrace();
			return false;
		}
		return returnValue;
	}



}
