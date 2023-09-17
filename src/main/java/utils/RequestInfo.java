package utils;

import org.apache.http.NameValuePair;

import java.util.List;

/* Request info class */
public class RequestInfo {
    /* Request params */
    private List<NameValuePair> params;
    /* Request path */
    private String requestPath;
    /* Request method */
    private String method;
    public RequestInfo() {}

    public List<NameValuePair> getParamsList() {
        return params;
    }
    public String getRequestPath() {
        return requestPath;
    }
    public void setParamsList(List<NameValuePair> params) {
        this.params = params;
    }
    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getMethod() {
        return method;
    }
}
