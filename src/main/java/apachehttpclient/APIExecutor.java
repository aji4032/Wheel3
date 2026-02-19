package apachehttpclient;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import tools.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class APIExecutor {
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
            StringBuilder objStringBuilder = new StringBuilder();
            objStringBuilder.append("--------------------------------------------------\n");
            objStringBuilder.append("Url: ").append(url).append("\n");
            objStringBuilder.append("Method: ").append(method).append("\n");
            objStringBuilder.append("Headers: ").append(headers).append("\n");
            objStringBuilder.append("Status: ").append(statusCode).append("\n");
            objStringBuilder.append("Request Name: ").append(apiName).append("\n");
            objStringBuilder.append("Payload: ").append(payLoad).append("\n");
            objStringBuilder.append("Response: ").append(responseBody).append("\n");
            objStringBuilder.append("--------------------------------------------------\n");
            Log.info(objStringBuilder.toString());
            objResponseObject = new ResponseObject(apiName, String.valueOf(statusCode), responseBody);
        } catch (IOException e) {
            Log.fail(e.getMessage());
        } finally {
            try {httpClient.close();} catch (Exception ignored) {}
        }
        return objResponseObject;
    }
}
