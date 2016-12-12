package org.labkey.test.data.mobileappstudy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Created by iansigmon on 11/23/16.
 */
public class GroupedQuestionResponse extends QuestionResponse
{
    public GroupedQuestionResponse(String questionId, Date start, Date end, boolean skipped, QuestionResult...result)
    {
        super(SupportedResultType.GROUPED_RESULT, questionId, start, end, skipped, result != null ? Arrays.asList(result) : Collections.emptyList());
    }

    @Override
    public String getResultJsonString()
    {
        return String.format(getType().getFormatString(),
            String.join(",\n",
                    ((Collection<QuestionResult>)getResult()).stream().map(QuestionResult::getJsonString).collect(Collectors.toList())
            )
        );
    }
}
