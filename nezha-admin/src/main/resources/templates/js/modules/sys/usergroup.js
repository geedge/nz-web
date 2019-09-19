<script>
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'sys/usergroup/list',
        datatype: "json",
        colModel: [		
            { label: 'ID', name: 'id', width: 30, key: true, hidden: true },
            { label: '<@spring.message "common.name"/>', name: 'name', sortable: false },
			{ label: '<@spring.message "common.createTime"/>', name: 'createDate', sortable: false },
			{ label: '<@spring.message "common.remark"/>', name: 'remark', width: 80 }
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
        },
    });
});
//引用vuelidate组件
Vue.use(window.vuelidate.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-builtin-validators
var required = window.validators.required;
var helpers = window.validators.helpers;//引入自定义校验规则助手 helpers
//自定义规则，校验业务系统名称不能重复;
var checkName = function(value, usergroup) {
	if (helpers.req(usergroup.name) && helpers.req(value)) {
		var params = "?name=" + usergroup.name;
		if (usergroup.id) {
			params = params + "&id=" + usergroup.id;
		}
		var flag = true;
		$.ajax({
			type: "get",
			url: baseURL + "sys/usergroup/nameCheck" + params,
			async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
			dataType: "json",
			success: function(r){
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
	el:'#rrapp',
	data:{
		showList: true,
		title: null,
		usergroup: {
			name: "",
			remark: "",
			id: null,
			systemIds: [],
			userIds: [],
			systems: [],
			users: []
		},
		users: [],
		systems: []
	},
	validations: { //定义校验规则
		usergroup: {
			name: {
				required,
				checkName
			}
		}
	}, 
	methods: {
		add: function(){
			vm.$v.usergroup.$reset(); //进入新增页面时，将校验的触发状态清空($dirty=true -> false)
			vm.showList = false;
			vm.title = "@spring.message 'common.add'/>";
			vm.usergroup = {name:null,systems:[],users:[],remark:null,id:null};
		},
		update: function () {
			vm.$v.usergroup.$reset(); //进入新增页面时，将校验触发状态清空($dirty=true -> false)
			var id = getSelectedRow();
			if(id == null){
				return ;
			}
			vm.showList = false;
            vm.title = "<@spring.message 'common.edit'/>";
            vm.getInfo(id);
		},
		getInfo: function(id){
			$.get(baseURL + "sys/usergroup/detail?id="+id, function(r){
                vm.usergroup = r.data;
                vm.usergroup.systemIds = [];
                for (var i = 0; i < vm.usergroup.systems.length; i++) {
                	vm.usergroup.systemIds[i] = vm.usergroup.systems[i].id;
                }
                vm.usergroup.userIds = [];
                for (var i = 0; i < vm.usergroup.users.length; i++) {
                	vm.usergroup.userIds[i] = vm.usergroup.users[i].userId;
                }
            });
		},
		getAllSystems: function(id) {
			$.get(baseURL + "system/list?limit=999", function(r){
                vm.systems = r.data.list;
            });
		},
		getAllUsers: function(id) {
			$.get(baseURL + "sys/user/list?limit=999", function(r){
				vm.users = r.page.list;
			});
		},
		del: function (event) {
			var ids = getSelectedRows();
			if(ids == null){
				return ;
			}
			confirm("<@spring.message 'common.confirmDelete'/>", function(){
				$.ajax({
					type: "DELETE",
				    url: baseURL + "sys/usergroup/delete",
                    contentType: "application/json",
				    data: JSON.stringify(ids),
				    success: function(r){
						if(r.code == 200){
							alert("<@spring.message 'common.success'/>", function(index){
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
			var url = vm.usergroup.id == null ? "sys/usergroup/save" : "sys/usergroup/update";
			$.ajax({
				type: vm.usergroup.id == null ? "POST" : "PUT",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.usergroup),
			    success: function(r){
			    	if(r.code === 200){
						alert("<@spring.message 'common.success'/>", function(index){
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
                page:1
            }).trigger("reloadGrid");
		},
		validate: function () {//提交时的校验
			vm.$v.usergroup.$touch(); //提交时，不论是否已触发校验，都触发一下($dirty=false -> true)
			return !vm.$v.usergroup.$error;
		}
	},
	created: function() {
		this.getAllSystems();
		this.getAllUsers();
	}
});
</script>