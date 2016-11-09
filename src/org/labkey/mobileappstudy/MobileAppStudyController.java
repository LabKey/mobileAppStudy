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
import org.labkey.api.action.*;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.mobileappstudy.data.*;
import org.labkey.mobileappstudy.view.EnrollmentTokenBatchesWebPart;
import org.labkey.mobileappstudy.view.EnrollmentTokensWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

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
            if (form == null)
                errors.reject(ERROR_MSG, "Invalid input format.  Please check the log for errors.");
            else if (StringUtils.isEmpty(form.getShortName()))
                errors.reject(ERROR_REQUIRED, "Study short name must be provided.");
            else if (MobileAppStudyManager.get().studyExistsElsewhere(form.getShortName(), getContainer()))
                errors.rejectValue("shortName", ERROR_MSG, "Study short name '" + form.getShortName() + "' is already associated with a different container. Each study can be associated with only one container.");
            else if (MobileAppStudyManager.get().hasStudyParticipants(getContainer()))
                errors.rejectValue("shortName", ERROR_MSG, "This container already has a study with participant data associated with it.  Each container can be configured with only one study and cannot be reconfigured once participant data is present.");
        }

        @Override
        public Object execute(StudyConfigForm form, BindException errors) throws Exception
        {
            // if submitting again with the same id in the same container, return the existing study object
            MobileAppStudy study = MobileAppStudyManager.get().getStudy(getContainer());
            if (study == null || !study.getShortName().equals(form.getShortName()))
                study = MobileAppStudyManager.get().insertOrUpdateStudy(form.getShortName(), getContainer(), getUser());

            return success(PageFlowUtil.map("rowId", study.getRowId(), "shortName", study.getShortName()));
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
            //TODO: improve error messages
            if (form == null)
                errors.reject(ERROR_MSG, "Invalid input format.  Please check the log for errors.");
            else if (StringUtils.isBlank(form.getParticipantId()))
                errors.reject(ERROR_REQUIRED, "ParticipantId was not included in request");
            else if (!MobileAppStudyManager.get().participantExists(form.getParticipantId()))
                errors.reject(ERROR_REQUIRED, "Unable to identify participant");
            else if (form.getResponse() == null)
                errors.reject(ERROR_REQUIRED, "Responses not found");

            if (errors.hasErrors())
                return;

            MobileAppStudy study = MobileAppStudyManager.get().getStudyFromApptoken(form.getAppToken());

            SurveyInfo info = form.getSurveyInfo();
            if (info == null)
                errors.reject(ERROR_REQUIRED, "SurveyInfo not found");
            else if (isBlank(info.getSurveyId()))
                errors.reject(ERROR_REQUIRED, "Invalid SurveyId. SurveyId not included in request");
            else if (!MobileAppStudyManager.get().surveyExists(info.getSurveyId(), study.getContainer(), getUser()))
                errors.reject(ERROR_REQUIRED, "Invalid SurveyId. Survey not found");
            else if (isBlank(info.getVersion()))
                errors.reject(ERROR_REQUIRED, "Invalid Survey version. Survey version not included in request");
            else if (!MobileAppStudyManager.get().collectionActive(info))
                errors.reject(ERROR_MSG, "Response collection is not currently enabled for this survey [Study: " + info.getStudyId() + ", Survey: " + info.getSurveyId() + "].");

            //TODO: Check version against DB?
        }

        @Override
        public Object execute(MobileAppSurveyResponseForm form, BindException errors) throws Exception
        {
            //Record response blob
            MobileAppStudyManager manager = MobileAppStudyManager.get();
            SurveyResponse resp = form.getResponseRow();
            resp = manager.insertResponse(resp);

            //Add a parsing job
            final Integer rowId = resp.getRowId();
            manager.enqueueSurveyResponse(() -> MobileAppStudyManager.get().shredSurveyResponses(rowId));

            //TODO: Determine appropriate properties to return
            return success();
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class StudyCollectionAction extends ApiAction<StudyCollectionForm>
    {
        @Override
        public void validateForm(StudyCollectionForm form, Errors errors)
        {
            if (form == null)
                errors.reject(ERROR_MSG, "Invalid input format.  Please check the log for errors.");
            else if (StringUtils.isEmpty(form.getShortName()))
                errors.reject(ERROR_REQUIRED, "Study short name must be provided.");
        }

        @Override
        public Object execute(StudyCollectionForm form, BindException errors) throws Exception
        {
            MobileAppStudyManager manager = MobileAppStudyManager.get();
            MobileAppStudy study = manager.getStudy(getContainer());
            if (study.getCollectionEnabled() != form.getCollectionEnabled())
                study = manager.updateResponseCollection(study, form.getCollectionEnabled(), getUser());

            return success(PageFlowUtil.map("rowId", study.getRowId(), "shortName", study.getShortName(), "collectionEnabled", study.getCollectionEnabled()));
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
                errors.reject(ERROR_REQUIRED, "Study short name is required for enrollment");
            else if (!MobileAppStudyManager.get().studyExists(form.getShortName()))
                errors.rejectValue("shortName", ERROR_MSG, "Study with short name '" + form.getShortName() + "' does not exist");
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

    public static class StudyConfigForm
    {
        private String _shortName;

        public String getShortName()
        {
            return _shortName;
        }

        public void setShortName(String shortName)
        {
            _shortName = shortName;
        }
    }

    public static class StudyCollectionForm
    {
        private String _shortName;
        private Boolean _collectionEnabled;

        public Boolean getCollectionEnabled() {
            return _collectionEnabled;
        }

        public void setCollectionEnabled(Boolean collectionEnabled) {
            _collectionEnabled = collectionEnabled;
        }

        public String getShortName()
        {
            return _shortName;
        }

        public void setShortName(String shortName)
        {
            _shortName = shortName;
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
        public void setSurveyInfo(SurveyInfo surveyInfo)
        {
            _surveyInfo = surveyInfo;
        }

        public JsonNode getResponse()
        {
            return _response;
        }

        public void setResponse (JsonNode response)
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

        public SurveyResponse getResponseRow()
        {
            SurveyResponse resp = new SurveyResponse();
            resp.setStatus(SurveyResponse.ResponseStatus.PENDING);
            resp.setAppToken(getParticipantId());

            if(getResponse() != null)
                resp.setResponse(getResponse().toString());

            if(getSurveyInfo() != null)
            {
                resp.setSurveyVersion(getSurveyInfo().getVersion());
                resp.setSurveyId(getSurveyInfo().getSurveyId());
            }

            return resp;
        }
    }
}