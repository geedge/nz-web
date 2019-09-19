$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + '/deteSet/list',
        datatype: "json",
        colModel: [
            {label: 'ID', name: 'id', width: 30, key: true,hidden:true},
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


var vm=new Vue({
    data:{
        showList:true,
        setStateArr:[]
    },
    created:function(){
        this.getSetState();
    },
    methods:{
        getSetState:function(){

            $.getJSON(baseURL + "sys/dict/all?type=setState&_" + $.now(), function (r) {

                for (var i = 0; i < r.data.length; i++) {
                    var m = {"code": r.data[i].code, "value": r.data[i].value}
                    vm.setStateArr.push(m.value);
                }

            })
        },
        query: function () {
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'name': vm.params.name},
                page: page
            }).trigger("reloadGrid");
        }
    }

})