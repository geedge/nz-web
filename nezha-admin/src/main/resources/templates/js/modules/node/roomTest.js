<script >
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
var roomNameCheck = function (value, nodeRoom) {
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
var cabNameCheck = function (value, nodeCabinet) {
    var flag = true;
    if (nodeCabinet.roomId == null) {
        return flag;
    } else {
        if (helpers.req(value)) {
            var params = "?name=" + value + "&roomId=" + nodeCabinet.roomId;
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
    roomList: [],     // 机房列表
    addrList: [],     // 地区列表
    roomDetail: null, // 校验行列可用时接口需要行列必须,解决列为空校验行问题
    q: {              // 查询条件
        name: null,
        addr: null
    },
    showRoomFrom: true, // 机房表单控制
    showCabFrom: true,  // 机柜表单控制
    nodeRoom: {
        id: null,
        name: null,
        roomRows: null,
        roomCols: null,
        addr: null,
        remarks: null
    },
    nodeCabinet: {
        id: null,
        name: null,
        roomName: null,
        roomId: null,
        roomRow: null,
        roomCol: null,
        ubit: null,
        remarks: null
    },
    room: null,         // 当前机房
    cabs: [],           // 当前机房包含机柜集合
    rowList: [],        // 行列表数据
    readFlag: null,     // 控制表单readonly选项值
    occUbit: null       // 机柜校验 前台提示行列信息
};


var vm = new Vue({
    el: '#roomTestApp',
    data: this.data,
    validations: {
        nodeRoom: {
            name: {
                required, roomNameCheck
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
        },
        nodeCabinet: {
            name: {
                required, cabNameCheck
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
        getRooms: function () {
            $.ajax({
                type: "GET",
                url: baseURL + "room/list?limit=999",
                async: false,
                dataType: "json",
                success: function (r) {
                    data.roomList = r.page.list;
                }
            });
        },
        getRoomAddrs: function () { // 获取机房所属地区列表d
            $.getJSON(baseURL + "sys/dict/all?name=addr", function (r) {
                data.addrList = r.data;
            });
        },
        getRoomInfo: function (roomId) {
            $.ajax({
                type: "GET",
                url: baseURL + "room/detail?id=" + roomId,
                async: false, // 关闭异步
                dataType: "json",
                success: function (r) {
                    vm.nodeRoom = r.data;
                }
            });
            // 验证时使用 解决空验证问题
            vm.roomDetail = JSON.parse(JSON.stringify(vm.nodeRoom));
            // 处理下拉框回显问题
            vm.addrList.some(item => {
                if (item.code == vm.nodeRoom.addr) {
                    vm.$set(vm.nodeRoom, "addr", item);
                    return true;
                }
            })
        },
        getCabInfo: function (cabId) {
            $.ajax({
                type: "GET",
                url: baseURL + "cab/detail?id=" + cabId,
                async: false, // 关闭异步
                dataType: "json",
                success: function (r) {
                    vm.nodeCabinet = r.data;
                }
            });
        },
        query: function (addr) {
            layer.closeAll('page');
            var page = ""; // 临时查询条件 并不存在该条件查询
            if (addr != null) {
                vm.q.addr = addr;
            }
            if (vm.q.addr != null) {
                page += "&addr=" + vm.q.addr;
            }

            if (vm.q.name != null) {
                page += "&name=" + vm.q.name;
            }

            $.ajax({
                type: "GET",
                url: baseURL + "room/list?limit=999" + page,
                async: false,
                dataType: "json",
                success: function (r) {
                    vm.roomList = r.page.list;
                }
            });
            // 查询之后刷新行列表区域
            vm.showRowList(vm.roomList[0]);
        },
        reset: function () {
            vm.q.name = vm.q.addr = null;
            this.getRooms();
            // 刷新行列表
            vm.showRowList(vm.roomList[0]);
        },
        addRoom: function () {
            layer.closeAll('page');
            vm.$v.nodeRoom.$reset(); // 进入新增页面时，将校验的触发状态清空($dirty=true -> false)
            vm.nodeRoom = {
                id: null,
                name: null,
                roomRows: null,
                roomCols: null,
                addr: null,
                remarks: null
            }
            this.open("添加", "roomForm");
        },
        addCab: function (room) { // 当前机房
            layer.closeAll('page');
            vm.$v.nodeCabinet.$reset(); // 进入添加页面  刷新校验状态
            vm.$set(vm, "readFlag", false);
            vm.nodeCabinet = {
                id: null,
                name: null,
                roomName: null,
                roomId: null,
                roomRow: null,
                roomCol: null,
                ubit: null,
                remarks: null
            }
            vm.$set(vm.nodeCabinet, "roomId", room.id);
            vm.$set(vm.nodeCabinet, "roomName", room.name);
            vm.$set(vm.nodeCabinet, "roomRow", room.row);
            vm.$set(vm.nodeCabinet, "roomCol", room.col);
            this.open("添加", "cabForm");
        },
        updateRoom: function (room) {
            vm.$v.nodeRoom.$reset();    //进入新增页面时，将校验触发状态清空($dirty=true -> false)
            this.getRoomInfo(room.id);
            this.open("修改", "roomForm");
        },
        updateCab: function (cabinet) {
            vm.$v.nodeCabinet.$reset(); //进入新增页面时，将校验触发状态清空($dirty=true -> false)
            vm.$set(vm, "readFlag", true);
            this.getCabInfo(cabinet.id);
            this.open("修改", "cabForm");
        },
        delRoom: function (room) {
            confirm("确认删除吗?", function () {
                var form = new FormData();
                form.append("ids", room.id);
                $.ajax({
                    type: "DELETE",
                    url: baseURL + "room/delete",
                    processData: false,
                    contentType: false,
                    mimeType: "multipart/form-data",
                    data: form,
                    success: function (r) {
                        if (r.code == 200) {
                            alert("成功", function () {
                                vm.reload();
                                vm.showRowList(vm.roomList[0])
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        delCab: function (cabinet) {
            confirm("确认删除吗?", function () {
                var form = new FormData();
                form.append("ids", cabinet.id);
                $.ajax({
                    type: "DELETE",
                    url: baseURL + "cab/delete",
                    async: false, // 关闭异步，否则会在执行完ajax前就跳到下一段代码
                    processData: false,
                    contentType: false,
                    mimeType: "multipart/form-data",
                    data: form,
                    success: function (r) {
                        if (r.code == 200) {
                            alert("成功", function () {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
                // 刷新
                vm.showRowList(data.room);
            });
        },
        saveOrUpdateRoom: function () {
            if (!vm.validateRoom()) { // 校验机房参数
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
                        alert("成功", function () {
                            // 刷新当前行列表区域
                            vm.showRowList(data.nodeRoom);
                            vm.reload();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        saveOrUpdateCab: function () {
            if (!vm.validateCab()) {
                return;
            }
            var url;
            if (vm.nodeCabinet.id == null) {
                url = "cab/save";
            } else {
                url = "cab/update";
            }
            $.ajax({
                type: vm.nodeCabinet.id == null ? "POST" : "PUT",
                url: baseURL + url,
                contentType: "application/json",
                async: false, // 关闭异步，否则会在执行完ajax前就跳到下一段代码
                data: JSON.stringify(vm.nodeCabinet),
                success: function (r) {
                    if (r.code === 200) {
                        alert("<@spring.message 'common.success'/>", function () {
                            // 刷新当前行列表区域
                            vm.showRowList(data.room);
                            layer.closeAll();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        reload: function () {
            var tempRoom = JSON.parse(JSON.stringify(vm.room));
            vm.query(null);
            layer.closeAll();
            vm.showRowList(tempRoom);
        },
        validateRoom: function () {
            vm.$v.nodeRoom.$touch(); // 提交时，不论是否已触发校验，都触发一下($dirty=false -> true)
            return !vm.$v.nodeRoom.$error;
        },
        validateCab: function () {
            vm.$v.nodeCabinet.$touch(); // 提交时，不论是否已触发校验，都触发一下($dirty=false -> true)
            return !vm.$v.nodeCabinet.$error;
        },
        // 展示右侧行列区
        showRowList: function (room) {
            data.room = room; // 赋值行列 页面循环
            $.ajax({
                type: "GET",
                url: baseURL + "cab/list?roomId=" + room.id,
                async: false, // 关闭异步，否则会在执行完ajax前就跳到下一段代码
                dataType: "json",
                success: function (r) {
                    if (r.code === 200) {
                        data.cabs = r.data.list;
                    } else {
                        alert(r.msg);
                    }
                }
            });
            // 每次重新初始化
            data.rowList = [];
            for (let i = 0; i < data.room.roomRows; i++) {
                data.rowList[i] = [];
                for (let j = 0; j < data.room.roomCols; j++) {
                    var flag = false;
                    data.cabs.forEach(item => {
                        if (item.roomRow - 1 == i && item.roomCol - 1 == j) {
                            // var obj = JSON.parse(JSON.stringify(item));
                            // vm.$set(data.rowList[i], j, item);
                            data.rowList[i].splice(j, 0, item);
                            flag = true;
                        }
                    });
                    if (!flag) {
                        // 点击添加机柜的时候 表单需要默认回显当前所属机房名称 行列值 在下方addcab方法中
                        //vm.$set(data.rowList[i], j, {name: data.room.name, row: i + 1, col: j + 1});
                        data.rowList[i].splice(j, 0, {id: data.room.id, name: data.room.name, row: i + 1, col: j + 1});
                    }
                }
            }
            // data.rowList = Object.assign(data.rowList, data.rowList)
            console.log(data.rowList)
        },
        open: function (title, content) {
            layer.open({
                area: ['50%', '100%'],
                shade: 0,
                title: title,
                type: 1,
                fixed: false,
                maxmin: false,
                scrollbar: false,
                offset: 'r',
                closeBtn: 1,
                move: false,
                content: $("#" + content) // 本页面的元素
            });
        },
        checkCab: function (cabinet) {
            if (cabinet.hasOwnProperty("row")) {
                return true;
            }
            return false;
        },
        getCabName: function (cabinet) {
            return cabinet.name;
        },
        showRoomOption: function (roomId) {
            $("#option" + roomId).removeClass("myoption-close").addClass("myoption-open");
        },
        closeRoomOption: function (roomId) {
            $("#option" + roomId).removeClass("myoption-open").addClass("myoption-close");
        },
        showCabOption: function (cabId) {
            if ($("#cab" + cabId).hasClass("myoption-close")) {
                $("#cab" + cabId).removeClass("myoption-close").addClass("myoption-open");
            } else {
                $("#cab" + cabId).removeClass("myoption-open").addClass("myoption-close");
            }
        }
    },
    created: function () {
        // 1. 初始化实例查询机房列表
        this.getRooms();
        this.getRoomAddrs();
        // 2.默认展开第一个机房的行列图
        this.showRowList(data.roomList[0])
    },
    mounted: function () {
        // 1.默认机房列表第一条高亮
        $("#tr" + data.roomList[0].id).removeClass("tr-normal").addClass("tr-higHlight");
    },
    watch: {
        "room": function (newValue, oldValue) {
            // 监听点击的机房  改变高亮样式
            if (oldValue != null) {
                $("#tr" + oldValue.id).removeClass("tr-higHlight").addClass("tr-normal");
            }
            if ($("#tr" + newValue.id).hasClass("tr-higHlight")) {
                return;
            } else {
                $("#tr" + newValue.id).removeClass("tr-normal").addClass("tr-higHlight");
            }
        }
    },
    computed: {}
});
</script>