<script>
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + '/deteSet/list',
        datatype: "json",
        colModel: [
            {label: 'ID', name: 'id', width: 30, key: true, hidden: true},
            {label: '<@spring.message "common.name"/>', name: 'name', sortable: true, width: 60},
            {label: '<@spring.message "deteType"/>', name: 'type.name', sortable: false, width: 100},
            {
                label: '<@spring.message "common.status"/>',
                name: 'state',
                sortable: false,
                width: 60,
                formatter: function (value, options, rowData) {
                    return "2"==value?"<span style='color: red;'><@spring.message 'common.stopped'/></span>":"<span style='color: green;'><@spring.message 'deteSet.detecting'/></span>"
                }
            },
            {label: '<@spring.message "param"/>', name: 'params', sortable: false, width: 100},
            {label: '<@spring.message "common.opTime"/>', name: 'opTime', sortable: true, width: 100},
            {label: '<@spring.message "common.remark"/>', name: 'remark', sortable: false, width: 100},
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
            //隐藏grid底部滚动条
            $("#jqGrid").closest(".ui-jqgrid-bdiv").css({"overflow-x": "hidden"});
        }
    });
});

Vue.component("select-panel", pannel);
Vue.component("left-tree", znodeTree);
// Vue.component("right-table",gqGridTable);

