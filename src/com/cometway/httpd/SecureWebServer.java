
package com.cometway.httpd;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.security.NoSuchProviderException;
import java.security.KeyManagementException;
import java.security.InvalidKeyException;
import java.security.SignatureException;

import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateException;

import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;

import javax.net.ServerSocketFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLHandshakeException;
/**
 * Routes HTTPS Requests from a server socket
 */
public class SecureWebServer extends WebServer
{

	public static final String VERSION_STR = "Comet Way Secure Web Server 2.4 2006-05-15";

	protected PublicKey signKey;

    /**
     * Initializes this agent's properties by providing default values for each of the following missing properties:
     * 'bind_port' (default: 443)
     * 'certificate1' (default: testcert.pem) This must be a non-passphrased X509 certificate in PEM format
     * 'server_key' (default: testkey.key) This must be a non-passphrased PKCS8 encoded private key associated with the certificate in PEM format
     *
     * The certificate and key properties are pathnames to a certificate and private key. If there is no path, the
     * current directory will be used.
     *
     * @see com.cometway.httpd.WebServer for the rest of the properties
     */
	public void initProps()
	{
	    // Specific to this agent.
		setDefault("bind_port", "443");
		setDefault("certificate1","testcert.pem");
		setDefault("server_key","testkey.key");

		// This is the X509Encoded key which will be used to verify the signature an incoming connection's certificate
		setDefault("verify_signed_key","");
		// This option, if set to true, requires all clients to have a certificate and if verify_signed_key is set, must have a valid signature
		setDefault("require_certificate","false");
		setDefault("require_certificate_error_url","");

		// Set the rest of the properties from the WebServer
		super.initProps();
	}


	public void start()
	{
		super.start();

		if(getTrimmedString("verify_signed_key").length()>0) {
			try {
				File signKeyFile = new File(getString("verify_signed_key"));
				X509EncodedKeySpec signKeyX509 = new X509EncodedKeySpec(readBinaryFile(signKeyFile));
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				signKey = keyFactory.generatePublic(signKeyX509);
				debug("Signkey = "+signKey);
			}
			catch(java.security.spec.InvalidKeySpecException ikse) {
				error("Could not read X509 key",ikse);
			}
			catch(java.security.NoSuchAlgorithmException nsae) {
				error("Could not read X509 key",nsae);
			}
		}
	}


    /**
     * This method uses the server key and certificate to create an SSLServerSocket.
     */
	protected ServerSocket getServerSocket(String bind_address, int port) throws java.io.IOException
	{
		ServerSocket rval = null;
		FileInputStream fis = null;

		try {
			File cert = new File(getString("certificate1"));
			File key = new File(getString("server_key"));
	    
			fis = new FileInputStream(cert);
				    
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(fis);
			X509Certificate[] certs = new X509Certificate[1];
			certs[0] = certificate;
		
			printCertificateInfo(certificate);

			PKCS8EncodedKeySpec encodedKey = new PKCS8EncodedKeySpec(readBinaryFile(key));
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PrivateKey privatekey = keyFactory.generatePrivate(encodedKey);
		
			//			KeyStore store = KeyStore.getInstance("JKS","SUN");
			KeyStore store = KeyStore.getInstance("JKS");
			store.load(null, null);
			store.setKeyEntry("server", privatekey, new char[0], certs);
		
		
			SSLContext sc = SSLContext.getInstance("TLS");
			KeyManagerFactory keyManager = KeyManagerFactory.getInstance("SunX509");
			keyManager.init(store,new char[0]);
		
			//				    TrustManager[] tms = new TrustManager[1];
			//				    tms[0] = new TrustEverything();
			//				    sc.init(keyManager.getKeyManagers(),tms,null);
			sc.init(keyManager.getKeyManagers(),null,null);
			ServerSocketFactory ssf = sc.getServerSocketFactory();
			//			ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
			if (bind_address.equals("all")) {
				rval = ssf.createServerSocket(port,50);
			}
			else {
				InetAddress address = InetAddress.getByName(bind_address);
				rval = ssf.createServerSocket(port,50,address);
			}		

			((SSLServerSocket)rval).setUseClientMode(false);

			// If we have a sign key, request client cert
			if(signKey!=null) {
				((SSLServerSocket)rval).setWantClientAuth(true);
			}

			// Require client cert
			if(getBoolean("require_certificate")) {
				//				((SSLServerSocket)rval).setNeedClientAuth(true);
				((SSLServerSocket)rval).setWantClientAuth(true);
			}
		}
		catch(CertificateException ce) {
			error("Exception while opening socket", ce);
		}
		catch(NoSuchAlgorithmException nsae) {
			error("Exception while opening socket", nsae);
		}
		catch(InvalidKeySpecException ikse) {
			error("Exception while opening socket", ikse);
		}
		catch(KeyStoreException kse) {
			error("Exception while opening socket", kse);
		}
		catch(KeyManagementException kme) {
			error("Exception while opening socket", kme);
		}
		//		catch(NoSuchProviderException nspe) {
		//			error("Exception while opening socket", nspe);
		//		}
		catch(UnrecoverableKeyException uke) {
			error("Exception while opening socket", uke);
		}

		try
		{
			fis.close();
		}
		catch (Exception e)
		{;}

		return (rval);
	}


