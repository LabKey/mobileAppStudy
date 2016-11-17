/*
 * Copyright (c) 2016 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('LABKEY.MobileAppStudy.StudySetupPanel', {

    extend: 'Ext.form.Panel',

    border: false,

    isEditable: true,

    shortName: null,

    canChangeCollection: null,

    collectionEnabled: null,

    trackResetOnLoad: true,

    initComponent: function()
    {
        if (this.isEditable) {
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
        }
        else {
            if (this.canChangeCollection) {
                this.dockedItems = [{
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [this.getSubmitButton()]
                }];
            }

            this.callParent();
            this.add({
                tag: 'div',
                padding: '10px 10px 0px 10px',
                itemId: 'messageEl',
                html: 'The study short name associated with this folder is ' + this.shortName + '.',
                border: false
            });
        }

        this.add(this.getFormFields());
        this.add(this.getEnableCollectionControl());
    },

    getSubmitButton: function() {
        if (!this.submitButton) {
            this.submitButton = Ext4.create('Ext.button.Button', {
                text: 'Submit',
                itemId: 'submitBtn',
                disabled: true,
                handler: function (btn) {
                    btn.up('form').enableCollectionWarning(btn);
                }
            })
        }
        return this.submitButton;
    },

    getStudyIdField: function() {
        if (!this.studyIdField) {
            this.studyIdField = Ext4.create("Ext.form.field.Text", {
                width: 200,
                name: 'shortName',
                value: this.shortName,
                padding: '10px 10px 0px 10px',
                allowBlank: false,
                emptyText: "Enter Study Short Name",
                submitEmptyText: false,
                // disabled: !this.isEditable,
                hidden: !this.isEditable,
                readOnly: !this.isEditable,
                validateOnChange: true,
                allowOnlyWhitespace: false,
                listeners: {
                    change: this.validateForm
                }
            });
        }
        return this.studyIdField;
    },

    getFormFields: function() {
        return [this.getStudyIdField()];
    },
    enableCollectionWarning: function(btn) {
        btn.setDisabled(true);
        var collectionCheckbox = this.getEnableCollectionControl();
        if (!collectionCheckbox.checked)// && collectionCheckbox.isDirty())
            Ext4.Msg.show({
                title: 'Response collection stopped',
                msg: 'Response collection is disabled for this study. No data will be collected until it is enabled.',
                buttons: Ext4.Msg.OKCANCEL,
                icon: Ext4.Msg.WARNING,
                fn: function(val) {
                    if (val == 'ok'){
                        btn.up('form').doSubmit(btn);
                    }
                    else btn.setDisabled(false);
                },
                scope: this
            });
        else //No confirmation needed to enable collection
            btn.up('form').doSubmit(btn);
    },

    doSubmit: function(btn) {

        function onSuccess(response, options) {
            var obj = Ext4.decode(response.responseText);
            if (obj.success) {
                //reload form control values for Dirty tracking
                btn.up('form').getForm().setValues(obj.data);
                this.validateForm(btn);

                //Set panel values
                this.shortName = obj.data.shortName;
                this.collectionEnabled = obj.data.collectionEnabled;

                //TODO: Display a successful save message?
            }
            else
            {
                Ext4.Msg.alert("Error", "There was a problem.  Please check the logs or contact an administrator.");
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

    },
    getEnableCollectionControl: function() {
        if (!this.collectionCheckbox) {
            this.collectionCheckbox = Ext4.create("Ext.form.field.Checkbox",{
                name: 'collectionEnabled',
                boxLabel: 'Enable Response Collection',
                padding:'0 0 0 10',
                id: 'collectionEnabled',
                checked: this.collectionEnabled,
                width: 200,
                value: this.collectionEnabled,
                disabled: !this.canChangeCollection,
                listeners: {
                    change: this.validateForm
                }
            });
        }
        return this.collectionCheckbox;
    },
    validateForm: function(field){
        var form = field.up('form');
        var saveBtn = form.getSubmitButton();
        if (saveBtn.hidden)
            saveBtn.show();

        saveBtn.setDisabled(!(form.isDirty() && form.isValid()));
    }
});