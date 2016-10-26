Ext4.define('LABKEY.MobileAppStudy.StudySetupPanel', {

    extend: 'Ext.form.Panel',

    border: false,

    isEditable : true,

    shortName : null,

    initComponent : function()
    {
        if (this.isEditable)
        {
            this.dockedItems = [{
                xtype: 'toolbar',
                dock: 'bottom',
                ui: 'footer',
                items: [this.getSubmitButton()]
            }];


            this.callParent();
            this.add({
                tag: 'div',
                padding: '10px 10px 0px 10px',
                itemId: 'messageEl',
                html: 'Enter the study short name to be associated with this folder.  The short name should be the same as it appears in the study design interface.',
                border: false
            });

            this.add(this.getFormFields());
        }
        else
        {
            this.callParent();
            this.add({
                tag: 'div',
                padding: '10px 10px 0px 10px',
                itemId: 'messageEl',
                html: 'The study short name associated with this folder is ' + this.shortName + '.',
                border: false
            });
        }
    },

    getSubmitButton: function()
    {
        if (!this.submitButton)
        {
            this.submitButton = Ext4.create('Ext.button.Button', {
                text: 'Submit',
                itemId: 'submitBtn',
                disabled: true,
                handler: function (btn)
                {
                    btn.up('form').doSubmit(btn);
                }
            })
        }
        return this.submitButton;
    },

    getStudyIdField : function()
    {
        if (!this.studyIdField)
        {
            this.studyIdField = Ext4.create("Ext.form.field.Text",
                    {
                        width: 200,
                        name: 'shortName',
                        value: this.shortName,
                        padding: '10px 10px 0px 10px',
                        allowBlank: false,
                        emptyText: "Enter Study Short Name",
                        submitEmptyText: false,
                        disabled: !this.isEditable,
                        validateOnChange: true,
                        allowOnlyWhitespace: false,
                        listeners: {
                            change: function(field, newValue, oldValue){
                                var submitBtn = field.up('form').getSubmitButton();
                                submitBtn.setDisabled(!field.isValid() || newValue == this.up('form').shortName);
                            }
                        }
                    }
            );
        }
        return this.studyIdField;
    },

    getFormFields : function()
    {
        return [this.getStudyIdField()];
    },

    doSubmit: function(btn){
        btn.setDisabled(true);

        function onSuccess(response, options) {
            var obj = Ext4.decode(response.responseText);
            if (obj.success)
                this.shortName = obj.data.shortName;
            else
            {
                Ext4.Msg.alert("Error", "There was a problem storing the study short name.  Please check the logs or contact an administrator.");
            }
        }

        function onError(response, options){
            btn.setDisabled(false);

            var obj = Ext4.decode(response.responseText);
            if (obj.errors)
            {
                Ext4.Msg.alert("Error", "There were problems storing the study short name. " + obj.errors[0].message);
            }
        }

        Ext4.Ajax.request({
            url: LABKEY.ActionURL.buildURL('mobileappstudy', 'studyConfig.api'),
            method: 'POST',
            jsonData: this.getForm().getFieldValues(),
            success: onSuccess,
            failure: onError,
            scope: this
        });

    }

});