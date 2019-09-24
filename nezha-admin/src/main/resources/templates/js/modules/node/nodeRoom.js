<script>
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + '/room/list',
        datatype: "json",
        colModel: [
            {label: "ID", name: 'id', width: 60, key: true, hidden: true},
            {label: "<@spring.message 'node.room.name'/>", name: 'name', width: 60, sortable: false},
            {label: "<@spring.message 'node.room.addr'/>", name: 'addrName', width: 60, sortable: false},
            {label: "<@spring.message 'node.room.roomRows'/>", name: 'roomRows', width: 60, sortable: false},
            {label: "<@spring.message 'node.room.roomCols'/>", name: 'roomCols', width: 60, sortable: false},
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
            root: "page.list",
            page: "page.currPage",
            total: "page.totalPage",
            records: "page.totalCount"
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
var numeric = window.validators.numeric;
var minValue = window.validators.minValue;
var integer = window.validators.integer;
var helpers = window.validators.helpers;

// 自定义规则，;第一个参数是当前字段值，第二个参数是当前校验的整个对象(此处是vm.data.nodeRoom)
var nameCheck = function (value, nodeRoom) {
    if (helpers.req(value)) {
        var flag = true;
        var params = "?name=" + value;
        if (nodeRoom.id) {
            params += "&id=" + nodeRoom.id;
        }
        $.ajax({
            type: "GET",
            url: baseURL + "room/roomNameCheck" + params,
            async: false, // 关闭异步，否则会在执行完ajax前就跳到下一段代码
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
var roomRowsCheck = function (value, nodeRoom) {
    if (nodeRoom.id == null) {// 添加不需校验
        return true;
    }
    if (helpers.req(value)) {
        var flag = true;
        var params = "?id=" + nodeRoom.id + "&roomRows=" + value + "&roomCols=" + vm.roomDetail.roomCols;
        $.ajax({
            type: "GET",
            url: baseURL + "room/roomSizeCheck" + params,
            async: false, // 关闭异步，否则会在执行完ajax前就跳到下一段代码
            dataType: "json",
            success: function (r) {
                if (r.code != 200) {
                    flag = false;
                    vm.occRow = r.data;
                }
            }
        });
        return flag;
    } else {
        return true;
    }
}
var roomColsCheck = function (value, nodeRoom) {
    if (nodeRoom.id == null) {// 添加不需校验
        return true;
    }
    if (helpers.req(value)) {
        var flag = true;
        var params = "?id=" + nodeRoom.id + "&roomRows=" + vm.roomDetail.roomRows + "&roomCols=" + value;
        $.ajax({
            type: "GET",
            url: baseURL + "room/roomSizeCheck" + params,
            async: false, // 关闭异步，否则会在执行完ajax前就跳到下一段代码
            dataType: "json",
            success: function (r) {
                if (r.code != 200) {
                    flag = false;
                    vm.occCol = r.data;
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
        name: null, addr: null
    },
    nodeRoom: {
        id: null,
        name: null,
        roomRows: null,
        roomCols: null,
        addr: null,
        remarks: null
    },
    addrList: [],
    roomDetail: null, // 校验行列可用时接口需要行列必须,解决列为空校验行问题
    occRow: null,
    occCol: null
};

var vm = new Vue({
    el: '#app',
    data: this.data,
    validations: {
        nodeRoom: {
            name: {
                required, nameCheck
            },
            roomRows: {
                required, integer, minValue: minValue(1), roomRowsCheck
            },
            roomCols: {
                required, integer, minValue: minValue(1), roomColsCheck
            },
            addr: {
                required
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
                    "name": vm.q.name,
                    "addr": vm.q.addr == null ? null : vm.q.addr.code
                },
                page: 1
            }).trigger("reloadGrid");
        },
        reset:function(){
            vm.q.name = vm.q.addr = null;
            vm.query();
        },
        getInfo: function (id) {
            $.ajax({
                type: "GET",
                url: baseURL + "room/detail?id=" + id,
                async: false, // 关闭异步，否则会在执行完ajax前就跳到下一段代码
                dataType: "json",
                success: function (r) {
                    vm.nodeRoom = r.data;
                }
            });

            // 克隆一份机房详情对象
            vm.roomDetail = JSON.parse(JSON.stringify(vm.nodeRoom));

            // 处理下拉框回显问题
            vm.addrList.some(item => {
                if (item.code == vm.nodeRoom.addr) {
                    vm.$set(vm.nodeRoom, "addr", item);
                    return true;
                }
            })
        },
        add: function () {
            vm.$v.nodeRoom.$reset(); // 进入新增页面时，将校验的触发状态清空($dirty=true -> false)
            vm.showList = false;
            vm.title = "<@spring.message 'common.add'/>";
            vm.nodeRoom = {
                id: null,
                name: null,
                roomRows: null,
                roomCols: null,
                addr: null,
                remarks: null
            }
        },
        update: function () {
            vm.$v.nodeRoom.$reset(); // 进入新增页面时，将校验触发状态清空($dirty=true -> false)
            var id = getSelectedRow();
            if (id == null) {
                return;
            }
            vm.showList = false;
            vm.title = "<@spring.message 'common.edit'/>";
            this.getInfo(id);
        },
        saveOrUpdate: function () {
            if (!vm.validate()) { // 在下方定义的validate方法
                return;
            }
            var url;
            if (vm.nodeRoom.id == null) {
                url = "room/save";
            } else {
                url = "room/update";
            }
            vm.$set(vm.nodeRoom, "addr", vm.nodeRoom.addr.code);
            $.ajax({
                type: vm.nodeRoom.id == null ? "POST" : "PUT",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.nodeRoom),
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
                    url: baseURL + "room/delete",
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
        getRoomAddrs: function () { // 获取机房所属地区列表
            $.getJSON(baseURL + "sys/dict/all?name=addr", function (r) {
                vm.addrList = r.data;
            });
        },
        validate: function () {
            vm.$v.nodeRoom.$touch(); // 提交时，不论是否已触发校验，都触发一下($dirty=false -> true)
            return !vm.$v.nodeRoom.$error;
        }
    },
    created: function () {
        this.getRoomAddrs();
    }
});
</script>
