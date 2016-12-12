package org.labkey.test.data.mobileappstudy;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by iansigmon on 11/23/16.
 */
public abstract class AbstractQuestionResponse implements QuestionResult
{
    public enum SupportedResultType
    {
        BOOL("bool", "%1$s"),
        CHOICE("choice", "[%1$s]"),
        DATE("date", "\"%1$tY-%1$tm-%1$td\""),
        GROUPED_RESULT("groupedResult", "[%1$s]"),
        NUMBER("number", "%1$s"),
        SCALE("scale", "%1$s"),
        TEXT("text", "\"%1$s\"");

        SupportedResultType(String displayText, String formatString)
        {
            this.displayText = displayText;
            _formatString = formatString;
        }

        private final String displayText;
        private final String _formatString;

        public final String getDisplayText()
        {
            return displayText;
        }
        public final String getFormatString()
        {
            return _formatString;
        }
    }

    private SupportedResultType _type;
    private String _questionId;
    private Date _start;
    private Date _end;
    private boolean _skipped;

    private boolean _omitType = false;
    private boolean _omitQuestionId = false;
    private boolean _omitStart = false;
    private boolean _omitEnd = false;
    private boolean _omitSkipped = false;
    private boolean _omitResult = false;

    public boolean getSkipped()
    {
        return _skipped;
    }
    public void setSkipped(boolean skipped)
    {
        _skipped = skipped;
    }
    public void setOmitSkipped(boolean omitSkipped)
    {
        _omitSkipped = omitSkipped;
    }

    public Date getEnd()
    {
        return _end;
    }
    public void setEnd(Date end)
    {
        _end = end;
    }
    public void setOmitEnd(boolean omitEnd)
    {
        _omitEnd = omitEnd;
    }

    public Date getStart()
    {
        return _start;
    }
    public void setStart(Date start)
    {
        _start = start;
    }
    public void setOmitStart(boolean omitStart)
    {
        _omitStart = omitStart;
    }

    public String getQuestionId()
    {
        return _questionId;
    }
    public void setQuestionId(String questionId)
    {
        _questionId = questionId;
    }
    public void setOmitQuestionId(boolean omitQuestionId)
    {
        _omitQuestionId = omitQuestionId;
    }

    public SupportedResultType getType()
    {
        return _type;
    }
    public void setType(SupportedResultType type)
    {
        _type = type;
    }
    public void setOmitType(boolean omitType)
    {
        _omitType = omitType;
    }

    public abstract String getJsonString();
    public abstract Object getResult();
    public void setOmitResult(boolean flag)
    {
        _omitResult = flag;
    }

    String getQuestionResponseJsonFormat()
    {
        ArrayList<String> props = new ArrayList();
        if (!_omitType)
            props.add("\"type\": \"%1$s\"");
        if (!_omitQuestionId)
            props.add("\"identifier\": \"%2$s\"");
        if (!_omitStart)
            props.add("\"start\": \"%3$tY-%3$tm-%3$td %3$tH:%3$tM:%3$tS %3$tz\"");
        if (!_omitEnd)
            props.add("\"end\": \"%4$tY-%4$tm-%4$td %4$tH:%4$tM:%4$tS %4$tz\"");
        if (!_omitSkipped)
            props.add("\"skipped\": %5$b");
        if (!_omitResult)
            props.add("\"result\": %6$s");

        return "{\n" + String.join(",\n", props) + "}";
    }

}
