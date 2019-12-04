/*
 * Copyright (c) 2017-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.mobileappstudy;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.PropertyStorageSpec;
import org.labkey.api.exp.ChangePropertyDescriptorException;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.property.Lookup;
import org.labkey.api.security.LimitedUser;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.security.roles.SubmitterRole;
import org.labkey.mobileappstudy.data.MobileAppStudy;
import org.labkey.mobileappstudy.data.SurveyResponse;
import org.labkey.mobileappstudy.surveydesign.InvalidDesignException;
import org.labkey.mobileappstudy.surveydesign.SurveyDesign;
import org.labkey.mobileappstudy.surveydesign.SurveyDesignProvider;
import org.labkey.mobileappstudy.surveydesign.SurveyStep;
import org.labkey.mobileappstudy.surveydesign.SurveyStep.StepResultType;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class to process and apply a survey design to the underlying lists
 */
public class SurveyDesignProcessor extends DynamicListProcessor
{
    /**
     * List properties that are needed for survey relationships
     */
    private enum StandardProperties
    {
        Key("Key", JdbcType.INTEGER),
        ParticipantId("ParticipantId", JdbcType.INTEGER),
        ParentId("ParentId", JdbcType.INTEGER);

        private String key;
        private JdbcType type;

        StandardProperties(String key, JdbcType type)
        {
            this.key = key;
            this.type = type;
        }

        public static void ensureStandardProperties(Container container, Domain domain, String parentListName) throws InvalidDesignException
        {
            if (domain == null)
                throw new InvalidDesignException("Invalid list domain");

            for (StandardProperties val : values())
            {
                //ParentId uses parentListName as the field name
                String key = val.key == ParentId.key ? getParentListKey(parentListName) : val.key;
                DomainProperty prop = domain.getPropertyByName(key);

                if (prop == null)
                {
                    prop = addStandardProperty(container, val, domain, parentListName);
                    if (prop != null)
                        prop.setPropertyURI(domain.getTypeURI() + "#" + key);
                }
            }
        }

        /**
         * Add properties that are common to the list implementation and any special aspects of that property like Lookups
         * @param container hosting the list
         * @param propName name of the property
         * @param listDomain domain property will belong to
         * @param parentListName (Optional) of parent list. Required if ParentId property is needed
         */
        private static DomainProperty addStandardProperty(@NotNull Container container, @NotNull StandardProperties propName, @NotNull Domain listDomain, @Nullable String parentListName)
        {
            DomainProperty prop = null;
            switch (propName)
            {
                case Key:
                    prop = listDomain.addProperty(new PropertyStorageSpec(propName.key, propName.type));
                    break;
                case ParticipantId:
                    //Most lists use participantId
                    prop = listDomain.addProperty(new PropertyStorageSpec(ParticipantId.key, propName.type));
                    prop.setLookup(new Lookup(container, MobileAppStudySchema.NAME, MobileAppStudySchema.PARTICIPANT_TABLE));
                    break;
                case ParentId:
                    if (StringUtils.isNotBlank(parentListName))
                    {
                        prop = listDomain.addProperty(new PropertyStorageSpec( getParentListKey(parentListName), propName.type));
                        prop.setLookup(new Lookup(container, "lists", parentListName));
                    }
                    break;
            }

            return prop;
        }

        private static String getParentListKey(String parentName)
        {
            return parentName + "Id";
        }
    }


    public SurveyDesignProcessor(Logger logger)
    {
        super(logger);
    }

    public void updateSurveyDesign(@NotNull SurveyResponse surveyResponse, User user) throws Exception
    {
        //get study from response
        MobileAppStudy study = MobileAppStudyManager.get().getStudyFromAppToken(surveyResponse.getAppToken());

        if (study == null)
            throw new Exception("No study associated with app token '" + surveyResponse.getAppToken() + "'");
        logger.info(String.format(LogMessageFormats.START_UPDATE_SURVEY, study.getShortName(), surveyResponse.getActivityId(), surveyResponse.getSurveyVersion()));
        SurveyDesignProvider provider = MobileAppStudyManager.get().getSurveyDesignProvider(study.getContainer());
        if (provider == null)
            throw new InvalidDesignException(LogMessageFormats.PROVIDER_NULL);

        SurveyDesign design = provider.getSurveyDesign(study.getContainer(), study.getShortName(), surveyResponse.getActivityId(), surveyResponse.getSurveyVersion());
        if (design == null)
            throw new InvalidDesignException(LogMessageFormats.DESIGN_NULL);
        else if (!design.isValid())
            throw new InvalidDesignException(LogMessageFormats.MISSING_METADATA);

        // if a user isn't provided, need to create a LimitedUser to use for checking permissions, wrapping the Guest user
        User insertUser = new LimitedUser((user == null)? UserManager.getGuestUser() : user,
                new int[0], Collections.singleton(RoleManager.getRole(SubmitterRole.class)), false);

        ListDefinition listDef = ensureList(study.getContainer(), insertUser, design.getSurveyName(), null);
        applySurveyUpdate(study.getContainer(), insertUser, listDef.getDomain(), design.getSteps(), design.getSurveyName(), "");
    }

