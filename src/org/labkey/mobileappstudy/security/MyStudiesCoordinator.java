package org.labkey.mobileappstudy.security;

import org.labkey.api.security.roles.AbstractModuleScopedRole;
import org.labkey.mobileappstudy.MobileAppStudyModule;

public class MyStudiesCoordinator extends AbstractModuleScopedRole
{
    public MyStudiesCoordinator()
    {
        super("MyStudies Coordinator", "MyStudies Coordinators may generate enrollment tokens.", MobileAppStudyModule.class, GenerateEnrollmentTokensPermission.class);
        excludeGuests();
    }
}
