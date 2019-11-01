<script>

Vue.directive('id', function (el, binding, vnode) {
        el.id = binding.arg + "-" + binding.value;
    }
)
Vue.directive('width', function (el, binding, vnode) {
        el.width = binding.value + "px";

    }
)

var rRow = Vue.extend({
    template: "#rRow",
    components: {},
    props: {
        tabNo:{},
        rowData: {
            type: Object,
            required: true
        },
        colModel: {
            type: Array,
            required: true
        },
        index: {
            type: Number,
            required: true
        },
        refKey:{
            type:String,
            required:true
        },
        identification:{
            type:String,
            required:true
        },
        trId: {
            type: String,
            default: "r-tr-id"
        },
        tdId: {
            type: String,
            default: "r-td-id"
        }
    },

    data: function () {
        return {
            ref:'',
            checked: false, //选中状态
            trSelector: "",
            checkboxSelector: "",
            trDom: null,
            checkboxDom: null,
            checkboxDomParent: null,
        }
    },
    methods: {
        init: function () {
            var temp = this;
            temp.reset();
            if (!temp.rowData.disabled&&temp.rowData.checked != undefined && temp.rowData.checked != null) {
                temp.checked = temp.rowData.checked;
            }

            temp.rowData.trIndex=temp.index;

            temp.ref=temp.refKey+temp.rowData[temp.identification];

            temp.trSelector = "#tab-"+temp.tabNo+"-" + this.trId + "-" + this.index;
            temp.checkboxSelector = temp.trSelector + "-checkbox";

            console.log(temp.trSelector)
            console.log(temp.checkboxSelector)

            temp.trDom = $(temp.trSelector);
            temp.checkboxDom = $(temp.checkboxSelector);
            temp.checkboxDomParent = temp.checkboxDom.parent();

            if(temp.rowData.disabled==undefined || temp.rowData.disabled==null||!temp.rowData.disabled){
                temp.trDom.off('mouseover').on('mouseover', temp.trMouseover);
                temp.trDom.off('mouseout').on('mouseout', temp.trMouseout);

                temp.checkboxDom.off('mouseover').on('mouseover', temp.checkboxMouseover);
                temp.checkboxDom.off('mouseout').on('mouseout', temp.checkboxMouseout);

                temp.trDom.off('click').on('click', temp.trDomClick)
                temp.checkboxDomParent.off("click").on("click", temp.checkboxDomClick)
            }else{
                temp.trDom.addClass("danger")
                temp.checkboxDom.off('mouseover').on('mouseover', function(){
                    temp.checkboxDom.get(0).style.cursor="not-allowed"
                });

            }

        },
        reset: function () {
            var temp = this;
            temp.checked = false;
            temp.trSelector = '';
            temp.checkboxSelector = '';
            temp.trDom = null;
            temp.checkboxDom = null;
            temp.checkboxDomParent = null;
        },
        changeChecked: function () {
            this.checked = !this.checked;
        },
        trMouseover: function () {
            this.trDom.addClass("active")
            this.checkboxMouseover();
        },
        trMouseout: function () {
            this.trDom.removeClass("active")
            this.checkboxMouseout();
        },
        trDomClick: function () {
            var temp = this;
            temp.changeChecked();
            if (temp.checked) {
                temp.trDom.addClass("success");
            } else {
                temp.trDom.removeClass("success");
            }
            temp.checkboxDomClick();
        },
        checkboxMouseover: function () {
            var temp = this;
            temp.removeCheckboxClass();
            if (temp.checked) {
                temp.checkboxDom.addClass("checkbox_true_full_focus");
            } else {
                temp.checkboxDom.addClass("checkbox_false_full_focus");
            }
        },
        checkboxMouseout: function () {
            var temp = this;
            temp.removeCheckboxClass();
            if (temp.checked) {
                temp.checkboxDom.addClass("checkbox_true_full");
            } else {
                temp.checkboxDom.addClass("checkbox_false_full");
            }
        },
        checkboxDomClick: function (e) {
            var temp = this;
            temp.removeCheckboxClass();
            if (temp.checked) {
                temp.checkboxDom.addClass("checkbox_true_full");
                temp.submitCheckData();
            } else {
                temp.checkboxDom.addClass("checkbox_false_full");
                temp.submitUnCheckData();
            }
            temp.appendCheckState();
            temp.checkboxMouseover();

        },
        removeCheckboxClass: function () {
            var temp = this;
            temp.checkboxDom.removeClass("checkbox_false_full");
            temp.checkboxDom.removeClass("checkbox_false_full_focus");
            temp.checkboxDom.removeClass("checkbox_true_full");
            temp.checkboxDom.removeClass("checkbox_true_full_focus");
            temp.checkboxDom.removeClass("checkbox_false_part");
            temp.checkboxDom.removeClass("checkbox_false_part_focus");
            temp.checkboxDom.removeClass("checkbox_true_part");
            temp.checkboxDom.removeClass("checkbox_true_part_focus");
        },
        appendCheckState: function () {
            this.rowData.checked = this.checked;
        },
        submitCheckData: function () { //向父组件报告选中
            var temp = this;
            this.$emit("checkRow", temp.index, temp.rowData);
        },
        submitUnCheckData: function () { //向父组件报告取消选中
            var temp = this;
            this.$emit("unCheckRow", temp.index, temp.rowData);
        },
        uncheckTr:function(){ //取消选中
            var temp=this;
            if(temp.checked){
                temp.trDomClick();
                temp.trMouseout();
            }
        }
    },
    computed: {},
    watch: {
        rowData: {
            handler() {
                this.init();
            },
            deep: true
        }
    },
    mounted: function () {
        this.init();
        if (!this.rowData.disabled&&this.checked) {
            this.changeChecked();
            this.trDomClick();
            this.trMouseout();
        }
    }
})


