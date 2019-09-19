$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + '/deteSet/list',
        datatype: "json",
        colModel: [
            {label: 'ID', name: 'id', width: 30, key: true, hidden: true},
            {label: '名称', name: 'name', sortable: true, width: 60},
            {label: '监测类别', name: 'type.name', sortable: false, width: 100},
            {
                label: '状态',
                name: 'state',
                sortable: false,
                width: 60,
                formatter: function (value, options, rowData) {
                    return vm.setStateArr[value - 1];
                }
            },
            {label: '备注', name: 'remark', sortable: true, width: 100},
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

Vue.component("select-panel",pannel);
Vue.component("left-tree", znodeTree);
// Vue.component("right-table",gqGridTable);

var vm = new Vue({
    el: "#deteSet",
    data: {
        showList: true,
        setStateArr: [],
        nodeTypesArr:[],
        params: {
            name: '',
            typeId: null
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
            nodeIds: []
        },
        title: '监测设置',
        deteTypes: [],
        showNodeSelect: false,
        curSystem: null,
        nodeGroups: [],//select-panel 组件左侧树 填充数据
        nodeInfoData:[], //select-panel 组件右侧列表 填充数据
        columnModel:[    //select-panel 组件右侧列表 显示列设置  api doc：http://doc.huangsw.com/vue-easytable/app.html#/table/api
            {width: 60, titleAlign: 'center',columnAlign:'center',type: 'selection',isFrozen:true},
            {field: 'name', title: '名称', width: 80, titleAlign: 'center', columnAlign: 'center',isResize: true},
            {field: 'ip', title: 'IP', width: 150, titleAlign: 'center', columnAlign: 'center',isResize: true},
            {field: 'type', title: '节点类型', width: 80, titleAlign: 'center', columnAlign: 'center',isResize: true},
        ],
        queryParams:null,
        nodeGroupsChecked:[], //选中的节点组
        nodeInfosChecked:[], //选中的节点
        nodeFreshPoint:0, //记录切换时 nodeInfosChecked下标位置
        uncheckGroup:null, //标记左侧树取消选中的节点，用于左右联动
        viewLevels:[],
        viewers:[]
    },
    created: function () {
        this.getSetState();
        this.getDeteTypes();
        this.getCurSystem();
        this.getNodeType();
        this.getViewLevels();
        this.getUserGroups();
    },
    validations: {
        deteSet: {
            name: {
                required
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
                        $.each(r.data.list, function (i, v) {
                            if (systemId == v.id) {
                                vm.nodeGroups = v.nodegroups;
                            }
                        });
                    }
                }
            })
        },
        getCurSystem: function () {
            $.getJSON(baseURL + "system/detail", function (r) {
                vm.curSystem = r.data;
                vm.init(vm.curSystem.id);
            });
        },
        getSetState: function () {
            this.setStateArr=getSysDicArr("setState")
        },
        getNodeType:function(){
            this.nodeTypesArr=getSysDicArr("nodeType")
        },
        query: function () {
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'name': vm.params.name},
                page: page
            }).trigger("reloadGrid");
        },
        reload: function () {
            vm.query();
        },
        add: function () {
            vm.$v.deteSet.$reset();
            vm.showList = false;

            vm.deteSet = {
                id: null,
                name: '',
                typeId: '',
                params: '',
                viewLevel: '',
                viewerId: [],
                remark: '',
                i18nCode: '',
                nodeGroupIds: [],
                nodeIds: []
            }
        },
        update: function () {


        },
        getInfo: function (id) {
            $.get(baseURL + "deteSet/detail?id=" + id, function (r) {
                vm.deteType = r.data;
                vm.metaCount = r.data.deteTypeMetas.length;
                vm.paramCount = r.data.deteTypeParams.length;
                vm.formatDeteTypeToObject();
            });
        },
        saveOrUpdate: function () {
            layer.closeAll();
            if (vm.validate()) { //在下方定义的validate方法
                return;
            }

            var cloneStr=JSON.stringify(vm.deteSet);

            var url = vm.deteSet.id == null ? "deteSet/save" : "deteSet/update";
            var method = vm.deteSet.id == null ? "POST" : "PUT";

            //处理json数据
            vm.formatDeteSet();
            console.log("submit data-->"+JSON.stringify(vm.deteSet))
            $.ajax({
                type: method,
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.deteSet),
                success: function (r) {
                    if (r.code === 200) {
                        alert('成功', function (index) {
                            vm.nodeInfoData=[];
                            vm.nodeGroupsChecked=[];
                            vm.nodeInfosChecked=[];
                            vm.reload();
                        });
                    } else {
                        var cloneObj=eval("(" + cloneStr + ")");
                        vm.deteSet=cloneObj;
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
            confirm("确认删除吗？", function () {
                $.ajax({
                    type: "DELETE",
                    url: baseURL + "/deteSet/delete",
                    contentType: "application/json",
                    data: JSON.stringify(ids),
                    success: function (r) {
                        if (r.code == 200) {
                            alert('删除成功', function (index) {
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
            vm.$v.deteSet.$touch();
            console.log(vm.$v.deteSet.$error)
            return vm.$v.deteSet.$error
        },
        getDeteTypes: function () {
            $.getJSON(baseURL + "deteType/list?limit=999&_" + $.now(), function (r) {
                for (var i = 0; i < r.data.list.length; i++) {
                    var m = {"code": r.data.list[i].id, "value": r.data.list[i].name}
                    vm.deteTypes.push(m);
                }
            });
        },
        openNodePannel: function () {
            vm.showNodeSelect = true;
            var index=layer.open({
                area: ['50%', '100%'],
                shade: 0,
                title: "pannel",
                type: 1,
                maxmin: false,
                scrollbar: false,
                offset:'r',
                closeBtn:1,
                move:false,
                content: $("#layerView")
            });
        },
        getNodeList:function(nodeGroup){
            // this.queryParams={"groupId":nodeGroup.id}
            $.getJSON(baseURL+"node/list?groupId="+nodeGroup.id,function(r){
                vm.nodeFreshPoint=vm.nodeInfosChecked.length;
                $.each(r.data.list,function(i,newNodeInfo){
                    for(var j=0;j<vm.nodeInfosChecked.length;j++){
                        var nodeInfo=vm.nodeInfosChecked[j];
                        if(newNodeInfo.uuid== nodeInfo.uuid){
                            newNodeInfo._checked=true;
                            // newNodeInfo._disabled=true;
                            break;
                        }
                    }
                })

                vm.nodeInfoData= r.data.list;
            })
        },
        checkNodeGroup:function(nodeGroup){
            if(nodeGroup.checked){
                var fullPath=printNodeGroupName(nodeGroup);
                nodeGroup.fullPath=fullPath;
                vm.nodeGroupsChecked.push(nodeGroup)
            }else{
                $.each(vm.nodeGroupsChecked,function(i,v){
                    if(v.tId == nodeGroup.tId){
                        vm.nodeGroupsChecked.splice(i,1);
                    }
                })
            }

        },
        unCheckGroup:function(index){
            vm.uncheckGroup=vm.nodeGroupsChecked[index];
            vm.nodeGroupsChecked.splice(index,1);
        },
        checkNodeInfos:function(nodeInfos){
            if(vm.nodeFreshPoint>0){
                vm.nodeInfosChecked.splice(vm.nodeFreshPoint);
                $.each(nodeInfos,function(i,v){
                    var flag=true;
                    var index=-1;
                    for(var j=0;j<vm.nodeInfosChecked.length;j++){
                        var checkedNode=vm.nodeInfosChecked[j];
                        if(checkedNode.uuid==v.uuid){
                            flag=false;
                            index=j;
                            break;
                        }
                    }

                    if(flag){
                        vm.nodeInfosChecked.push(v);
                    }else{
                        console.log("节点存在"+(index+1)+"号位置")
                    }
                })

            }else{
                vm.nodeInfosChecked=nodeInfos;
            }
        },
        unCheckNodeInfo:function(index){
            var delNode=JSON.parse(JSON.stringify(vm.nodeInfosChecked[index]));
            var delIndex=-1;
            $.each(vm.nodeInfoData,function(i,v){
                for (var j = 0; j < vm.nodeInfosChecked.length;j++){
                    var checkedNode=vm.nodeInfosChecked[j];
                    if(checkedNode.uuid==v.uuid){
                        v._checked=true;
                    }
                }

                if(delNode.uuid==v.uuid){
                    delIndex=i;
                }
            })
            vm.nodeInfosChecked.splice(index,1);
            delNode._checked=false;
            // delNode._disabled=false;
            vm.nodeInfoData.splice(delIndex,1,delNode);
        },
        formatDeteSet:function(){
            vm.deteSet.typeId=vm.deteSet.typeId.code;
            $.each(vm.nodeGroupsChecked,function(i,v){
                vm.deteSet.nodeGroupIds.push(v.id);
            })
            $.each(vm.nodeInfosChecked,function(i,v){
                vm.deteSet.nodeIds.push(v.uuid);
            })
        },
        getViewLevels:function(){
            this.viewLevels =getSysDicMap("viewLevel");
            console.log("viewlevels-->"+JSON.stringify(this.viewLevels))
        },
        getUserGroups:function(){
            $.getJSON(baseURL + "system/detail?_" + $.now(), function (r) {
                if(r.data.usergroups!=null){
                    for (var i = 0; i < r.data.usergroups.length; i++) {
                        var m = {"code": r.data.list[i].id, "value": r.data.list[i].name}
                        vm.viewers.push(m);
                    }
                }

            })
        }
    },
    mounted(){
        $("input[type='checkbox' ][value='check-all']").parent().hide();
    }

})

function printNodeGroupName (nodeGroup){
   var fullPath="";
   var func=function(nodeGroup){
       var pid=nodeGroup.pid;
       if(pid != 0){
           $.each(vm.nodeGroups,function(i,v){
               if(pid == v.id){
                   fullPath += printNodeGroupName(v)+"/"+nodeGroup.name;
               }
           })
       }else{
           fullPath+=nodeGroup.name;
       }

   }
   func(nodeGroup);
   return fullPath;
}