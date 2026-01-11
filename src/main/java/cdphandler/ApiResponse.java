package cdphandler;

public final class ApiResponse {
    public final String url;
    public final int status;
    public final String mimeType;
    public final String body;

    public ApiResponse(String url, int status, String mimeType, String body) {
        this.url = url;
        this.status = status;
        this.mimeType = mimeType;
        this.body = body;
    }
}