var rTable = Vue.extend({
    template: "#rTable",
    components: {
        "r-tr": rRow
    },
    props: {
        tabNo:{
            default:'1'
        },
        tableData: {
            type: Array,
            required: true
        },
        colModel: {
            type: Array,
            required: true
        },
        identification: {
            type: String,
            default: "id"
        }
    },

    data: function () {
        return {
            trRefKey:"r-tr-",
            checked: false, //选中状态
            partChecked: false,//部分选中状态
            disabledSize:0,
            trReset: true,
            colLen: null,
            checkboxSelector: '',
            checkboxDom: null,
            checkboxDomParent: null,
            checkedKeyPre: 'rtab',
            checkedDatas: new Map(),
            unCheckedDatas: new Map(),
        }
    },
    created: function () {
        this.getColLen();
    },
    methods: {
        init: function () {
            var temp = this;
            temp.resetTab();
            temp.getDisabledSize();
            temp.checkboxSelector="#r-tab-"+temp.tabNo+"-checkall";
            temp.checkboxDom = $(temp.checkboxSelector);
            temp.checkboxDomParent = $(temp.checkboxSelector).parent();
            temp.bindDomEvent();

        },
        bindDomEvent: function () {
            var temp = this;

            temp.checkboxDomParent.off('click').on('click', temp.checkboxClick);
            temp.checkboxDom.off('mouseover').on('mouseover', temp.checkboxMouseover);

            temp.checkboxDom.off('mouseout').on('mouseout', temp.checkboxMouseout);
        },
        resetTab: function () {
            var temp = this;
            temp.checked = false;
            temp.partChecked = false;
            temp.disabledSize=0;
            temp.checkedDatas = new Map();
            temp.unCheckedDatas = new Map();
            temp.resetTrDom();
        },
        getColLen: function () {
            this.colLen = this.colModel.length + 2;
        },
        getDisabledSize:function(){
            var temp=this;
            for(let rowData of temp.tableData){
                if(rowData.disabled){
                    temp.disabledSize++;
                }
            }
        },
        changeChecked: function () {
            this.checked = !this.checked;
            this.partChecked = false;
        },
        checkboxMouseover: function () {
            var temp = this;
            temp.removeCheckboxClass();
            if (temp.checked) {
                temp.checkboxDom.addClass("checkbox_true_part_focus");
            } else if (temp.partChecked) {
                temp.checkboxDom.addClass("checkbox_false_part_focus");
            } else {
                temp.checkboxDom.addClass("checkbox_false_full_focus");
            }
        },
        checkboxMouseout: function () {
            var temp = this;
            temp.removeCheckboxClass();
            if (temp.checked) {
                temp.checkboxDom.addClass("checkbox_true_part");
            } else if (temp.partChecked) {
                temp.checkboxDom.addClass("checkbox_false_part");
            } else {
                temp.checkboxDom.addClass("checkbox_false_full");
            }
        },
        checkboxClick: function () {
            var temp = this;
            temp.removeCheckboxClass();
            temp.changeChecked();
            if (temp.checked) {
                temp.checkboxDom.addClass("checkbox_true_part");
                temp.selectAll();
            } else {
                temp.checkboxDom.addClass("checkbox_false_full");
                temp.unSelectAll();
            }
            temp.checkboxMouseover();
        },
        removeCheckboxClass: function () {
            var temp = this;
            temp.checkboxDom.removeClass("checkbox_false_full");
            temp.checkboxDom.removeClass("checkbox_false_full_focus");
            temp.checkboxDom.removeClass("checkbox_true_full");
            temp.checkboxDom.removeClass("checkbox_true_full_focus");
            temp.checkboxDom.removeClass("checkbox_false_part");
            temp.checkboxDom.removeClass("checkbox_false_part_focus");
            temp.checkboxDom.removeClass("checkbox_true_part");
            temp.checkboxDom.removeClass("checkbox_true_part_focus");
        },
        getCheckMapKey:function(rowData){
            return this.checkedKeyPre + rowData[this.identification];
        },
        checkRow: function (index, rowData) {
            var key = this.getCheckMapKey(rowData);
            if (!this.checkedDatas.has(key)) {
                this.checkedDatas.set(key, rowData);
            }
            if (this.unCheckedDatas.has(key)) {
                this.unCheckedDatas.delete(key);
            }
            this.selectedChange();
        },
        unCheckRow: function (index, rowData) {
            var key = this.getCheckMapKey(rowData);
            if (!this.unCheckedDatas.has(key)) {
                this.unCheckedDatas.set(key, rowData);
            }
            if (this.checkedDatas.has(key)) {
                this.checkedDatas.delete(key);
            }
            this.selectedChange();
        },
        selectAll: function () {
            var temp = this;
            temp.checkedDatas.clear();
            temp.unCheckedDatas.clear();
            for (let rowData of temp.tableData) {
                if(!rowData.disabled){
                    var key = temp.checkedKeyPre + rowData[temp.identification];
                    rowData.checked = true;
                    temp.checkedDatas.set(key, rowData);
                }

            }
            temp.resetTrDom();
            temp.selectedChange();

        },
        unSelectAll: function () {
            var temp = this;
            temp.checkedDatas.clear();
            temp.unCheckedDatas.clear();
            for (let rowData of temp.tableData) {
                if(!rowData.disabled) {
                    var key = temp.checkedKeyPre + rowData[temp.identification];
                    rowData.checked = false;
                    temp.unCheckedDatas.set(key, rowData);
                }
            }
            temp.resetTrDom();
            temp.selectedChange();
        },
        checkboxStatus: function () {
            var temp = this;
            temp.removeCheckboxClass();
            var enableSize=temp.tableData.length-temp.disabledSize;
            if(temp.tableData.length==0||enableSize==0){
                temp.checked?temp.checkboxDom.addClass("checkbox_true_part"):temp.checkboxDom.addClass("checkbox_false_full");
            }else{

                if (temp.checkedDatas.size >= enableSize) {
                    temp.checked = true;
                    temp.partChecked = false;
                    temp.checkboxDom.addClass("checkbox_true_part");
                } else if (temp.checkedDatas.size > 0 && temp.checkedDatas.size < enableSize) {
                    temp.checked = false;
                    temp.partChecked = true;
                    temp.checkboxDom.addClass("checkbox_false_part");
                } else {
                    temp.checked = false;
                    temp.partChecked = false;
                    temp.checkboxDom.addClass("checkbox_false_full");
                }
            }
        },
        selectedChange: function () {
            var checkedDatas = this.checkedDatas.values();
            checkedDatas = this.produceArray(checkedDatas);

            var unCheckedDatas = this.unCheckedDatas.values();
            unCheckedDatas = this.produceArray(unCheckedDatas);
            this.checkboxStatus();
            this.$emit("selectedDatasChange", checkedDatas, unCheckedDatas);
        },
        produceArray: function (mapIterator) {
            if (mapIterator != null || mapIterator != undefined) {
                var arr = [];
                for (let val of mapIterator) {
                    arr.push(val)
                }
                return arr;
            }
            return null;
        },
        resetTrDom: function () {//强制刷新dom
            var temp = this;
            temp.trReset = false;
            temp.$nextTick(() => {
                temp.trReset = true
            });
        },
        uncheckTabData:function(rowData){//取消选中某个tr
            for (let trComponet of this.$refs["r-tr-ref"]){
                if(rowData[this.identification]==trComponet.rowData[this.identification]){
                    trComponet.uncheckTr();
                    break;
                }
            }
        },
        uncheckAllTabData:function(){
            $.each(this.$refs["r-tr-ref"],function(i,v){
                v.uncheckTr();
            })
        }
    },
    computed: {},
    watch: {
        tableData: function () {
            this.init();
            this.checkboxDom.attr("class", "r-checkbox checkbox_false_full");
        }
    },
    mounted: function () {
        this.init();
    }
})


