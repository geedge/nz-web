<script >
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'deteType/dicList',
        datatype: "json",
        colModel: [
            {label: 'ID', name: 'id', width: 30, key: true, hidden: true,},
            {
                label: '<@spring.message "metaMapping.type"/>',
                name: 'type',
                sortable: true,
                width: 60,
                // cellattr: function(rowId, tv, rawObject, cm, rdata) {
                //     //合并单元格
                //     return 'id=\'type' + rowId + "\'";
                //     //if (Number(rowId) < 5) { return ' colspan=2' }
                // }
            },
            {
                label: '<@spring.message "metaMapping.key"/>',
                name: 'val',
                sortable: false,
                width: 100,
                formatter: function (value, options, rowData) {
                    return spliteTable(rowData.mappings, "val")

                }
            },
            {
                label: '<@spring.message "metaMapping.val"/>',
                name: 'mapping',
                sortable: false,
                width: 100,
                formatter: function (value, options, rowData) {
                    return spliteTable(rowData.mappings, "mapping")
                }
            },
        ],
        viewrecords: true,
        height: top.$(".content").height() - 130,
        rowNum: 15,
        rowList: [15, 30, 50],
        rownumbers: true,
        rownumWidth: 25,
        autowidth: true,
        multiselect: true,
        pager: "#jqGridPager",
        jsonReader: {
            root: "data.list",
            page: "data.currPage",
            total: "data.totalPage",
            records: "data.totalCount"
        },
        prmNames: {
            page: "page",
            rows: "limit",
            order: "order"
        },
        gridComplete: function () {
            //隐藏grid底部滚动条
            $("#jqGrid").closest(".ui-jqgrid-bdiv").css({"overflow-x": "hidden"});
            // var gridName = "jqGrid";
            // Merger(gridName, 'type');
            console.log($(".costomTable").parent().css("padding-left", "0px"))
            console.log($(".costomTable").parent().css("padding-right", "0px"))
        },
        onSelectAll: function (rowids, status) {
            if (status) {
                toolsBtnHandle();
            }
        },
        onSelectRow: function (rowid, status) {
            toolsBtnHandle();
        }
    });
});


