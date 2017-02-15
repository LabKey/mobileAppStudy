package org.labkey.mobileappstudy.surveydesign;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.exp.PropertyType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by iansigmon on 2/2/17.
 */
public class SurveyStep
{

    /**
     * Enum describing the various Step Types
     */
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

    /**
     * Enum describing the various possible result types for any given step
     */
    public enum StepResultType
    {
        Scale("scale", (step) -> PropertyType.DOUBLE),
        ContinuousScale("continuousScale", (step) -> PropertyType.DOUBLE),
        TextScale("textScale", (step) -> PropertyType.STRING),
        ValuePicker ("valuePicker", (step) -> PropertyType.STRING),
        ImageChoice ("imageChoice", (step) -> PropertyType.STRING),
        TextChoice ("textChoice", (step) -> PropertyType.STRING),       //Keep type for value field in Choice list
        GroupedResult("grouped", (step) -> null),
        Boolean ("boolean", (step) -> PropertyType.BOOLEAN),
        Numeric ("numeric", (step) -> PropertyType.DOUBLE),           //TODO: Per result schema value should always be a double
        TimeOfDay("timeOfDay", (step) -> PropertyType.TIME),
        Date("date", (step) -> PropertyType.DATE_TIME),               //TODO: Per result schema value should always be the same date format
//        Date("date", StepResultType::getDateResultType),
        Text("text", (step) -> PropertyType.STRING),
        Email ("email", (step) -> PropertyType.STRING),
        TimeInterval ("timeInterval", (step) -> PropertyType.DOUBLE),
        Height("height", (step) -> PropertyType.DOUBLE),
        Location("location", (step) -> PropertyType.STRING),

        UNKNOWN("Unknown", (step) -> null);

        private static final Map<String, StepResultType> resultTypeMap;

        //String representing this in the design json
        private String resultTypeString;

        //Corresponding backing property type
        private Function<SurveyStep, PropertyType> resultTypeDelegate;

        static {
            Map<String, StepResultType> map = new HashMap<>();
            for (StepResultType resultType : values())
                map.put(resultType.resultTypeString, resultType);

            resultTypeMap = Collections.unmodifiableMap(map);
        }


        StepResultType(String key, Function<SurveyStep, PropertyType> interpretFormat)
        {
            resultTypeString = key;
            resultTypeDelegate = interpretFormat;
        }

        public static StepResultType getStepResultType(String key)
        {
            return resultTypeMap.getOrDefault(key, UNKNOWN);
        }

        @Nullable
        public PropertyType getPropertyType(SurveyStep step)
        {
            return resultTypeDelegate.apply(step);
        }

        //TODO: not sure if this is needed anymore,
//        @Nullable
//        public static PropertyType getNumericResultType(SurveyStep step)
//        {
//            switch(step.getStyle())
//            {
//                case 0:
//                    return PropertyType.INTEGER;
//                case 1:
//                    return PropertyType.DOUBLE;
//                default:
//                    return null;
//            }
//        }
//
//        @Nullable
//        public static PropertyType getDateResultType(SurveyStep step)
//        {
//            switch(step.getStyle())
//            {
//                case 0:
//                    return PropertyType.DATE;
//                case 1:
//                    return PropertyType.DATE_TIME;
//                default:
//                    return null;
//            }
//        }
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

    private final static String MAXLENGTH_KEY = "maxLength";
    private final static String STYLE_KEY = "style";

    private String type;
    private String resultType;
    private String key;
    private String phi;
    private String title;
    private List<SurveyStep> steps = null;

    public boolean getRepeatable()
    {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable)
    {
        this.repeatable = repeatable;
    }

    private Map<String, Object> format;

    //Currently ignored since we need to make the field
    private boolean skippable = true;

    //Currently ignored. Only applicable to forms and choice, and those are distinguished by result type
    private boolean repeatable = false;

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

    public Map<String, Object> getFormat()
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
        if (getFormat() == null || getFormat().get(MAXLENGTH_KEY) == null)
            return null;

        String val = String.valueOf(getFormat().get(MAXLENGTH_KEY));
        if (StringUtils.isBlank(val))
            return null;

        //Json MaxLength = 0 indicates Max text size
        Integer intVal = Integer.valueOf(val);
        return intVal == 0 ? Integer.MAX_VALUE : intVal;
    }

    @Nullable
    public List<SurveyStep> getSteps()
    {
        return steps;
    }

    public Integer getStyle()
    {
        if (getFormat() == null)
            return null;

        String val = String.valueOf(getFormat().get(STYLE_KEY));
        if (StringUtils.isBlank(val))
            return null;

        return Integer.valueOf(val);
    }

    public PropertyType getPropertyType()
    {
        return getResultType().getPropertyType(this);
    }
}
