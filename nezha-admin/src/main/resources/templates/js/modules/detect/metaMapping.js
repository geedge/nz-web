<script>
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'deteType/dicList',
        datatype: "json",
        colModel: [
            {label: 'ID', name: 'id', width: 30, key: true, hidden: true,},
            {
                label: '<@spring.message "metaMapping.type"/>',
                name: 'type',
                sortable: true,
                width: 60,
                // cellattr: function(rowId, tv, rawObject, cm, rdata) {
                //     //合并单元格
                //     return 'id=\'type' + rowId + "\'";
                //     //if (Number(rowId) < 5) { return ' colspan=2' }
                // }
            },
            {
                label: '<@spring.message "metaMapping.key"/>',
                name: 'val',
                sortable: false,
                width: 100,
                formatter:function(value, options, rowData){
                    return  spliteTable(rowData.mappings,"val")

                }
            },
            {
                label: '<@spring.message "metaMapping.val"/>',
                name: 'mapping',
                sortable: false,
                width: 100,
                formatter:function(value, options, rowData){
                    return  spliteTable(rowData.mappings,"mapping")
                }
            },
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
            // var gridName = "jqGrid";
            // Merger(gridName, 'type');
            console.log($(".costomTable").parent().css("padding-left","0px"))
            console.log($(".costomTable").parent().css("padding-right","0px"))
        }
    });
});