var rTree = Vue.extend({
    template: "#nzTree",
    props: {
        treeData: {
            type: Array,
            required: true
        },
        selectedNodeId: "",
    },

    data() {
        return {
            setting: {
                view: {
                    selectedMulti: true,
                    showIcon: false
                },
                edit: {
                    enable: false,
                },
                data: {
                    simpleData: {
                        enable: true,
                        idKey: "id",
                        pIdKey: "pid",
                        rootPId: 0
                    }
                },
                check: {
                    enable: true,
                    checkStyle: "checkbox",
                    nocheckInherit: true,
                    chkboxType: {"Y": "", "N": ""}
                },
                callback: {
                    onClick: this.zTreeOnClick,
                    onCheck: this.zTreeOnCheck
                }
            },
            zTree: null,
            checkedNodes: []

        };
    },
    methods: {
        // 初始化ztree
        initZtree() {
            this.zTree = $.fn.zTree.init($("#beTree"), this.setting, this.treeData);
            this.zTree.expandAll(true);
            // for (let node of this.treeData){
            //     this.zTreeOnCheck(null,null,node);
            // }
        },
        // 树节点点击事件
        zTreeOnClick: function (event, treeId, treeNode) {
            var abPath=this.printNodeGroupName(treeNode);
            treeNode.absolutePath=abPath;
            this.$emit("tree-click", treeNode);
        },
        zTreeOnCheck: function (event, treeId, treeNode) {
            var abPath=this.printNodeGroupName(treeNode);
            treeNode.absolutePath=abPath;
            this.$emit("tree-select", treeNode);
        },
        uncheckNode:function(node){
            if (node.tId == undefined || node.tId == null) { //解决更新操作中 找不到关联节点的问题
                node = this.zTree.getNodeByParam("id", node.id, null);
            }
            node.checked = false;
            this.zTree.updateNode(node, false);
        },
        uncheckAllNode:function(){
            this.initZtree();
        },
        printNodeGroupName: function (treeNode) {
            var fullPath = "";
            var temp=this;
            var func = function (treeNode) {
                var pid = treeNode.pid;
                if (pid != 0) {
                    $.each(temp.treeData, function (i, v) {
                        if (pid == v.id) {
                            fullPath += temp.printNodeGroupName(v) + "/" + treeNode.name;
                        }
                    })
                } else {
                    fullPath += treeNode.name;
                }

            }
            func(treeNode);
            return fullPath;
        }

    },
    watch: {
        // 监听树节点的变化更新树
        treeData: function (val) {
            this.initZtree();
        }
    },
    mounted() {
        this.initZtree();
    }
})