var nameRepeat = function (name, deteSet) {
    if (helpers.req(name) && helpers.req(deteSet)) {
        var params = "?name=" + name;
        if (deteSet.id) {
            params += "&id=" + deteSet.id;
        }
        var flag = true;
        $.ajax({
            type: "get",
            url: baseURL + "/deteSet/nameCheck" + params,
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
    el: "#deteSet",
    data: {
        showList: true,
        setStateArr: [],
        nodeTypesArr: [],
        params: {
            name: '',
            type: null
        },
        deteSet: {
            id: null,
            name: '',
            typeId: '',
            params: '',
            viewLevel: '',
            viewerId: [],
            remark: '',
            i18nCode: '',
            nodeGroupIds: [],
            nodeIds: [],
            nodeGroups: [],
            nodes:[]
        },
        title: '监测设置',
        deteTypes: [],
        showNodeSelect: false,
        curSystem: null,
        nodeGroups: [],//select-panel 组件左侧树 填充数据
        nodeInfoData: [], //select-panel 组件右侧列表 填充数据
        columnModel: [    //select-panel 组件右侧列表 显示列设置  api doc：http://doc.huangsw.com/vue-easytable/app.html#/table/api
            {width: 60, titleAlign: 'center', columnAlign: 'center', type: 'selection', isFrozen: true},
            {field: 'name', title: '<@spring.message "common.name"/>', width: 80, titleAlign: 'center', columnAlign: 'center', isResize: true},
            {field: 'ip', title: 'IP', width: 150, titleAlign: 'center', columnAlign: 'center', isResize: true},
            {
                field: 'type',
                title: '<@spring.message "node.type"/>',
                width: 80,
                titleAlign: 'center',
                columnAlign: 'center',
                isResize: true,
                formatter: function (rowData, rowIndex, pagingIndex, field) {
                    switch (rowData.type) {
                        case "1":
                            return "<@spring.message 'node.type.server'/>";
                        case "2":
                            return "<@spring.message 'node.type.net'/>";
                        default:
                            return "<span style='color:red;'><@spring.message 'node.type.none'/></span>";
                    }
                }
            },
            {
                field: 'state',
                title: '<@spring.message "common.onLineState"/>',
                width: 80,
                titleAlign: 'center',
                columnAlign: 'center',
                isResize: true,
                formatter: function (rowData, rowIndex, pagingIndex, field) {
                    switch (rowData.state) {
                        case "1":
                            return "<@spring.message 'common.onLine'/>";
                        case "2":
                            return "<span style='color:red;'><@spring.message 'common.offLine'/></span>";
                        default:
                            return "<span style='color:red;'><@spring.message 'common.state.none'/></span>";
                    }
                }
            }
        ],
        queryParams: null,
        nodeGroupsChecked: [], //选中的节点组
        nodeInfosChecked: [], //选中的节点
        nodeFreshPoint: 0, //记录切换时 nodeInfosChecked下标位置
        uncheckGroup: null, //标记左侧树取消选中的节点，用于左右联动
        viewLevels: [],
        viewers: [],
        typeParams:[],
        paramVals:[],
        nodeOrGroupEmpty:false,
        nodeOrGroupEmptyMsg:"",
        deteTypeIsNull:false,
        userGroupsEmpty:false
    },
    created: function () {
        this.getSetState();
        this.getDeteTypes();
        this.getCurSystem();
        this.getNodeType();
        this.getViewLevels();

    },
    validations: {
        deteSet: {
            name: {
                required,
                nameRepeat
            },
            typeId: {
                required
            },
            params: {},
            viewLevel: {
                required
            },
            viewerId: {},
            remark: {},
            i18nCode: {},
            nodeGroupIds: {},
            nodeIds: {}
        }
    },
    methods: {
        init: function (systemId) {
            $.ajax({
                url: baseURL + "/nodegroup/list",
                type: "GET",
                async: false,
                success: function (r) {
                    if (200 == r.code) {
                        for(let i in r.data.list){
                            var v=r.data.list[i];
                            if (systemId == v.id) {
                                vm.nodeGroups = v.nodegroups;
                                break;
                            }
                        }
                    }
                }
            })
        },
        getCurSystem: function () {
            $.getJSON(baseURL + "system/detail", function (r) {
                vm.curSystem = r.data;
                vm.init(vm.curSystem.id);
                vm.getUserGroups();
            });
        },
        getSetState: function () {
            this.setStateArr = getSysDicArr("setState")
        },
        getNodeType: function () {
            this.nodeTypesArr = getSysDicArr("nodeType")
        },
        query: function () {
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'name': vm.params.name,"typeId":vm.params.type==null?null:vm.params.type.id},
                page: 1
            }).trigger("reloadGrid");
        },
        reload: function () {
            vm.query();
        },
        add: function () {
            vm.nodeInfoData = [];
            vm.nodeGroupsChecked = [];
            vm.nodeInfosChecked = [];
            vm.$v.deteSet.$reset();
            vm.showList = false;

            vm.deteSet = {
                id: null,
                name: '',
                typeId: '',
                params: '',
                viewLevel: '2',
                viewerId: [],
                remark: '',
                i18nCode: '',
                nodeGroupIds: [],
                nodeIds: []
            }
        },
        update: function () {
            var id = getSelectedRow();
            if (id == null||id==undefined||id=='') {
                return;
            }
            vm.nodeInfoData = [];
            vm.nodeGroupsChecked = [];
            vm.nodeInfosChecked = [];
            vm.$v.deteSet.$reset();
            vm.showList = false;
            vm.getInfo(id);
            for(var j=0;j<vm.nodeGroups.length;j++){
                var v=vm.nodeGroups[j];

                for(var i=0;i<vm.nodeGroupsChecked.length;i++){
                    var checkedGroup=vm.nodeGroupsChecked[i];
                    if(v.id==checkedGroup.id){
                        v.checked=true;
                        break ;
                    }
                }
            }
            var selectPanel=this.$refs.selectPanel;
            selectPanel.resetTree(); //重新加载树
        },
        getInfo: function (id) {
            $.ajax({
                url:baseURL + "deteSet/detail?id=" + id,
                type:"GET",
                async:false,
                success:function (r) {
                    vm.deteSet = r.data;
                    vm.formatDeteSetToObject();
                }
            });
        },
        saveOrUpdate: function () {

            if (vm.validate()) { //在下方定义的validate方法
                return;
            }
            layer.closeAll();
            var cloneStr = JSON.stringify(vm.deteSet);

            var url = vm.deteSet.id == null ? "deteSet/save" : "deteSet/update";
            var method = vm.deteSet.id == null ? "POST" : "PUT";

            //处理json数据
            vm.formatDeteSetToSimple();
            console.log("submit data-->" + JSON.stringify(vm.deteSet))
            $.ajax({
                type: method,
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.deteSet),
                success: function (r) {
                    if (r.code === 200) {
                        alert('<@spring.message "common.success"/>', function (index) {
                            vm.nodeInfoData = [];
                            vm.nodeGroupsChecked = [];
                            vm.nodeInfosChecked = [];
                            vm.reload();
                        });
                    } else {
                        var cloneObj = eval("(" + cloneStr + ")");
                        vm.deteSet = cloneObj;
                        alert(r.msg);
                    }
                }
            });


        },
        del: function (event) {
            var ids = getSelectedRows();
            if (ids == null) {
                return;
            }
            var form = new FormData();
            form.set("state",'1');
            form.set("ids",ids);
            confirm("<@spring.message 'common.confirmDelete'/>", function () {
                var xhr=new XMLHttpRequest();
                xhr.open("PUT",baseURL + "deteSet/changeState");
                xhr.send(form);
                xhr.onload=function(){
                    if(xhr.status==200){
                        var response=JSON.parse(xhr.response);
                        if(response.code==200){
                            alert('<@spring.message "common.success"/>', function (index) {
                                vm.query();
                            });
                        }else{
                            alert(response.msg);
                        }
                    }
                }


            })
        },
        validate: function () {//提交时的校验
            vm.$v.deteSet.$touch();
            var nodeGroupsEmpty=vm.nodeGroupsChecked==null||vm.nodeGroupsChecked.length<1;
            var nodesEmpty=vm.nodeInfosChecked==null||vm.nodeInfosChecked.length<1;
            vm.nodeOrGroupEmpty=(nodeGroupsEmpty&&nodesEmpty);
            if(vm.nodeOrGroupEmpty){
                vm.nodeOrGroupEmptyMsg="<@spring.message 'validate.nodeOrNodegroupRequired'/>";
            }
            if(vm.deteSet.typeId!=null&&vm.nodeOrGroupEmpty){
                vm.openNodePannel();
            }

            if(vm.deteSet.viewLevel=='1'){
                var userGroupCheckFlag=true;
                for(var i=0;i<vm.viewers.length;i++){
                    var viewer=vm.viewers[i];
                    if(viewer.checked){
                        userGroupCheckFlag=false;
                        break;
                    }
                }
                vm.userGroupsEmpty=userGroupCheckFlag;
            }


            return vm.$v.deteSet.$error||vm.nodeOrGroupEmpty||vm.userGroupsEmpty;
        },
        getDeteTypes: function () {
            $.getJSON(baseURL + "deteType/list?limit=999&_" + $.now(), function (r) {
                vm.deteTypes = r.data.list;
            });
        },
        openNodePannel: function () {
            if (this.deteSet.typeId == null || this.deteSet.typeId == "") {
                vm.deteTypeIsNull=true;
                return;
            }

            vm.deteTypeIsNull=false;
            vm.showNodeSelect = true;
            var index = layer.open({
                area: ['50%', '100%'],
                shade: 0,
                title: "<@spring.message 'common.selectNodeOrNodeGroup'/>",
                type: 1,
                maxmin: false,
                scrollbar: false,
                offset: 'r',
                closeBtn: 1,
                move: false,
                content: $("#layerView"),
                success:function(){
                    if(vm.nodeOrGroupEmpty&&vm.nodeOrGroupEmptyMsg!=''){
                        vm.$refs.selectPanel.showWarnMessage();
                        setTimeout(function(){
                            vm.$refs.selectPanel.hideWarnMessage();
                            vm.nodeOrGroupEmptyMsg="";
                        },1500)
                    }
                }
            });
        },
        getNodeList: function (nodeGroup) {
            // this.queryParams={"groupId":nodeGroup.id}
            $.getJSON(baseURL + "node/list?groupId=" + nodeGroup.id+"&sidx=state&order=asc", function (r) {
                vm.nodeFreshPoint = vm.nodeInfosChecked.length;
                $.each(r.data.list, function (i, newNodeInfo) {
                    for (var j = 0; j < vm.nodeInfosChecked.length; j++) {
                        var nodeInfo = vm.nodeInfosChecked[j];
                        if (newNodeInfo.uuid == nodeInfo.uuid) {
                            newNodeInfo._checked = true;
                            // newNodeInfo._disabled=true;
                            break;
                        }
                    }
                })

                vm.nodeInfoData = r.data.list;
            })
        },
        checkNodeGroup: function (nodeGroup) {
            vm.nodeOrGroupEmpty=false;
            if (nodeGroup.checked) {
                var fullPath = printNodeGroupName(nodeGroup);
                nodeGroup.fullPath = fullPath;
                vm.nodeGroupsChecked.push(nodeGroup)
            } else {
                $.each(vm.nodeGroupsChecked, function (i, v) {
                    if (v.id == nodeGroup.id) {
                        vm.nodeGroupsChecked.splice(i, 1);
                    }
                })
            }

        },
        unCheckGroup: function (index) {
            vm.uncheckGroup = vm.nodeGroupsChecked[index];
            vm.nodeGroupsChecked.splice(index, 1);
        },
        checkNodeInfos: function (nodeInfos) {
            vm.nodeOrGroupEmpty=false;
            if (vm.nodeFreshPoint > 0) {
                vm.nodeInfosChecked.splice(vm.nodeFreshPoint);
                $.each(nodeInfos, function (i, v) {
                    var flag = true;
                    var index = -1;
                    for (var j = 0; j < vm.nodeInfosChecked.length; j++) {
                        var checkedNode = vm.nodeInfosChecked[j];
                        if (checkedNode.uuid == v.uuid) {
                            flag = false;
                            index = j;
                            break;
                        }
                    }

                    if (flag) {
                        vm.nodeInfosChecked.push(v);
                    } else {
                        console.log("节点存在" + (index + 1) + "号位置")
                    }
                })

            } else {
                vm.nodeInfosChecked = nodeInfos;
            }
        },
        unCheckNodeInfo: function (index) {
            var delNode = JSON.parse(JSON.stringify(vm.nodeInfosChecked[index]));
            var delIndex = -1;
            $.each(vm.nodeInfoData, function (i, v) {
                for (var j = 0; j < vm.nodeInfosChecked.length; j++) {
                    var checkedNode = vm.nodeInfosChecked[j];
                    if (checkedNode.uuid == v.uuid) {
                        v._checked = true;
                    }
                }

                if (delNode.uuid == v.uuid) {
                    delIndex = i;
                }
            })
            vm.nodeInfosChecked.splice(index, 1);
            delNode._checked = false;
            // delNode._disabled=false;
            vm.nodeInfoData.splice(delIndex, 1, delNode);
        },
        formatDeteSetToSimple: function () {
            if(vm.deteSet.nodeGroupIds==null||vm.deteSet.nodeGroupIds==undefined){
                vm.deteSet.nodeGroupIds=[];
            }
            if(vm.deteSet.nodeIds==null||vm.deteSet.nodeIds==undefined){
                vm.deteSet.nodeIds=[];
            }
            vm.deteSet.typeId = vm.deteSet.typeId.id;
            $.each(vm.nodeGroupsChecked, function (i, v) {
                vm.deteSet.nodeGroupIds.push(v.id);
            })
            $.each(vm.nodeInfosChecked, function (i, v) {
                vm.deteSet.nodeIds.push(v.uuid);
            })
            if(vm.paramVals!=null && vm.paramVals.length>0){
                var params="";
                $.each(vm.paramVals, function (i, v) {
                    params+=(v+",");
                })
            }

            vm.deteSet.params=params.substr(0,params.length-1);
            $.each(vm.viewers,function(i,v){
                if(v.checked){
                    vm.deteSet.viewerId.push(v.code);
                }
            })
        },
        formatDeteSetToObject: function(){
            for(var i=0;i<vm.deteTypes.length;i++){
                var deteType=vm.deteTypes[i];
                if(vm.deteSet.typeId==deteType.id){
                    vm.deteSet.typeId=deteType;
                    break;
                }
            }
            vm.nodeGroupsChecked=vm.deteSet.nodeGroups;
            $.each(vm.nodeGroupsChecked,function(i,v){
                var fullPath=printNodeGroupName(v);
                v.fullPath=fullPath;
            })
            vm.nodeInfosChecked=vm.deteSet.nodes;
            vm.getDeteTypeDetail(vm.deteSet.typeId,null);
            vm.paramVals=vm.deteSet.params.split(",");
            if(vm.deteSet.viewLevel=='1'){
                $.each(vm.viewers,function(index,item){
                    for(var i=0;i<vm.deteSet.viewer.length;i++){
                        var viewer=vm.deteSet.viewer[i];
                        if(viewer.id==item.code){
                            item.checked=true;
                            break;
                        }
                    }
                })
                console.log(JSON.stringify(vm.viewers))
            }

        },
        getViewLevels: function () {
            this.viewLevels = getSysDicMap("viewLevel");
        },
        getUserGroups: function () {
            $.getJSON(baseURL + "sys/user/info/" , function (r) {
                if (r.user.usergroups != null) {
                    for (var i = 0; i < r.user.usergroups.length; i++) {
                        var m = {"code": r.user.usergroups[i].id, "value": r.user.usergroups[i].name,"checked":false}
                        vm.viewers.push(m);
                    }
                }
            })
        },
        columnClassChange: function (rowIndex, columnName, rowData) {
            if (rowData.state == '2') { //下线节点
                rowData._disabled = true;
                return "table-row-disabled";
            } else {
                if (vm.deteSet.typeId.method == "2") { //snmp 监测 节点类型 网元
                    if (rowData.type == '1') { //服务器
                        rowData._disabled = true;
                        return "table-row-disabled";
                    }
                } else {//snmp 监测 节点类型 服务器
                    if (rowData.type == '2') {//网元
                        rowData._disabled = true;
                        return "table-row-disabled";
                    }
                }
            }
            rowData._disabled=false;
            return "";
        },
        getDeteTypeDetail:function(selectedOption,id){
            $.getJSON(baseURL+"deteType/detail?id="+selectedOption.id,function(r){
                vm.typeParams=r.data.deteTypeParams;
                for(let i in vm.typeParams){
                    vm.paramVals.push('');
                }
            })
        }
    },
    watch:{
        viewers:{
            handler(){
                // var userGroupCheckFlag=true;
                // for(var i=0;i<this.viewers.length;i++){
                //     var viewer=this.viewers[i];
                //     if(viewer.checked){
                //         userGroupCheckFlag=false;
                //         break;
                //     }
                // }
                // console.log("111")
                // this.userGroupsEmpty=userGroupCheckFlag;
            },
            deep:true
        }
    },
    mounted() {
        $("input[type='checkbox' ][value='check-all']").parent().hide();
        // this.$nextTick(function () {
        //     console.log("222")
        //     this.userGroupsEmpty=false;
        //
        // })

    }

})
</script>