	protected Socket acceptConnection(ServerSocket ss) throws IOException
	{
		Socket rval = null;
		SSLServerSocket ssock = (SSLServerSocket)ss;

		while(rval==null) {
			try {
				SSLSocket sock = (SSLSocket)ss.accept();
				rval = sock;

				if(signKey!=null) {
					SSLSession session = sock.getSession();
					debug(session.getCipherSuite());
					debug(session.getProtocol());
					String[] sessionNames = session.getValueNames();
					for(int x=0;x<sessionNames.length;x++) {
						debug(sessionNames[x]+"="+session.getValue(sessionNames[x]));
					}
					
					Certificate[] clientChain = session.getPeerCertificates();
					debug("Number of certs in chain = "+clientChain.length);
					boolean verified = false;
					for(int x=0;x<clientChain.length;x++) {
						try {
							clientChain[x].verify(signKey);
							verified = true;
							break;
						}
						catch(CertificateException ce) {
							// Invalid certificate
							error("Invalid client certificate",ce);
						}
						catch(NoSuchAlgorithmException nsae) {
							// This could be a problem if the cert has some unsupported algorthm
							error("No such algorithm",nsae);
						}
						catch(InvalidKeyException ike) {
							// This shouldn't happen
							error("Invalid Key",ike);
						}
						catch(NoSuchProviderException nspe) {
							// Dunno what causes this
							error("No default provider",nspe);
						}
						catch(SignatureException se) {
							// I think this is the only one that we need to be concerned about
							error("Client's certificate has not been signed by our key");
						}
					}
					if(!verified) {
						// we need to invalidate the session
						session.invalidate();
						try {
							rval.close();
						}
						catch(Exception e) {;}
						rval = null;
					}
				}		
			}
			catch(SSLHandshakeException sshe) {
				// This happens if client certificate is required and the client doesn't have one
				error("The client is required to provide a certificate, but hasn't provided one.");
				if(rval!=null) {
					clientCertificateError(rval);
				}
			}
			catch(SSLPeerUnverifiedException ssle) {    // This happens if require certificates isn't set but a pkcs8 key is given to check signatures
				// I think we just want to return the socket here
				if(getBoolean("require_certificate")) {
					error("The client is required to provide a certificate, but hasn't provided one.");
					clientCertificateError(rval);
				}
				else {
					println("The client isn't required to provide a certificate, but a key is given to check client certificate signatures.");
				}
			}
		}

		return(rval);
	}


	protected void clientCertificateError(Socket sock)
	{
		try {
			if(getTrimmedString("require_certificate_error_url").length()>0) {
				sock.getOutputStream().write(getHTMLByCode(MOVED,"","Location: "+getTrimmedString("require_certificate_error_url")+"\r\n").getBytes());
				sock.getOutputStream().flush();
			}
			else {
				sock.getOutputStream().write(getHTMLByCode(FORBIDDEN).getBytes());
				sock.getOutputStream().flush();
			}
		}
		catch(Exception e) {
			error("Exception while replying to client",e);
		}

		try {
			sock.close();
		}
		catch(Exception e) {;}
		
	}


	/**
	 * This method prints out relevant info for the certificate that it will serve via the SSL/TLS handshake
	 */
	protected void printCertificateInfo(X509Certificate cert)
	{
		println("X509Certificate Issuer:    "+cert.getIssuerX500Principal());
		println("X509Certificate Subject:   "+cert.getSubjectX500Principal());
		println("X509Certificate Version:   "+cert.getVersion());
		println("X509Certificate Serial:    "+cert.getSerialNumber());
		println("X509Certificate Signature: "+cert.getSigAlgName());
		try {
			cert.checkValidity();
			println("X509Certificate: This certificate is currently valid");
			// Calculate expire time
			long after = cert.getNotAfter().getTime();
			long now = System.currentTimeMillis();
			// we'll use minutes as the smallest unit
			after = (long)(after/60000);
			now = (long)(now/60000);
			long diff = after - now;
			int minutes = (int)(diff % 60);
			diff = (long)(diff/60);
			int hours = (int)(diff % 24);
			diff = (long)(diff/24);
			StringBuffer timeString = new StringBuffer();
			timeString.append("X509Certificate: This certificate will expire in ");
			if(diff>0) {
				if(diff==1) {
					timeString.append("1 day, ");
				}
				else {
					timeString.append(diff+" days, ");
				}
			}
			if(hours>0) {
				if(hours==1) {
					timeString.append("1 hour, ");
				}
				else {
					timeString.append(hours+" hours, ");
				}
			}
			if(minutes>0) {
				if(minutes==1) {
					timeString.append("1 minute, ");
				}
				else {
					timeString.append(minutes+" minutes, ");
				}
			}
			// If cert expires in less than 7 days, we issue a warning
			if(diff<7) {
				warning(timeString.toString());
			}
			else {
				println(timeString.toString());
			}
		}
		catch(CertificateExpiredException expired) {
			error("X509Certificate: This certificate, "+getString("certificate1")+", has expired on "+cert.getNotAfter());
		}
		catch(CertificateNotYetValidException notValid) {
			error("X509Certificate: This certificate, "+getString("certificate1")+", is not valid until "+cert.getNotBefore());
		}
	}

    
	protected byte[] readBinaryFile(File file)
	{
		byte[] rval = null;

		FileInputStream in = null;
		if (file.exists()) {
			rval = new byte[(int)file.length()];

			try {
				in = new FileInputStream(file);
				in.read(rval);
			}
			catch(Exception e) {
				error("Error reading file: "+file,e);
			}

			try {
				in.close();
			}
			catch(Exception e) {
				;
			}
		}

		return (rval);
	}
}
