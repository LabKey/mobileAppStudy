package org.labkey.test.mockserver.mobileappstudy;

import org.labkey.test.tests.mobileappstudy.ForwardResponseTest;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static org.mockserver.model.HttpResponse.notFoundResponse;
import static org.mockserver.model.HttpResponse.response;

public class MockServerPostCallback implements ExpectationResponseCallback
{
    public static final int PUT_SUCCESS_RESPONSE_CODE = 202;
    public static final int PUT_FAILURE_RESPONSE_CODE = 500;

    @Override
    public HttpResponse handle(HttpRequest httpRequest)
    {
        if (httpRequest.getPath().getValue().contains(ForwardResponseTest.STUDY_NAME01) || httpRequest.getPath().getValue().contains(ForwardResponseTest.STUDY_NAME03))
            return response()
                    .withStatusCode(PUT_SUCCESS_RESPONSE_CODE);
        else if(httpRequest.getPath().getValue().contains(ForwardResponseTest.STUDY_NAME02))
            return response()
                    .withStatusCode(PUT_FAILURE_RESPONSE_CODE);
        else
            return notFoundResponse();
    }
}
