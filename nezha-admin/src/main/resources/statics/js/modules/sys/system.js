$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'system/list',
        datatype: "json",
        colModel: [		
            { label: 'ID', name: 'id', width: 30, key: true, hidden: true },
            { label: '名称', name: 'name', sortable: false },
			{ label: '创建时间', name: 'createDate', sortable: false },
			{ label: '备注', name: 'remarks', width: 80 },
			{ label: '是否内置', name: 'builtIn', hidden: true }
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
            root: "data.list",
            page: "data.currPage",
            total: "data.totalPage",
            records: "data.totalCount"
        },
        prmNames : {
            page:"page", 
            rows:"limit", 
            order: "order"
        },
        gridComplete:function(){
        	//隐藏grid底部滚动条
        	$("#jqGrid").closest(".ui-jqgrid-bdiv").css({ "overflow-x" : "hidden" }); 
        	
	        /*builtin=1的记录禁用复选框*/
	        //1.先将对应记录的checkbox设为disabled
        	var dataRows = $("#jqGrid").find(".jqgrow");
        	$.each(dataRows, function(index) {
        		var builtIn = $(this).find("td").get(6);//builtIn字段所在列
        		if ($(builtIn).text() == "1") {
        			$(this).find("input[type=checkbox]").attr("disabled", "disabled");
        		}
        	});
        },
        //2.单选时，不勾选复选框
        beforeSelectRow: function(rowid, e) {
            var cbsdis = $("tr#" + rowid + ".jqgrow > td > input[type=checkbox]:disabled");
            if (cbsdis.length === 0) {
                return true;
            } else {
                return false;
            }
        },
        //3.全选时，不勾选对应复选框
        onSelectAll: function(rowids, status) {
            if (status) {
                var cbs = $("tr.jqgrow > td > input[type=checkbox]:disabled");
                cbs.removeAttr("checked");
                cbs.parent().parent().removeClass("success");
            }
        }
    });
});
//引用vuelidate组件
Vue.use(window.vuelidate.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-builtin-validators
var required = window.validators.required;
var helpers = window.validators.helpers;//引入自定义校验规则助手 helpers

var vm = new Vue({
	el:'#rrapp',
	data:{
		showList: true,
		title: null,
		system: {
			name: "",
			remarks: "",
			id: null,
			usergroupIds: [],
			usergroups: []
		},
		usergroups: []
	},
	validations: { //定义校验规则
		system: {
			name: {
				required
			}
		}
	}, 
	methods: {
		add: function(){
			vm.$v.system.$reset(); //进入新增页面时，将校验的触发状态清空($dirty=true -> false)
			vm.showList = false;
			vm.title = "新增";
			vm.system = {code:null,lang:null,value:null,remark:null,id:null};
		},
		update: function () {
			vm.$v.system.$reset(); //进入新增页面时，将校验触发状态清空($dirty=true -> false)
			var builtIn = getParam('builtIn');
			var id = getSelectedRow();
			if(id == null){
				return ;
			}
			vm.showList = false;
            vm.title = "修改";
            vm.getInfo(id);
		},
		getInfo: function(id){
			$.get(baseURL + "system/detail?id="+id, function(r){
                vm.system = r.data;
                vm.system.usergroupIds = [];
                for (var i = 0; i < vm.system.usergroups.length; i++) {
                	vm.system.usergroupIds[i] = vm.system.usergroups[i].id;
                }
            });
		},
		getAllUsergroups: function(id) {
			$.get(baseURL + "sys/usergroup/list", function(r){
                vm.usergroups = r.data.list;
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
				    url: baseURL + "system/delete",
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
			if (!vm.validate()) { //在下方定义的validate方法
				return;
			}
			var url = vm.system.id == null ? "system/save" : "system/update";
			$.ajax({
				type: vm.system.id == null ? "POST" : "PUT",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.system),
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
                page:page
            }).trigger("reloadGrid");
		},
		validate: function () {//提交时的校验
			vm.$v.system.$touch(); //提交时，不论是否已触发校验，都触发一下($dirty=false -> true)
			return !vm.$v.system.$error;
		}
	},
	created: function() {
		this.getAllUsergroups();
	}
});