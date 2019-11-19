package org.labkey.mobileappstudy;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.PropertyStorageSpec;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.User;
import org.labkey.mobileappstudy.data.MobileAppStudy;
import org.labkey.mobileappstudy.participantproperties.ParticipantPropertiesDesign;
import org.labkey.mobileappstudy.surveydesign.InvalidDesignException;

public abstract class DynamicListProcessor
{
    protected Logger logger;

    public DynamicListProcessor()
    {
        this(Logger.getLogger(MobileAppStudy.class));
    }

    public DynamicListProcessor(Logger logger)
    {
        this.logger = logger != null ? logger : Logger.getLogger(MobileAppStudy.class);
    }

    /**
     * Get existing list or create new one
     *
     * @param listName name of list
     * @param container where list resides
     * @param user accessing/creating list
     * @return ListDefinition representing list
     * @throws InvalidDesignException if list is not able to be created, this is a wrapper of any other exception
     */
    protected ListDefinition ensureList(Container container, User user, String listName, String parentListName) throws InvalidDesignException
    {
        ListDefinition listDef = ListService.get().getList(container, listName);
        return listDef != null ?
                listDef :
                newListDefinition(container, user, listName, parentListName);
    }

    private ListDefinition newListDefinition(Container container, User user, String listName, String parentListName) throws InvalidDesignException
    {
        try
        {
            ListDefinition list = ListService.get().createList(container, listName, ListDefinition.KeyType.AutoIncrementInteger);
            list.setKeyName("Key");
//            setKey(list, listName);
            list.save(user);

            logger.info(String.format(LogMessageFormats.LIST_CREATED, listName));

            //Return a refreshed version of listDefinition
            return ListService.get().getList(container, listName);
        }
        catch (Exception e)
        {
            throw new InvalidDesignException(String.format(LogMessageFormats.UNABLE_CREATE_LIST, listName), e);
        }
    }

    private ListDefinition newParticipantPropertiesListDefinition(Container container, User user, String listName) throws InvalidDesignException
    {
        try
        {
            ListDefinition list = ListService.get().createList(container, listName, ListDefinition.KeyType.Varchar);
            setKey(list, listName);
            list.save(user);

            logger.info(String.format(LogMessageFormats.LIST_CREATED, listName));

            //Return a refreshed version of listDefinition
            return ListService.get().getList(container, listName);
        }
        catch (Exception e)
        {
            throw new InvalidDesignException(String.format(LogMessageFormats.UNABLE_CREATE_LIST, listName), e);
        }
    }

    private void setKey(ListDefinition list, String listName)
    {
        if (listName == ParticipantPropertiesDesign.PARTICIPANT_PROPERTIES_LIST_NAME)
        {
            list.setKeyName("EnrollmentToken");
            list.setKeyType(ListDefinition.KeyType.Varchar);
        }
        else
            list.setKeyName("Key");
    }

    protected DomainProperty ensureStepProperty(Domain listDomain, IDynamicListField field) throws InvalidDesignException
    {
        DomainProperty prop = listDomain.getPropertyByName(field.getKey());
        if (prop != null)
        {
            //existing property
            if (prop.getPropertyDescriptor().getJdbcType() != field.getPropertyStorageType())
                throw new InvalidDesignException(String.format(LogMessageFormats.RESULT_TYPE_MISMATCH, field.getKey()));

            //Update a string field's size. Increase only.
            if (prop.getPropertyType() == PropertyType.STRING && field.getMaxLength() != null)
            {
                //Logged in List audit log
                if (field.getMaxLength() > prop.getScale())
                    prop.setScale(field.getMaxLength());
            }
        }
        else
        {
            //New property
            prop = getNewDomainProperty(listDomain, field);
        }

        return prop;
    }

    protected static DomainProperty getNewDomainProperty(Domain domain, IDynamicListField step)
    {
        return getNewDomainProperty(domain, step.getKey(), step.getPropertyStorageType(),null, step.getDescription(), step.getMaxLength());
    }

    protected static DomainProperty getNewDomainProperty(Domain domain, String key, JdbcType propertyType, String label, String description, Integer length)
    {
        DomainProperty prop = domain.addProperty(new PropertyStorageSpec(key, propertyType));
        prop.setName(key);
        prop.setLabel(label);
        prop.setDescription(description);
        prop.setPropertyURI(domain.getTypeURI() + "#" + key);
        if (prop.getPropertyType() == PropertyType.STRING && length != null)
            prop.setScale(length);

        prop.setMeasure(false);
        prop.setDimension(false);
        prop.setRequired(false);

        return prop;
    }

    protected static class LogMessageFormats
    {
        public static final String UNABLE_TO_APPLY_SURVEY = "Unable to apply changes";
        public static final String STEP_IS_NULL = "Step is null";
        public static final String RESULT_TYPE_MISMATCH = "Can not change question result types. Field: %1$s";
        public static final String INVALID_RESULT_TYPE = "Unknown step result type for key: %1$s";
        public static final String PROVIDER_NULL = "No SurveyDesignProvider configured.";
        public static final String DESIGN_NULL = "Unable to parse design metadata";
        public static final String MISSING_METADATA = "Design document does not contain all the required fields (activityId, steps)";
        public static final String START_UPDATE_SURVEY = "Getting new survey version: Study: %1$s, Survey: %2$s, Version: %3$s";
        public static final String END_SURVEY_UPDATE = "Survey update completed";
        public static final String UNABLE_CREATE_LIST = "Unable to create new list. List: %1$s";
        public static final String LIST_CREATED = "List [%1$s] successfully created.";
        public static final String SUBLIST_PROPERTY_ERROR = "Unable to add sub-list property: %1$s";
        public static final String NO_GROUP_STEPS = "Form contains no steps: Step: %1$s";
        public static final String DUPLICATE_FIELD_KEY = "Design schema contains duplicate field keys: %1$s";
        public static final String START_UPDATE_PARTICIPANT_PROPERTIES = "Checking for participant properties for study [%1$s] current version [%2$s] new version [%3$s]";
        public static final String UPDATE_PARTICIPANT_PROPERTIES = "Applying update for participant properties for study [%1$s] current version [%2$s] new version [%3$s]";
        public static final String FINISH_UPDATE_PARTICIPANT_PROPERTIES = "Updated participant properties for study [%1$s] from [%2$s --> %3$s]";
        public static final String PARTICIPANT_PROPERTIES_MISSING_METADATA = "Design document does not contain required fields (study metadata and properties)";
        public static final String PARTICIPANT_PROPERTIES_END_UPDATE = "Participant Properties update completed";
    }
}
