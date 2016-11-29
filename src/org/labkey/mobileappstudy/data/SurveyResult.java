package org.labkey.mobileappstudy.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.labkey.api.data.Container;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by susanh on 11/28/16.
 */
public class SurveyResult
{
    public enum Type {
        DATE("Date"),
        BOOLEAN("Bool"),
        CHOICE("choice"),
        INTEGER("scale"),
        FLOAT("number"),
        GROUPED_RESULT("groupedResult"),
        STRING("text");

        private String _typeName;

        Type(String typeName)
        {
            _typeName = typeName;
        }

        public String getTypeName()
        {
            return _typeName;
        }

        public static Type fromTypeName(String name)
        {
            if (name != null)
            {
                for (Type t : Type.values())
                {
                    if (t.getTypeName().equalsIgnoreCase(name))
                    {
                        return t;
                    }
                }
            }
            throw new IllegalArgumentException("No constant with text " + name + " found");
        }

    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private String type;
    private String identifier;
    private Container container;
    private String listName;
    private Integer surveyId;
    private String questionId;
    private Date startTime;
    private Date endTime;
    private Boolean skipped;
    private Date created;
    private Object result;

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    @JsonIgnore
    public Container getContainer()
    {
        return container;
    }

    @JsonIgnore
    public void setContainer(Container container)
    {
        this.container = container;
    }

    public String getListName()
    {
        return listName;
    }

    public void setListName(String listName)
    {
        this.listName = listName;
    }

    public Integer getSurveyId()
    {
        return surveyId;
    }

    public void setSurveyId(Integer surveyId)
    {
        this.surveyId = surveyId;
    }

    public String getQuestionId()
    {
        return questionId;
    }

    public void setQuestionId(String questionId)
    {
        this.questionId = questionId;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStart(Date startTime)
    {
        this.startTime = startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEnd(Date endTime)
    {
        this.endTime = endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public Boolean getSkipped()
    {
        return skipped;
    }

    public void setSkipped(Boolean skipped)
    {
        this.skipped = skipped;
    }

    public Date getCreated()
    {
        return created;
    }

    public void setCreated(Date created)
    {
        this.created = created;
    }

    public Object getResult()
    {
        return result;
    }

    public void setResult(Object result) throws IllegalArgumentException
    {
        if (result == null)
        {
            this.result = null;
            return;
        }
        Type resultType = Type.fromTypeName(getType());
        switch (resultType)
        {
            case DATE:
                if (result instanceof String)
                {
                    try
                    {
                        this.result = DATE_FORMAT.parse((String) result);
                    }
                    catch (ParseException e)
                    {
                        throw new IllegalArgumentException("Invalid date string ("+ getResult() + ")");
                    }
                }
                else
                    throw new IllegalArgumentException("Invalid date string ("+ result + ")");
                break;
            case BOOLEAN:
                if (result instanceof Boolean)
                    this.result = result;
                else
                    throw new IllegalArgumentException("Invalid boolean value (" + result + ")");
                break;
            case CHOICE:
                if (result instanceof ArrayList)
                {
                    this.result = result;
                    // TODO verification of results inside
                }
                else
                    throw new IllegalArgumentException("Invalid choice value. Expecting an array of objects but got " + result.getClass());
                break;
            case INTEGER:
                if (result instanceof Integer)
                {
                    this.result = result;
                }
                else
                    throw new IllegalArgumentException("Invalid integer value ("+ result + ")");
                break;
            case FLOAT:
                if (result instanceof Float)
                {
                    this.result = result;
                }
                else
                    throw new IllegalArgumentException("Invalid float value (" + result + ")");
            case GROUPED_RESULT:
                if (result instanceof ArrayList)
                {
                    this.result = result;
                    // TODO more verification of elements in the group
                }
                else
                    throw new IllegalArgumentException("Invalid grouped result value.  Expecting an array of objects but got " + result.getClass());
                break;
            case STRING:
                if (result instanceof String)
                    this.result = result;
                else
                    throw new IllegalArgumentException("Invalid string result value.  Expecting String but got " + result.getClass());
                break;
        }
    }}
