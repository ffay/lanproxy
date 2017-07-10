package org.fengfei.lanproxy.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
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

    public SSLContext initSSLContext() {
        logger.info("Checking SSL configuration properties...");
        final String jksPath = Config.getInstance().getStringValue("server.ssl.jksPath");
        logger.info("Initializing SSL context. KeystorePath = {}.", jksPath);
        if (jksPath == null || jksPath.isEmpty()) {
            // key_store_password or key_manager_password are empty
            logger.warn("The keystore path is null or empty. The SSL context won't be initialized.");
            return null;
        }

        // if we have the port also the jks then keyStorePassword and
        // keyManagerPassword
        // has to be defined
        final String keyStorePassword = Config.getInstance().getStringValue("server.ssl.keyStorePassword");
        final String keyManagerPassword = Config.getInstance().getStringValue("server.ssl.keyManagerPassword");
        if (keyStorePassword == null || keyStorePassword.isEmpty()) {

            // key_store_password or key_manager_password are empty
            logger.warn("The keystore password is null or empty. The SSL context won't be initialized.");
            return null;
        }

        if (keyManagerPassword == null || keyManagerPassword.isEmpty()) {

            // key_manager_password or key_manager_password are empty
            logger.warn("The key manager password is null or empty. The SSL context won't be initialized.");
            return null;
        }

        // if client authentification is enabled a trustmanager needs to be
        // added to the ServerContext
        boolean needsClientAuth = Config.getInstance().getBooleanValue("server.ssl.needsClientAuth", false);

        try {
            logger.info("Loading keystore. KeystorePath = {}.", jksPath);
            InputStream jksInputStream = jksDatastore(jksPath);
            SSLContext serverContext = SSLContext.getInstance("TLS");
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(jksInputStream, keyStorePassword.toCharArray());
            logger.info("Initializing key manager...");
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyManagerPassword.toCharArray());
            TrustManager[] trustManagers = null;
            if (needsClientAuth) {
                logger.warn(
                        "Client authentication is enabled. The keystore will be used as a truststore. KeystorePath = {}.",
                        jksPath);
                // use keystore as truststore, as server needs to trust
                // certificates signed by the
                // server certificates
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                trustManagers = tmf.getTrustManagers();
            }

            // init sslContext
            logger.info("Initializing SSL context...");
            serverContext.init(kmf.getKeyManagers(), trustManagers, null);
            logger.info("The SSL context has been initialized successfully.");

            return serverContext;
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