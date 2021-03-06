package cc.coodex.concrete.jaxrs.client;

import cc.coodex.util.Common;
import cc.coodex.util.Profile;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davidoff shen on 2016-12-09.
 */
public class OkHttp3InvokerFactory implements InvokerFactory {
    private static final Profile PROFILE = Profile.getProfile("okHttp3.properties");

    private static final Map<String, Invoker> INVOKERS = new HashMap<String, Invoker>();

    @Override
    public boolean accept(String domain) {
        return getDomainRule(domain) != null;
    }

    private String getDomainRule(String domain) {
        for (Object key : PROFILE.getProperties().keySet()) {
            if (key instanceof String) {
                String sKey = (String) key;
                if (domain.equalsIgnoreCase(PROFILE.getString(sKey))) {
                    return sKey;
                }
            }
        }
        return null;
    }

    @Override
    public Invoker getInvoker(String domain) {
        synchronized (INVOKERS) {

            Invoker invoker = INVOKERS.get(domain);
            if (invoker == null) {
                invoker = newInvoker(domain);
                INVOKERS.put(domain, invoker);
            }
            return invoker;
        }
    }

    private Invoker newInvoker(String domain) {
        String key = getDomainRule(domain);
        if (key != null) {
            try {
                if (!Common.isBlank(PROFILE.getString(key + ".ssl")))
                    return getSSLInvoker(domain, key);
            } catch (Throwable th) {
                throw new RuntimeException(th);
            }
        }
        return new OkHttp3Invoker(domain, null, null);
    }

    // TODO 配置化
    private Invoker getSSLInvoker(String domain, String key)
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustManager}, null);
        return new OkHttp3Invoker(domain, sslContext, trustManager);
    }
}
