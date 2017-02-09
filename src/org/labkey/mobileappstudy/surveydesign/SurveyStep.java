package org.labkey.mobileappstudy.surveydesign;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.exp.PropertyType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by iansigmon on 2/2/17.
 */
public class SurveyStep
{
    public enum SurveyStepType
    {
        Form("form"),
        Instruction("instruction"),
        Question("question"),
        Task("task"),
        UNKNOWN(null);

        private static final Map<String, SurveyStepType> stepTypeMap;

        static {
            Map<String, SurveyStepType> map = new HashMap<>();
            for (SurveyStepType resultType : values())
                map.put(resultType.designTypeString, resultType);

            stepTypeMap = Collections.unmodifiableMap(map);
        }

        //String representing this in the design json
        private String designTypeString;

        SurveyStepType(String type)
        {
            designTypeString = type;
        }

        static SurveyStepType getStepType(String key)
        {
            return stepTypeMap.getOrDefault(key, UNKNOWN);
        }
    }

    public enum StepResultType
    {
        Scale("scale", (format) -> PropertyType.INTEGER),
        ContinuousScale("continuousScale", (format) -> PropertyType.DOUBLE),
        TextScale("textScale", (format) -> PropertyType.STRING),
        ValuePicker ("valuePicker", (format) -> PropertyType.STRING),
        ImageChoice ("imageChoice", (format) -> PropertyType.STRING),       //will hold string path to image?
        TextChoice ("textChoice", (format) -> PropertyType.INTEGER),        //will hold reference to other list
        GroupedResult("groupedResult", (format) -> PropertyType.INTEGER),   //will hold reference to other list
        Boolean ("boolean", (format) -> PropertyType.BOOLEAN),
        Numeric ("numeric", StepResultType::getNumericResultType),
        TimeOfDay("timeOfDay", StepResultType::getDateResultType),              //TODO: verify PropertyType is appropriate
        Date("date", StepResultType::getDateResultType),
        Text("text", (format) -> PropertyType.STRING),
        Email ("email ", (format) -> PropertyType.STRING),
        TimeInterval ("timeInterval", (format) -> PropertyType.BIGINT),    //TODO: verify PropertyType is appropriate
        Height("height", (format) -> PropertyType.DOUBLE),
        Location("location", (format) -> PropertyType.STRING),              //TODO: verify PropertyType is appropriate

        UNKNOWN("Unknown", (format) -> null);

        private static final Map<String, StepResultType> resultTypeMap;

        //String representing this in the design json
        private String resultTypeString;

        //Corresponding backing property type
        private Function<ResultFormat, PropertyType> resultTypeDelegate;

        static {
            Map<String, StepResultType> map = new HashMap<>();
            for (StepResultType resultType : values())
                map.put(resultType.resultTypeString, resultType);

            resultTypeMap = Collections.unmodifiableMap(map);
        }


        StepResultType(String key, Function<ResultFormat, PropertyType> interpretFormat)
        {
            resultTypeString = key;
            resultTypeDelegate = interpretFormat;
        }

        public static StepResultType getStepResultType(String key)
        {
            return resultTypeMap.getOrDefault(key, UNKNOWN);
        }

        @Nullable
        public PropertyType getPropertyType(ResultFormat format)
        {
            return resultTypeDelegate.apply(format);
        }

        @Nullable
        public static PropertyType getNumericResultType(ResultFormat format)
        {
            switch(format.getStyle())
            {
                case 0:
                    return PropertyType.INTEGER;
                case 1:
                    return PropertyType.DOUBLE;
                default:
                    return null;
            }
        }

        @Nullable
        public static PropertyType getDateResultType(ResultFormat format)
        {
            switch(format.getStyle())
            {
                case 0:
                    return PropertyType.DATE;
                case 1:
                    return PropertyType.DATE_TIME;
                default:
                    return null;
            }
        }
    }

    public enum PHIClassification
    {
        PHI("PHI"),
        Limited("Limited"),
        NotPHI("NotPHI");

        private static final Map<String, PHIClassification> CONVERTER;

        private String jsonString;
        static {
            Map<String, PHIClassification> map = new HashMap<>();
            for(PHIClassification val : values())
                map.put(val.jsonString, val);

            CONVERTER = Collections.unmodifiableMap(map);
        }

        PHIClassification(String val)
        {
            jsonString = val;
        }

         public static PHIClassification getClassicication(String key)
         {
             if (StringUtils.isNotBlank(key))
                 return CONVERTER.get(key);
             else
                 //TODO: not sure what default should be, so defaulting to most limited
                 return PHIClassification.PHI;
         }

         public String getJsonValue()
         {
             return jsonString;
         }
    }

    private String type;
    private String resultType;
    private String key;
    private String phi;
    private String title;
    private boolean skippable = true;
    private boolean repeatable = false;

    //TODO: Convert this to JSON Object
    private ResultFormat format;

    public SurveyStepType getStepType()
    {
        return SurveyStepType.getStepType(type);
    }

    public String getKey()
    {
        return key;
    }

    void setKey(String key)
    {
        this.key = key;
    }

    public StepResultType getResultType()
    {
        return StepResultType.getStepResultType(this.resultType);
    }

    void setResultType(String resultType)
    {
        this.resultType = resultType;
    }

    void setType(String type)
    {
        this.type = type;
    }

    public ResultFormat getFormat()
    {
        return format;
    }

    public void setPhiClassification(PHIClassification classification)
    {
        this.phi = classification.getJsonValue();
    }

    public PHIClassification getPHIClassification()
    {
        return PHIClassification.getClassicication(phi);
    }

    public String getTitle()
    {
        return title;
    }

    @Nullable
    public Integer getMaxLength()
    {
        if (getFormat() == null)
            return null;

        return getFormat().getMaxLength();
    }

    private class ResultFormat
    {
        public ResultFormat()
        {
        }

        private Integer style = null;
        private Integer maxLength = null;

        public Integer getStyle()
        {
            return style;
        }
        public Integer getMaxLength()
        {
            return maxLength;
        }
//TODO: Determine which are actually needed
//        private String validationRegex;
//        private String invalidMessage;
//        private Boolean multipleLine;
//        private String placeholder;
//        private String unit;
//        private Integer minValue;
//        private Integer maxValue;
//
//
//        public Integer getMaxValue()
//        {
//            return maxValue;
//        }
//
//        public void setMaxValue(Integer maxValue)
//        {
//            this.maxValue = maxValue;
//        }
//
//        public Integer getMinValue()
//        {
//            return minValue;
//        }
//
//        public void setMinValue(Integer minValue)
//        {
//            this.minValue = minValue;
//        }
//
//        public String getUnit()
//        {
//            return unit;
//        }
//
//        public void setUnit(String unit)
//        {
//            this.unit = unit;
//        }
//
//        public String getPlaceholder()
//        {
//            return placeholder;
//        }
//
//        public void setPlaceholder(String placeholder)
//        {
//            this.placeholder = placeholder;
//        }
//
//        public Boolean getMultipleLine()
//        {
//            return multipleLine;
//        }
//
//        public void setMultipleLine(Boolean multipleLine)
//        {
//            this.multipleLine = multipleLine;
//        }
//
//        public String getInvalidMessage()
//        {
//            return invalidMessage;
//        }
//
//        public void setInvalidMessage(String invalidMessage)
//        {
//            this.invalidMessage = invalidMessage;
//        }
//
//        public String getValidationRegex()
//        {
//            return validationRegex;
//        }
//
//        public void setValidationRegex(String validationRegex)
//        {
//            this.validationRegex = validationRegex;
//        }
//
    }
}
