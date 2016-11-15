package org.labkey.test.commands.mobileappstudy;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;


public abstract class MobileAppCommand
{
    public static final String CONTROLLER_NAME = "mobileappstudy";

    protected static final String MESSAGE_TAG = "Message";
    protected static final String ERRORS_TAG = "Errors";
    protected static final String EXCEPTION_MESSAGE_TAG = "exception";
    protected static final String SUCCESS_TAG = "success";

    private String _exceptionMessage;
    private boolean _success;

    public boolean getSuccess()
    {
        return _success;
    }
    public void setSuccess(boolean success)
    {
        _success = success;
    }

    public String getExceptionMessage()
    {
        return _exceptionMessage;
    }
    public void setExceptionMessage(String exceptionMessage)
    {
        _exceptionMessage = exceptionMessage;
    }

    private Consumer<String> logger;
    protected boolean isExecuted = false;
    protected JSONObject _jsonResponse;

    public abstract void execute(int expectedStatusCode);
    public abstract String getTargetURL();

    protected void parseResponse(JSONObject response)
    {
        _jsonResponse = response;
        setSuccess(response.getBoolean(SUCCESS_TAG));

        if (getSuccess())
            parseSuccessfulResponse(response);
        else
            parseErrorResponse(response);
    }

    protected void parseSuccessfulResponse(JSONObject response)
    {
        //do nothing here
    }

    protected void parseErrorResponse(JSONObject response)
    {
        setExceptionMessage(response.getString(EXCEPTION_MESSAGE_TAG));
    }

    public void log(String text)
    {
        logger.accept(text);
    }

    public void setLogger(Consumer<String> logger)
    {
        this.logger = logger;
    }

    protected void execute(HttpUriRequest request, int expectedStatusCode)
    {
        HttpResponse response = null;
        log("Submitting request using url: " + request.getURI());

        try (CloseableHttpClient client = HttpClients.createDefault())
        {
            response = client.execute(request);
            isExecuted = true;
            log("Survey response posted.");

            int statusCode = response.getStatusLine().getStatusCode();
            assertEquals("Unexpected response status", statusCode, expectedStatusCode);

            String body = EntityUtils.toString(response.getEntity());
            parseResponse(new JSONObject(body));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Test failed requesting the URL,", e);
        }
        finally
        {
            if (response != null)
                EntityUtils.consumeQuietly(response.getEntity());
        }
    }
}
