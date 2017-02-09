package org.labkey.mobileappstudy;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.PropertyStorageSpec;
import org.labkey.api.exp.ChangePropertyDescriptorException;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.LimitedUser;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.security.roles.SubmitterRole;
import org.labkey.mobileappstudy.data.MobileAppStudy;
import org.labkey.mobileappstudy.data.SurveyResponse;
import org.labkey.mobileappstudy.surveydesign.InvalidDesignException;
import org.labkey.mobileappstudy.surveydesign.SurveyDesign;
import org.labkey.mobileappstudy.surveydesign.SurveyStep;
import org.labkey.mobileappstudy.surveydesign.SurveyStep.StepResultType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class to process and apply a survey design to the underlying lists
 */
public class SurveyDesignProcessor
{
    private Logger logger;

    /**
     * List properties that are need for survey relationships
     */
    private static final List<String> REQUIRED_SUBLIST_PROPERTIES = Arrays.asList("Key", "ParticipantId", "SurveyId");

    public SurveyDesignProcessor(Logger logger)
    {
        this.logger = logger != null ? logger : Logger.getLogger(MobileAppStudy.class);
    }

    public void updateSurveyDesign(SurveyResponse surveyResponse, User user) throws InvalidDesignException
    {
        //Get survey schema
        // * What format/structure?
        MobileAppStudy study = MobileAppStudyManager.get().getStudyFromAppToken(surveyResponse.getAppToken());

        logger.info(String.format(LogMessageFormats.START_UPDATE_SURVEY,
                study.getShortName(), surveyResponse.getSurveyId(), surveyResponse.getSurveyVersion()));
        SurveyDesign design = MobileAppStudyManager.get().getSurveyDesignProvider().getSurveyDesign(study.getContainer(), study.getShortName(), surveyResponse.getSurveyId(), surveyResponse.getSurveyVersion());

        if (design != null)
        {
            // if a user isn't provided, need to create a LimitedUser to use for checking permissions, wrapping the Guest user
            User insertUser = new LimitedUser((user == null)? UserManager.getGuestUser() : user,
                    new int[0], Collections.singleton(RoleManager.getRole(SubmitterRole.class)), false);

            //Get existing list or create new one
            ListDefinition listDef = ListService.get().getList(study.getContainer(), design.getSurveyName());
            listDef = listDef != null ?
                    listDef :
                    newSurveyListDefinition(insertUser, study.getContainer(), design.getSurveyName(), false);

            applySurveyUpdate(listDef, design, insertUser, study.getContainer());
        }
        else
            throw new InvalidDesignException(LogMessageFormats.DESIGN_NULL);
    }

    private ListDefinition newSurveyListDefinition(User user, Container container, String listName, boolean isSublist) throws InvalidDesignException
    {
        try
        {
            ListDefinition list = ListService.get().createList(container, listName, ListDefinition.KeyType.AutoIncrementInteger);
            list.setKeyName("Key");
            list.getDomain().addProperty(new PropertyStorageSpec("Key", JdbcType.INTEGER));
            list.getDomain().addProperty(new PropertyStorageSpec("ParticipantId", JdbcType.INTEGER));

            if (isSublist)
                list.getDomain().addProperty(new PropertyStorageSpec("SurveyId", JdbcType.INTEGER));

            list.save(user);

            logger.info(String.format(LogMessageFormats.LIST_CREATED, listName));
            return list;
        }
        catch (Exception e)
        {
            throw new InvalidDesignException(String.format(LogMessageFormats.UNABLE_CREATE_LIST, listName), e);
        }
    }

    private void applySurveyUpdate(ListDefinition list, SurveyDesign design, User user, Container container) throws InvalidDesignException
    {
        Domain listDomain = list.getDomain();

        try
        {
            for (SurveyStep step: design.getSteps())
            {
                StepResultType resultType = step.getResultType();
                //need to check for choice/group
                switch(resultType)
                {
                    case TextChoice:
                        updateChoiceList(container, user, design, step);
                        break;
                    case GroupedResult:
                        //TODO
                        break;
                    case UNKNOWN:
                        throw new InvalidDesignException(String.format(LogMessageFormats.INVALID_RESULTTYPE, step.getKey()));
                    default:
                        ensureStepProperty(listDomain, step);
                        break;
                }
            }

            listDomain.save(user);
            logger.info(LogMessageFormats.END_SURVEY_UPDATE);
        }
        catch (InvalidDesignException e)
        {
            //Pass it through
            throw e;
        }
        catch (Exception e)
        {
            //Wrap any other exception
            throw new InvalidDesignException(LogMessageFormats.UNABLE_TO_APPLY_SURVEY, e);
        }
    }