    private void applySurveyUpdate(Container container, User user, Domain listDomain, List<SurveyStep> steps, String listName, String parentListName) throws InvalidDesignException
    {
        StandardProperties.ensureStandardProperties(container, listDomain, parentListName);

        try
        {
            //Check for duplicate field keys (sub-lists ok to overlap)
            Set<String> fieldKeys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (SurveyStep step: steps)
            {
                if (step.getType().equalsIgnoreCase("instruction"))
                    continue;

                if (fieldKeys.contains(step.getKey()))
                    throw new InvalidDesignException(String.format(LogMessageFormats.DUPLICATE_FIELD_KEY, step.getKey()));

                StepResultType resultType = StepResultType.getStepResultType(step.getResultType());
                //need to check for choice/group
                switch(resultType)
                {
                    case TextChoice:
                        updateChoiceList(container, user, listName, step);
                        break;
                    case GroupedResult:
                        updateGroupList(container, user, listName, step);
                        break;
                    case FetalKickCounter:
                    case TowerOfHanoi:
                    case SpatialSpanMemory:
                        step.setSteps(resultType.getDataValues());
                        updateGroupList(container, user, listName, step);
                        break;
                    case UNKNOWN:
                        throw new InvalidDesignException(String.format(LogMessageFormats.INVALID_RESULT_TYPE, step.getKey()));
                    default:
                        ensureStepProperty(listDomain, step);
                        break;
                }

                fieldKeys.add(step.getKey());
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


    private void updateGroupList(Container container, User user, String parentListName, SurveyStep step) throws InvalidDesignException
    {
        if (step == null)
            throw new InvalidDesignException(LogMessageFormats.STEP_IS_NULL);
        if (step.getSteps() == null)
            throw new InvalidDesignException(String.format(LogMessageFormats.NO_GROUP_STEPS, step.getKey()));

        String subListName = parentListName + step.getKey();
        ListDefinition listDef = ensureList(container, user, subListName, parentListName);
        applySurveyUpdate(container, user, listDef.getDomain(), step.getSteps(), subListName, parentListName);
    }


    private void updateChoiceList(Container container, User user, String parentSurveyName, SurveyStep step) throws InvalidDesignException, ChangePropertyDescriptorException
    {
        String listName = parentSurveyName + step.getKey();

        //Get existing list or create new one
        ListDefinition listDef = ensureList(container, user, listName, parentSurveyName);
        Domain domain = listDef.getDomain();

        //Check for key, participantId, and parent survey fields
        StandardProperties.ensureStandardProperties(container, domain, parentSurveyName);

        //Add value property
        ensureStepProperty(domain, step);

        if(step.hasOtherOption())
            ensureOtherOption(domain, step);

        try
        {
            domain.save(user);
        }
        catch (ChangePropertyDescriptorException e)
        {
            throw new InvalidDesignException(String.format(LogMessageFormats.SUBLIST_PROPERTY_ERROR, step.getKey()), e);
        }
    }

    private static final String OTHER_OPTION_BASE_DESCRIPTION = "Optional text provided by respondent";
    private static final int OTHER_OPTION_MAX_LENGTH = 4000;

    private void ensureOtherOption(Domain listDomain, SurveyStep step) throws InvalidDesignException
    {
        String otherTextKey = MobileAppStudyManager.getOtherOptionKey(step.getKey());
        JdbcType propType = JdbcType.VARCHAR;
        DomainProperty prop = listDomain.getPropertyByName(otherTextKey);
        if (prop == null)
        {
            //New property
            getNewDomainProperty(listDomain, otherTextKey, propType, null, OTHER_OPTION_BASE_DESCRIPTION, OTHER_OPTION_MAX_LENGTH );
        }
        // Else field already exists, no need to generate it...
    }
}
