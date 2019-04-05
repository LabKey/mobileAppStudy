/*
 * Copyright (c) 2016-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('LABKEY.MobileAppStudy.ForwardingConfigurationPanel', {

    extend: 'Ext.panel.Panel',

    border: false,

    isEditable: true,

    forwardingEnabled: null,

    url: null,

    username: null,

    password: null,

    validateForm: null,

    trackResetOnLoad: true,

    initComponent: function () {
        this.callParent();

        this.add(this.getURLField());
        this.add(this.getUserField());
        this.add(this.getPasswordField());
    },
    getURLField: function() {
        if (!this.urlField) {
            this.urlField = Ext4.create("Ext.form.field.Text", {
                width: 400,
                name: 'url',
                fieldLabel: 'Server URL',
                value: this.url,
                padding: '10px 10px 0px 10px',
                emptyText: "Enter URL",
                submitEmptyText: false,
                readOnly: !this.isEditable,
                validateOnChange: true,
                validator: this.validateField,
                allowBlank: true,
                vtype:'url',
                listeners: {
                    change: this.validateForm, //TODO:
                    scope: this
                }
            });
        }
        return this.urlField;
    },
    getUserField: function() {
        if (!this.userField) {
            this.userField = Ext4.create("Ext.form.field.Text", {
                width: 250,
                name: 'user',
                fieldLabel: 'User',
                value: this.username,
                padding: '10px 10px 0px 10px',
                emptyText: "Enter Username",
                submitEmptyText: false,
                readOnly: !this.isEditable,
                validateOnChange: true,
                validator: this.validateField,
                allowBlank: true,
                listeners: {
                    change: this.validateForm, //TODO:
                    scope: this
                }
            });
        }
        return this.userField;
    },
    getPasswordField: function() {
        if (!this.passwordField) {
            this.passwordField = Ext4.create("Ext.form.field.Text", {
                width: 250,
                inputType: 'password',
                name: 'password',
                fieldLabel: 'Password',
                value: this.password,
                padding: '10px 10px 0px 10px',
                emptyText: "Enter Password",
                submitEmptyText: false,
                readOnly: !this.isEditable,
                validateOnChange: true,
                allowBlank: true,
                validator: this.validateField,
                listeners: {
                    change: this.validateForm, //TODO:
                    scope: this
                }
            });
        }
        return this.passwordField;
    },
    validateField: function(value) {
        //'this' is the field...
        if (this.up('panel').hidden)
            return true;

        return !!value || "Field required.";
    },
    getForwardingControls: function () {
        if (!this.forwarderControls) {
            this.forwarderControls = []
        }
        return this.forwarderControls;
    }
});