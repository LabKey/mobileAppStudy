package org.labkey.mobileappstudy.participantproperties;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.data.PropertyStorageSpec;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.exp.list.ListDefinition;
import org.labkey.api.exp.list.ListService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.LimitedUser;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.security.roles.SubmitterRole;
import org.labkey.mobileappstudy.DynamicListProcessor;
import org.labkey.mobileappstudy.MobileAppStudyManager;
import org.labkey.mobileappstudy.MobileAppStudySchema;
import org.labkey.mobileappstudy.data.MobileAppStudy;
import org.labkey.mobileappstudy.data.ParticipantPropertyMetadata;
import org.labkey.mobileappstudy.surveydesign.InvalidDesignException;
import org.labkey.mobileappstudy.surveydesign.SurveyDesignProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ParticipantPropertiesProcessor extends DynamicListProcessor
{
    private static final String PARTICIPANT_PROPERTIES_PROPERTY_CATEGORY = "ParticipantProperties";
    private static final String PARTICIPANT_PROPERTIES_VERSION_KEY = "Version";

    private enum BaseProperties
    {
        EnrollmentToken("EnrollmentToken", JdbcType.VARCHAR);

        private String key;
        private JdbcType type;

        BaseProperties(String key, JdbcType type)
        {
            this.key = key;
            this.type = type;
        }

        public static void ensureStandardProperties(Container container, Domain domain) throws InvalidDesignException
        {
            if (domain == null)
                throw new InvalidDesignException("Invalid list domain");

            for (BaseProperties val : values())
            {
                //ParentId uses parentListName as the field name
                DomainProperty prop = domain.getPropertyByName(val.key);

                if (prop == null)
                {
                    prop = addStandardProperty(container, val, domain);
                    if (prop != null)
                        prop.setPropertyURI(domain.getTypeURI() + "#" + val.key);
                }
            }
        }

        /**
         * Add properties that are common to the list implementation and any special aspects of that property like Lookups
         * @param container hosting the list
         * @param propName name of the property
         * @param listDomain domain property will belong to
         */
        private static DomainProperty addStandardProperty(@NotNull Container container, @NotNull BaseProperties propName, @NotNull Domain listDomain)
        {
            DomainProperty prop = null;
            switch (propName)
            {
                case EnrollmentToken:
                    //ParticipantProperties list uses enrollment token
                    if (MobileAppStudyManager.PARTICIPANT_PROPERTIES_LIST_NAME.compareToIgnoreCase(listDomain.getName()) == 0)
                    {
                        prop = listDomain.addProperty(new PropertyStorageSpec(EnrollmentToken.key, propName.type));
                    }
                    break;
            }

            return prop;
        }
    }


    public ParticipantPropertiesProcessor(Logger logger)
    {
        super(logger);
    }


    public void updateParticipantPropertiesDesign(@NotNull MobileAppStudy study, @Nullable User user) throws Exception
    {

        SurveyDesignProvider provider = MobileAppStudyManager.get().getSurveyDesignProvider(study.getContainer());
        if (provider == null)
            throw new InvalidDesignException(LogMessageFormats.PROVIDER_NULL);

        ParticipantPropertiesDesign design = provider.getParticipantPropertiesDesign(study.getContainer(), study.getShortName());
        if (design == null)
        {
            logger.debug(String.format(LogMessageFormats.PARTICIPANT_PROPERTIES_DESIGN_NOT_FOUND, study.getShortName()));
            return;
        }

        if (!design.isValid())
            throw new InvalidDesignException(LogMessageFormats.PARTICIPANT_PROPERTIES_MISSING_METADATA);
        else if (!design.getMetadata().getStudyId().equalsIgnoreCase(study.getShortName()))
            throw new InvalidDesignException(String.format(LogMessageFormats.DESIGN_STUDYID_MISMATCH, design.getMetadata().getStudyId(), study.getShortName()));

        // if a user isn't provided, need to create a LimitedUser to use for checking permissions, wrapping the Guest user
        User insertUser = new LimitedUser((user == null)? UserManager.getGuestUser() : user,
                new int[0], Collections.singleton(RoleManager.getRole(SubmitterRole.class)), false);

        String currentVersion = getParticipantPropertiesDesignVersion(user, study.getContainer());
        logger.debug(String.format(LogMessageFormats.START_UPDATE_PARTICIPANT_PROPERTIES, study.getShortName(), currentVersion, design.getStudyVersion()));

        if (StringUtils.isBlank(currentVersion) || compareVersionString(currentVersion, design.getStudyVersion()) < 0)
        {
            logger.info(String.format(LogMessageFormats.UPDATE_PARTICIPANT_PROPERTIES, study.getShortName(), currentVersion, design.getStudyVersion()));
            ListDefinition listDef = ensureList(study.getContainer(), insertUser, MobileAppStudyManager.PARTICIPANT_PROPERTIES_LIST_NAME, null);
            applyParticipantPropertiesUpdate(study.getContainer(), insertUser, listDef, design.getParticipantProperties());
            updateParticipantPropertiesVersion(study.getContainer(), insertUser, design.getStudyVersion());
        }
        logger.debug(String.format(LogMessageFormats.FINISH_UPDATE_PARTICIPANT_PROPERTIES, study.getShortName(), currentVersion, design.getStudyVersion()));
    }

    /**
     * Compare each section of a version string
     *
     * Adapted from: https://stackoverflow.com/a/11024200
     * @param a left side of comparision
     * @param b right side of comparision
     * @return comparable int value
     */
    private int compareVersionString(@NotNull String a, @NotNull String b)
    {
        String[] aParts = a.split("\\.");
        String[] bParts = b.split("\\.");
        int length = Math.max(aParts.length, bParts.length);
        for(int i = 0; i < length; i++) {
            int thisPart = i < aParts.length ?
                    Integer.parseInt(aParts[i]) : 0;
            int thatPart = i < bParts.length ?
                    Integer.parseInt(bParts[i]) : 0;
            if(thisPart < thatPart)
                return -1;
            if(thisPart > thatPart)
                return 1;
        }
        return 0;
    }

    private static String getParticipantPropertiesDesignVersion(User user, Container container)
    {
        return PropertyManager.getProperty(user, container, PARTICIPANT_PROPERTIES_PROPERTY_CATEGORY, PARTICIPANT_PROPERTIES_VERSION_KEY);
    }

    private void applyParticipantPropertiesUpdate(Container container, User user, ListDefinition list, Collection<ParticipantProperty> properties) throws InvalidDesignException
    {
        try
        {
            Domain listDomain = list.getDomain();
            BaseProperties.ensureStandardProperties(container, listDomain);

            Map<String, ParticipantPropertyMetadata> propertyMetadatas = getParticipantPropertyMetadatas(container, list.getListId());

            for (ParticipantProperty property : properties)
            {
                DomainProperty listProp = ensureStepProperty(listDomain, property);
                ParticipantPropertyMetadata metadata = propertyMetadatas.get(listProp.getPropertyURI());

                if (metadata == null)
                    insertPropertyMetadata(property, list.getListId(), listProp);
                else if (metadata.getPropertyType() != property.getPropertyType())
                    updatePropertyMetadata(metadata.getRowId(), property.getPropertyType(), list.getListId());
            }

            listDomain.save(user);
            logger.info(LogMessageFormats.PARTICIPANT_PROPERTIES_END_UPDATE);
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

    private Map<String, ParticipantPropertyMetadata> getParticipantPropertyMetadatas(Container container, Integer listId)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        TableInfo ti = schema.getTableInfoParticipantPropertyMetadata();
        SimpleFilter filter = SimpleFilter.createContainerFilter(container);
        filter.addCondition(FieldKey.fromParts("listId"), listId);

        return new TableSelector(ti, filter,null).stream(ParticipantPropertyMetadata.class)
                .collect(Collectors.toMap(ParticipantPropertyMetadata::getPropertyURI, ppm -> ppm));
    }

    private void insertPropertyMetadata(ParticipantProperty property, Integer listId, DomainProperty listProp)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        TableInfo ti = schema.getTableInfoParticipantPropertyMetadata();

        ParticipantPropertyMetadata metadata = new ParticipantPropertyMetadata();
        metadata.setListId(listId);
        metadata.setPropertyURI(listProp.getPropertyURI());
        metadata.setPropertyType(property.getPropertyType());
        metadata.setContainer(listProp.getContainer());
        Table.insert(null, ti, metadata);
    }

    private void updatePropertyMetadata(Integer rowId, ParticipantProperty.ParticipantPropertyType propertyType, Integer listId)
    {
        MobileAppStudySchema schema = MobileAppStudySchema.getInstance();
        TableInfo ti = schema.getTableInfoParticipantPropertyMetadata();

        Map<String,Object> values = new HashMap<>();
        values.put(FieldKey.fromParts("listId").toString(), listId);
        values.put(FieldKey.fromParts("propertyType").toString(), propertyType);

        Table.update(null, ti, values, rowId);
    }

    private void updateParticipantPropertiesVersion(Container container, User user, String newVersion)
    {
        PropertyManager.PropertyMap versionProperties = PropertyManager.getWritableProperties(user, container, PARTICIPANT_PROPERTIES_PROPERTY_CATEGORY, true);
        versionProperties.put(PARTICIPANT_PROPERTIES_VERSION_KEY, newVersion);
        versionProperties.save();
    }

    @Override
    protected ListDefinition ensureList(Container container, User user, String listName, String parentListName) throws InvalidDesignException
    {
        ListDefinition listDef = ListService.get().getList(container, listName);
        listDef = listDef != null ?
                listDef :
                newListDefinition(container, user, listName, parentListName, BaseProperties.EnrollmentToken.key, ListDefinition.KeyType.Varchar);

        try
        {
            MobileAppStudyManager.get().insertParticipantPropertiesEnrollmentTokens(container, user, listDef);
        }
        catch (Exception e)
        {
            throw new InvalidDesignException(String.format("Failed to insert EnrollmentTokens into ParticipantProperties list for container [%1$s].", container.getName()), e);
        }

        return listDef;
    }
}