var selector = Vue.extend({
    template: "#nzNodeSelector",
    props: {
        selectedTabDataChange: {
            type: Function
        },
        selectedTreeDataChange: {
            type: Function
        },
        disableSwitch:{
            type:Function
        },
        notifySelectedTabDatas:{
            type:Array
        },
        notifySelectedTreeDatas:{
            type:Array
        }
    },
    data: function () {
        return {
            tableData: [],
            tabSelectedKeyPre: "tab-checked-",
            treeSelectedKeyPre:"tree-checked-",
            key: "uuid", //数据的唯一标识
            colModel: [
                {name: "名称", field: "name", width: 120},
                {name: "IP", field: "ip", width: 120},
                {name: "类型", field: "type", width: 70,formatter:function(rowData){
                        return rowData["type"]=='1'?"服务器":"网元";
                    }},
                {name: "状态", field: "state", width: 60,formatter:function(rowData){
                        return rowData["state"]=='1'?"在线":"<span style='color:#cccccc;'>下线</span>";
                    }}
            ],
            treeData: [],
            selectedTableDatas: new Map(),
            selectedTreeDatas: new Map(),
            curSystem:null
        }
    },
    components: {
        "nz-table": rTable,
        "nz-tree": rTree
    },
    created: function () {
        this.getCurSysNodeGroups();
    },
    methods: {
        init:function(){
            this.initTab();
            this.initTree();
        },
        initTab:function(){
            var temp=this;
            temp.selectedTableDatas.clear();
            if(temp.notifySelectedTabDatas!=undefined && temp.notifySelectedTabDatas!=null && temp.notifySelectedTabDatas instanceof Array){
                for(let rowData of temp.notifySelectedTabDatas){
                    var key= temp.getSelectedTabKey(rowData);
                    temp.selectedTableDatas.set(key,rowData)
                }
                temp.selectedTabDataChange(temp.notifySelectedTabDatas);
            }
        },
        initTree:function(){
            var temp=this;
            temp.selectedTreeDatas.clear();
            if(temp.notifySelectedTreeDatas!=undefined && temp.notifySelectedTreeDatas!=null && temp.notifySelectedTreeDatas instanceof Array){
                for(let node of temp.notifySelectedTreeDatas){
                    var key= temp.getSelectedTreeKey(node);
                    var absolutePath=temp.$refs.treeChild.printNodeGroupName(node);
                    node.absolutePath=absolutePath;
                    temp.selectedTreeDatas.set(key,node)
                }
                temp.selectedTreeDataChange(temp.notifySelectedTreeDatas);
            }
            temp.getUnderSysNodeGroups(temp.curSystem.id);
        },
        getCurSysNodeGroups: function () {//获取当前业务系统下的节点组列表
            var temp = this;
            $.getJSON(baseURL + "system/detail", function (r) {
                temp.curSystem=r.data;
                temp.getUnderSysNodeGroups(r.data.id);
            });
        },
        getUnderSysNodeGroups: function (systemId) { //根据业务系统id获取节点组列表
            var temp = this;
            $.ajax({
                url: baseURL + "/nodegroup/list",
                type: "GET",
                async: false,
                success: function (r) {
                    if (200 == r.code) {
                        for (let i in r.data.list) {
                            var v = r.data.list[i];
                            if (systemId == v.id) {
                                for(let nodeGroup of  v.nodegroups){
                                    var key=temp.getSelectedTreeKey(nodeGroup);
                                    if(temp.selectedTreeDatas.has(key)){
                                        nodeGroup.checked=true;
                                    }
                                }
                                temp.treeData = v.nodegroups;
                                break;
                            }
                        }
                    }
                }
            })
        },
        getNodeInfos: function (nodeGroup) { //根据节点组获取 该节点组下的节点列表
            var temp = this;
            $.getJSON(baseURL + "node/list?groupId=" + nodeGroup.id + "&sidx=state&order=asc", function (r) {
                    for(let index in r.data.list){
                        var rowData=r.data.list[index];
                        var key = temp.getSelectedTabKey(rowData);
                        if(temp.selectedTableDatas.has(key)){

                            rowData.checked=true;
                        }
                    if(temp.disableSwitch !=undefined && temp.disableSwitch instanceof Function){
                        temp.disableSwitch(rowData);
                    }
                }

                temp.tableData = r.data.list;
            })
        },
        clickNode: function (node) {
            this.getNodeInfos(node)
        },
        getSelectedTabKey:function(rowData){
            return this.tabSelectedKeyPre + rowData[this.key];
        },
        getSelectedTreeKey:function(node){
            return this.treeSelectedKeyPre+node.id
        },
        selectedTableDatasChange: function (selectedDatas, unSelectedDatas) {
            var temp = this;
            for (let rowData of selectedDatas) {
                var key = temp.getSelectedTabKey(rowData);
                if (!temp.selectedTableDatas.has(key)) {
                    temp.selectedTableDatas.set(key, rowData);
                }
            }

            for (let rowData of unSelectedDatas) {
                var key = temp.getSelectedTabKey(rowData);
                if (temp.selectedTableDatas.has(key)) {
                    temp.selectedTableDatas.delete(key);
                }
            }
            var selectedTabDatas = temp.produceArray(temp.selectedTableDatas.values());
            if(temp.selectedTabDataChange!=undefined&&temp.selectedTabDataChange instanceof  Function){
                temp.selectedTabDataChange(selectedTabDatas);
            }

        },
        selectedTreeDatasChange: function (node) {
            var temp = this;
            var key =temp.getSelectedTreeKey(node);
            if (!temp.selectedTreeDatas.has(key)) {
                if(node.checked){
                    temp.selectedTreeDatas.set(key, node)
                }
            }else{
                if(!node.checked){
                    temp.selectedTreeDatas.delete(key)
                }
            }
            var selectedTreeDatas = temp.produceArray(temp.selectedTreeDatas.values());
            if(temp.selectedTreeDataChange!=undefined&&temp.selectedTreeDataChange instanceof  Function){
                temp.selectedTreeDataChange(selectedTreeDatas);
            }
        },
        uncheckTabData:function(rowData){
            var temp=this;
            temp.selectedTableDatasChange([],[rowData]);
            this.$refs.nzTable.uncheckTabData(rowData);
        },
        uncheckAllTabData:function(){
            this.$refs.nzTable.uncheckAllTabData();
        },
        uncheckTreeData:function(treeNode){
            var temp=this;
            treeNode.checked=false;
            temp.selectedTreeDatasChange(treeNode); //通知父组件
            temp.$refs.treeChild.uncheckNode(treeNode);//通知子组件
        },
        uncheckAllTreeData:function(){
            var temp=this
            this.$refs.treeChild.uncheckAllNode();
            temp.selectedTreeDatas.clear();
            var selectedTreeDatas = temp.produceArray(temp.selectedTreeDatas.values());
            if(temp.selectedTreeDataChange!=undefined&&temp.selectedTreeDataChange instanceof  Function){
                temp.selectedTreeDataChange(selectedTreeDatas);
            }
        },
        produceArray: function (mapIterator) {
            if (mapIterator != null || mapIterator != undefined) {
                var arr = [];
                for (let val of mapIterator) {
                    arr.push(val)
                }
                return arr;
            }
            return null;
        }

    },
    watch: {
        notifySelectedTabDatas:{
            handler: function (val, oldVal) {
                this.initTab();

            },
            deep: true,
        },

        notifySelectedTreeDatas:{
            handler: function (val, oldVal) {
                this.initTree();

            },
            deep: true,
        },
    },
    mounted() {

    }
})
</script>