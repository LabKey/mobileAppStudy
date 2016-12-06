package org.labkey.mobileappstudy.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.labkey.api.data.JdbcType;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by susanh on 11/28/16.
 */
public class SurveyResult extends ResponseMetadata
{
    public enum ValueType
    {
        DATE("Date", true, JdbcType.TIMESTAMP), // in Lists, we use DateTime even for displaying dates
        BOOLEAN("Bool", true, JdbcType.BOOLEAN),
        CHOICE("choice", false, null),
        INTEGER("scale", true, JdbcType.INTEGER),
        FLOAT("number", true, JdbcType.REAL),
        GROUPED_RESULT("groupedResult", false, null),
        TEXT("text", true, JdbcType.VARCHAR),
        STRING("string", true, JdbcType.VARCHAR);

        private String _typeName;
        private Boolean _singleValued;
        private JdbcType _jdbcType;

        ValueType(String typeName, Boolean singleValued, JdbcType jdbcType)
        {
            _typeName = typeName;
            _singleValued = singleValued;
            _jdbcType = jdbcType;
        }

        public Boolean isSingleValued()
        {
            return _singleValued;
        }

        public JdbcType getJdbcType()
        {
            return _jdbcType;
        }

        public String getTypeName()
        {
            return _typeName;
        }

        public static ValueType fromTypeName(String name)
        {
            if (name != null)
            {
                for (ValueType t : ValueType.values())
                {
                    if (t.getTypeName().equalsIgnoreCase(name))
                    {
                        return t;
                    }
                }
            }
            throw new IllegalArgumentException("No field value type with name " + name + " found");
        }

    }

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private String _type;
    private String _identifier;
    private Object _result;
    private Object _value;
    private String _listName;


    public ValueType getValueType()
    {
        return ValueType.fromTypeName(getType());
    }

    public String getType()
    {
        return _type;
    }

    public void setType(String type)
    {
        this._type = type;
    }

    public String getIdentifier()
    {
        return _identifier;
    }

    public void setIdentifier(String identifier)
    {
        this._identifier = identifier;
    }

    @Override
    public String getListName()
    {
        return _listName;
    }

    @Override
    public void setListName(String listName)
    {
        this._listName = listName;
    }

    public Object getResult()
    {
        return _result;
    }

    public Object getValue()
    {
        if (_value == null && !getSkipped() && _result != null)
            setValue();
        return _value;
    }

    private void setValue()
    {
        switch (getValueType())
        {
            case DATE:
                if (_result instanceof String)
                {
                    try
                    {
                        this._value = DATE_FORMAT.parse((String) _result);
                    }
                    catch (ParseException e)
                    {
                        throw new IllegalArgumentException("Invalid date string format for field '" + getIdentifier() + "' ("+ _result + ")");
                    }
                }
                else
                    throw new IllegalArgumentException("Value type for Date field '" + getIdentifier() + "' expected to be String but got "+ _result.getClass());
                break;
            case BOOLEAN:
                if (_result instanceof Boolean)
                    this._value = _result;
                else
                    throw new IllegalArgumentException("Value type for field '" + getIdentifier() + "' expected to be Boolean but got " + _result.getClass());
                break;
            case CHOICE:
                if (_result instanceof List)
                {
                    this._value = _result;
                }
                else
                    throw new IllegalArgumentException("Value type for choice field '" + getIdentifier() + "' expected to be ArrayList but got " + _result.getClass());
                break;
            case INTEGER:
                if (_result instanceof Integer)
                {
                    this._value = _result;
                }
                else
                    throw new IllegalArgumentException("Value type for field '" + getIdentifier() + "' expected to be Integer but got " + _result.getClass());
                break;
            case FLOAT:
                if (_result instanceof Float || _result instanceof Integer)
                {
                    this._value = _result;
                }
                else
                    throw new IllegalArgumentException("Value type for field '" + getIdentifier() + "' expected to be Integer or Float but got " + _result.getClass());
                break;
            case GROUPED_RESULT:
                if (_result instanceof List)
                {
                    this._value = convertSurveyResults((List) _result);
                }
                else
                    throw new IllegalArgumentException("Value type for grouped result field '" + getIdentifier() + "' expected to be ArrayList but got " + _result.getClass());
                break;
            case TEXT:
            case STRING:
                if (_result instanceof String)
                    this._value = _result;
                else
                    throw new IllegalArgumentException("Value type for field '" + getIdentifier() + "' expected to be String but got " + _result.getClass());
                break;
        }
    }


    public void setResult(Object result) throws IllegalArgumentException
    {
        _result = result;
    }

    /**
     * recursively convert a list of survey results, which may itself contain lists of survey results, into a list of objects.
     * The leaves of this object tree are of type SurveyResult.
     *
     * @param list The list of objects to be converted
     * @return a list of either SurveyResult objects or List containing the result of a conversion.
     */
    private List<Object> convertSurveyResults(List list)
    {
        List<Object> results = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(SurveyResponse.DATE_TIME_FORMAT);
        for (Object item : list)
        {
            if (item instanceof List)
            {
                results.add(convertSurveyResults((List) item));
            }
            else if (item instanceof LinkedHashMap)
            {
                results.add(mapper.convertValue(item, SurveyResult.class));
            }
            else
            {
                throw new IllegalArgumentException("Value type for grouped result field '" + getIdentifier() + "' expected to be ArrayList or HashMap but got " + item.getClass());
            }
        }
        return results;
    }
}
