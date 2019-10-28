<script >
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + '/node/list',
        datatype: "json",
        colModel: [
            {label: "ID", name: 'uuid', width: 60, key: true, hidden: true},
            {label: '<@spring.message "common.name"/>', name: 'name', width: 60, sortable: false},
            {label: '<@spring.message "node.hostName"/>', name: 'hostName', width: 60, sortable: false},
            {label: '<@spring.message "node.ipAddress"/>', name: 'ip', width: 105, sortable: false, resizable: false},
            {label: '<@spring.message "node.type"/>', name: 'type', width: 60, sortable: false, formatter: typeFormat},
            {label: '<@spring.message "node.tag"/>', name: 'tagName', width: 60, sortable: false},
            /*{label: '<@spring.message "node.cabinet.name"/>', name: 'cabinetInfo.name', width: 60, sortable: false},
            {label: '<@spring.message "node.cabinetStart"/>', name: 'cabinetStart', width: 60, sortable: false},*/
            {label: '<@spring.message "node.uSize"/>', name: 'uSize', width: 60, sortable: false},
            {label: '<@spring.message "node.snmpPort"/>', name: 'snmpPort', width: 60, sortable: false},
            {label: '<@spring.message "node.snmpCommunity"/>', name: 'snmpCommunity', width: 60, sortable: false},
            {
                label: '<@spring.message "common.onLineState"/>',
                name: 'state', width: 60, sortable: false, formatter: stateFormat
            },
            {label: '<@spring.message "common.remark"/>', name: 'remark', width: 60, sortable: false},
            {label: "test", name: 'state', width: 60, sortable: false, hidden: true}//此列用作获取状态值
        ],
        viewrecords: true,
        height: typeof(isGroup) != "undefined" && isGroup ? top.$(".content").height()-167 : top.$(".content").height()-130,
        rowNum: 15,
		rowList : [15,30,50],
        /*height: 385,
        rowNum: 10,
        rowList: [10, 30, 50],*/
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
        },
        onSelectAll: function(rowids, status) {
            if (status) {
                toolsBtnHandle();
            }
        },
        onSelectRow: function(rowid,status) {
            toolsBtnHandle();
        }

    });

    // type值显示转化
    function typeFormat(cellvalue, options, rowObject) {
        return cellvalue == 1 ? "<@spring.message 'node.type.server'/>" : "<@spring.message 'node.type.net'/>";
    }

    // state值显示转化
    function stateFormat(cellvalue, options, rowObject) {
        return cellvalue == 1 ? "<font color='green'><@spring.message 'common.onLine'/></font>" : "<font color='red'><@spring.message 'common.offLine'/></font>";
    }

});

