$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'sys/i18n/list',
        datatype: "json",
        colModel: [		
            { label: 'ID', name: 'id', width: 30, key: true },
            { label: 'CODE', name: 'code', sortable: false, width: 60 },
			{ label: '语言', name: 'lang', sortable: false, width: 60 },
			{ label: 'VALUE', name: 'value', width: 100 },
			{ label: '备注', name: 'remark', width: 80 }
        ],
		viewrecords: true,
        height: 385,
        rowNum: 10,
		rowList : [10,30,50],
        rownumbers: true, 
        rownumWidth: 25, 
        autowidth:true,
        multiselect: true,
        pager: "#jqGridPager",
        jsonReader : {
            root: "page.list",
            page: "page.currPage",
            total: "page.totalPage",
            records: "page.totalCount"
        },
        prmNames : {
            page:"page", 
            rows:"limit", 
            order: "order"
        },
        gridComplete:function(){
        	//隐藏grid底部滚动条
        	$("#jqGrid").closest(".ui-jqgrid-bdiv").css({ "overflow-x" : "hidden" }); 
        }
    });
});
//注册select组件
Vue.component('multiselect', window.VueMultiselect.default);

var vm = new Vue({
	el:'#rrapp',
	data:{
		q:{
            code: null,
            lang: null
		},
		showList: true,
		title: null,
		i18n: {
			code: null,
			lang: null,
			value: null,
			remark: null,
			id: null
		},
		langList: []
	},
	methods: {
		query: function () {
			vm.reload();
		},
		getLangList: function(event) {
			$.getJSON(baseURL + "sys/dict/all?type=lang&_"+$.now(), function(r){
				var langList = [];
				for (var i = 0; i < r.data.length; i++) {
					langList.push(r.data[i].value);
				}
				vm.langList = langList;
			});
		},
		add: function(){
			vm.showList = false;
			vm.title = "新增";
			vm.i18n = {code:null,lang:null,value:null,remark:null,id:null};
		},
		update: function () {
			var id = getSelectedRow();
			if(id == null){
				return ;
			}
			vm.showList = false;
            vm.title = "修改";
            vm.getInfo(id);
		},
		getInfo: function(id){
			$.get(baseURL + "sys/i18n/detail?id="+id, function(r){
                vm.i18n = r.data;
            });
		},
		del: function (event) {
			var ids = getSelectedRows();
			if(ids == null){
				return ;
			}
			
			confirm('确定要删除选中的记录？', function(){
				$.ajax({
					type: "DELETE",
				    url: baseURL + "sys/i18n/delete",
                    contentType: "application/json",
				    data: JSON.stringify(ids),
				    success: function(r){
						if(r.code == 200){
							alert('操作成功', function(index){
								vm.reload();
							});
						}else{
							alert(r.msg);
						}
					}
				});
			});
		},
		saveOrUpdate: function (event) {
			var url = vm.i18n.id == null ? "sys/i18n/save" : "sys/i18n/update";
			$.ajax({
				type: vm.i18n.id == null ? "POST" : "PUT",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.i18n),
			    success: function(r){
			    	if(r.code === 200){
						alert('操作成功', function(index){
							vm.reload();
						});
					}else{
						alert(r.msg);
					}
				}
			});
		},
		reload: function (event) {
			vm.showList = true;
			var page = $("#jqGrid").jqGrid('getGridParam','page');
			$("#jqGrid").jqGrid('setGridParam',{ 
                postData:{'code': vm.q.code, 'lang': vm.q.lang},
                page:page
            }).trigger("reloadGrid");
		}
	},
	created: function(){
		this.getLangList();
	}
});