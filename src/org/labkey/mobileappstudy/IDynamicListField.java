package org.labkey.mobileappstudy;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.JdbcType;

public interface IDynamicListField
{
    String getKey();

    JdbcType getPropertyStorageType();

    @Nullable Integer getMaxLength();

    String getDescription();

    String getLabel();
}
