package org.labkey.mobileappstudy.security;

import org.labkey.api.security.permissions.AbstractPermission;

public class GenerateEnrollmentTokensPermission extends AbstractPermission
{
    public GenerateEnrollmentTokensPermission()
    {
        super("Generate Enrollment Tokens Permission", "Allows generation of enrollment tokens");
    }
}
