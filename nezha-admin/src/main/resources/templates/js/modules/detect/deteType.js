<script>
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + '/deteType/list',
        datatype: "json",
        colModel: [
            {label: 'ID', name: 'id', width: 30, key: true,hidden:true},
            {label: '<@spring.message "common.name"/>', name: 'name', sortable: true, width: 60},
            {
                label: '<@spring.message "deteType.method"/>',
                name: 'method',
                sortable: false,
                width: 60,
                formatter: function (value, options, rowData) {
                    return vm.deteMethodsArr[value - 1];
                }
            },
            {label: '<@spring.message "deteType.deteInterval"/>', name: 'deteInterval', sortable: false, width: 100},
            {label: '<@spring.message "deteType.version"/>', name: 'version', sortable: false, width: 100},
            {label: '<@spring.message "common.opTime"/>', name: 'opTime', sortable: true, width: 100},
            {label: '<@spring.message "common.remark"/>', name: 'remark', sortable: false, width: 80}
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

Vue.component("nz-detect-addtype",addType)
var vm = new Vue({
    el: '#deteType',
    data: {
        params: {
            name: ''
        },
        showList: true,
        title: 'title',
        deteMethodsArr:[],
        typeId:null
    },

    created: function () {
        this.getDeteMethod();
    },
    methods: {
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
            vm.showList=false;
            vm.typeId=null;
        },
        update: function () {

            var id = getSelectedRow();
            if (id == null) {
                return;
            }
            vm.typeId=id;
            console.log("update-->"+vm.typeId);
            vm.showList=false;
        },
        del: function (event) {
            var ids = getSelectedRows();
            if (ids == null) {
                return;
            }
            confirm("<@spring.message 'common.confirmDelete'/>", function () {
                $.ajax({
                    type: "DELETE",
                    url: baseURL + "/deteType/delete",
                    contentType: "application/json",
                    data: JSON.stringify(ids),
                    success: function (r) {
                        if (r.code == 200) {
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
        getDeteMethod: function () {
            $.getJSON(baseURL + "sys/dict/all?type=dtmethond&_" + $.now(), function (r) {
                var deteMethodsArr = [];
                for (var i = 0; i < r.data.length; i++) {
                    var m = {"code": r.data[i].code, "value": r.data[i].value}
                    deteMethodsArr.push(r.data[i].value);
                    if ("1" == r.data[i].code) { //默认为脚本
                        vm.deteMethod = m;

                    }
                }
                vm.deteMethodsArr = deteMethodsArr;
            });
        },

    },
    watch: {


    }
});
</script>