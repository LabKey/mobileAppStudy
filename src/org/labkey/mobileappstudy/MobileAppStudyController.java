/*
 * Copyright (c) 2016 LabKey Corporation
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.action.Action;
import org.labkey.api.action.ActionType;
import org.labkey.api.action.ApiAction;
import org.labkey.api.action.ApiQueryResponse;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.query.QueryForm;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.HttpView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.mobileappstudy.data.EnrollmentTokenBatch;
import org.labkey.mobileappstudy.data.MobileAppStudy;
import org.labkey.mobileappstudy.data.Participant;
import org.labkey.mobileappstudy.data.SurveyMetadata;
import org.labkey.mobileappstudy.data.SurveyResponse;
import org.labkey.mobileappstudy.query.ReadResponsesQuerySchema;
import org.labkey.mobileappstudy.view.EnrollmentTokenBatchesWebPart;
import org.labkey.mobileappstudy.view.EnrollmentTokensWebPart;
import org.springframework.beans.PropertyValues;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Marshal(Marshaller.Jackson)
public class MobileAppStudyController extends SpringActionController
{
    private static final Logger LOG = Logger.getLogger(MobileAppStudyController.class);
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(MobileAppStudyController.class);

    public static final String NAME = "mobileappstudy";

    public MobileAppStudyController()
    {
        setActionResolver(_actionResolver);
    }

    public ActionURL getEnrollmentTokenBatchURL()
    {
        return new ActionURL(TokenBatchAction.class, getContainer());
    }


    @RequiresPermission(AdminPermission.class)
    public class TokenBatchAction extends SimpleViewAction
    {
        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }

        @Override
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            setTitle("Enrollment Token Batches");
            return new EnrollmentTokenBatchesWebPart(getViewContext());
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class TokenListAction extends SimpleViewAction
    {
        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Token Batches", getEnrollmentTokenBatchURL()).addChild("Enrollment Tokens");
        }

        @Override
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            setTitle("Enrollment Tokens");
            return new EnrollmentTokensWebPart(getViewContext());
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class GenerateTokensAction extends ApiAction<GenerateTokensForm>
    {
        @Override
        public void validateForm(GenerateTokensForm form, Errors errors)
        {
            if (form == null)
                errors.reject(ERROR_MSG, "Invalid input format. Please check the log for errors.");
            else if (form.getCount() == null || form.getCount() <= 0)
                errors.reject(ERROR_MSG, "Count must be provided and greater than 0.");
        }
        @Override
        public Object execute(GenerateTokensForm form, BindException errors) throws Exception
        {
            EnrollmentTokenBatch batch = MobileAppStudyManager.get().createTokenBatch(form.getCount(), getUser(), getContainer());

            return success(PageFlowUtil.map("batchId", batch.getRowId()));
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class StudyConfigAction extends ApiAction<StudyConfigForm>
    {
        @Override
        public void validateForm(StudyConfigForm form, Errors errors)
        {
            MobileAppStudy study = MobileAppStudyManager.get().getStudy(getContainer());
            if (form == null)
                errors.reject(ERROR_MSG, "Invalid input format.  Please check the log for errors.");
            else if (StringUtils.isEmpty(form.getShortName()))
                errors.reject(ERROR_REQUIRED, "StudyId must be provided.");
            else if (MobileAppStudyManager.get().studyExistsAsSibling(form.getShortName(), getContainer()))
                errors.rejectValue("shortName", ERROR_MSG, "StudyId '" + form.getShortName() + "' is already associated with a different container within this folder. Each study can be associated with only one container per folder.");
            //Check if study exists, name has changed, and at least one participant has enrolled
            else if (study != null && !study.getShortName().equals(form.getShortName()) && MobileAppStudyManager.get().hasStudyParticipants(getContainer()))
                errors.rejectValue("shortName", ERROR_MSG, "This container already has a study with participant data associated with it.  Each container can be configured with only one study and cannot be reconfigured once participant data is present.");
        }

        @Override
        public Object execute(StudyConfigForm form, BindException errors) throws Exception
        {
            // if submitting again with the same id in the same container, return the existing study object
            MobileAppStudy study = MobileAppStudyManager.get().getStudy(getContainer());
            if (study == null || !study.getShortName().equals(form.getShortName()) || study.getCollectionEnabled() != form.getCollectionEnabled())
                study = MobileAppStudyManager.get().insertOrUpdateStudy(form.getShortName(), form.getCollectionEnabled(), getContainer(), getUser());

            return success(PageFlowUtil.map(
                "rowId", study.getRowId(),
                "studyId", study.getShortName(),
                "collectionEnabled", study.getCollectionEnabled()
            ));
        }
    }

    /*
    Ignores container POST-ed from. Pulls container context from the appToken used in request
     */
    @RequiresNoPermission
    public class ProcessResponseAction extends ApiAction<ResponseForm>
    {
        @Override
        public void validateForm(ResponseForm form, Errors errors)
        {
            //Check if form is valid
            if (form == null)
            {
                errors.reject(ERROR_MSG, "Please check the log for errors.");
                return;
            }

            form.validate(errors);
        }

        @Override
        public Object execute(ResponseForm form, BindException errors) throws Exception
        {
            //Record response blob
            MobileAppStudyManager manager = MobileAppStudyManager.get();
            //Null checks are done in the validate method
            SurveyResponse resp = new SurveyResponse(
                    form.getParticipantId(),
                    form.getData().toString(),
                    form.getMetadata().getActivityId(),
                    form.getMetadata().getVersion()
            );
            resp = manager.insertResponse(resp);

            //Add a parsing job
            final Integer rowId = resp.getRowId();
            manager.enqueueSurveyResponse(() -> MobileAppStudyManager.get().shredSurveyResponse(rowId, getUser()));

            return success();
        }
    }

    /*
    Ignores container POST-ed from. Pulls container context from the appToken used in request
    */
    @RequiresNoPermission
    public class WithdrawFromStudy extends ApiAction<WithdrawFromStudyForm>
    {
        public void validateForm(WithdrawFromStudyForm form, Errors errors)
        {
            //Check if form is valid
            if (form == null)
            {
                errors.reject(ERROR_MSG, "Please check the log for errors.");
                return;
            }

            if (StringUtils.isBlank(form.getParticipantId()))
                errors.reject(ERROR_REQUIRED, "ParticipantId not included in request.");
            else if(!MobileAppStudyManager.get().participantExists(form.getParticipantId()))
                errors.reject(ERROR_REQUIRED, "Invalid ParticipantId.");
        }

        @Override
        public Object execute(WithdrawFromStudyForm form, BindException errors) throws Exception
        {
            MobileAppStudyManager.get().withdrawFromStudy(form.getParticipantId(), form.isDelete());
            return success();
        }
    }

    @RequiresNoPermission
    private abstract class BaseEnrollmentAction extends ApiAction<EnrollmentForm>
    {
        public void validateForm(EnrollmentForm form, Errors errors)
        {
            if (form == null)
            {
                errors.reject(ERROR_MSG, "Invalid input format.");
            }
            else
            {
                if (StringUtils.isEmpty(form.getShortName()))
                    //StudyId typically refers to the Study.rowId, however in this context it is the Study.shortName.  Issue #28419
                    errors.reject(ERROR_REQUIRED, "StudyId is required");
                else if (!MobileAppStudyManager.get().studyExists(form.getShortName()))
                    errors.rejectValue("studyId", ERROR_MSG, "Study with StudyId '" + form.getShortName() + "' does not exist");
                else if (StringUtils.isNotEmpty(form.getToken()))
                {
                    if (MobileAppStudyManager.get().hasParticipant(form.getShortName(), form.getToken()))
                        errors.reject(ERROR_MSG, "Token already in use");
                    else if (!MobileAppStudyManager.get().isChecksumValid(form.getToken()))
                        errors.rejectValue("token", ERROR_MSG, "Invalid token: '" + form.getToken() + "'");
                    else if (!MobileAppStudyManager.get().isValidStudyToken(form.getToken(), form.getShortName()))
                        errors.rejectValue("token", ERROR_MSG, "Unknown token: '" + form.getToken() + "'");
                }
                // we allow for the possibility that someone can enroll without using an enrollment token
                else if (MobileAppStudyManager.get().enrollmentTokenRequired(form.getShortName()))
                {
                    errors.reject(ERROR_REQUIRED, "Token is required");
                }
            }
        }
    }

    @RequiresNoPermission
    /**
     * Execute the validation steps for an enrollment token without enrolling
     */
    public class ValidateEnrollmentTokenAction extends BaseEnrollmentAction
    {
        @Override
        public void validateForm(EnrollmentForm form, Errors errors)
        {
            super.validateForm(form, errors);
        }

        @Override
        public Object execute(EnrollmentForm enrollmentForm, BindException errors) throws Exception
        {
            //If action passes validation then it was successful
            return success();
        }
    }

    @RequiresNoPermission
    public class EnrollAction extends BaseEnrollmentAction
    {
        @Override
        public void validateForm(EnrollmentForm form, Errors errors)
        {
            super.validateForm(form, errors);

            //If errors were already found return
            if (errors.hasErrors())
                return;

        }


        @Override
        public Object execute(EnrollmentForm enrollmentForm, BindException errors) throws Exception
        {
            Participant participant = MobileAppStudyManager.get().enrollParticipant(enrollmentForm.getShortName(), enrollmentForm.getToken());
            return success(PageFlowUtil.map("appToken", participant.getAppToken()));
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class ReprocessResponseAction extends ApiAction<ReprocessResponseForm>
    {
        @Override
        public void validateForm(ReprocessResponseForm form, Errors errors)
        {
            if (form == null)
            {
                errors.reject(ERROR_MSG, "Invalid input format.");
                return;
            }

            Set<String> listIds = DataRegionSelection.getSelected(getViewContext(), form.getKey(), true, false);
            Set<Integer> ids = listIds.stream().map(Integer::valueOf).collect(Collectors.toSet());
            if (listIds.size() == 0)
                errors.reject(ERROR_REQUIRED, "No responses to reprocess");

        }

        @Override
        public Object execute(ReprocessResponseForm form, BindException errors) throws Exception
        {
            Set<String> listIds = DataRegionSelection.getSelected(getViewContext(), form.getKey(), true, true);
            Set<Integer> ids = listIds.stream().map(Integer::valueOf).collect(Collectors.toSet());
            Set<Integer> nonErrorIds = MobileAppStudyManager.get().getNonErrorResponses(ids);
            int enqueued = MobileAppStudyManager.get().reprocessResponses(getUser(),
                    ids);

            return success(PageFlowUtil.map("countReprocessed", enqueued, "notReprocessed", nonErrorIds));
        }
    }

    private abstract class BaseQueryAction<FORM extends SelectRowsForm> extends ApiAction<FORM>
    {
        @Override
        public final BindException defaultBindParameters(FORM form, PropertyValues params)
        {
            ParticipantForm participantForm = new ParticipantForm();
            BindException exception = defaultBindParameters(participantForm, getCommandName(), getPropertyValues());

            if (!exception.hasErrors())
                exception = super.defaultBindParameters(form, params);

            if (!exception.hasErrors())
                form.setParticipantForm(participantForm);

            return exception;
        }

        @Override
        public final void validateForm(FORM form, Errors errors)
        {
            super.validateForm(form, errors);
            form.getParticipantForm().validateForm(errors);
        }

        @Override
        public final Object execute(FORM form, BindException errors) throws Exception
        {
            Participant participant = form.getParticipant();

            // ApiQueryResponse constructs a DataView that initializes its ViewContext from the root context, so we need
            // to modify the root with a read-everywhere user and the study container.
            ViewContext root = HttpView.getRootContext();

            // Shouldn't be null, but just in case
            if (null != root)
            {
                // Setting a ContextualRole would be cleaner, but HttpView.initViewContext() doesn't copy it over
                root.setUser(User.getSearchUser());
                root.setContainer(participant.getContainer());
            }

            // Set our special, filtered schema on the form so getQuerySettings() works right
            UserSchema schema = ReadResponsesQuerySchema.get(participant);
            form.setSchema(schema);

            return getResponse(form, schema, errors);
        }

        @Override
        protected String getCommandClassMethodName()
        {
            return "getResponse";
        }

        abstract public ApiQueryResponse getResponse(FORM form, UserSchema schema, BindException errors);
    }

    @RequiresNoPermission
    @Action(ActionType.SelectData.class)
    public class SelectRowsAction extends BaseQueryAction<SelectRowsForm>
    {
        @Override
        public ApiQueryResponse getResponse(SelectRowsForm form, UserSchema schema, BindException errors)
        {
            // TODO:
            //ensureQueryExists(form);

            // First parameter (ViewContext) is ignored, so just pass null
            QueryView view = QueryView.create(null, schema, form.getQuerySettings(), errors);

            return new ApiQueryResponse(view, false, true,
                    schema.getName(), form.getQueryName(), form.getQuerySettings().getOffset(), null,
                    false, false, false, false);
        }
    }

    @RequiresNoPermission
    @Action(ActionType.SelectData.class)
    public class ExecuteSqlAction extends BaseQueryAction<ExecuteSqlForm>
    {
        @Override
        public ApiQueryResponse getResponse(ExecuteSqlForm form, UserSchema schema, BindException errors)
        {
            String sql = form.getSql();

            // TODO:
            //ensureSqlExists(form);

            // NYI: execute that SQL... this is just a stub

            return null;
        }
    }

    public static class SelectRowsForm extends QueryForm
    {
        private UserSchema _userSchema = null;
        private ParticipantForm _participantForm = null;

        void setSchema(UserSchema userSchema)
        {
            _userSchema = userSchema;
        }

        @Nullable
        @Override
        public UserSchema getSchema()
        {
            return _userSchema;
        }

        Participant getParticipant()
        {
            return _participantForm.getParticipant();
        }

        ParticipantForm getParticipantForm()
        {
            return _participantForm;
        }

        void setParticipantForm(ParticipantForm participantForm)
        {
            _participantForm = participantForm;
        }
    }

    public static class ExecuteSqlForm extends SelectRowsForm
    {
        private String _sql;

        public String getSql()
        {
            return _sql;
        }

        public void setSql(String sql)
        {
            _sql = sql;
        }
    }

    public static class ReprocessResponseForm
    {
        private String _key;

        public String getKey()
        {
            return _key;
        }
        public void setKey(String key)
        {
            _key = key;
        }
    }


    public static class StudyConfigForm
    {
        private String _shortName;
        private boolean _collectionEnabled;

        public String getShortName()
        {
            return _shortName;
        }

        public void setShortName(String shortName)
        {
            _shortName = shortName;
        }

        public boolean getCollectionEnabled() {
            return _collectionEnabled;
        }
        public void setCollectionEnabled(boolean collectionEnabled) {
            _collectionEnabled = collectionEnabled;
        }

        //StudyId typically refers to the Study.rowId, however in this context it is the Study.shortName.  Issue #28419
        //Adding this since it could potentially be exposed
        public void setStudyId(String studyId)
        {
            setShortName(studyId);
        }

        public String getStudyId()
        {
            return _shortName;
        }
    }

    public static class EnrollmentForm
    {
        private String _token;
        private String _shortName;

        public String getToken()
        {
            return _token;
        }
        public void setToken(String token)
        {
            _token = isBlank(token) ? null : token.trim().toUpperCase();
        }

        public String getShortName()
        {
            return _shortName;
        }
        public void setShortName(String shortName)
        {
            _shortName = isBlank(shortName) ? null : shortName.trim().toUpperCase();
        }

        //StudyId typically refers to the Study.rowId, however in this context it is the Study.shortName.  Issue #28419
        public void setStudyId(String studyId)
        {
            setShortName(studyId);
        }

        public String getStudyId()
        {
            return _shortName;
        }
    }

    public static class WithdrawFromStudyForm
    {
        private String _participantId;
        private boolean _delete;

        public boolean isDelete()
        {
            return _delete;
        }
        public void setDelete(boolean delete)
        {
            _delete = delete;
        }

        public String getParticipantId()
        {
            return _participantId;
        }
        public void setParticipantId(String participantId)
        {
            _participantId = participantId;
        }
    }

    public static class GenerateTokensForm
    {
        private Integer _count;

        public Integer getCount()
        {
            return _count;
        }

        public void setCount(Integer count)
        {
            _count = count;
        }
    }

    public static class ParticipantForm
    {
        private String _appToken;

        // Filled in by successful validate()
        protected Participant _participant;
        protected MobileAppStudy _study;

        //ParticipantId from JSON request is really the apptoken internally

        @JsonIgnore
        public String getAppToken()
        {
            return getParticipantId();
        }

        //ParticipantId from JSON request is really the apptoken internally
        public String getParticipantId()
        {
            return _appToken;
        }

        public void setParticipantId(String appToken)
        {
            _appToken = appToken;
        }

        public final void validate(Errors errors)
        {
            validateForm(errors);

            // If we have participant and study then we shouldn't have errors (and vice versa)
            assert (null != _participant && null != _study) == !errors.hasErrors();

            if (errors.hasErrors())
                LOG.error("Problem processing participant request: " + errors.getAllErrors().toString());
        }

        protected void validateForm(Errors errors)
        {
            String appToken = getAppToken();

            if (StringUtils.isBlank(appToken))
            {
                errors.reject(ERROR_REQUIRED, "ParticipantId not included in request");
            }
            else
            {
                //Check if there is an associated participant for the appToken
                _participant = MobileAppStudyManager.get().getParticipantFromAppToken(appToken);

                if (_participant == null)
                    errors.reject(ERROR_MSG, "Unable to identify participant");
                else if (Participant.ParticipantStatus.Withdrawn == _participant.getStatus())
                    errors.reject(ERROR_MSG, "Participant has withdrawn from study");
                else
                {
                    //Check if there is an associated study for the appToken
                    _study = MobileAppStudyManager.get().getStudyFromParticipant(_participant);
                    if (_study == null)
                        errors.reject(ERROR_MSG, "AppToken not associated with study");
                }
            }
        }

        @JsonIgnore
        public Participant getParticipant()
        {
            return _participant;
        }
    }

    public static class ResponseForm extends ParticipantForm
    {
        private JsonNode _data;
        private SurveyMetadata _metadata;

        public SurveyMetadata getMetadata()
        {
            return _metadata;
        }
        public void setMetadata(@NotNull SurveyMetadata metadata)
        {
            _metadata = metadata;
        }

        public JsonNode getData()
        {
            return _data;
        }
        public void setData(@NotNull JsonNode data)
        {
            _data = data;
        }

        @Override
        protected void validateForm(Errors errors)
        {
            super.validateForm(errors);

            if (!errors.hasErrors())
            {
                //Check if form's required fields are present
                SurveyMetadata info = getMetadata();

                if (info == null)
                {
                    errors.reject(ERROR_REQUIRED, "Metadata not found");
                }
                else
                {
                    if (isBlank(info.getActivityId()))
                        errors.reject(ERROR_REQUIRED, "ActivityId not included in request");
                    if (isBlank(info.getVersion()))
                        errors.reject(ERROR_REQUIRED, "SurveyVersion not included in request");
                    if (getData() == null)
                        errors.reject(ERROR_REQUIRED, "Response not included in request");
                    if (!_study.getCollectionEnabled())
                        errors.reject(ERROR_MSG, String.format("Response collection is not currently enabled for study [ %1s ]", _study.getShortName()));
                }
            }
        }
    }
}