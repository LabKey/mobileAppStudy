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
        BOOLEAN("boolean", true, JdbcType.BOOLEAN),
        CHOICE("textChoice", false, null),
//        INTEGER("scale", true, JdbcType.INTEGER),     //TODO: should we keep?
//        DOUBLE("number", true, JdbcType.DOUBLE),
        GROUPED_RESULT("grouped", false, null),
        TEXT("text", true, JdbcType.VARCHAR),
        STRING("string", true, JdbcType.VARCHAR),
        SCALE("scale", true, JdbcType.DOUBLE),
        CONTINUOUS_SCALE("continuousScale", true, JdbcType.DOUBLE),
        TEXT_SCALE("textScale", true, JdbcType.VARCHAR),
        VALUE_PICKER("valuePicker", true, JdbcType.VARCHAR),
        IMAGE_CHOICE("imageChoice", true, JdbcType.VARCHAR),
        TIME_OF_DAY("timeOfDay", true, JdbcType.TIMESTAMP),
        EMAIL("email", true, JdbcType.VARCHAR),
        TIME_INTERVAL("timeInterval", true, JdbcType.DOUBLE),
        HEIGHT("height", true, JdbcType.DOUBLE),
        LOCATION("location", true, JdbcType.VARCHAR),

        //The storage type is dependant on json values that are not passed in the response so use the larger types.
        DATE("date", true, JdbcType.TIMESTAMP), // in Lists, we use DateTime even for displaying dates
        NUMERIC("numeric", true, JdbcType.DOUBLE),
        ;

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
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private String _resultType;
    private String _key;
    private Object _value;
    private Object _parsedValue;
    private String _listName;

    public ValueType getValueType()
    {
        return ValueType.fromTypeName(getResultType());
    }

    public String getResultType()
    {
        return _resultType;
    }

    public void setResultType(String resultType)
    {
        this._resultType = resultType;
    }

    public String getKey()
    {
        return _key;
    }

    public void setKey(String key)
    {
        this._key = key;
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

    public Object getValue()
    {
        return _value;
    }
    public void setValue(Object result) throws IllegalArgumentException
    {
        _value = result;
    }


    public Object getParsedValue()
    {
        if (_parsedValue == null && !getSkipped() && _value != null)
            setParsedValue();
        return _parsedValue;
    }

    private void setParsedValue()
    {
        switch (getValueType())
        {
            case DATE:
                if (_value instanceof String)
                {
                    try
                    {
                        this._parsedValue = DATE_FORMAT.parse((String) _value);
                    }
                    catch (ParseException e)
                    {
                        throw new IllegalArgumentException("Invalid date string format for field '" + getKey() + "' ("+ _value + ")");
                    }
                }
                else
                    throw new IllegalArgumentException("Value type for Date field '" + getKey() + "' expected to be String but got "+ _value.getClass());
                break;
            case BOOLEAN:
                if (_value instanceof Boolean)
                    this._parsedValue = _value;
                else
                    throw new IllegalArgumentException("Value type for field '" + getKey() + "' expected to be Boolean but got " + _value.getClass());
                break;
            case CHOICE:
                if (_value instanceof List)
                {
                    this._parsedValue = _value;
                }
                else
                    throw new IllegalArgumentException("Value type for choice field '" + getKey() + "' expected to be ArrayList but got " + _value.getClass());
                break;
            case TIME_OF_DAY:
                if (_value instanceof String)
                {
                    try
                    {
                        this._parsedValue = TIME_FORMAT.parse((String) _value);
                    }
                    catch (ParseException e)
                    {
                        throw new IllegalArgumentException("Invalid date string format for field '" + getKey() + "' ("+ _value + ")");
                    }
                }
                else
                    throw new IllegalArgumentException("Value type for Date field '" + getKey() + "' expected to be String but got "+ _value.getClass());
                break;
            //TODO: should we keep?
//            case INTEGER:
//                if (_value instanceof Integer)
//                {
//                    this._parsedValue = _value;
//                }
//                else
//                    throw new IllegalArgumentException("Value type for field '" + getKey() + "' expected to be Integer but got " + _value.getClass());
//                break;
//            case DOUBLE:
            case NUMERIC:
            case HEIGHT:
            case SCALE:
            case CONTINUOUS_SCALE:
            case TIME_INTERVAL:
                if (_value instanceof Double || _value instanceof Float || _value instanceof Integer)
                {
                    this._parsedValue = _value;
                }
                else
                    throw new IllegalArgumentException("Value type for field '" + getKey() + "' expected to be Integer or Float but got " + _value.getClass());
                break;
            case GROUPED_RESULT:
                if (_value instanceof List)
                {
                    this._parsedValue = convertSurveyResults((List) _value);
                }
                else
                    throw new IllegalArgumentException("Value type for grouped result field '" + getKey() + "' expected to be ArrayList but got " + _value.getClass());
                break;
            case TEXT_SCALE:
            case VALUE_PICKER:
            case IMAGE_CHOICE:
            case EMAIL:    //TODO: validation?
            case LOCATION: //TODO: validation?
            case TEXT:
            case STRING:
                if (_value instanceof String)
                    this._parsedValue = _value;
                else
                    throw new IllegalArgumentException("Value type for field '" + getKey() + "' expected to be String but got " + _value.getClass());
                break;
        }
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
                throw new IllegalArgumentException("Value type for grouped result field '" + getKey() + "' expected to be ArrayList or HashMap but got " + item.getClass());
            }
        }
        return results;
    }
}