var typeRepeat = function (type, metaMappings) {
    if (helpers.req(type) && helpers.req(metaMappings)) {
        var params = "?type=" + type;
        var flag = true;
        $.ajax({
            type: "get",
            url: baseURL + "/deteType/dicNameCheck" + params,
            async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
            dataType: "json",
            success: function (r) {
                if (r.code != 200) {
                    flag = false;
                }
            }
        });
        return flag;
    } else {
        return true;
    }

}
var vm = new Vue({
    el: "#metaMapping",
    data: {
        single: true,
        multi: true,
        showList: true,
        params: {
            type: '',
        },
        title: '<@spring.message "deteTypeMeta"/>',
        metaMappings: {
            type: '',
            mappings: []
        },
        hackReset: true,
        newMapping: false,//true 添加操作 false 更新操作
        mappingDirty: false,
        mappingRequired: true
    },
    created: function () {

    },
    validations: function () {
        let temp = this;
        if (temp.newMapping) {
            return {
                metaMappings: {
                    type: {
                        required,
                        typeRepeat
                    }
                }
            }
        } else {
            return {
                metaMappings: {
                    type: {
                        required
                    }
                }
            }
        }

    },
    methods: {
        reset: function () {
            var temp = this;
            temp.$v.metaMappings.$reset();
            temp.metaMappings = {
                type: '',
                mappings: []
            }
            temp.newMapping = false;
            temp.mappingDirty = false;
            temp.mappingRequired = true;
        },
        query: function () {
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'type': vm.params.type},
                page: 1
            }).trigger("reloadGrid");
        },
        reload: function () {
            vm.query();
        },
        add: function () {
            vm.showList = false;
            hackReset();
            vm.reset();
            vm.newMapping = true;
            $("#saveOrUpdate").unbind('click').bind('click',this.save)
        },
        toUpdate: function () {
            var type = getSelectedType();
            if (type == null) {
                return;
            }
            vm.reset();
            vm.getInfo(type);
            vm.showList = false;
            vm.newMapping = false;
            hackReset();
            $("#saveOrUpdate").unbind('click').bind('click',this.update)
        },
        getInfo: function (type) {
            $.ajax({
                url: baseURL + "/deteType/dicList?type=" + type,
                type: "GET",
                async: false,
                success: function (r) {
                    vm.metaMappings = r.data.list[0];
                }
            });
        },
        save: function () {
            console.log('save--')
            if (vm.validate()) { //在下方定义的validate方法
                return;
            }
            var url = "deteType/saveDic";
            var method = "POST";
            vm.saveOrUpdate(url, method);
        },
        update: function () {
            console.log('update--')
            if (vm.validate()) { //在下方定义的validate方法
                return;
            }
            var url = "deteType/updateDic";
            var method = "PUT";
            vm.saveOrUpdate(url, method);
        },
        saveOrUpdate: function (url, method) {

            $.ajax({
                type: method,
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.metaMappings),
                success: function (r) {
                    if (r.code === 200) {
                        alert('<@spring.message "common.success"/>', function (index) {
                            vm.reload();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        del: function (event) {
            var types = getSelectedTypes();
            if (types == null) {
                return;
            }
            console.log("types-->" + JSON.stringify(types))
            confirm("<@spring.message 'common.confirmDelete'/>", function () {
                $.ajax({
                    url: baseURL + "/deteType/deleteDic",
                    type: "DELETE",
                    datatype: "JSON",
                    data: JSON.stringify(types),
                    contentType: "application/json",
                    async: false,
                    success: function (r) {
                        if (r.code === 200) {
                            alert('<@spring.message "common.success"/>', function (index) {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            })

        },
        validate: function () {//提交时的校验
            vm.touchMappings();
            var elementError = false;
            var componets = vm.$refs.metaMapping;
            if (typeof componets != 'undefined' && componets.length > 1) {

                for (var i = 0; i < componets.length; i++) {
                    var componet = componets[i];
                    if (componet.validate()) {
                        elementError = true;
                        break;
                    }
                }

            } else {
                vm.mappingRequired = false;
                elementError = true;
            }
            console.log(vm.$v.metaMappings.$error + '|' + elementError)
            console.log(vm.$v.metaMappings)
            return vm.$v.metaMappings.$error || elementError;
        },
        formatmetaMappingToSimple: function () {

        },
        formatmetaMappingToObject: function () {

        },
        touchMappings: function () {
            let temp = this;
            temp.$v.metaMappings.$touch();
            temp.mappingDirty = true;
        },
        addMapping: function () {
            var temp = this;
            temp.mappingDirty = true;

            var mappingLen = temp.metaMappings.mappings.length;
            if (mappingLen > 0) {
                for (var i = 0; i < mappingLen; i++) {
                    temp.$refs['metaMapping'][i].submitSelf();
                    if (temp.$refs['metaMapping'][i].$v.metaMapping.$error) {
                        return;
                    }
                }
            }
            var mapping = {
                id: null,
                type: '',
                val: '',
                mapping: '',
                delFlag: '0',
                new: true,
                isEdited: false
            }
            this.metaMappings.mappings.push(mapping);

        },
        delMapping: function (index) {
            var mapping = this.metaMappings.mappings[index];
            this.metaMappings.mappings.splice(index, 1);
        }
    },
    watch: {
        'metaMappings.mappings': function () {
            var temp = this;
            var mappings = temp.metaMappings.mappings;
            if (mappings.length < 1) {
                temp.mappingRequired = false;
            } else {
                temp.mappingRequired = true;
            }
        }
    },
    mounted() {

    }
})


function spliteTable(array, field) {
    var showDiv = "<table class='costomTable' width='100%'>";
    if (array.length == 1) {
        showDiv += "<tr><td>" + array[0][field] + "</td></tr>"

    } else {
        $.each(array, function (i, v) {
            if (i == 0) {
                showDiv += "<tr><td style='border-bottom: 1px solid #ddd;padding:0px 2px 8px 8px;'>" + v[field] + "</td></tr>"
            } else if (i == array.length - 1) {
                showDiv += "<tr><td style='padding:8px 2px 0px 8px'>" + v[field] + "</td></tr>"
            } else {
                showDiv += "<tr><td style='border-bottom: 1px solid #ddd;padding:8px 2px 8px 8px;'>" + v[field] + "</td></tr>"
            }
        })
    }
    showDiv += "</table>"
    return showDiv;

}


function hackReset() {
    vm.hackReset = false //解决删除后子组件不刷新问题，利用v-if会销毁dom重新渲染的机制，强制刷新子组件
    vm.$nextTick(() => {
        vm.hackReset = true
    });
}

function getSelectedType() {
    var grid = $("#jqGrid");
    var rowKey = grid.getGridParam("selrow");
    if (!rowKey) {
        alert("<@spring.message 'common.selectOne'/>");
        return;
    }

    var selectedIDs = grid.getGridParam("selarrrow");
    if (selectedIDs.length > 1) {
        alert("<@spring.message 'common.onlySelectOne'/>");
        return;
    }
    var rowData = grid.jqGrid('getRowData', selectedIDs[0]);
    return rowData.type;
}

function getSelectedTypes() {
    var grid = $("#jqGrid");
    var rowKey = grid.getGridParam("selrow");
    if (!rowKey) {
        alert("<@spring.message 'common.selectOne'/>");
        return;
    }

    var selectedIDs = grid.getGridParam("selarrrow");
    var types = [];
    for (let i in selectedIDs) {
        var rowData = grid.jqGrid('getRowData', selectedIDs[i]);
        types.push(rowData.type);
    }

    return types;
}

var inPageValRepeat = function (mappings, index) {
    return helpers.withParams({type: inPageValRepeat}, function (value) {
        if (helpers.req(value)) {
            for (let i in mappings) {
                if (i != index) {
                    var mapping = mappings[i];
                    if (helpers.req(mapping.val)) {
                        if (mapping.val == value) {
                            return false;
                        }
                    }

                }
            }
        }
        return true;
    })
}
var inPageMappingRepeat = function (mappings, index) {
    return helpers.withParams({type: inPageMappingRepeat}, function (value) {
        if (helpers.req(value)) {
            for (let i in mappings) {
                if (i != index) {
                    var mapping = mappings[i];
                    if (helpers.req(mapping.mapping)) {
                        if (mapping.mapping == value) {
                            return false;
                        }
                    }

                }
            }
        }
        return true;
    })
}
Vue.component("meta-mapping", {
    template: "#metaTemplate",
    props: {
        metaMapping: {
            type: Object,
            required: true
        },
        metaMappings: {
            type: Array,
            required: true
        },
        index: {
            type: Number,
            required: true
        }
    },
    data: function () {
        return {
            editSwitch: false,
            listPreKey: 'r-mapping-view',
            lsitDom: null,
            editPreKey: 'r-mapping-edit',
            editDom: null,
        }
    },
    create: function () {

    },
    validations: function () {
        let temp = this;
        return {
            metaMapping: {
                val: {
                    required,
                    inPageValRepeat: inPageValRepeat(temp.metaMappings, temp.index)
                },
                mapping: {
                    required,
                    inPageMappingRepeat: inPageMappingRepeat(temp.metaMappings, temp.index)
                }
            }
        }

    },
    methods: {
        init: function () {
            var temp = this;
            temp.listSelector = "#" + temp.listPreKey + "-" + temp.index;
            temp.editSelector = "#" + temp.editPreKey + "-" + temp.index;
            temp.listDom = $(temp.listSelector);
            temp.editDom = $(temp.editSelector);
            if (typeof temp.metaMapping.new != 'undefined' && temp.metaMapping.new != null && !temp.metaMapping.isEdited) {
                temp.editSwitch = true;
            }
        },
        editSelf: function () {
            var temp = this;

            if (temp.metaMapping.delFlag == '0') {
                temp.editSwitch = true;
            } else {
                return false;
            }

        },
        delSelf: function (e) {
            var temp = this;
            var p = temp.metaMapping;
            console.log('p-->')
            console.log(p)
            if (typeof p.new != 'undefined' && p.new != null && p.new) {
                temp.$emit("del-mapping", this.index);
            } else {
                $("#add-box_op_edit_" + temp.index).bind('mouseover', function (event) {
                    event.target.style.cursor = 'not-allowed';
                });
                p.delFlag = '1';
            }

        },
        recSelf: function (e) {
            var temp = this;
            temp.listDom.removeClass('text-disabled');
            $("#add-box_op_edit_" + temp.index).bind('mouseover', function (event) {
                event.target.style.cursor = 'pointer';
            });
            temp.metaMapping.delFlag = '0';
        },
        submitSelf: function () {
            var temp = this;

            if (temp.validate()) {
                temp.showValWarningMsg();
                temp.showMappingWarningMsg();
                return;
            }

            temp.editSwitch = false;
            temp.metaMapping.isEdited = true;

        },
        validate: function () {
            this.$v.metaMapping.$touch();
            return this.$v.metaMapping.$error;
        },
        showValWarningMsg: function () {
            var temp = this;
            if (temp.$v.metaMapping.val.$anyError) {
                $("#r-mapping-val-input-" + temp.index).blur();
                var msg = '';
                if (temp.$v.metaMapping.val.$dirty && !temp.$v.metaMapping.val.required) {
                    msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.required'/></div>"
                }
                if (temp.$v.metaMapping.val.$dirty && !temp.$v.metaMapping.val.inPageValRepeat) {
                    msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.duplicate'/></div>"
                }
                if (temp.editSwitch) {
                    layer.tips(msg, "#r-mapping-val-input-" + temp.index, {
                        tips: [1, "white"],
                        tipsMore: true
                    });
                }


            }

        },
        showMappingWarningMsg: function () {
            var temp = this;
            if (temp.$v.metaMapping.mapping.$anyError) {
                $("#r-mapping-val-input-" + this.index).blur();
                var msg = '';
                if (temp.$v.metaMapping.mapping.$dirty && !temp.$v.metaMapping.mapping.required) {
                    msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.required'/></div>"
                }
                if (temp.$v.metaMapping.mapping.$dirty && !temp.$v.metaMapping.mapping.inPageMappingRepeat) {
                    msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.duplicate'/></div>"
                }
                if (temp.editSwitch) {
                    layer.tips(msg, "#r-mapping-mapping-input-" + temp.index, {
                        tips: [1, "white"],
                        tipsMore: true
                    });
                }

            }
        }
    },
    mounted() {
        this.init();
    },
    watch: {
        "$v.metaMapping.val": {
            handler() {
                this.showValWarningMsg();
            },
            deep: true
        },
        "$v.metaMapping.mapping": {
            handler() {
                this.showMappingWarningMsg();
            },
            deep: true
        }
    }
})
</script>