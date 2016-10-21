/*
 * Copyright (c) 2015 LabKey Corporation
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

package org.labkey.mobileappsurvey;

import org.labkey.api.action.ApiAction;
import org.labkey.api.action.Marshal;
import org.labkey.api.action.Marshaller;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.JspView;
import org.labkey.api.view.NavTree;
import org.labkey.mobileappsurvey.view.EnrollmentTokenBatchesWebPart;
import org.labkey.mobileappsurvey.view.EnrollmentTokensWebPart;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

@Marshal(Marshaller.Jackson)
public class MobileAppSurveyController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(MobileAppSurveyController.class);
    public static final String NAME = "mobileappsurvey";

    public MobileAppSurveyController()
    {
        setActionResolver(_actionResolver);
    }

    public ActionURL getEnrollmentTokenBatchURL()
    {
        return new ActionURL(TokenBatchAction.class, getContainer());
    }

    @RequiresPermission(ReadPermission.class)
    public class BeginAction extends SimpleViewAction
    {
        public ModelAndView getView(Object o, BindException errors) throws Exception
        {
            return new JspView("/org/labkey/mobileappsurvey/view/hello.jsp");
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return root;
        }
    }

    @RequiresPermission(AdminPermission.class)
    public class TokenBatchAction extends SimpleViewAction
    {
        @Override
        public NavTree appendNavTrail(NavTree root)
        {
            return root.addChild("Enrollment Token Batches");
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
            Integer batchId = MobileAppSurveyManager.get().insertNewTokenBatch(form.getCount(), getUser(), getContainer());

            return success(batchId);
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