var groupTree1 = Vue.component('groupTree1', {
    template: "#treeTemplate",
    props: {
        znodes: { // 只包含用于生成树形结构的节点组列表
            type: Array
        },
        selectedNodeId: "",
        node: null     // 代表节点信息 包含业务系统和包含的节点组
    },
    data() {
        return {
            selectedNode: {},
            setting: {
                view: {
                    addHoverDom: this.addHoverDom,
                    removeHoverDom: this.removeHoverDom,
                    showIcon: false,
                    nameIsHTML: true // 名称支持Html标签转换
                },
                edit: {
                    enable: true,
                    removeTitle: "<@spring.message 'common.delete'/>",
                    renameTitle: "<@spring.message 'common.edit'/>"
                },
                data: {
                    simpleData: {
                        enable: true,
                        idKey: "id",
                        pIdKey: "pid",
                        rootPId: 0
                    },
                    key:{
                        title:"nodeTitle" // 因为name 拼接了带有html的字段 所以默认的title 会展示html标签
                    }
                },
                check: {
                    enable: true,
                    checkStyle: "checkbox",
                    nocheckInherit: true,
                    chkboxType: {"Y": "", "N": ""}
                },
                callback:{
                    beforeRemove: this.beforeRemove,         // 删除callback
                    beforeCheck: this.beforeCheck,           // 修改callback
                    beforeEditName: this.beforeEditName,     // 点击编辑时触发，用来判断该节点是否能编辑
                    onCheck: this.zTreeOnCheck               // 选中时触发
                }
            },
            zTree:null      // 当前ztreeobject
        };
    },
    methods: {
        // 初始化ztree
        init() {
            // 获取当前的集合
            this.znodes.forEach(item => {
                this.$set(item,"nodeTitle",item.name);
                if(item.allowAss == 1){ // 证明是叶子节点
                    var count = "";
                    $.ajax({
                        type: "GET",
                        url: baseURL + "nodegroup/getNodeCount?id="+item.id,
                        async: false,
                        dataType: "json",
                        success: function (r) {
                            count = r.data;
                        }
                    });
                    this.$set(item,"name",item.name + "     <font color='gray'>("+ count +")</font>");
                }
            });
            // 生成树结构
            this.zTree = $.fn.zTree.init($("#treeDemo" + this.node.id), this.setting, this.znodes);
            this.zTree.expandAll(true);
        },
        // 删除
        beforeRemove: function (treeId, treeNode) {
            var parent = this.$parent;
            confirm("<@spring.message 'common.confirmDelete'/>", function () {
                parent.deleteNodeGroup(treeNode);
            });
            return false;
        },
        // 鼠标移除删除添加按钮
        removeHoverDom: function (treeId, treeNode) {
            $("#addBtn_" + treeNode.tId).unbind().remove();
        },
        addRootGroup: function () {
            // 将数据传至父组件 也就是外面的vm 实例中进行添加操作
             this.$emit('addrootgroup', this.node, true);
        },
        // 鼠标划过展示功能按钮
        addHoverDom: function (treeId, treeNode) {
            var parent = this.$parent;
            var sObj = $("#" + treeNode.tId + "_span");
            if (treeNode.editNameFlag || $("#addBtn_" + treeNode.tId).length > 0 || treeNode.allowSub == 0) { // 可以让不可添加的按钮隐藏
                return;
            }
            // 添加子节点组
            var addStr = "<span class='button add' id='addBtn_" + treeNode.tId + "' title='<@spring.message 'common.add'/><@spring.message 'nodegroup.child.group'/>' onfocus='this.blur();'></span>";
            sObj.append(addStr);
            var btn = $("#addBtn_" + treeNode.tId);
            if (btn) btn.bind("click", function () {
                parent.addNodeGroup(treeNode, false);
                return false; // 结束方法
            });
        },
        // 鼠标移除删除添加按钮
        removeHoverDom: function (treeId, treeNode) {
            $("#addBtn_" + treeNode.tId).unbind().remove();
        },
        beforeEditName: function (treeId, treeNode) {   // 修改
            this.$parent.updateNodeGroup(treeNode);
            return false;
        },
        beforeCheck:function(treeId, treeNode){          // 选择前判断是否可选
            if(treeNode.allowAss == "1"){ // 允许选择
                return true;
            }
            layer.msg("此节点组不支持分配节点");
            return false;
        },
        zTreeOnCheck:function(event, treeId, treeNode){
            if(treeNode.checked){
                // 节点页面展示业务系统名
                treeNode.systemName = this.node.name;
                // 节点页面展示节点组层级路径
                treeNode.absoluteGroupPath = this.getNodePath(treeNode);
                this.$emit("tree-select", treeNode);
            }else{
                this.$emit("tree-unselect", treeNode);
            }
        },
        forUpdateNode:function(nodeIds){ // 修改时调用此方法
            var temp = this;
            nodeIds.forEach(item=>{
                var nodeObj = this.zTree.getNodesByParam("id", item.groupId, null);
                if(nodeObj.length >0){
                    // 1. 将节点组名称层级关系传递父组件
                    var currNode = nodeObj[0];
                    currNode.systemName = this.node.name;
                    currNode.absoluteGroupPath = temp.getNodePath(currNode);
                    temp.$emit("tree-select", currNode);
                    // 2. 节点状态选中
                    currNode.checked = true;
                    this.zTree.updateNode(currNode, true);
                }
            });
        },
        getNodePath: function(treeNode){ // 递归拼接节点组层级关系
            if(treeNode==null)return "";
            var nodeName = treeNode.nodeTitle;
            var pNode = treeNode.getParentNode();
            if(pNode!=null){
                nodeName = this.getNodePath(pNode) +"/"+ nodeName;
            }
            return nodeName;
        },
        uncheckTreeData:function(node){
            if(node.systemId != this.node.id){
                return;
            }else{
                this.showCurrTree(this.node);
            }
            this.zTree.checkNode(node,false,true);
        },
        myFilter:function(node){
           // return (node.showCurrTree != undefined);
        },
        // 一下是 展示/样式相关方法
        showTree: function (item) {
            $("#box" + item.id).toggle(200);
            if ($("#icon" + item.id).hasClass("mytreeicon-open")) {
                $("#icon" + item.id).removeClass("mytreeicon-open").addClass("mytreeicon-close");
            } else {
                $("#icon" + item.id).removeClass("mytreeicon-close").addClass("mytreeicon-open");
            }
        },
        showCurrTree:function(item){
            $("#box" + item.id).show(200);
        },
        showBtn: function (item) {
            $("#icon2" + item.id).css("display", "inline-block");
        },
        hideBtn: function (item) {
            $("#icon2" + item.id).hide();
        },
        showBtn2: function (item) {
            $("#icon2" + item.id).removeClass("mytreeicon-add_unact").addClass("mytreeicon-add_act");
        },
        hideBtn2: function (item) {
            $("#icon2" + item.id).removeClass("mytreeicon-add_act").addClass("mytreeicon-add_unact");
        },
    },
    watch: {
        // 监听树节点的变化更新树
       /*znodes: function (val) {
           this.init();
       }*/
    },
    mounted() {
        console.log("mounted 重新挂载")
         this.init();
        // 如果是global 下隐藏树
        var currSys;
        $.ajax({
            type: "GET",
            url: baseURL + "system/detail",
            async: false,
            dataType: "json",
            success: function (r) {
                currSys = r.data;
            }
        });
        if (currSys.id > 1) { // 当前不是global
            $("#box" + this.node.id).show();
        }else{
            $("#box" + this.node.id).hide();
        }
    }
});

// 注册select组件
Vue.component('multiselect', window.VueMultiselect.default);

// 引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-v-values
Vue.use(window.vuelidate.default);
var required = window.validators.required;
var ipAddress = window.validators.ipAddress;
var numeric = window.validators.numeric;
var minValue = window.validators.minValue;
var integer = window.validators.integer;
// 引入自定义校验规则助手 helpers
var helpers = window.validators.helpers;

