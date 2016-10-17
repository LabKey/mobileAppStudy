Ext4.define('LABKEY.MobileAppSurvey.StudySetupPanel', {

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
                items: [
                    {
                        text: 'Submit',
                        itemId: 'submitBtn',
                        formBind: true,
                        handler: function (btn)
                        {
                            btn.up('form').doSubmit(btn);
                        }
                    }
                ]
            }];


            this.callParent();
            this.add({
                tag: 'div',
                padding: '10px 10px 0px 10px',
                itemId: 'messageEl',
                html: 'Enter the study ID to be associated with this folder.  The ID should be the same as it appears in the study design interface.',
                border: false
            });

            this.add(this.getFormFields());

            this.on(
                    {
                        afterRender: function ()
                        {
                            this.getSubmitButton().setDisabled(true);
                        }
                    }
            )
        }
        else
        {
            this.callParent();
            this.add({
                tag: 'div',
                padding: '10px 10px 0px 10px',
                itemId: 'messageEl',
                html: 'The study ID associated with this folder is ' + this.shortName + ".",
                border: false
            });
        }


    },

    getFormFields : function()
    {
        var items = [];
        items.push(
                {
                    xtype: 'textfield',
                    width: 200,
                    name: 'shortName',
                    value: this.shortName,
                    padding: '10px 10px 0px 10px',
                    allowBlank: false,
                    emptyText: "Enter Study Id",
                    submitEmptyText: false,
                    disabled: !this.isEditable,
                    validateOnChange: false,
                    allowOnlyWhitespace: false,
                    listeners: {
                        change: function(field, newValue, oldValue){
                            var submitBtn = field.up('form').getSubmitButton();
                            submitBtn.setDisabled(newValue == undefined || newValue.trim() == "");
                        }
                    }
                }
        );
        return items;
    },

    getSubmitButton: function()
    {
        return this.dockedItems.items[0].items.items[0];
    },

    doSubmit: function(btn){
        btn.setDisabled(true);

        function onSuccess(response, options){
            console.log("Study configuration succeeded.");
        }

        function onError(response, options){
            btn.setDisabled(false);

            var obj = Ext4.decode(response.responseText);
            if (obj.errors)
            {
                Ext4.Msg.alert("Error", "There were problems string the study id. " + obj.errors[0].message);
            }
        }

        Ext4.Ajax.request({
            url: LABKEY.ActionURL.buildURL('mobileappsurvey', 'studyConfig.api'),
            method: 'POST',
            jsonData: this.getForm().getFieldValues(),
            success: onSuccess,
            failure: onError,
            scope: this
        });

    }

});