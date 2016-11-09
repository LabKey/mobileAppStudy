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

            this.add(this.getFormFields());
        }
        else { //TODO: should this be it's own panel?
            if (this.canChangeCollection) {
                this.dockedItems = [{
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [this.getSaveButton()]
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

            this.add(this.getEnableCollectionControl());
        }
    },

    getSubmitButton: function() {
        if (!this.submitButton) {
            this.submitButton = Ext4.create('Ext.button.Button', {
                text: 'Submit',
                itemId: 'submitBtn',
                disabled: true,
                handler: function (btn) {
                    btn.up('form').doSubmit(btn);
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
                disabled: !this.isEditable,
                validateOnChange: true,
                allowOnlyWhitespace: false,
                listeners: {
                    change: function(field, newValue, oldValue){
                        var submitBtn = field.up('form').getSubmitButton();
                        submitBtn.setDisabled(!field.isValid() || newValue == this.up('form').shortName);
                    }
                }
            });
        }
        return this.studyIdField;
    },

    getFormFields: function() {
        return [this.getStudyIdField()];
    },

    doSubmit: function(btn) {
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

    },

    getSaveButton: function() {
        if (!this.saveButton) {
            this.saveButton = Ext4.create('Ext.button.Button', {
                text: 'Save',
                itemId: 'saveBtn',
                disabled: true,
                hidden: true,
                handler: function (btn) {
                    if (!this.collectionEnabled)
                        Ext4.Msg.confirm('Response collection stopped', 'Response collection is disabled. No data will be collected for this study until it is enabled. Check the box to enable response collection.', function(val){
                            if (val == 'yes'){
                                btn.up('form').enableCollection(btn);
                            }
                        }, this);
                    else
                    //No confirmation needed to enable collection
                        btn.up('form').enableCollection(btn);
                }
            })
        }
        return this.saveButton;
    },

    getEnableCollectionControl: function() {
        if (!this.collectionCheckbox) {
            this.collectionCheckbox = Ext4.create("Ext.form.field.Checkbox",{
                boxLabel: 'Enable Response Collection',
                padding:'0 0 0 20',
                id: 'enableCollection',
                checked: this.collectionEnabled,
                width: 200,
                name: 'enableCollection',
                value: this.collectionEnabled,
                validateOnChange: true,
                disabled: !this.canChangeCollection,
                listeners: {
                    change: function(field, newValue){
                        var form = field.up('form');
                        var saveBtn = form.getSaveButton();
                        if (saveBtn.hidden)
                            saveBtn.show();

                        saveBtn.setDisabled(!field.isValid() || newValue == form.collectionEnabled);
                    }
                }
            });
        }

        return this.collectionCheckbox;
    },

    enableCollection: function(btn) {
        btn.setDisabled(true);

        function onSuccess(response, options) {
            btn.hide();

            var obj = Ext4.decode(response.responseText);
            if (obj.success)
                this.collectionEnabled = obj.data.collectionEnabled;
            else
            {
                Ext4.Msg.alert("Error", "There was a problem enabling response collection for this survey.  Please check the logs or contact an administrator.");
            }
        }

        function onError(response, options){
            btn.setDisabled(false);

            var obj = Ext4.decode(response.responseText);
            if (obj.errors)
            {
                Ext4.Msg.alert("Error", "There was a problem enabling response collection for this survey. " + obj.errors[0].message);
            }
        }

        var data = {
            shortName: this.shortName,
            collectionEnabled: this.collectionCheckbox.checked
        };

        Ext4.Ajax.request({
            url: LABKEY.ActionURL.buildURL('mobileappstudy', 'studyCollection.api'),
            method: 'POST',
            jsonData: JSON.stringify(data),
            success: onSuccess,
            failure: onError,
            scope: this
        });
    }
});