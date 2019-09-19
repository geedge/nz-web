<script >
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + '/cab/list',
        datatype: "json",
        colModel: [
            {label: "ID", name: 'id', width: 60, key: true, hidden: true},
            {label: "<@spring.message 'common.name'/>", name: 'name', width: 60, sortable: false},
            {label: "<@spring.message 'node.room.name'/>", name: 'roomName', width: 60, sortable: false},
            {label: "<@spring.message 'node.cabinet.roomRow'/>", name: 'roomRow', width: 60, sortable: false},
            {label: "<@spring.message 'node.cabinet.roomCol'/>", name: 'roomCol', width: 60, sortable: false},
            {label: "<@spring.message 'node.cabinet.uSize'/>", name: 'ubit', width: 60, sortable: false},
            {label: "<@spring.message 'common.remark'/>", name: 'remarks', width: 60, sortable: false}
        ],
        viewrecords: true,
        height: 385,
        rowNum: 10,
        rowList: [10, 30, 50],
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
            $("#jqGrid").closest(".ui-jqgrid-bdiv").css({"overflow-x": "hidden"}); // 隐藏grid底部滚动条
        }
    });
});

// 注册select组件
Vue.component('multiselect', window.VueMultiselect.default);

// 引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-v-values
Vue.use(window.vuelidate.default);
var required = window.validators.required;
var minValue = window.validators.minValue;
var integer = window.validators.integer;
var helpers = window.validators.helpers;

// 自定义规则，;第一个参数是当前字段值，第二个参数是当前校验的整个对象(此处是vm.data.nodeCabinet)
var nameCheck = function (value, nodeCabinet) {
    var flag = true;
    if (nodeCabinet.roomId == null) {
        return flag;
    } else {
        if (helpers.req(value)) {
            var params = "?name=" + value + "&roomId=" + nodeCabinet.roomId.roomId;
            if (nodeCabinet.id != null) {
                params += "&id=" + nodeCabinet.id;
            }
            $.ajax({
                type: "GET",
                url: baseURL + "cab/cabNameCheck" + params,
                async: false, // 关闭异步
                dataType: "json",
                success: function (r) {
                    if (r.code != 200) {
                        flag = false;
                    }
                }
            });
            return flag;
        } else {
            return flag;
        }
    }
}
var cabUbitCheck = function (value, nodeCabinet) {
    if (nodeCabinet.id == null) {
        return true;
    }
    if (helpers.req(value)) {
        var flag = true;
        // 进行校验
        var params = "?id=" + nodeCabinet.id + "&ubit=" + value;
        $.ajax({
            type: "GET",
            url: baseURL + "cab/cabUbitCheck" + params,
            async: false, // 关闭异步
            dataType: "json",
            success: function (r) {
                if (r.code != 200) {
                    flag = false;
                    vm.occUbit = r.data;
                }
            }
        });
        return flag;
    } else {
        return true;
    }

}

var data = {
    showList: true,
    title: null,
    q: {
        cabName: null
    },
    nodeCabinet: {
        id: null,
        name: null,
        roomId: null,
        roomRow: null,
        roomCol: null,
        ubit: null,
        remarks: null
    },
    rooms: [],
    roomRows: [],
    roomCols: [],
    occUbit: null
};

