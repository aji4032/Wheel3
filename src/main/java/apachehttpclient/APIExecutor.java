package apachehttpclient;

import logger.Log;
import logger.Logger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class APIExecutor {
    private static final Logger log = Log.getLogger(APIExecutor.class);

    public static ResponseObject execute(String apiName, String url, String method, HashMap<String, String> headers, String payLoad) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        RequestBuilder requestBuilder = RequestBuilder.create(method).setUri(url);
        for(Map.Entry<String, String> header: headers.entrySet()) {
            requestBuilder.setHeader(header.getKey(), header.getValue());
        }
        HttpUriRequest request = requestBuilder.setEntity(new StringEntity(payLoad, ContentType.APPLICATION_JSON)).build();
        ResponseObject objResponseObject = null;
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = "";
            try {responseBody = EntityUtils.toString(response.getEntity());} catch (Exception ignored) {}
            String objStringBuilder = "--------------------------------------------------\n" +
                    "Url: " + url + "\n" +
                    "Method: " + method + "\n" +
                    "Headers: " + headers + "\n" +
                    "Status: " + statusCode + "\n" +
                    "Request Name: " + apiName + "\n" +
                    "Payload: " + payLoad + "\n" +
                    "Response: " + responseBody + "\n" +
                    "--------------------------------------------------\n";
            log.info(objStringBuilder);
            objResponseObject = new ResponseObject(apiName, String.valueOf(statusCode), responseBody);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {httpClient.close();} catch (Exception ignored) {}
        }
        return objResponseObject;
    }
}
