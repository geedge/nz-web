<script>
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'sys/user/list',
        datatype: "json",
        colModel: [			
			{ label: 'ID', name: 'userId', index: "user_id", width: 45, key: true, hidden: true },
			{ label: '<@spring.message "user.username"/>', name: 'username', width: 75 },
			{ label: '<@spring.message "user.email"/>', name: 'email', width: 90 },
			{ label: '<@spring.message "common.status"/>', name: 'status', width: 60, formatter: function(value, options, row){
				return value === 0 ? 
					'<span class="label label-danger"><@spring.message "common.disabled"/></span>' : 
					'<span class="label label-success"><@spring.message "common.normal"/></span>';
			}},
			{ label: '<@spring.message "common.createTime"/>', name: 'createTime', index: "create_time", width: 85}
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
var setting = {
    data: {
        simpleData: {
            enable: true,
            idKey: "deptId",
            pIdKey: "parentId",
            rootPId: -1
        },
        key: {
            url:"nourl"
        }
    }
};
var ztree;
//引用vuelidate组件
Vue.use(window.vuelidate.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-builtin-validators
var required = window.validators.required;
var email = window.validators.email;
var helpers = window.validators.helpers;//引入自定义校验规则助手 helpers
//自定义规则，校验业务系统名称不能重复;
var checkName = function(value, user) {
	if (helpers.req(user.username) && helpers.req(value)) {
		var params = "?name=" + user.username;
		if (user.userId) {
			params = params + "&id=" + user.userId;
		}
		var flag = true;
		$.ajax({
			type: "get",
			url: baseURL + "sys/user/nameCheck" + params,
			async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
			dataType: "json",
			success: function(r){
				console.info(r)
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
        q:{
            username: null
        },
        showList: true,
        title:null,
        roleList:{},
        user:{
            status:1,
            deptId:null,
            deptName:null,
            roleIdList:[],
            usergroupIds: [],
            username: '',
            userId: null,
            password: '',
            email: ''
        },
        usergroups: []
    },
    validations: function() { //定义校验规则
    	var valid = {
			user: {
				username: {
					required,
					checkName
				},
				email: {
					required,
					email
				},
				password: {required}
			}
    	};
    	/*if (!this.user.userId) {
    		valid.user.password = {required};
    	}
    	console.info(valid)*/
    	return valid;
	},
    methods: {
        query: function () {
            vm.reload();
        },
        add: function(){
            vm.showList = false;
            vm.title = "<@spring.message 'common.add'/>";
            vm.roleList = {};
            vm.user = {deptName:null, deptId:null, status:1, roleIdList:[], usergroupIds: [], username: '', userId: null, password: '', email: ''};

            //获取角色信息
            this.getRoleList();

            //vm.getDept();
        },
        update: function () {
            var userId = getSelectedRow();
            if(userId == null){
                return ;
            }

            vm.showList = false;
            vm.title = "<@spring.message 'common.edit'/>";

            vm.getUser(userId);
            //获取角色信息
            this.getRoleList();
        },
        permissions: function () {
            var userId = getSelectedRow();
            if(userId == null){
                return ;
            }

            window.location.href=baseURL+"sys/permissions/index/"+userId;
        },
        del: function () {
            var userIds = getSelectedRows();
            if(userIds == null){
                return ;
            }

            confirm("<@spring.message 'common.confirmDelete'/>", function(){
                $.ajax({
                    type: "POST",
                    url: baseURL + "sys/user/delete",
                    contentType: "application/json",
                    data: JSON.stringify(userIds),
                    success: function(r){
                        if(r.code == 200){
                            alert("<@spring.message 'common.success'/>", function(){
                                vm.reload();
                            });
                        }else{
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        saveOrUpdate: function () {
        	if (!vm.validate()) { //在下方定义的validate方法
				return;
			}
            var url = vm.user.userId == null ? "sys/user/save" : "sys/user/update";
            $.ajax({
                type: "POST",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.user),
                success: function(r){
                    if(r.code === 200){
                        alert("<@spring.message 'common.success'/>", function(){
                            vm.reload();
                        });
                    }else{
                        alert(r.msg);
                    }
                }
            });
        },
        getUser: function(userId){
            $.get(baseURL + "sys/user/info/"+userId, function(r){
                vm.user = r.user;

                //vm.getDept();
            });
        },
        getRoleList: function(){
            $.get(baseURL + "sys/role/select", function(r){
                vm.roleList = r.list;
            });
        },
        getAllUsergroups: function(id) {
			$.get(baseURL + "sys/usergroup/list?limit=999", function(r){
                vm.usergroups = r.data.list;
            });
		},
        reload: function () {
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam','page');
            $("#jqGrid").jqGrid('setGridParam',{
                postData:{'username': vm.q.username},
                page:1
            }).trigger("reloadGrid");
        },
		validate: function () {//提交时的校验
			vm.$v.user.$touch(); //提交时，不论是否已触发校验，都触发一下($dirty=false -> true)
			return !vm.$v.user.$error;
		}
    },
    created: function() {
    	this.getAllUsergroups();
    }
});
</script>