<script >
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + '/node/list',
        datatype: "json",
        colModel: [
            {label: "ID", name: 'uuid', width: 60, key: true, hidden: true},
            {label: '<@spring.message "common.name"/>', name: 'name', width: 60, sortable: false},
            {label: '<@spring.message "node.hostName"/>', name: 'hostName', width: 60, sortable: false},
            {label: '<@spring.message "node.ipAddress"/>', name: 'ip', width: 60, sortable: false},
            {label: '<@spring.message "node.type"/>', name: 'type', width: 60, sortable: false, formatter: typeFormat},
            {label: '<@spring.message "node.tag"/>', name: 'tagName', width: 60, sortable: false},
            /*{label: '<@spring.message "node.cabinet.name"/>', name: 'cabinetInfo.name', width: 60, sortable: false},
            {label: '<@spring.message "node.cabinetStart"/>', name: 'cabinetStart', width: 60, sortable: false},*/
            {label: '<@spring.message "node.uSize"/>', name: 'uSize', width: 60, sortable: false},
            {label: '<@spring.message "node.snmpPort"/>', name: 'snmpPort', width: 60, sortable: false},
            {label: '<@spring.message "node.snmpCommunity"/>', name: 'snmpCommunity', width: 60, sortable: false},
            {
                label: '<@spring.message "common.onLineState"/>',
                name: 'state',
                width: 60,
                sortable: false,
                formatter: stateFormat
            },
            {label: '<@spring.message "common.remark"/>', name: 'remark', width: 60, sortable: false},
            {label: "test", name: 'state', width: 60, sortable: false, hidden: true}//此列用作获取状态值
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
            // 隐藏grid底部滚动条
            $("#jqGrid").closest(".ui-jqgrid-bdiv").css({"overflow-x": "hidden"});
        }

    });

    // type值显示转化
    function typeFormat(cellvalue, options, rowObject) {
        return cellvalue == 1 ? "server" : "net";
    }

    // state值显示转化
    function stateFormat(cellvalue, options, rowObject) {
        return cellvalue == 1 ? "<font color='green'><@spring.message 'common.onLine'/></font>" : "<font color='red'><@spring.message 'common.offLine'/></font>";
    }

});

//注册select组件
Vue.component('multiselect', window.VueMultiselect.default);

