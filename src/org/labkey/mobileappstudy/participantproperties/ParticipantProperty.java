package org.labkey.mobileappstudy.participantproperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.JdbcType;
import org.labkey.mobileappstudy.IDynamicListField;

import java.util.Collections;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParticipantProperty implements IDynamicListField
{
    public enum ParticipantPropertyType {
        PreEnrollment("preenrollment", 0),
        PostEnrollment("postenrollment", 1);

        static {}

        private String key;
        private int id;

        ParticipantPropertyType(String key, int id)
        {
            this.key = key;
            this.id = id;
        }

        public int getId()
        {
            return id;
        }

        public String getKey()
        {
            return key;
        }
    }

    public enum ParticipantPropertyDataType {
        Boolean ("boolean", JdbcType.BOOLEAN),
        Date("date", JdbcType.TIMESTAMP),
        Numeric ("numeric", JdbcType.DOUBLE),
        String("string", JdbcType.VARCHAR),
        Time("time", JdbcType.VARCHAR),
        UNKNOWN("Unknown", JdbcType.VARCHAR);

        private static final Map<String, ParticipantPropertyDataType> resultTypeMap;

        //String representing this in the design json
        private String resultTypeString;
        private JdbcType jdbcType;

        static {
            Map<String, ParticipantPropertyDataType> map = new CaseInsensitiveHashMap<>();
            for (ParticipantPropertyDataType resultType : values())
                map.put(resultType.resultTypeString, resultType);

            resultTypeMap = Collections.unmodifiableMap(map);
        }

        ParticipantPropertyDataType(String key, JdbcType jdbcType)
        {
            this.jdbcType = jdbcType;
            resultTypeString = key;
        }

        public String getResultTypeString()
        {
            return resultTypeString;
        }
        public static ParticipantPropertyDataType getStepResultType(String key)
        {
            return resultTypeMap.getOrDefault(key, UNKNOWN);
        }

        public JdbcType getDefaultJdbcType()
        {
            return jdbcType;
        }
    }

    //Needed for deserialization
    public ParticipantProperty()
    {
    }

    public ParticipantProperty(String propertyId, Object value)
    {
        setPropertyId(propertyId);
        setValue(value);
    }

    private String propertyId;

    private String propertyName;
    private ParticipantPropertyType propertyType;
    private ParticipantPropertyDataType propertyDataType;
    private Object value;

    public void setValue(Object value)
    {
        this.value = value;
    }

    public Object getValue()
    {
        return this.value;
    }

    public void setPropertyDataType(ParticipantPropertyDataType propertyDataType)
    {
        this.propertyDataType = propertyDataType;
    }

    @JsonIgnore
    public ParticipantPropertyDataType getPropertyDataType()
    {
        return this.propertyDataType;
    }

    public void setPropertyType(ParticipantPropertyType propertyType)
    {
        this.propertyType = propertyType;
    }

    @JsonIgnore
    public ParticipantPropertyType getPropertyType()
    {
        return this.propertyType;
    }

    public void setPropertyName(String propertyName)
    {
        this.propertyName = propertyName;
    }

    @JsonIgnore
    public String getPropertyName()
    {
        return this.propertyName;
    }

    public void setPropertyId(String propertyId)
    {
        this.propertyId = propertyId;
    }

    public String getPropertyId()
    {
        return this.propertyId;
    }

    @Override
    @JsonIgnore
    public String getKey()
    {
        return getPropertyId();
    }

    @JsonIgnore
    @Override
    public JdbcType getPropertyStorageType()
    {
        return getPropertyDataType().getDefaultJdbcType();
    }

    @Override
    @JsonIgnore
    public @Nullable Integer getMaxLength()
    {
        return null;
    }

    @Override
    @JsonIgnore
    public String getDescription()
    {
        return null;
    }

    @Override
    @JsonIgnore
    public String getLabel()
    {
        return getPropertyName();
    }

}
