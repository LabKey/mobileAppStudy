

Ext4.define('LABKEY.MobileAppSurvey.EnrollmentTokenBatchFormPanel', {

    extend: 'Ext.form.Panel',

    title: 'Generate Tokens',

    floating: true,

    closable: true,

    cls: 'x4-window-default',

    initComponent : function()
    {
        this.dockedItems = [{
            xtype: 'toolbar',
            dock: 'bottom',
            ui: 'footer',
            items: [
                '->',
                {
                    text: 'Submit',
                    itemId: 'submitBtn',
                    formBind: true,
                    handler: function (btn)
                    {
                        var panel = btn.up('form');
                        panel.doSubmit(btn);
                    }
                },
                {
                    text: 'Cancel',
                    itemId: 'cancelBtn',
                    handler: function (btn, key)
                    {
                        btn.up('form').close()
                    }
                }
            ]
        }];

        this.callParent();
        this.add({
            tag: 'div',
            padding: '10px 10px 0px 10px',
            itemId: 'messageEl',
            html:'How many tokens would you like to generate?',
            border: false
        });
        this.add(this.getFormFields());

        this.on (
                {
                    afterRender: function (cmp) {
                        console.log('after render now')
                    }
                }
        )


    },

    getFormFields : function()
    {
        var items = [];
        items.push(
                {
                    xtype: 'radiogroup',
                    padding: '14px',
                    defaultType: 'radio',
                    layout: 'vbox',
                    items: [
                        {
                            boxLabel: '100',
                            name: 'count',
                            inputValue: '100',
                            id: 'radio1'
                        }, {
                            boxLabel: '1,000',
                            name: 'count',
                            inputValue: '1000',
                            id: 'radio2'
                        }, {
                            boxLabel: '10,000',
                            name: 'count',
                            inputValue: '10000',
                            id: 'radio3'
                        }, {
                            xtype: 'fieldcontainer',
                            layout: 'hbox',
                            items: [
                                {
                                    boxLabel: 'Other',
                                    xtype: 'radio',
                                    name: 'count',
                                    inputValue: 'other',
                                    id: 'radio4'
                                } ,
                                {
                                    xtype: 'numberfield',
                                    name: 'otherCount',
                                    disabled: true,
                                    minValue: 1,
                                    padding: '0 0 0 5px'
                                }
                            ]
                        }
                    ],
                    listeners: {
                        change: function(radiogroup, radio){
                            var isOther = radio.count == 'other';
                            var form = radiogroup.up('form');
                            var otherCount = form.getForm().findField('otherCount');
                            otherCount.setDisabled(!isOther);

                        }
                    }
                }
        );
        return items;
    },

    doSubmit: function(btn){
        btn.setDisabled(true);

        function onSuccess(response, options){
            var batchId = JSON.parse(response.responseText).data;
            if (batchId)
            {
                btn.up('form').close();
                window.location = LABKEY.ActionURL.buildURL('mobileappsurvey', 'tokenList.view', null, {'query.BatchId/RowId~eq': batchId});
            }
        }

        function onError(response, options){
            btn.setDisabled(false);

            var obj = Ext4.decode(response.responseText);
            if (obj.errors && obj.errors[0].field == "form")
            {
                Ext4.Msg.alert("Error", "There were problems generating the tokens. " + obj.errors[0].message);
            }
        }

        Ext4.Ajax.request({
            url: LABKEY.ActionURL.buildURL('mobileappsurvey', 'generateTokens.api'),
            method: 'POST',
            jsonData: this.getFieldValues(),
            success: onSuccess,
            failure: onError,
            scope: this
        });

    },

    getFieldValues: function()
    {
        var values = this.getForm().getFieldValues();
        if (values.count == 'other' && values.otherCount)
        {
            values.count = values.otherCount;
            delete values.otherCount;
        }
        return values;
    }
});