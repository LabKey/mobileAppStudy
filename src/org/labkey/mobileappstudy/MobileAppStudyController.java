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
import org.jetbrains.annotations.NotNull;
import org.labkey.api.action.ApiAction;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.data.DataRegionSelection;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewForm;
import org.labkey.mobileappstudy.data.EnrollmentTokenBatch;
import org.labkey.mobileappstudy.data.MobileAppStudy;
import org.labkey.mobileappstudy.data.Participant;
import org.labkey.mobileappstudy.data.SurveyInfo;
import org.labkey.mobileappstudy.data.SurveyResponse;
import org.labkey.mobileappstudy.view.EnrollmentTokenBatchesWebPart;
import org.labkey.mobileappstudy.view.EnrollmentTokensWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Marshal(Marshaller.Jackson)
public class MobileAppStudyController extends SpringActionController
{
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
            else if (MobileAppStudyManager.get().studyExistsElsewhere(form.getShortName(), getContainer()))
                errors.rejectValue("shortName", ERROR_MSG, "StudyId '" + form.getShortName() + "' is already associated with a different container. Each study can be associated with only one container.");
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
    Ignores container POST-ed to. Pulls container context from the appToken used in request
     */
    @RequiresNoPermission
    public class ProcessResponseAction extends ApiAction<MobileAppSurveyResponseForm>
    {
        @Override
        public void validateForm(MobileAppSurveyResponseForm form, Errors errors)
        {
            //Check if form is valid
            if (form == null)
            {
                errors.reject(ERROR_MSG, "Please check the log for errors.");
                return;
            }

            //Check if form's required fields are present
            SurveyInfo info = form.getSurveyInfo();
            if (info == null)
                errors.reject(ERROR_REQUIRED, "SurveyInfo not found.");
            else
            {
                if (isBlank(info.getSurveyId()))
                    errors.reject(ERROR_REQUIRED, "SurveyId not included in request");
                if (isBlank(info.getVersion()))
                    errors.reject(ERROR_REQUIRED, "SurveyVersion not included in request.");
            }
            if (form.getResponse() == null)
                errors.reject(ERROR_REQUIRED, "Response not included in request.");
            if (StringUtils.isBlank(form.getParticipantId()))
                errors.reject(ERROR_REQUIRED, "ParticipantId not included in request.");
            if (errors.hasErrors())
                return;


            //Check if there is an associated participant for the appToken
            if (!MobileAppStudyManager.get().participantExists(form.getAppToken()))
                errors.reject(ERROR_MSG, "Unable to identify participant.");

            //Check if there is an associated study for the appToken
            MobileAppStudy study = MobileAppStudyManager.get().getStudyFromAppToken(form.getAppToken());
            if(study == null)
                errors.reject(ERROR_MSG, "AppToken not associated with study");
            else
            {
                if (!MobileAppStudyManager.get().surveyExists(info.getSurveyId(), study.getContainer(), getUser()))
                    errors.reject(ERROR_MSG, "Survey not found.");
                else if (!study.getCollectionEnabled())
                    errors.reject(ERROR_MSG, String.format("Response collection is not currently enabled for study [ %1s ].", study.getShortName()));
            }
        }

        @Override
        public Object execute(MobileAppSurveyResponseForm form, BindException errors) throws Exception
        {
            //Record response blob
            MobileAppStudyManager manager = MobileAppStudyManager.get();
            //Null checks are done in the validate method
            SurveyResponse resp = new SurveyResponse(
                    form.getParticipantId(),
                    form.getResponse().toString(),
                    form.getSurveyInfo().getSurveyId(),
                    form.getSurveyInfo().getVersion()
            );
            resp = manager.insertResponse(resp);

            //Add a parsing job
            final Integer rowId = resp.getRowId();
            manager.enqueueSurveyResponse(() -> MobileAppStudyManager.get().shredSurveyResponse(rowId));

            return success();
        }
    }

    @RequiresNoPermission
    public class EnrollAction extends ApiAction<EnrollmentForm>
    {
        public void validateForm(EnrollmentForm form, Errors errors)
        {
            if (form == null)
                errors.reject(ERROR_MSG, "Invalid input format.");
            else if (StringUtils.isEmpty(form.getShortName()))
                //StudyId typically refers to the Study.rowId, however in this context it is the Study.shortName.  Issue #28419
                errors.reject(ERROR_REQUIRED, "StudyId is required for enrollment");
            else if (!MobileAppStudyManager.get().studyExists(form.getShortName()))
                errors.rejectValue("studyId", ERROR_MSG, "Study with StudyId '" + form.getShortName() + "' does not exist");
            else if (!StringUtils.isEmpty(form.getToken()))
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
                errors.reject(ERROR_REQUIRED, "Token is required for enrollment");
            }
        }

        @Override
        public Object execute(EnrollmentForm enrollmentForm, BindException errors) throws Exception
        {
            Participant participant = MobileAppStudyManager.get().enrollParticipant(enrollmentForm.getShortName(), enrollmentForm.getToken());
            return success(PageFlowUtil.map("appToken", participant.getAppToken()));
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class ReprocessResponseAction extends ApiAction<ViewForm>
    {
        @Override
        public void validateForm(ViewForm form, Errors errors)
        {
            if (form == null)
                errors.reject(ERROR_MSG, "Invalid input format.");

            Set<Integer> listIds = DataRegionSelection.getSelectedIntegers(form.getViewContext(), false);
            if (listIds.size() == 0)
                errors.reject(ERROR_REQUIRED, "No responses to reprocess");
            String nonErrorIds = MobileAppStudyManager.get().getNonErrorResponses(listIds);
            if (StringUtils.isNotBlank(nonErrorIds))
                errors.reject(ERROR_MSG, "Cannot re-process Response Id(s) [" + nonErrorIds + "]");
        }

        @Override
        public Object execute(ViewForm form, BindException errors) throws Exception
        {
            Set<Integer> listIds = DataRegionSelection.getSelectedIntegers(form.getViewContext(), true);
            int reprocessing = MobileAppStudyManager.get().reprocessResponses(getUser(), listIds);

            //TODO need to redirect ?somewhere?
            return success("countReprocessed", reprocessing);
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

    public static class MobileAppSurveyResponseForm
    {
        private String _type;
        private String _participantId;

        private JsonNode _response;
        private SurveyInfo _surveyInfo;

        public SurveyInfo getSurveyInfo()
        {
            return _surveyInfo;
        }
        public void setSurveyInfo(@NotNull SurveyInfo surveyInfo)
        {
            _surveyInfo = surveyInfo;
        }

        public JsonNode getResponse()
        {
            return _response;
        }
        public void setResponse (@NotNull JsonNode response)
        {
            _response = response;
        }

        //ParticipantId from JSON request is really the apptoken internally
        @JsonIgnore
        public String getAppToken()
        {
            return getParticipantId();
        }

        //ParticipantId from JSON request is really the apptoken internally
        public String getParticipantId()
        {
            return _participantId;
        }
        public void setParticipantId(String participantId)
        {
            _participantId = participantId;
        }

        public String getType()
        {
            return _type;
        }
        public void setType(String type)
        {
            _type = type;
        }
    }
}