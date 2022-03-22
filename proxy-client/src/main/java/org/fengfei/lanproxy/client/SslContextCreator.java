package org.fengfei.lanproxy.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.fengfei.lanproxy.common.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslContextCreator {

    private static Logger logger = LoggerFactory.getLogger(SslContextCreator.class);

    public static SSLContext createSSLContext() {
        return new SslContextCreator().initSSLContext();
    }

    public SSLContext initSSLContext() {
        logger.info("Checking SSL configuration properties...");
        final String jksPath = Config.getInstance().getStringValue("ssl.jksPath");
        final String caPath = Config.getInstance().getStringValue("ssl.TrustkeyStorePath");
        logger.info("Initializing SSL context. KeystorePath = {}.{}", jksPath,caPath);
        if (jksPath == null || jksPath.isEmpty() ||caPath == null || caPath.isEmpty()) {
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
            InputStream caInputStream = jksDatastore(caPath);
            SSLContext clientSSLContext = SSLContext.getInstance("TLS");
            final KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(jksInputStream, keyStorePassword.toCharArray());

            final KeyStore caks = KeyStore.getInstance("JKS");
            caks.load(caInputStream, keyStorePassword.toCharArray());

            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(caks);
            TrustManager[] trustManagers = tmf.getTrustManagers();

            // init sslContext
            logger.info("Initializing SSL context...");
            clientSSLContext.init(kmf.getKeyManagers(), trustManagers, null);
            logger.info("The SSL context has been initialized successfully.");

            return clientSSLContext;
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException | KeyStoreException
                | KeyManagementException | IOException ex) {
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