var arrayEmpty=function(value,metaMappings){
    return !(metaMappings.mappings == null || metaMappings.mappings == undefined || metaMappings.mappings.length < 1);
}
var typeRepeat = function (type, metaMappings) {
    if (helpers.req(type) && helpers.req(metaMappings)) {
        var params = "?type=" + type;
        var flag = true;
        $.ajax({
            type: "get",
            url: baseURL + "/deteType/dicNameCheck" + params,
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
        el: "#metaMapping",
        data: {
            showList: true,
            params: {
                type: '',
            },
            title: '<@spring.message "deteTypeMeta"/>',
            metaMappings:{
                type:'',
                mappings:[
                    {
                        id:null,
                        type:null,
                        val:'',
                        mapping:'',
                        delFlag:'0'
                    }
                ]
            },
            hackReset:true,
            newMapping:false
        },
        created: function () {

        },
        validations: {
            metaMappings:{
                type:{
                    required,
                    typeRepeat
                },
                mappings:{
                    arrayEmpty
                }
            }
        },
        methods: {
            init: function (systemId) {

            },
            query: function () {
                vm.showList = true;
                var page = $("#jqGrid").jqGrid('getGridParam', 'page');
                $("#jqGrid").jqGrid('setGridParam', {
                    postData: {'type': vm.params.type},
                    page: 1
                }).trigger("reloadGrid");
            },
            reload: function () {
                vm.query();
            },
            add: function () {
                vm.showList=false;
                hackReset();
                vm.$v.metaMappings.$reset();
                vm.metaMappings={
                    type:'',
                    mappings:[
                        {
                            id:null,
                            type:null,
                            val:'',
                            mapping:'',
                            delFlag:'0'
                        }
                    ]
                }
                vm.newMapping=true;
                $("#saveOrUpdate").click(vm.save)
            },
            toUpdate: function () {
                var type=getSelectedType();
                if(type == null){
                    return ;
                }
                vm.getInfo(type);
                vm.showList=false;
                hackReset();
                vm.$v.metaMappings.$reset();
                vm.newMapping=false;
                $("#saveOrUpdate").click(vm.update)
            },
            getInfo: function (type) {
                $.ajax({
                    url:baseURL + "/deteType/dicList?type=" + type,
                    type:"GET",
                    async:false,
                    success:function (r) {
                        vm.metaMappings = r.data.list[0];
                    }
                });
            },
            save: function () {
                console.log('save')
                if (vm.validate()) { //在下方定义的validate方法
                    return;
                }
                var url = "deteType/saveDic";
                var method = "POST";
                saveOrUpdate(url,method);
            },
            update:function(){
                if (vm.validate()) { //在下方定义的validate方法
                    return;
                }
                var url = "deteType/updateDic";
                var method = "PUT";
                saveOrUpdate(url,method);
            },
            del: function (event) {
                var types=getSelectedTypes();
                if(types == null){
                    return ;
                }
                console.log("types-->"+JSON.stringify(types))
                confirm("<@spring.message 'common.confirmDelete'/>", function () {
                    $.ajax({
                        url:baseURL + "/deteType/deleteDic",
                        type:"DELETE",
                        datatype:"JSON",
                        data:JSON.stringify(types),
                        contentType:"application/json",
                        async:false,
                        success:function (r) {
                            if (r.code === 200) {
                                alert('<@spring.message "common.success"/>', function (index) {
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
                vm.$v.metaMappings.$touch();
                var elementError=false;
                if(vm.$refs.metaMapping!=undefined){
                    var elementValidateResults=[];
                    for(var i=0;i<vm.$refs.metaMapping.length;i++){
                        var refObj=vm.$refs.metaMapping[i];
                        elementValidateResults.push(refObj.validate());
                    }

                    for (let i in elementValidateResults){
                        if(elementValidateResults[i]){
                            elementError=true;
                            break;
                        }
                    }
                }else{
                    elementError=true;
                }

                return vm.$v.metaMappings.$error||elementError;
            },
            formatmetaMappingToSimple: function () {

            },
            formatmetaMappingToObject: function(){

            },
            addMapping:function(){
                var mapping={
                    id:null,
                    type:'',
                    val:'',
                    mapping:'',
                    delFlag:'0'
                }
                this.metaMappings.mappings.push(mapping)
            },
            delMapping:function(index){
                var mapping=this.metaMappings.mappings[index];
                if(mapping.id==null||mapping.id==undefined){
                    this.metaMappings.mappings.splice(index,1);
                }else{
                    mapping.delFlag='1';
                }

                hackReset();
            }
        },
        watch:{

        },
        mounted() {

        }
    })


function spliteTable(array,field){
    var showDiv="<table class='costomTable' width='100%'>";
    if (array.length==1){
        showDiv+="<tr><td>"+array[0][field]+"</td></tr>"

    }else{
        $.each(array,function(i,v){
            if(i==0){
                showDiv+="<tr><td style='border-bottom: 1px solid #ddd;padding:0px 2px 8px 8px;'>"+v[field]+"</td></tr>"
            }else if(i == array.length-1){
                showDiv+="<tr><td style='padding:8px 2px 0px 8px'>"+v[field]+"</td></tr>"
            }else{
                showDiv+="<tr><td style='border-bottom: 1px solid #ddd;padding:8px 2px 8px 8px;'>"+v[field]+"</td></tr>"
            }
        })
    }
    showDiv+="</table>"
    return showDiv;

}

function saveOrUpdate(url,method){
    $.ajax({
        type: method,
        url: baseURL + url,
        contentType: "application/json",
        data: JSON.stringify(vm.metaMappings),
        success: function (r) {
            if (r.code === 200) {
                alert('<@spring.message "common.success"/>', function (index) {
                    vm.reload();
                });
            } else {
                alert(r.msg);
            }
        }
    });
}

function hackReset(){
    vm.hackReset= false //解决删除后子组件不刷新问题，利用v-if会销毁dom重新渲染的机制，强制刷新子组件
    vm.$nextTick(() => {
        vm.hackReset= true
    });
}
function getSelectedType() {
    var grid = $("#jqGrid");
    var rowKey = grid.getGridParam("selrow");
    if(!rowKey){
        alert("<@spring.message 'common.selectOne'/>");
        return ;
    }

    var selectedIDs = grid.getGridParam("selarrrow");
    if(selectedIDs.length > 1){
        alert("<@spring.message 'common.onlySelectOne'/>");
        return ;
    }
    var rowData = grid.jqGrid('getRowData',selectedIDs[0]);
    return rowData.type;
}

function getSelectedTypes() {
    var grid = $("#jqGrid");
    var rowKey = grid.getGridParam("selrow");
    if(!rowKey){
        alert("<@spring.message 'common.selectOne'/>");
        return ;
    }

    var selectedIDs = grid.getGridParam("selarrrow");
    var types=[];
    for(let i in selectedIDs){
        var rowData = grid.jqGrid('getRowData',selectedIDs[i]);
        types.push(rowData.type);
    }

    return types;
}

Vue.component("meta-mapping",{
    template:"#metaTemplate",
    props:{
        metaMapping:{
            type:Object
        }
    },
    data:function(){
        return {
            map:this.metaMapping
        }
    },
    create:function(){

    },
    mounted() {

    },
    watch: {
        metaMapping: function(){

        }
    },
    validations: {
        map:{
            val:{
                required
            },
            mapping:{
                required
            }
        }
    },
    methods: {
        validate:function(){
            this.$v.map.$touch();
            return this.$v.map.$error;
        }
    }
})
</script>