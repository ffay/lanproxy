package cn.dxbtech.portbridge.client;

import cn.dxbtech.portbridge.commons.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class SslContextCreator {

    private static Logger logger = LoggerFactory.getLogger(SslContextCreator.class);

    public static SSLContext createSSLContext() {
        return new SslContextCreator().initSSLContext();
    }

    public SSLContext initSSLContext() {
        logger.info("Checking SSL configuration properties...");
        final String jksPath = Config.getInstance().getStringValue("ssl.jksPath");
        logger.info("Initializing SSL context. KeystorePath = {}.", jksPath);
        if (jksPath == null || jksPath.isEmpty()) {
            // key_store_password or key_manager_password are empty
            logger.warn("The keystore path is null or empty. The SSL context won't be initialized.");
            return null;
        }

        // if we have the port also the jks then keyStorePassword and
        // keyManagerPassword
        // has to be defined
        final String keyStorePassword = Config.getInstance().getStringValue("ssl.keyStorePassword");
        // if client authentification is enabled a trustmanager needs to be
        // added to the ServerContext

        try {
            logger.info("Loading keystore. KeystorePath = {}.", jksPath);
            InputStream jksInputStream = jksDatastore(jksPath);
            SSLContext clientSSLContext = SSLContext.getInstance("TLS");
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(jksInputStream, keyStorePassword.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            TrustManager[] trustManagers = tmf.getTrustManagers();

            // init sslContext
            logger.info("Initializing SSL context...");
            clientSSLContext.init(null, trustManagers, null);
            logger.info("The SSL context has been initialized successfully.");

            return clientSSLContext;
        } catch (NoSuchAlgorithmException | CertificateException | KeyStoreException | KeyManagementException
                | IOException ex) {
            logger.error("Unable to initialize SSL context. Cause = {}, errorMessage = {}.", ex.getCause(),
                    ex.getMessage());
            return null;
        }
    }

    private InputStream jksDatastore(String jksPath) throws FileNotFoundException {
        URL jksUrl = getClass().getClassLoader().getResource(jksPath);
        if (jksUrl != null) {
            logger.info("Starting with jks at {}, jks normal {}", jksUrl.toExternalForm(), jksUrl);
            return getClass().getClassLoader().getResourceAsStream(jksPath);
        }

        logger.warn("No keystore has been found in the bundled resources. Scanning filesystem...");
        File jksFile = new File(jksPath);
        if (jksFile.exists()) {
            logger.info("Loading external keystore. Url = {}.", jksFile.getAbsolutePath());
            return new FileInputStream(jksFile);
        }

        logger.warn("The keystore file does not exist. Url = {}.", jksFile.getAbsolutePath());
        return null;
    }
}