    private void ensureStepProperty(Domain listDomain, SurveyStep step) throws InvalidDesignException
    {
        DomainProperty prop = listDomain.getPropertyByName(step.getKey());
        if (prop != null)
        {
            //existing property
            if (prop.getPropertyType() != step.getResultType().getPropertyType(step.getFormat()))
                throw new InvalidDesignException(String.format(LogMessageFormats.RESULTTYPE_MISMATCH, step.getKey()));

            //TODO: Check properties of recursive types
        }
        else
        {
            //TODO: what about repeatables?
            //TODO: log?
            //New property
            prop = getNewDomainProperty(listDomain, step);
        }

        //Update a string field's size. Increase only.
        if (prop.getPropertyType() == PropertyType.STRING && step.getMaxLength() != null)
        {
            //TODO: log?
            //MaxLength = 0 indicates Max text size
            if (step.getMaxLength() == 0)
                prop.setScale(Integer.MAX_VALUE);
            else if (step.getMaxLength() > prop.getScale())
                prop.setScale(step.getMaxLength());
        }
    }

    private void updateChoiceList(Container container, User user, SurveyDesign design, SurveyStep step) throws InvalidDesignException, ChangePropertyDescriptorException
    {
        String listName = design.getSurveyName() + step.getKey();

        //Get existing list or create new one
        ListDefinition listDef = ListService.get().getList(container, listName);
        listDef = listDef != null ?
                listDef :
                newSurveyListDefinition(user, container, listName, true);

        Domain domain = listDef.getDomain();

        //Check for key, participantId, and parent survey fields
        ensureStandardProperties(domain, step, user);

        //Add value property
        ensureStepProperty(domain, step);

        try
        {
            domain.save(user);
        }
        catch (ChangePropertyDescriptorException e)
        {
            throw new InvalidDesignException(String.format(LogMessageFormats.SUBLIST_PROPERTY_ERROR, step.getKey()), e);
        }
    }

    private void ensureStandardProperties(Domain domain, SurveyStep step, User user) throws InvalidDesignException
    {
        if (domain == null) //TODO: put in a real message
            throw new InvalidDesignException("Uh we got problems");

        for (String propName : REQUIRED_SUBLIST_PROPERTIES)
        {
            DomainProperty prop = domain.getPropertyByName(propName);

            if (prop == null)   //TODO: add instead of throw
                throw new InvalidDesignException("Sub-list missing required fields");
        }
    }

    private static DomainProperty getNewDomainProperty(Domain domain, SurveyStep step)
    {
        DomainProperty prop = domain.addProperty();
        prop.setName(step.getKey());
        prop.setPropertyURI(domain.getTypeURI() + "#" + step.getKey());
        prop.setDescription(step.getTitle());

        //Group and Choice will use Integer RowId to appropriate list
        prop.setRangeURI(step.getResultType().getPropertyType(step.getFormat()).getTypeUri());

        //TODO: not sure if these are needed...
//                prop.setMeasure(false);
//                prop.setDimension(false);
//                prop.setRequired(true);

        return prop;
    }

    private static class LogMessageFormats
    {
        public static final String UNABLE_TO_APPLY_SURVEY = "Unable to apply survey changes";
        public static final String RESULTTYPE_MISMATCH = "Can not change question result types. Field: %1$s";
        public static final String INVALID_RESULTTYPE = "Unknown step result type for key: %1$s";
        public static final String DESIGN_NULL = "Design was null";
        public static final String START_UPDATE_SURVEY = "Getting new survey version: Study: %1$s, Survey: %2$s, Version: %3$s";
        public static final String END_SURVEY_UPDATE = "Survey update completed";
        public static final String UNABLE_CREATE_LIST = "Unable to create new list. Study: %1$s, Survey: %2$s, Version: %3$s, List: %4$s";
        public static final String LIST_CREATED = "Survey list [%1$s] successfully created.";
        public static final String SUBLIST_PROPERTY_ERROR = "Unable to add sub-list property: %1$s";
    }
}
