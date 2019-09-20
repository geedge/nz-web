//注册select组件
Vue.component('multiselect', window.VueMultiselect.default);
//引用vuelidate组件
Vue.use(window.vuelidate.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-v-values
var required = window.validators.required;
var helpers = window.validators.helpers;
var integer = window.validators.integer;

var pannel = Vue.extend({
    template: "#popUpWindow",
    props:{
        znode:{
            type:Array
        },
        columns:{
            type:Array
        },
        tdata:{
            type:Array
        },
        uncheckNode:{
            type:Object
        },
        tableRowClassChange:{
            type:Function
        }
    },
    data:function(){
        return {
            selectedRow:[]
        }
    },
    created: function () {

    },
    methods: {
        clickNode:function(node){
            this.$emit("node-click",node);
        },
        checkNode:function(node){
            this.$emit("node-check",node);
        },
        selectALL(selection){
            this.$emit("row-check",selection);
        },
        selectGroupChange(selection){
            this.$emit("row-check",selection);
        },
        selectChange(selection,rowData){
        },
        resetTree:function(){
            console.log("resetTree");
            this.$refs.treeChild.initZtree();
        }

    },
    watch:{
        tdata:{
            handler(){
            },
            immediate:true,
            deep:true
        }
    }
})


var znodeTree = Vue.extend({
    template: "#leftTree",
    props: {
        znode: {
            type: Array
        },
        selectedNodeId: "",
        uncheckNode:{
            type:Object
        }

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
                    checkStyle:"checkbox",
                    nocheckInherit: true,
                    chkboxType: { "Y": "", "N": "" }
                },
                callback: {
                    onClick: this.zTreeOnClick,
                    onCheck: this.zTreeOnCheck
                }
            },
            zTree:null

        };
    },
    methods: {
        // 初始化ztree
        initZtree() {
        	console.log("初始化树")
        	console.log(this.setting)
        	console.log(this.znode)
           this.zTree=$.fn.zTree.init($("#beTree"), this.setting, this.znode);
           this.zTree.expandAll(true);
        },
        // 树节点点击事件
        zTreeOnClick: function (event, treeId, treeNode) {
            this.$emit("tree-click", treeNode);
        },
        zTreeOnCheck:function(event, treeId, treeNode){
            console.log("option node-->"+JSON.stringify(treeNode))
            this.$emit("tree-select", treeNode);
        }
    },
    watch: {
        // 监听树节点的变化更新树
        znode: function (val) {
            this.initZtree();
        },
        uncheckNode:function(){
            if(this.uncheckNode.tId==undefined||this.uncheckNode.tId==null){ //解决更新操作中 找不到关联节点的问题
                this.uncheckNode=this.zTree.getNodeByParam("id",this.uncheckNode.id,null);
            }
            this.uncheckNode.checked=false;
            this.zTree.updateNode(this.uncheckNode,false);
        }
    },
    mounted() {
        this.initZtree();
    }
})



function getSysDicArr(dtype){
    var container=[];
    $.ajax({
        url:baseURL+"sys/dict/all?type="+dtype+"&_" + $.now(),
        async:false,
        type:"GET",
        success:function(r){
            for (var i = 0; i < r.data.length; i++) {
                var m = {"code": r.data[i].code, "value": r.data[i].value}
                container.push(m.value);
            }
        },
        error : function(result) {
            console.error("error-->"+JSON.stringify(result));
        }
    })
    return container;
}

function getSysDicMap(dtype){
    var container=[];
    $.ajax({
        url:baseURL+"sys/dict/all?type="+dtype+"&_" + $.now(),
        async:false,
        type:"GET",
        success:function(r){
            for (var i = 0; i < r.data.length; i++) {
                var m = {"code": r.data[i].code, "value": r.data[i].value}
                container.push(m);
            }
        },
        error : function(result) {
            console.error("error-->"+JSON.stringify(result));
        }
    })
    return container;
}

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