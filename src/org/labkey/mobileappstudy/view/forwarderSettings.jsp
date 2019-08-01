<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@ page import="org.labkey.api.view.template.ClientDependencies" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.labkey.mobileappstudy.MobileAppStudyManager" %>
<%@ page import="org.labkey.mobileappstudy.MobileAppStudyManager.ForwardingType" %>
<%@ page import="org.labkey.mobileappstudy.forwarder.ForwarderProperties" %>
<%@ page import="org.labkey.api.view.ActionURL" %>
<%@ page import="org.labkey.mobileappstudy.MobileAppStudyController.ForwardingSettingsForm" %>
<%@ page import="org.labkey.mobileappstudy.MobileAppStudyController.ForwardingSettingsAction" %>
<%@ page import="org.labkey.api.view.JspView" %>
<%@ page import="org.labkey.api.view.HttpView" %>
<%@ page import="org.labkey.mobileappstudy.MobileAppStudyController" %>
<%@ page extends="org.labkey.api.jsp.JspBase" %>
<%@ taglib prefix="labkey" uri="http://www.labkey.org/taglib" %>

<%!
    @Override
    public void addClientDependencies(ClientDependencies dependencies)
    {
        dependencies.add("internal/jQuery");
        dependencies.add("Ext4");
        dependencies.add("mobileAppStudy/panel/forwardingPanel.js");
    }
%>
<%
    JspView<ForwardingSettingsForm> me = (JspView<ForwardingSettingsForm>) HttpView.currentView();
    ForwardingSettingsForm bean = me.getModelBean();

    String renderId = "labkey-mobileappstudy-forwardingsetup";

    MobileAppStudyManager manager = MobileAppStudyManager.get();

    Map<String, String> forwardingProperties = manager.getForwardingProperties(getContainer(), getUser());
    ForwardingType authType = ForwarderProperties.getForwardingType(getContainer());

    String basicAuthURL = forwardingProperties.get(ForwarderProperties.URL_PROPERTY_NAME);
    String basicAuthUser = forwardingProperties.get(ForwarderProperties.USER_PROPERTY_NAME);
    String basicAuthPassword = StringUtils.isNotBlank(bean.getPassword()) ?
            ForwarderProperties.PASSWORD_PLACEHOLDER :
            "";

    String oauthRequestURL = forwardingProperties.get(ForwarderProperties.TOKEN_REQUEST_URL);
    String oauthTokenFieldPath = forwardingProperties.get(ForwarderProperties.TOKEN_FIELD);
    String oauthTokenHeader= forwardingProperties.get(ForwarderProperties.TOKEN_HEADER);
    String oauthURL = forwardingProperties.get(ForwarderProperties.OAUTH_URL);
%>

<labkey:errors></labkey:errors>

<labkey:form name="mobileAppStudyForwardingSettingsForm" action="<%=h(new ActionURL(ForwardingSettingsAction.class, getContainer()))%>" method="POST">
    <div id="authTypeSelector" >
        <label>
            <input type="radio" name="forwardingType" value="<%=h(ForwardingType.Disabled.name())%>" <%=checked(authType == ForwardingType.Disabled) %> />
            Disabled
        </label><br>
        <label>
            <input type="radio" name="forwardingType" value="<%=h(ForwardingType.Basic.name())%>" <%=checked(authType == ForwardingType.Basic) %>/>
            Basic Authorization
        </label><br>
        <label>
            <input type="radio" name="forwardingType" value="<%=h(ForwardingType.OAuth.name())%>" <%=checked(authType == ForwardingType.OAuth)%> />
            OAuth
        </label><br>
    </div>

    <div style="padding: 10px" >
        <div id="basicAuthPanel" class="requests-editor">
            <labkey:input type="text" label="User" name="username" value="<%=h(basicAuthUser)%>" />
            <labkey:input type="password" label="Password" name="password" value="<%=h(basicAuthPassword)%>"/>
            <labkey:input type="text" label="Endpoint URL" name="basicURL" value="<%=h(basicAuthURL)%>" />
        </div>
        <div id="oauthPanel" >
            <labkey:input type="text" label="Token Request URL" name="tokenRequestURL" value="<%=h(oauthRequestURL)%>" />
            <labkey:input type="text" label="Token Field" name="tokenField" value="<%=h(oauthTokenFieldPath)%>"/>
            <labkey:input type="text" label="Header Name" name="header" value="<%=h(oauthTokenHeader)%>" />
            <labkey:input type="text" label="Endpoint URL" name="oauthURL" value="<%=h(oauthURL)%>" />
        </div>
    </div>
    <div id="buttonBar">
        <labkey:button submit="true" text="submit" />
        <button type="reset" text="Cancel" value="Cancel">Cancel</button>
    </div>
</labkey:form>

<script type="text/javascript">

    +function ($) {

        function showAuthPanel() {
            const selected = $("input[name='forwardingType']:checked").val();
            const basicPanel = $('#basicAuthPanel');
            const oauthPanel = $('#oauthPanel');


            switch (selected)
            {
                case 'Basic':
                    basicPanel.show();
                    oauthPanel.hide();
                    break;
                case 'OAuth':
                    basicPanel.hide();
                    oauthPanel.show();
                    break;
                case 'Disabled':
                default:
                    basicPanel.hide();
                    oauthPanel.hide();
                    break;
            }
        }

        $('#authTypeSelector').change(showAuthPanel);

        showAuthPanel();
    } (jQuery);
</script>
