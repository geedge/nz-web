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
        height: top.$(".content").height()-130,
        rowNum: 15,
		rowList : [15,30,50],
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
    var ps1 = new PerfectScrollbar('.psNg');
    var ps2 = new PerfectScrollbar('.psN');
});

// Vue.component("select-panel", pannel);
// Vue.component("left-tree", znodeTree);
Vue.component("nz-node-selector",selector);
Vue.component("nz-detect-addtype",addType)
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
        queryParams: null,
        nodeGroupsChecked: [], //选中的节点组
        nodeInfosChecked: [], //选中的节点
        viewLevels: [],
        viewers: [],
        typeParams:[],
        paramVals:[],
        nodeOrGroupEmpty:false,
        nodeOrGroupEmptyMsg:"",
        deteTypeIsNull:false,
        userGroupsEmpty:false,
        selectorReset:true,
        openAddTypeWindow:false,
        typeId:null,
        layerId:null,
        layerReset:true
    },
    created: function () {
        this.getSetState();
        this.getDeteTypes();
        this.getNodeType();
        this.getViewLevels();
        this.getUserGroups();
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
            viewerId: [],
            remark: {},
            i18nCode: {},
            nodeGroupIds: {},
            nodeIds: {}
        }
    },
    methods: {
        reset:function(){
            vm.nodeInfoData = [];
            vm.nodeGroupsChecked = [];
            vm.nodeInfosChecked = [];
            vm.typeParams=[];
            vm.paramVals=[];
            vm.$v.deteSet.$reset();

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
            vm.showList = false;
            this.reset();
            vm.resetSelector();
        },
        update: function () {
            var id = getSelectedRow();
            if (id == null||id==undefined||id=='') {
                return;
            }
            this.reset();
            vm.showList = false;
            vm.getInfo(id);

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
                        //显示提示信息
                    }
                }
            });
            var ps3 = new PerfectScrollbar('.nodeTree');
        },
        openAddTypePannel: function () {
            var index = layer.open({
                area: ['600px', '100%'],
                shade: 0,
                title: "",
                type: 1,
                maxmin: false,
                scrollbar: false,
                offset: 'r',
                closeBtn: 1,
                move: false,
                content: $("#add-type-layer-view"),
                zIndex: layer.zIndex,
                success: function(layero){
                    layero.find(".layui-layer-close").removeClass('layui-layer-close2');
                    layero.find(".layui-layer-close").addClass('layui-layer-close1');
                },
            });
            this.layerId=index;
            var ps3 = new PerfectScrollbar('.nodeTree');
        },
        getTabDatas:function(tabData){
            vm.nodeInfosChecked=tabData;
        },
        getTreeDatas:function(treeData){
            vm.nodeGroupsChecked=treeData;
        },
        formatTab:function(rowData){
            console.log(rowData)
            if (rowData.state == '2') { //下线节点
                rowData.disabled=true;
                rowData.trRemark="下线节点";
            } else {
                if (vm.deteSet.typeId.method == "2") { //snmp 监测 节点类型 网元
                    if (rowData.type == '1') { //服务器
                        rowData.disabled=true;
                        rowData.trRemark="网元类型监测不能作用于服务器节点";
                    }
                } else {//snmp 监测 节点类型 服务器
                    if (rowData.type == '2') {//网元
                        rowData.disabled=true;
                        rowData.trRemark="服务器类型监测不能作用于网元节点";
                    }
                }
            }
        },
        unCheckGroup:function(nodeGroup){
            this.$refs.nzSelector.uncheckTreeData(nodeGroup);
            this.openNodePannel();
        },
        uncheckAllGroup:function(){
            this.$refs.nzSelector.uncheckAllTreeData();
        },
        unCheckNodeInfo:function(rowData){
            this.$refs.nzSelector.uncheckTabData(rowData);
            this.openNodePannel();
        },
        uncheckAllNodeInfo:function(){
            this.$refs.nzSelector.uncheckAllTabData();
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
            console.log(vm.deteSet.viewerId)
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

            vm.getDeteTypeDetail(vm.deteSet.typeId,null);
            console.log('paramVals')
            console.log(vm.deteSet.params)
            vm.paramVals=vm.deteSet.params.split(",");

            console.log(vm.paramVals)
            console.log(vm.paramVals.length)
            if(vm.deteSet.viewLevel=='1'){
                vm.deteSet.viewerId=[];
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
        getDeteTypeDetail:function(selectedOption,id){
            $.getJSON(baseURL+"deteType/detail?id="+selectedOption.id,function(r){
                vm.typeParams=r.data.deteTypeParams;
                for(let i in vm.typeParams){
                    vm.paramVals.push('');
                }
            })
        },
        resetSelector: function () {//强制刷新dom
            var temp = this;
            temp.selectorReset = false;
            temp.$nextTick(() => {
                temp.selectorReset = true
            });
        },
        resetAddTypeLayer: function () {//强制刷新dom
            var temp = this;
            temp.layerReset = false;
            temp.$nextTick(() => {
                temp.layerReset = true
            });
        },
        openAddDetectType:function(index){
            this.openAddTypeWindow=true;
            this.openAddTypePannel();
        },
        closeAddType:function(){
            layer.closeAll();
            this.resetAddTypeLayer();
            this.getDeteTypes();
        }
    },
    watch:{
        viewers:{
            handler(){
            },
            deep:true
        }
    },
    mounted() {

    }

})
</script>