Vue.use(window.vuelidate.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-v-values
var required = window.validators.required;
var ipAddress = window.validators.ipAddress;
var numeric = window.validators.numeric;
var minValue = window.validators.minValue;
var integer = window.validators.integer;
//引入自定义校验规则助手 helpers
var helpers = window.validators.helpers;

//自定义规则，;第一个参数是当前字段值，第二个参数是当前校验的整个对象(此处是vue.data.nodeInfo)
var nameCheck = function (value, nodeInfo) {
    if (helpers.req(value)) {
        var flag = true;
        var params = "?name=" + value;
        if (nodeInfo.uuid) {
            params += "&uuid=" + nodeInfo.uuid;
        }
        $.ajax({
            type: "GET",
            url: baseURL + "node/nodeNameCheck" + params,
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
// ip 唯一校验
var ipCheck = function (value, nodeInfo) {
    if (helpers.req(value)) {
        var flag = true;
        var params = "?ip=" + value;
        if (nodeInfo.uuid) {
            params += "&uuid=" + nodeInfo.uuid;
        }
        $.ajax({
            type: "GET",
            url: baseURL + "node/ipCheck" + params,
            async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
            dataType: "json",
            success: function (r) {
                if (r.code != 200) {
                    flag = false;
                }
                if (r.data) { // 如果此IP已在其他业务系统存在
                    // $('form').find('input,select').attr('readonly',true) 或者使用 prop('readonly',true).
                }
            }
        });
        return flag;
    } else {
        return true;
    }
}
// 机柜必选状态
var cabRequired = function (value, nodeInfo) {
    if (nodeInfo.nodeRoom != null && value == null) {
        return false;
    }
    return true;
}
// 起始位置必选状态
var startRequired = function (value, nodeInfo) {
    if (nodeInfo.cabinetId != null && value == null) {
        return false;
    }
    return true;
}
// 节点U位必选状态
var sizeRequired = function (value, nodeInfo) {
    if (nodeInfo.nodeRoom != null || nodeInfo.cabinetId != null) {
        if (value == null || value == '') {
            return false;
        }
    }
    return true;
}

var data = {
    showList: true,
    title: "<@spring.message 'common.add'/>",
    nodeList: [],
    q: {
        name: null, type: null, state: null, ip: null, tag: null
    },
    nodeTypes: [], // 创建实例时从字典中获取
    nodeStates: [],// 创建实例时从字典中获取
    nodeTags: [{name: null, value: null}],
    queryTags: [],
    rooms: [],
    cabinets: [],
    starts: [],
    nodeInfo: {
        id: null,
        uuid: null,
        name: null,
        hostName: null,
        ip: null,
        type: {},
        tag: null,
        userName: null,
        password: null,
        state: null,
        cabinetId: null,
        cabinetStart: null,
        uSize: 1,
        snmpPort: null,
        snmpCommunity: null,
        remark: null,
        nodeRoom: null
    },
    nodeInfoDto: null,
    room: null,
    cab: null,
    nodeGroup: null,
    isAdvancedSearch: false
};

var vue = new Vue({
    el: '#nodeInfoApp',
    data: this.data,
    validations: {
        nodeInfo: {
            name: {
                required,
                nameCheck
            },
            ip: {
                required,
                ipAddress,
                ipCheck
            },
            type: {
                required
            },
            snmpPort: {
                numeric
            },
            state: {
                required
            },
            uSize: {
                integer,
                minValue: minValue(1),
                sizeRequired
            },
            cabinetId: {
                cabRequired
            },
            cabinetStart: {
                startRequired
            }
        }
    },
    methods: {
        query: function () {
            this.reload();
        },
        reset: function () {
            vue.q.name = vue.q.type = vue.q.state = vue.q.ip = vue.q.tag = null;
            //vue.nodeGroup = null;
            vue.query();
        },
        getInfo: function (uuid) {
            $.ajax({
                type: "GET",
                url: baseURL + "node/detail?uuid=" + uuid,
                async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
                dataType: "json",
                success: function (r) {
                    vue.nodeInfo = r.data;
                }
            });
            // 解决下拉框回显问题
            vue.nodeStates.some((item, index) => {
                if (item.code == vue.nodeInfo.state) {
                    vue.$set(vue.nodeInfo, 'state', item);
                    return true;
                }
            });
            vue.nodeTypes.some((item, index) => {
                if (item.code == vue.nodeInfo.type) {
                    vue.$set(vue.nodeInfo, 'type', item);
                    return true;
                }
            });
            var tag = JSON.parse(JSON.stringify(vue.nodeInfo.tag));
            this.getTags();
            vue.nodeTags.some((item, index) => {
                if (item.code == tag) {
                    vue.$set(vue.nodeInfo, 'tag', item);
                    return true;
                }
            });
            // 查询可用位置
            this.getRooms();

            // 回显机房
            vue.rooms.some(item => {
                if (item.roomName == vue.nodeInfo.roomName) {
                    vue.$set(vue.nodeInfo, 'nodeRoom', item);
                    vue.cabinets = item.cabinets;
                    return true;
                }
            });

            if (vue.nodeInfo.cabinetId == 0) { // 未指定机柜情况
                vue.nodeInfo.cabinetId = vue.nodeInfo.cabinetStart = null; // 赋空值 select placeholder属性 model为null显示
            }

            // 回显机柜
            vue.cabinets.some(item => {
                if (vue.nodeInfo.cabinetInfo != null && item.id == vue.nodeInfo.cabinetInfo.id) {
                    vue.$set(vue.nodeInfo, "cabinetId", item);
                    vue.starts = item.starts;
                    return true;
                }
            });
        },
        add: function () {
            vue.$v.nodeInfo.$reset(); //进入新增页面时，将校验的触发状态清空($dirty=true -> false)
            vue.showList = false;
            vue.title = "<@spring.message 'common.add'/>";
            vue.nodeInfo = {
                id: null,
                uuid: null,
                name: null,
                hostName: null,
                ip: null,
                type: null,
                tag: null,
                userName: null,
                password: null,
                state: null,
                cabinetId: null,
                cabinetStart: null,
                uSize: 1,
                snmpPort: null,
                snmpCommunity: null,
                remark: null,
                nodeRoom: null
            };
            vue.nodeInfoDto = null;
            vue.cabinets = [];
            // 重新获取可用机柜信息
            this.getRooms();
        },
        update: function () {
            vue.$v.nodeInfo.$reset(); //进入新增页面时，将校验触发状态清空($dirty=true -> false)
            var uuid = getSelectedRow();
            if (uuid == null) {
                return;
            }
            vue.showList = false;
            vue.title = "<@spring.message 'common.edit'/>";
            this.getInfo(uuid);
        },
        del: function (event) {
            var ids = getSelectedRows();
            if (ids == null) {
                return;
            }
            confirm("<@spring.message 'common.confirmDelete'/>", function () {
                var form = new FormData();
                form.append("uuids", ids);
                form.append("state", "3");
                $.ajax({
                    type: "PUT",
                    url: baseURL + "node/changeState",
                    processData: false,
                    contentType: false,
                    mimeType: "multipart/form-data",
                    data: form,
                    success: function (r) {
                        if (r.code == 200) {
                            alert("<@spring.message 'common.success'/>", function (index) {
                                vue.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        changeState: function (state) {
            var ids = getSelectedRows();
            if (ids == null) {
                return;
            }
            var statelist = []; // 定义选中状态集合
            for (let i in ids) {
                var rowData = $("#jqGrid").jqGrid('getRowData', ids[i]);
                statelist.push(rowData);
            }
            // 判断是否已经上/下线
            var flag = statelist.find(item => {
                return item.state == state;
            });
            if (flag == undefined) { // 未找到相同状态的
                var form = new FormData();
                form.append("uuids", ids);
                form.append("state", state);
                $.ajax({
                    type: "PUT",
                    url: baseURL + "node/changeState",
                    processData: false,
                    contentType: false,
                    mimeType: "multipart/form-data",
                    data: form,
                    success: function (r) {
                        if (r.code == 200) {
                            alert("<@spring.message 'common.success'/>", function (index) {
                                vue.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });
            } else {
                alert(flag.name + "  <@spring.message 'node.already'/>  " + (state == 1 ? "<@spring.message 'common.onLine'/>" : "<@spring.message 'common.offLine'/>"));
            }
        },
        saveOrUpdate: function () {
            if (!vue.validate()) { //在下方定义的validate方法
                return;
            }
            var url;
            if (vue.nodeInfo.uuid == null) {
                url = "node/save";
            } else {
                url = "node/update";
            }
            vue.nodeInfoDto = JSON.parse(JSON.stringify(vue.nodeInfo));
            vue.$set(vue.nodeInfoDto, 'type', vue.nodeInfo.type.code);
            vue.$set(vue.nodeInfoDto, 'tag', vue.nodeInfo.tag == null ? null : vue.nodeInfo.tag.code);
            vue.$set(vue.nodeInfoDto, 'state', vue.nodeInfo.state.code);
            vue.$set(vue.nodeInfoDto, 'cabinetId', vue.nodeInfo.cabinetId == null ? null : vue.nodeInfo.cabinetId.id);
            $.ajax({
                type: vue.nodeInfo.uuid == null ? "POST" : "PUT",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vue.nodeInfoDto),
                success: function (r) {
                    if (r.code === 200) {
                        alert("<@spring.message 'common.success'/>", function (index) {
                            vue.reload();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        reload: function () {
            vue.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {
                    "name": vue.q.name,
                    "ip": vue.q.ip,
                    "type": vue.q.type == null ? null : vue.q.type.code,
                    "tag": vue.q.tag == null ? null : vue.q.tag.code,
                    "state": vue.q.state == null ? null : vue.q.state.code,
                    "groupId": vue.nodeGroup == null ? null : vue.nodeGroup.id
                },
                page: page
            }).trigger("reloadGrid");
        },
        getTags: function () {
            // 清空缓存
            vue.nodeInfo.tag = null;
            if (vue.nodeInfo.type != null) {
                var param = "?type=" + vue.nodeInfo.type.value;
                $.ajax({
                    type: "GET",
                    url: baseURL + "sys/dict/list" + param,
                    async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
                    dataType: "json",
                    success: function (r) {
                        vue.nodeTags = r.page.list;
                    }
                });
            }
            if (vue.nodeInfo.type != null && vue.nodeInfo.type.code == 2) {
                vue.nodeInfo.snmpPort = 161;
                vue.nodeInfo.snmpCommunity = "public";
            } else {
                vue.nodeInfo.snmpPort = vue.nodeInfo.snmpCommunity = null;
            }
        },
        getRooms: function () {
            var params = "?uSize=" + data.nodeInfo.uSize;
            if (data.nodeInfo.uuid != null) {
                params += "&uuid=" + data.nodeInfo.uuid;
            }
            $.ajax({
                type: "GET",
                url: baseURL + "node/selectCab" + params,
                async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
                dataType: "json",
                success: function (r) {
                    vue.rooms = r.data.list;
                }
            });
        },
        getCabinets: function (event) { // 获取机房下机柜列表
            // 没有切换机房的情况
            if (vue.nodeInfo.nodeRoom != null && event != null && vue.nodeInfo.nodeRoom.roomId == event.roomId
            ) {
                return;
            }
            vue.nodeInfo.cabinetId = vue.nodeInfo.cabinetStart = null;
            vue.cabinets = vue.starts = [];
            if (vue.rooms != null && vue.nodeInfo.nodeRoom != null) {
                vue.rooms.some(item => {
                    if (vue.nodeInfo.nodeRoom.roomName == item.roomName) {
                        vue.cabinets = item.cabinets;
                        return true;
                    }
                });
            }
        },
        getStarts: function (event) {  // 获取机柜下可用的起始位置
            // 没有切换机柜的情况
            if (vue.nodeInfo.cabinetId != null && event != null && vue.nodeInfo.cabinetId.id == event.id) {
                return;
            }
            if (vue.nodeInfo.uSize == null || vue.nodeInfo.uSize == "") {
                return;
            }
            vue.nodeInfo.cabinetStart = null;
            vue.starts = [];
            vue.cabinets.some((item, index) => {
                if (vue.nodeInfo.cabinetId != null && vue.nodeInfo.cabinetId.name == item.name) {
                    vue.starts = item.starts;
                    return true;
                }
            });
        },
        getQueryTag: function () {
            vue.queryTags = [];
            vue.q.tag = null;
            if (vue.q.type != null) {
                var param = "?type=" + vue.q.type.value;
                $.ajax({
                    type: "GET",
                    url: baseURL + "sys/dict/list" + param,
                    async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
                    dataType: "json",
                    success: function (r) {
                        vue.queryTags = r.page.list;
                    }
                });
            }
        },
        checkUsize: function () {
            if (vue.nodeInfo.uSize == null || vue.nodeInfo.uSize == '') { // 节点高度为null
                alert("<@spring.message 'node.pleaseWrite.uSize'/>!");
            }
        },
        checkNodeRoom: function () {
            if (vue.nodeInfo.uSize == null || vue.nodeInfo.uSize == '') { // 节点高度为null
                alert("<@spring.message 'node.pleaseWrite.uSize'/>!");
                return;
            }
            if (vue.nodeInfo.uSize != null && vue.nodeInfo.nodeRoom == null) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.room.name'/>!");
            }
        },
        checkCabStart: function () {
            if (vue.nodeInfo.uSize == null || vue.nodeInfo.uSize == '') { // 节点高度为null
                alert("<@spring.message 'node.pleaseWrite.uSize'/>!");
            } else if (vue.nodeInfo.nodeRoom == null) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.room.name'/>!");
            } else if (vue.nodeInfo.nodeRoom != null && vue.nodeInfo.cabinetId == null) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.cabinet.name'/>!");
            } else if (vue.nodeInfo.nodeRoom != null && vue.nodeInfo.cabinetId != null && vue.starts.length < 1) {
                // 暂无位置可选
                alert(vue.nodeInfo.cabinetId.name + "  <@spring.message 'node.notEnough.position'/>!");
            }
        },
        checkNodeType: function () {
            if (vue.nodeInfo.type == null) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.type'/>!");
            }
        },
        checkQueryTag: function () {
            if (vue.q.type == null) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.type'/>!");
            }
        },
        validate: function () {
            vue.$v.nodeInfo.$touch();
            return !vue.$v.nodeInfo.$error;
        },
        advancedSearch: function () { //高级搜索
            this.isAdvancedSearch = !this.isAdvancedSearch;
        }
    },
    created: function () {
        // 从字典中获取节点类型的集合
        $.getJSON(baseURL + "sys/dict/all?type=nodeType", function (r) {
            vue.nodeTypes = r.data;
        });
        $.getJSON(baseURL + "sys/dict/all?type=onLineState", function (r) {
            vue.nodeStates = r.data;
        });
    },
    watch: {
        "nodeInfo.uSize": function (newValue, oldValue) {
            // 1. 没有填写USize 不处理,返回
            if (newValue == '' || newValue == null) {
                vue.nodeInfo.cabinetId == null ? null : vue.nodeInfo.cabinetId.starts = [];
                vue.starts = [];
                vue.nodeInfo.cabinetStart = null;
                vue.$v.nodeInfo.uSize.$reset();
                vue.$v.nodeInfo.cabinetId.$reset()
                vue.$v.nodeInfo.cabinetStart.$reset();
                if (vue.nodeInfo.nodeRoom == null) { // 既没有指定uSize 当前也没有指定room
                    vue.rooms = [];
                }
                return;
            }
            // 2. 没有选择机房 不处理
            /*if (vue.nodeInfo.nodeRoom == null) {
                return;
            }*/
            // 3. 开始处理数据变化  先清空起始位置
            vue.starts = [];
            // 4.重新发起请求
            var params = "?uSize=" + newValue;
            if (this.nodeInfo.uuid) {
                params += "&uuid=" + this.nodeInfo.uuid;
            }
            // 获取数据
            $.ajax({
                type: "GET",
                url: baseURL + "node/selectCab" + params,
                async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
                dataType: "json",
                success: function (r) {
                    vue.rooms = r.data.list;
                }
            });

            // 5.处理变化数据
            if (vue.rooms == null) { // 新的uSize 没有位置可选
                vue.nodeInfo.cabinetStart = null;
            } else { // 存在机房列表 有可选位置存在
                var cab;
                vue.rooms.some(room => {
                    cab = room.cabinets.find(cabinet => {
                        if (vue.nodeInfo.cabinetId != null && cabinet.id == vue.nodeInfo.cabinetId.id) { // 如果机柜存在
                            return cabinet;
                        }
                    });
                    if (cab != undefined) { // 旧机柜仍然存在
                        return true; // 结束循环
                    }
                })

                // 更新cabs starts
                vue.starts = cab == undefined ? [] : cab.starts;
                vue.rooms.some(item => {
                    if (vue.nodeInfo.nodeRoom != null && item.roomId == vue.nodeInfo.nodeRoom.roomId) {
                        vue.cabinets = item.cabinets;
                        return true;
                    }
                });
            }

            // 6. 深克隆对象 用作修改时的cabinetStart 回显问题
            var cloneNodeInfo = JSON.parse(JSON.stringify(vue.nodeInfo));
            vue.nodeInfo.cabinetStart = null;
            var flag = false;
            vue.starts.some(item => {
                if (item == cloneNodeInfo.cabinetStart) {
                    flag = true;
                    return flag;
                }
            })
            if (vue.nodeInfo.uuid != null && vue.starts.length > 0 && flag) {
                vue.nodeInfo.cabinetStart = cloneNodeInfo.cabinetStart;
            }
        }
    },
    computed: {}
});
</script>