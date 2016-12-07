package org.labkey.test.data.mobileappstudy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Created by iansigmon on 11/28/16.
 */

/**
 * Test helper class to handle MobileAppStudy multi-select survey questions
 */
public class ChoiceQuestionResponse extends QuestionResponse
{
    public ChoiceQuestionResponse(String questionId, Date start, Date end, boolean skipped, String...values)
    {
        super(SupportedResultType.CHOICE, questionId, start, end, skipped, values != null ? Arrays.asList(values) : Collections.emptyList());
    }

    @Override
    public String getResultJsonString()
    {
        return String.format(getType().getFormatString(),
            String.join(",\n",
                    ((Collection<String>)getResult()).stream().map((response)-> "\"" + response + "\"").collect(Collectors.toList())
            )
        );
    }
}