var vm = new Vue({
    el: '#app',
    data: this.data,
    validations: {
        nodeCabinet: {
            name: {
                required, nameCheck
            },
            roomId: {
                required
            },
            roomRow: {
                required
            },
            roomCol: {
                required
            },
            ubit: {
                required, integer, minValue: minValue(1), cabUbitCheck
            }
        }
    },
    methods: {
        query: function () {
            this.reload();
        },
        reload: function () {
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {
                    "name": vm.q.cabName
                },
                page: page
            }).trigger("reloadGrid");
        },
        reset: function () {
            vm.q.cabName = null;
            vm.query();
        },
        getInfo: function (id) {
            $.ajax({
                type: "GET",
                url: baseURL + "cab/detail?id=" + id,
                async: false, // 关闭异步
                dataType: "json",
                success: function (r) {
                    vm.nodeCabinet = r.data;
                }
            });

            // 得到目前的机房 行号 列号 的数组集合
            var cab = JSON.parse(JSON.stringify(vm.nodeCabinet));
            this.getRooms();
            vm.rooms.some(item => {
                if (item.roomName == cab.roomName) {
                    vm.$set(vm.nodeCabinet, "roomId", item);
                }
            });
            this.getRoomRows();
            vm.roomRows.some(item => {
                if (item.roomRow == cab.roomRow) {
                    vm.$set(vm.nodeCabinet, "roomRow", item);
                    return true;
                }
            });
            this.getRoomCols();
            vm.roomCols.some(item => {
                if (item == cab.roomCol) {
                    vm.$set(vm.nodeCabinet, "roomCol", item);
                    return true;
                }
            });
        },
        add: function () {
            vm.$v.nodeCabinet.$reset(); // 进入新增页面时，将校验的触发状态清空($dirty=true -> false)
            vm.showList = false;
            vm.title = "<@spring.message 'common.add'/>";
            vm.nodeCabinet = {
                id: null,
                name: null,
                roomId: null,
                roomRow: null,
                roomCol: null,
                ubit: null,
                remarks: null
            }
            vm.roomRows = vm.roomCols = [];
            this.getRooms();
        },
        update: function () {
            vm.$v.nodeCabinet.$reset(); //进入新增页面时，将校验触发状态清空($dirty=true -> false)
            var id = getSelectedRow();
            if (id == null) {
                return;
            }
            vm.showList = false;
            vm.title = "<@spring.message 'common.edit'/>";
            this.getInfo(id);
        },
        del: function () {
            var ids = getSelectedRows();
            if (ids == null) {
                return;
            }
            confirm("<@spring.message 'common.confirmDelete'/>", function () {
                var form = new FormData();
                form.append("ids", ids);
                $.ajax({
                    type: "DELETE",
                    url: baseURL + "cab/delete",
                    processData: false,
                    contentType: false,
                    mimeType: "multipart/form-data",
                    data: form,
                    success: function (r) {
                        if (r.code == 200) {
                            alert("<@spring.message 'common.success'/>", function (index) {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        getRooms: function () { // 获取机房列表
            vm.rooms = [];      // 获取之前先置空
            var params = "";
            if (data.nodeCabinet.id) {
                params = "?id=" + data.nodeCabinet.id;
            }

            $.ajax({
                type: "GET",
                url: baseURL + "cab/optionalRoom" + params,
                async: false, // 关闭异步，否则会在执行完ajax前就跳到下一段代码
                dataType: "json",
                success: function (r) {
                    vm.rooms = r.data.list;
                }
            });
        },
        getRoomRows: function () { // 根据选择的机房获取对应的可用行信息
            vm.nodeCabinet.roomRow = vm.nodeCabinet.roomCol = null;
            if (vm.nodeCabinet.roomId == null) {
                return;
            }
            vm.rooms.some(item => {
                if (item.roomId == vm.nodeCabinet.roomId.roomId) {
                    vm.roomRows = item.roomRows;
                    return true;
                }
            });
        },
        getRoomCols: function () { // 根据选择的机房行数获取对应的列信息
            vm.nodeCabinet.roomCol = null;
            if (vm.nodeCabinet.roomRow == null) {
                return;
            }
            vm.roomRows.some(item => {
                if (item.roomRow == vm.nodeCabinet.roomRow.roomRow) {
                    vm.roomCols = item.roomCols;
                    return true;
                }
            });
        },
        saveOrUpdate: function () {
            if (!vm.validate()) { // 在下方定义的validate方法
                return;
            }
            var url;
            if (vm.nodeCabinet.id == null) {
                url = "cab/save";
            } else {
                url = "cab/update";
            }

            vm.$set(vm.nodeCabinet, "roomId", vm.nodeCabinet.roomId.roomId);
            vm.$set(vm.nodeCabinet, "roomRow", vm.nodeCabinet.roomRow.roomRow);
            $.ajax({
                type: vm.nodeCabinet.id == null ? "POST" : "PUT",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.nodeCabinet),
                success: function (r) {
                    if (r.code === 200) {
                        alert("<@spring.message 'common.success'/>", function (index) {
                            vm.reload();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        checkRoom: function () {
            if (vm.nodeCabinet.roomId == null) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.room.name'/>!")
            } else if (vm.roomRows.length < 1) {
                vm.roomCols = [];
                alert("<@spring.message 'node.room.notPosition'/>");
            }
        },
        checkRoomRow: function () {
            if (vm.nodeCabinet.roomId == null) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.room.name'/>!")
            } else if (vm.nodeCabinet.roomRow == null && vm.roomRows.length < 1) {
                alert("<@spring.message 'node.room.notPosition'/>");
            } else if (vm.nodeCabinet.roomRow == null && vm.roomRows.length > 0) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.cabinet.roomRow'/>");
            }
        },
        validate: function () {
            vm.$v.nodeCabinet.$touch(); // 提交时，不论是否已触发校验，都触发一下($dirty=false -> true)
            return !vm.$v.nodeCabinet.$error;
        }
    },
    created: function () {

    },
    watch: {}
});
</script>