package cc.coodex.concrete.jaxrs.client;

import cc.coodex.concrete.jaxrs.ErrorInfo;
import cc.coodex.concrete.jaxrs.struct.Unit;
import cc.coodex.util.TypeHelper;
import okhttp3.*;
import okhttp3.internal.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;

import static cc.coodex.concrete.jaxrs.JaxRSHelper.HEADER_ERROR_OCCURRED;

/**
 * Created by davidoff shen on 2016-12-07.
 */
public class OkHttp3Invoker extends AbstractInvoker {

    private final static Logger log = LoggerFactory.getLogger(OkHttp3Invoker.class);

    private final OkHttpClient client;

    public OkHttp3Invoker(String domain, SSLContext sslContext, X509TrustManager trustManager) {
        super(domain);
        OkHttpClient.Builder builder = new OkHttpClient.Builder().cookieJar(new CookieManager());
        if (sslContext != null) {
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
        }
        client = builder.build();
    }


    private Request build(String method, Request.Builder builder, RequestBody body) {
        if ("DELETE".equalsIgnoreCase(method))
            return builder.delete(body).build();
        else if ("PUT".equalsIgnoreCase(method))
            return builder.put(body).build();
        else if ("GET".equalsIgnoreCase(method))
            return builder.get().build();
        else
            return builder.post(body).build();

    }


    @Override
    protected Object invoke(String path, Unit unit, Object toSubmit) {
        Request.Builder builder = new Request.Builder().url(path)
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
//                .addHeader("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6,zh-TW;q=0.4")
                .addHeader("Content-Type", "application/json; charset=utf-8");


        RequestBody body = toSubmit == null ?
                Util.EMPTY_REQUEST :
                RequestBody.create(MediaType.parse("application/json; charset=utf-8"), toStr(toSubmit));

        try {
            log.debug("request:{} {}", unit.getInvokeType(), path);
            Response response = client.newCall(build(unit.getInvokeType(), builder, body)).execute();
            if (response.isSuccessful()) {
                return getJSONSerializer().parse(response.body().string(),
                        TypeHelper.toTypeReference(unit.getGenericReturnType(), unit.getDeclaringModule().getInterfaceClass()));
            } else {
                if (response.header(HEADER_ERROR_OCCURRED) != null) {
                    ErrorInfo errorInfo = getJSONSerializer().parse(response.body().string(), ErrorInfo.class);
                    throw new ClientException(errorInfo.getCode(), errorInfo.getMsg(), path, unit.getInvokeType());
                } else {
                    throw new ClientException(response.code(), response.body().string(), path, unit.getInvokeType());
                }
            }
        } catch (IOException e) {
            throw new ClientException(-1, e.getLocalizedMessage(), path, unit.getInvokeType());
        }

    }
}
