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

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.action.ApiAction;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.security.RequiresNoPermission;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NavTree;
import org.labkey.mobileappstudy.data.EnrollmentTokenBatch;
import org.labkey.mobileappstudy.data.MobileAppStudy;
import org.labkey.mobileappstudy.data.Participant;
import org.labkey.mobileappstudy.view.EnrollmentTokenBatchesWebPart;
import org.labkey.mobileappstudy.view.EnrollmentTokensWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

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
            _token = token == null ? null : token.trim().toUpperCase();
        }

        public String getShortName()
        {
            return _shortName;
        }

        public void setShortName(String shortName)
        {
            _shortName = shortName == null ? null : shortName.trim().toUpperCase();
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
}