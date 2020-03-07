package org.labkey.test.commands.mobileappstudy;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.PostCommand;

// Created as sample code for BTC. Not currently used by the tests, but could be useful when the corresponding action is finalized.
public class CreateFolderCommand extends PostCommand<CommandResponse>
{
    private String _name;
    private String _title;
    private String _description;

    public CreateFolderCommand(String name)
    {
        super("mobileAppStudy", "createFolder");
        _name = name;
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject result = super.getJsonObject();
        if (result == null)
        {
            result = new JSONObject();
        }
        result.put("name", _name);
        result.put("title", _title);
        result.put("description", _description);
        setJsonObject(result);
        return result;
    }

    @Override
    public double getRequiredVersion()
    {
        return 0;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String name)
    {
        _name = name;
    }

    public String getTitle()
    {
        return _title;
    }

    public void setTitle(String title)
    {
        _title = title;
    }

    public String getDescription()
    {
        return _description;
    }

    public void setDescription(String description)
    {
        _description = description;
    }
}