// 自定义规则，;第一个参数是当前字段值，第二个参数是当前校验的整个对象(此处是vue.data.nodeInfo)
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
            async: false,
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
// ip 唯一校验   特殊IP : IP存在与其他业务系统 则回显该节点数据 点击保存维护与当前系统的关系
var ipCheck = function (value, nodeInfo) {

    if (helpers.req(value)) {
        var flag = true;
        var params = "?ip=" + value;
        if (nodeInfo.uuid) {
            params += "&uuid=" + nodeInfo.uuid;
        }

        // IP 标识
        if (vm.ipFlag) {
            vm.oldNodeInfos.splice(0, 1, JSON.parse(JSON.stringify(vm.nodeInfo))); // 记录当前用户输入的内容
        } else {
            vm.oldNodeInfos.splice(1, 1, JSON.parse(JSON.stringify(vm.nodeInfo))); // 记录特殊IP对象数据
        }

        $.ajax({
            type: "GET",
            url: baseURL + "node/ipCheck" + params,
            async: false,
            dataType: "json",
            success: function (r) {
                if (r.code != 200) {
                    flag = false;
                }
                if (r.data) { // 存在data 则证明此IP存在于其他业务系统
                    vm.ipFlag = false;
                    var data = r.data;
                    // 页面挂载对象赋值
                    vm.$set(vm.nodeInfo, 'name', data.name);
                    vm.$set(vm.nodeInfo, 'hostName', data.hostName);
                    vm.$set(vm.nodeInfo, 'uSize', data.uSize);
                    vm.$set(vm.nodeInfo, 'userName', data.userName);
                    vm.$set(vm.nodeInfo, 'snmpPort', data.snmpPort);
                    vm.$set(vm.nodeInfo, 'snmpCommunity', data.snmpCommunity);
                    vm.$set(vm.nodeInfo, 'password', data.password);
                    vm.$set(vm.nodeInfo, 'remark', data.remark);

                    // 回显state
                    vm.nodeStates.some(item => {
                        if (item.code == data.state) {
                            vm.$set(vm.nodeInfo, 'state', item);
                            return true;
                        }
                    });
                    // 回显type
                    vm.nodeTypes.some(item => {
                        if (item.code == data.type) {
                            vm.$set(vm.nodeInfo, 'type', item);
                            return true;
                        }
                    });
                    // 深克隆tag 接下来的方法会影响tag的值
                    var tag = JSON.parse(JSON.stringify(data.tag));
                    vm.getTags(); // 动态获取Tags
                    // 回显tag
                    vm.nodeTags.some((item, index) => {
                        if (item.code == tag) {
                            vm.$set(vm.nodeInfo, 'tag', item);
                            return true;
                        }
                    });
                    // 查询可用位置
                    vm.getRooms(data.uuid);
                    // 回显机房
                    vm.rooms.some(item => {
                        if (item.roomName == data.roomName) {
                            vm.$set(vm.nodeInfo, 'nodeRoom', item);
                            vm.cabinets = item.cabinets;
                            return true;
                        }
                    });

                    if (data.cabinetId == 0 || data.cabinetId == null) { // 未指定机柜情况
                        vm.nodeInfo.cabinetId = vm.nodeInfo.cabinetStart = null; // 赋空值 select placeholder属性 model为null显示
                    }
                    // 回显机柜
                    vm.cabinets.some(item => {
                        if (data.cabinetInfo != null && item.id == data.cabinetInfo.id) {
                            vm.$set(vm.nodeInfo, "cabinetId", item);
                            vm.starts = item.starts;
                            return true;
                        }
                    });
                    // 回显起始位置
                    vm.$set(vm.nodeInfo, 'cabinetStart', data.cabinetStart);

                    // 将表单锁定 除却 IP
                    $('form').find('input').not("#ipAddress").prop('readonly', true);
                    vm.disabledFlag = true;
                    vm.$v.nodeInfo.$reset(); // 递归清空校验状态
                } else {
                    vm.ipFlag = true;
                    // 拿到用户输入的表单数据
                    var back = vm.oldNodeInfos[0];

                    // 给挂载对象回显  为null 则不回显
                    vm.$set(vm.nodeInfo, 'name', back.name);
                    vm.$set(vm.nodeInfo, 'hostName', back.hostName);
                    vm.$set(vm.nodeInfo, 'uSize', back.uSize);
                    vm.$set(vm.nodeInfo, 'snmpPort', back.snmpPort);
                    vm.$set(vm.nodeInfo, 'snmpCommunity', back.snmpCommunity);
                    vm.$set(vm.nodeInfo, 'userName', back.userName);
                    vm.$set(vm.nodeInfo, 'password', back.password);
                    vm.$set(vm.nodeInfo, 'remark', back.remark);

                    // 从新根据条件查询 下拉框数据 并回显数据
                    if (back.state != null) {
                        vm.nodeStates.some(item => {
                            if (item.code == back.state.code) {
                                vm.$set(vm.nodeInfo, 'state', item);
                                return true;
                            }
                        });
                    } else {
                        vm.$set(vm.nodeInfo, 'state', null);
                    }
                    if (back.type != null) {
                        vm.nodeTypes.some((item, index) => {
                            if (item.code == back.type.code) {
                                vm.$set(vm.nodeInfo, 'type', item);
                                return true;
                            }
                        });
                    } else {
                        vm.$set(vm.nodeInfo, 'type', null);
                    }
                    vm.getTags();
                    if (back.tag != null) {
                        vm.nodeTags.some(item => {
                            if (item.code == back.tag.code) {
                                vm.$set(vm.nodeInfo, 'tag', item);
                                return true;
                            }
                        });
                    } else {
                        vm.$set(vm.nodeInfo, 'tag', null);
                    }
                    // 查询可用位置
                    if (back.nodeRoom != null) {
                        // 回显机房
                        vm.rooms.some(item => {
                            if (item.roomName == back.nodeRoom.roomName) {
                                vm.$set(vm.nodeInfo, 'nodeRoom', item);
                                vm.cabinets = item.cabinets;
                                return true;
                            }
                        });
                    } else {
                        vm.$set(vm.nodeInfo, 'nodeRoom', null);
                    }
                    if (back.cabinetId == 0 || back.cabinetId == null) { // 未指定机柜情况
                        vm.nodeInfo.cabinetId = vm.nodeInfo.cabinetStart = null; // 赋空值 select placeholder属性 model为null显示
                    } else {
                        // 回显机柜
                        vm.cabinets.some(item => {
                            if (item.id == back.cabinetId.id) {
                                vm.$set(vm.nodeInfo, "cabinetId", item);
                                vm.starts = item.starts;
                                return true;
                            }
                        });
                    }
                    vm.$set(vm.nodeInfo, 'cabinetStart', back.cabinetStart);

                    // 将表单恢复可写模式
                    $('form').find('input').prop('readonly', false);
                    vm.disabledFlag = false;
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
    if (nodeInfo.nodeRoom != null && value == null) {
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
// 节点组名称校验
var groupNameCheck = function (value, nodeGroup) {
    if (helpers.req(value)) {
        var flag = true;
        var params = "?name=" + value + "&pid=" + vm.nodeGroupDto.parentGroup.id;
        if (nodeGroup.id) {
            params += "&id=" + nodeGroup.id;
        }
        if (nodeGroup.parentGroup.id == 0) { // 添加的是根节点 则参数需要传业务ID
            params += "&systemId=" + vm.system.id;
        }
        $.ajax({
            type: "GET",
            url: baseURL + "nodegroup/groupNameCheck" + params,
            async: false,
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

var data = {
    showList: true,      // 节点列表的显示与隐藏
    showGroupTree:true,  // nodeGroups tree 展示与隐藏
    showGroupForm:true,  // nodeGroup  form 展示与隐藏
    title: "<@spring.message 'common.add'/>",
    cabStartTitle:"<@spring.message 'common.pleaseSelect'/>",
    uSizeValidateFlag:false,   // 节点高度提示信息标识
    nodeList: [],
    q: {
        name: null, type: null, state: null, ip: null, tag: null
    },
    nodeTypes: [], // 创建实例时从字典中获取
    nodeStates: [],// 创建实例时从字典中获取
    nodeTags: [{name: null, value: null}], // 添加页面tags
    queryTags: [],  // 查询tags 用于与添加页面tags 区分
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
    nodeGroup: null,         // 节点组页面搜索使用
    nodeGroupDto:{           // 节点添加选择节点组  表单绑定对象
        name: null,
        id: null,
        systemId: null,
        pid: null,
        remark: null,
        parentGroup: {
            id: null,
            name: null
        }
    },
    isAdvancedSearch: false, // 高级搜索状态控制
    disabledFlag: false,     // 表单锁定状态控制
    oldNodeInfos: [],        // 增加/修改 回显旧对象数组
    ipFlag: true, // IP 标识 存在其他业务系统则为 true,
    showIp: true,
    selectFlag1: false,
    selectItems1: ["IP", "<@spring.message 'common.name'/>"],
    selectedItem1: "IP",
    // single  multi 控制工具栏单选 多选可点击状态
    single:true,
    multi:true,
    systems: null,  // 业务系统和节点组集合
    system: {       // 当前业务系统  节点组添加修改时使用
        id: null,
        name: null
    },
    currentZIndex:null,  // 当前弹出层zIndex值
    checkedGroups:[],    // 当前节点选中的节点组   数据为ztree 节点数据 包含ztree node 属性
    currSystem: null,     // 当前业务系统ID
};

Vue.config.devtools = true;
var vm = new Vue({
    el: '#nodeInfoApp',
    data: this.data,
    validations: {
        nodeInfo: {
            name: {
                required, nameCheck
            },
            ip: {
                required, ipAddress, ipCheck
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
                integer, minValue: minValue(1), sizeRequired
            },
            cabinetId: {
                cabRequired
            },
            cabinetStart: {
                startRequired
            }
        },
        nodeGroupDto: {
            name: {
                required, groupNameCheck
            }
        }
    },
    methods: {
        query: function () {
            this.reload();
        },
        reset: function () {
            vm.q.name = vm.q.type = vm.q.state = vm.q.ip = vm.q.tag = null;
            vm.query();
        },
        getInfo: function (uuid) {
            $.ajax({
                type: "GET",
                url: baseURL + "node/detail?uuid=" + uuid,
                async: false,
                dataType: "json",
                success: function (r) {
                    vm.nodeInfo = r.data;
                }
            });
            // 解决下拉框回显问题
            vm.nodeStates.some((item, index) => {
                if (item.code == vm.nodeInfo.state) {
                    vm.$set(vm.nodeInfo, 'state', item);
                    return true;
                }
            });
            vm.nodeTypes.some((item, index) => {
                if (item.code == vm.nodeInfo.type) {
                    vm.$set(vm.nodeInfo, 'type', item);
                    return true;
                }
            });
            var tag = JSON.parse(JSON.stringify(vm.nodeInfo.tag));
            this.getTags();
            vm.nodeTags.some((item, index) => {
                if (item.code == tag) {
                    vm.$set(vm.nodeInfo, 'tag', item);
                    return true;
                }
            });
            // 查询可用位置
            vm.getRooms();

            // 回显机房
            vm.rooms.some(item => {
                if (item.roomName == vm.nodeInfo.roomName) {
                    vm.$set(vm.nodeInfo, 'nodeRoom', item);
                    vm.cabinets = item.cabinets;
                    return true;
                }
            });

            if (vm.nodeInfo.cabinetId == 0) { // 未指定机柜情况
                vm.nodeInfo.cabinetId = vm.nodeInfo.cabinetStart = null; // 赋空值 select placeholder属性 model为null显示
            }

            // 回显机柜
            vm.cabinets.some(item => {
                if (vm.nodeInfo.cabinetInfo != null && item.id == vm.nodeInfo.cabinetInfo.id) {
                    vm.$set(vm.nodeInfo, "cabinetId", item);
                    vm.starts = item.starts;
                    return true;
                }
            });
        },
        add: function () {
            vm.$v.nodeInfo.$reset(); // 进入新增页面时，将校验的触发状态清空($dirty=true -> false)
            // 表单状态可写模式
            $('form').find('input').prop('readonly', false);
            vm.disabledFlag = false;
            vm.showList = false;
            vm.title = "<@spring.message 'common.add'/>";
            vm.nodeInfo = {
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
            vm.nodeInfoDto = null;
            vm.cabinets = [];
            // 重新获取可用机柜信息
            this.getRooms();
            // 初始化 节点组信息
            vm.checkedGroups = [];
            // 默认在线状态为上线
            vm.nodeStates.some(item=>{
                if(item.code == 1){
                    vm.nodeInfo.state = item;
                    return true;
                }
            })
        },
        update: function () {
            vm.$v.nodeInfo.$reset(); // 进入修改页面时，将校验触发状态清空($dirty=true -> false)
            // 表单状态可写模式
            $('form').find('input').prop('readonly', false);
            vm.disabledFlag = false;
            var uuid = getSelectedRow();
            if (uuid == null) {
                return;
            }
            vm.showList = false;
            vm.checkedGroups = [];
            vm.title = "<@spring.message 'common.edit'/>";
            // 查询详情
            this.getInfo(uuid);
            // 查询是否关联节点组
            this.getNodeGroupsById(uuid);
        },
        del: function (event) {
            var ids = getSelectedRows();
            if (ids == null) {
                return;
            }
            confirm("<@spring.message 'common.confirmDelete'/>", function () {
                // update / del 表单方式提交
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
                                vm.reload();
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
                                vm.reload();
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
            if (!vm.disabledFlag) {   // 表单没有锁定
                if (!vm.validate()) { // 校验表单内属性
                    return;
                }
            }
            var url;
            if (vm.nodeInfo.uuid == null) {
                url = "node/save";
            } else {
                url = "node/update";
            }
            vm.nodeInfoDto = JSON.parse(JSON.stringify(vm.nodeInfo));
            vm.$set(vm.nodeInfoDto, 'type', vm.nodeInfo.type.code);
            vm.$set(vm.nodeInfoDto, 'tag', vm.nodeInfo.tag == null ? null : vm.nodeInfo.tag.code);
            vm.$set(vm.nodeInfoDto, 'state', vm.nodeInfo.state.code);
            vm.$set(vm.nodeInfoDto, 'cabinetId', vm.nodeInfo.cabinetId == null ? null : vm.nodeInfo.cabinetId.id);
            if(vm.checkedGroups.length > 0){
                var groupIds = vm.checkedGroups.map(function(item) {
                    return item.id;
                });
                vm.$set(vm.nodeInfoDto , 'groupIds', groupIds);
            }
            console.log(vm.nodeInfoDto)
           $.ajax({
                type: vm.nodeInfo.uuid == null ? "POST" : "PUT",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.nodeInfoDto),
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
        reload: function () {
            vm.single = vm.multi = true;
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {
                    "name": vm.q.name,
                    "ip": vm.q.ip,
                    "type": vm.q.type == null ? null : vm.q.type.code,
                    "tag": vm.q.tag == null ? null : vm.q.tag.code,
                    "state": vm.q.state == null ? null : vm.q.state.code,
                    "groupId": vm.nodeGroup == null ? null : vm.nodeGroup.id
                },
                page: 1
            }).trigger("reloadGrid");
            layer.closeAll();
        },
        getTags: function () {
            // 清空缓存
            vm.nodeInfo.tag = null;
            if (vm.nodeInfo.type != null) {
                // 字典中tag type属性命名为 nodeTag + server (1) / net (2)
                var param = "?type=" + "nodeTag" + vm.nodeInfo.type.code;
                $.ajax({
                    type: "GET",
                    url: baseURL + "sys/dict/list" + param,
                    async: false,
                    dataType: "json",
                    success: function (r) {
                        vm.nodeTags = r.page.list;
                    }
                });
            }
            if (vm.nodeInfo.type != null && vm.nodeInfo.type.code == 2) {
                vm.$set(vm.nodeInfo, 'snmpPort', vm.nodeInfo.snmpPort == null ? 161 : vm.nodeInfo.snmpPort);
                vm.$set(vm.nodeInfo, 'snmpCommunity', vm.nodeInfo.snmpCommunity == null ? "public" : vm.nodeInfo.snmpCommunity);
            } else {
                vm.nodeInfo.snmpPort = vm.nodeInfo.snmpCommunity = null;
            }
        },
        getRooms: function (uuid) {
            var params = "?uSize=" + data.nodeInfo.uSize;
            if (data.nodeInfo.uuid != null) {
                params += "&uuid=" + data.nodeInfo.uuid;
            }
            if (uuid != null) {
                params += "&uuid=" + uuid;
            }
            $.ajax({
                type: "GET",
                url: baseURL + "node/selectCab" + params,
                async: false,
                dataType: "json",
                success: function (r) {
                    vm.rooms = r.data.list;
                }
            });
        },
        getCabinets: function (event) { // 获取机房下机柜列表
            if (vm.nodeInfo.nodeRoom != null && event != null && vm.nodeInfo.nodeRoom.roomId == event.roomId) { // 没有切换机房的情况
                return;
            }
            vm.nodeInfo.cabinetId = vm.nodeInfo.cabinetStart = null;
            vm.cabinets = vm.starts = [];
            if (vm.rooms != null && vm.nodeInfo.nodeRoom != null) {
                vm.rooms.some(item => {
                    if (vm.nodeInfo.nodeRoom.roomName == item.roomName) {
                        vm.cabinets = item.cabinets;
                        return true;
                    }
                });
            }
        },
        getStarts: function (event) {  // 获取机柜下可用的起始位置
            if (vm.nodeInfo.cabinetId != null && event != null && vm.nodeInfo.cabinetId.id == event.id) { // 没有切换机柜的情况
                return;
            }
            if (vm.nodeInfo.uSize == null || vm.nodeInfo.uSize == "") {
                return;
            }
            vm.nodeInfo.cabinetStart = null;
            vm.starts = [];
            vm.cabinets.some(item => {
                if (vm.nodeInfo.cabinetId != null && vm.nodeInfo.cabinetId.name == item.name) {
                    vm.starts = item.starts;
                    return true;
                }
            });
            if(vm.nodeInfo.cabinetId == null){
                vm.cabStartTitle = "<@spring.message 'common.pleaseSelect'/>";
                return;
            }
            if(vm.starts.length < 1){ // 没有起始位置可选
                vm.cabStartTitle = "<@spring.message 'node.notEnough.position'/>";
            }else{
                vm.cabStartTitle = "<@spring.message 'common.pleaseSelect'/>";
            }
        },
        getNodeGroupsById:function(uuid){ // 根据节点ID获取所属节点组集合
            var groups;
            $.ajax({
                type: "GET",
                url: baseURL + "node/getGroupsById?uuid=" + uuid,
                async: false,
                dataType: "json",
                success: function (r) {
                    groups = r.data;
                }
            });
            // 该节点包含节点组
            if(groups != undefined && groups != null && groups.length > 0){
                // 生成组件
                vm.getSystems();
                this.$nextTick(()=>{
                        for (let i = 0; i < vm.$refs.gtree.length; i++) {
                        var refObj = vm.$refs.gtree[i];
                        refObj.forUpdateNode(groups);
                    }
                })
            }
        },
        checkUsize:function(){
            if(vm.nodeInfo.uSize == ""){
                vm.uSizeValidateFlag = true;
                //vm.rooms = vm.cabinets = vm.starts = [];
                //vm.nodeInfo.nodeRoom = vm.nodeInfo.cabinetId = vm.nodeInfo.uSize = null;
            }
        },
       /* checkUsize: function () {
            if (vm.nodeInfo.uSize == null || vm.nodeInfo.uSize == '') { // 节点高度为null
                alert("<@spring.message 'node.pleaseWrite.uSize'/>!");
            }
        },*/
       /* checkNodeRoom: function () {
            if (vm.nodeInfo.uSize == null || vm.nodeInfo.uSize == '') { // 节点高度为null
                alert("<@spring.message 'node.pleaseWrite.uSize'/>!");
                return;
            }
            if (vm.nodeInfo.uSize != null && vm.nodeInfo.nodeRoom == null) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.room.name'/>!");
            }
        },*/
        /*checkCabStart: function () {
            if (vm.nodeInfo.uSize == null || vm.nodeInfo.uSize == '') { // 节点高度为null
                alert("<@spring.message 'node.pleaseWrite.uSize'/>!");
            } else if (vm.nodeInfo.nodeRoom == null) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.room.name'/>!");
            } else if (vm.nodeInfo.nodeRoom != null && vm.nodeInfo.cabinetId == null) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.cabinet.name'/>!");
            } else if (vm.nodeInfo.nodeRoom != null && vm.nodeInfo.cabinetId != null && vm.starts.length < 1) {
                // 暂无位置可选
                alert(vm.nodeInfo.cabinetId.name + "  <@spring.message 'node.notEnough.position'/>!");
            }
        },*/
        /*checkNodeType: function () {
            if (vm.nodeInfo.type == null) {
                alert("<@spring.message 'common.pleaseSelect'/><@spring.message 'node.type'/>!");
            }
        },*/
        validate: function () {
            vm.$v.nodeInfo.$touch();
            return !vm.$v.nodeInfo.$error;
        },
        validateGroup: function () { // 节点组表单校验
            vm.$v.nodeGroupDto.$touch();
            return !vm.$v.nodeGroupDto.$error;
        },
        advancedSearch: function () { //高级搜索
            this.isAdvancedSearch = !this.isAdvancedSearch;
            if (!this.isAdvancedSearch) {
                vm.q.state = vm.q.tag = null;
            }
        },
        adSearchDropdown1: function() {
        	this.selectFlag1 = !this.selectFlag1;
        },
        changeAdSearch1: function(item) {
        	this.selectedItem1 = item;
        },
        // group
        showNodeGroups:function () {
            if(vm.systems != null && vm.systems.length > 0){
                if(vm.checkedGroups != null && vm.checkedGroups.length > 0){
                     vm.checkedGroups.forEach(item=>{
                         $("#box" + item.systemId).show();
                     })
                }
                vm.showGroupTree = false;
                this.openGroupTree("选择节点组");
                return;
            }

            // 获取菜单数据
            vm.getSystems();
            vm.showGroupTree = false;
            this.openGroupTree("选择节点组");
        },
        getSystems:function(){ // 获取菜单数据 包含业务系统和节点组信息
            $.ajax({
                type: "GET",
                url: baseURL + "nodegroup/list",
                async: false,
                dataType: "json",
                success: function (r) {
                    vm.systems = r.data.list;
                }
            });
        },
        getParentSystem: function (systemId) { // 获取父节点的业务系统
            $.ajax({
                type: "GET",
                url: baseURL + "/system/detail?id=" + systemId,
                async: false,
                dataType: "json",
                success: function (r) {
                    vm.system = r.data;
                }
            });
        },
        addNodeGroup: function (nodeGroup, isRoot) {
            vm.$v.nodeGroupDto.$reset();
            if (isRoot) { // 添加根节点组
                vm.showGroupForm = true;
                // 获取当前权限
                vm.system = nodeGroup;
                vm.nodeGroupDto = {
                    name: null,
                    id: null,
                    systemId: null,
                    pid: null,
                    remark: null,
                    parentGroup: null
                };
                vm.nodeGroupDto.parentGroup = {name: "<@spring.message 'nodegroup.root.group'/>", id: 0}; // 根节点组
            } else {
                // 获取节点组的业务系统 作为子节点组的业务系统
                vm.getParentSystem(nodeGroup.systemId);
                vm.showGroupForm = true;
                vm.nodeGroupDto = {
                    name: null,
                    id: null,
                    systemId: null,
                    pid: null,
                    remark: null,
                    parentGroup: null
                };
                vm.nodeGroupDto.parentGroup = nodeGroup;
                vm.$set(vm.nodeGroupDto.parentGroup,"name",nodeGroup.nodeTitle);
            }
            this.openGroupForm("<@spring.message 'common.add'/>");
        },
        deleteNodeGroup:function(nodeGroup){
            var form = new FormData();
            form.append("id", nodeGroup.id);
            $.ajax({
                type: "DELETE",
                url: baseURL + "nodegroup/delete",
                processData: false,
                contentType: false,
                mimeType: "multipart/form-data",
                data: form,
                success: function (r) {
                    if (r.code == 200) {
                        alert("<@spring.message 'common.success'/>", function (index) {
                            // 从新挂载节点组tree
                            vm.getSystems();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        updateNodeGroup: function(nodeGroup){
            vm.$v.nodeGroupDto.$reset();
            $.ajax({
                type: "GET",
                url: baseURL + "nodegroup/detail?id=" + nodeGroup.id,
                async: false, // 关闭异步
                dataType: "json",
                success: function (r) {
                    vm.nodeGroupDto = r.data;
                }
            });
            vm.getParentSystem(nodeGroup.systemId);
            vm.nodeGroupDto.parentGroup = nodeGroup.getParentNode() == null ? {name: "<@spring.message 'nodegroup.root.group'/>", id: 0} : nodeGroup.getParentNode();
            this.openGroupForm("<@spring.message 'common.edit'/>");
        },
        saveOrUpdateGroup:function(){
            if (!this.validateGroup()) {
                return;
            }
            var url;
            if (vm.nodeGroupDto.id == null) {
                url = "nodegroup/save";
            } else {
                url = "nodegroup/update";
            }
            vm.$set(vm.nodeGroupDto, "pid", vm.nodeGroupDto.parentGroup.id);
            vm.$set(vm.nodeGroupDto, "systemId", vm.system == null ? null : vm.system.id);
            if (vm.nodeGroupDto.id) {
                vm.nodeGroupDto.pid = vm.nodeGroupDto.pids = vm.nodeGroupDto.systemId = null;
            }
            $.ajax({
                type: vm.nodeGroupDto.id == null ? "POST" : "PUT",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.nodeGroupDto),
                success: function (r) {
                    if (r.code === 200) {
                        alert("<@spring.message 'common.success'/>", function (index) {
                            // 关闭当前弹出层
                            layer.close(vm.currentZIndex);
                            // 从新挂载节点组tree
                            vm.getSystems();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        selectedTreeDatas:function(nodeGroup){  // 子组件向父组件传出 absolutePath
            vm.checkedGroups = vm.checkedGroups.concat(nodeGroup);
        },
        unSelectedTreeDatas:function(nodeGroup){  // 子组件撤销传出的 absolutePath
            vm.checkedGroups.some(function(item,index){
                if(item.id == nodeGroup.id){
                    vm.checkedGroups.splice(index,1);
                    return true;
                }
            });
        },
        unCheckGroup:function(nodeGroup){  // 从选中节点组中移除某个节点组
            // 在数组中移除元素
            vm.checkedGroups.splice(vm.checkedGroups.indexOf(nodeGroup),1)
            // 调用组件 取消选中
            for (let i = 0; i < vm.$refs.gtree.length; i++) {
                var refObj = vm.$refs.gtree[i];
                refObj.uncheckTreeData(nodeGroup);
            }
            // 判断当前节点组的弹层是否存在
            if(vm.showGroupTree) {
                vm.showGroupTree = false;
                this.openGroupTree("选择节点组");
            }
        },
        openGroupTree: function (title) {
            layer.open({
                area: ['500px', '100%'],
                shade: 0,
                title: title,
                type: 1,
                fixed: false,
                maxmin: false,
                scrollbar: false,
                offset: 'r',
                closeBtn: 1,
                move: false,
                content: $("#groupTree"), // 本页面的元素
                end:function () {
                    vm.showGroupTree = true;
                }
            });
        },
        openGroupForm:function(title){
            var index = layer.open({
                area: ['50%', '400px'],
                shade: 0,
                title: title,
                type: 1,
                fixed: false,
                maxmin: false,
                scrollbar: false,
                offset: 'r',
                closeBtn: 1,
                move: false,
                content: $("#groupForm") // 本页面的元素
            });
            vm.currentZIndex = index;
            return index;
        },
        closeGroupForm:function(){
            layer.close(vm.currentZIndex);
        },
        closeGroupTree:function () {
            layer.closeAll();
        }
    },
    created: function () {
        // 从字典中获取节点类型的集合
        $.getJSON(baseURL + "sys/dict/all?type=nodeType", function (r) {
            vm.nodeTypes = r.data;
        });
        $.getJSON(baseURL + "sys/dict/all?type=onLineState", function (r) {
            vm.nodeStates = r.data;
        });
        $.getJSON(baseURL + "sys/dict/all?name=nodeTag", function (r) {
            vm.queryTags = r.data;
        });
    },
    watch: {
        "nodeInfo.uSize": function (newValue, oldValue) {
            // 1. 表单锁定 或 值错误情况下 直接返回
            if (newValue == undefined) {
                return;
            }

            if (vm.disabledFlag) { // IP 在其他业务系统存在时 直接返回 不需要处理 表单为禁用状态
                return;
            }

            // 2. 没有填写USize 不处理,返回
            if (newValue == '' || newValue == null) {
                vm.nodeInfo.cabinetId == null ? null : vm.nodeInfo.cabinetId.starts = [];
                vm.starts = [];
                vm.nodeInfo.cabinetStart = null;
                vm.$v.nodeInfo.uSize.$reset();
                vm.$v.nodeInfo.cabinetId.$reset()
                vm.$v.nodeInfo.cabinetStart.$reset();
                if (vm.nodeInfo.nodeRoom == null) { // 既没有指定uSize 当前也没有指定room
                    vm.rooms = [];
                }
                return;
            }
            vm.uSizeValidateFlag = false;
            // 3. 开始处理数据变化  先清空起始位置
            vm.starts = [];
            // 4.重新发起请求
            var params = "?uSize=" + newValue;
            if (this.nodeInfo.uuid) {
                params += "&uuid=" + this.nodeInfo.uuid;
            }
            // 获取数据
            $.ajax({
                type: "GET",
                url: baseURL + "node/selectCab" + params,
                async: false,
                dataType: "json",
                success: function (r) {
                    vm.rooms = r.data.list;
                }
            });

            // 5.处理变化数据
            var cab;
            vm.rooms.some(room => {
                cab = room.cabinets.find(cabinet => {
                    if (vm.nodeInfo.cabinetId != null && cabinet.id == vm.nodeInfo.cabinetId.id) { // 如果机柜存在
                        return cabinet;
                    }
                });
                if (cab != undefined) { // 旧机柜仍然存在
                    return true; // 结束循环
                }
            })

            // 更新cabs starts
            vm.starts = cab == undefined ? [] : cab.starts;
            vm.rooms.some(item => {
                if (vm.nodeInfo.nodeRoom != null && item.roomId == vm.nodeInfo.nodeRoom.roomId) {
                    vm.cabinets = item.cabinets;
                    return true;
                }
            });


            // 6. 深克隆对象 用作修改时的cabinetStart 回显问题
            var cloneNodeInfo = JSON.parse(JSON.stringify(vm.nodeInfo));
            vm.nodeInfo.cabinetStart = null;
            var flag = false;
            vm.starts.some(item => {
                if (item == cloneNodeInfo.cabinetStart) {
                    flag = true;
                    return flag;
                }
            })
            if (vm.starts.length > 0 && flag) {// vm.nodeInfo.uuid != null 可加可不加 加上为只在修改上回显
                vm.nodeInfo.cabinetStart = cloneNodeInfo.cabinetStart;
            }

            // 7.关于起始位置提示
            if(data.starts.length < 1 && data.nodeInfo.nodeRoom != null){
                data.cabStartTitle = "<@spring.message 'node.notEnough.position'/>";
            }else{
                data.cabStartTitle = "<@spring.message 'common.pleaseSelect'/>";
            }
        },
        "selectedItem1": function(newValue, oldValue) {
        	if (newValue == 'IP') {
        		this.showIp = true;
        		this.q.name = "";
        	} else {
        		this.showIp = false;
        		this.q.ip = "";
        	}
        },
        "selectFlag1": function(newValue, oldValue) {
        	if (newValue) {
        		$(".ad-search-label").css("border-bottom", "0");
        	} else {
        		$(".ad-search-label").css("border-bottom", "1px solid #ccc");
        	}
        },
        "nodeInfo.type":function(newValue,oldValue){
            if(newValue == null){
                vm.nodeTags = [];
            }
        }
    },
    computed: {}
});
</script>