$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'mission/list',
        datatype: "json",
        colModel: [		
            { label: 'ID', name: 'id', width: 30, key: true },
            { label: '任务名称', name: 'name', sortable: false, width: 60 },
			{ label: '任务类型', name: 'type', sortable: false, width: 60,formatter:function(value,options,rowData){return vm.missionList[value-1]} },
			{ label: '周期任务', name: 'isLoop', width: 100,formatter: "select", editoptions:{value:"0:否;1:是"} },
			{ label: '任务状态', name: 'state', width: 100,formatter: "select", editoptions:{value:"0:非周期;1:运行;2:暂停;3:终止"} },
			/*{ label: '查看人', name: 'viewer', width: 100 },*/
			{ label: '备注', name: 'remark', width: 80 },
			{ label: '创建时间', name: 'opTime', width: 100}
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
//引用vuelidate组件
Vue.use(window.vuelidate.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-v-values
var required = window.validators.required;

var vm = new Vue({
	el:'#missionVIew',
	data:{
		q:{
            name: null
		},
		showList: true,
		title: null,
		mission:{
			id:"",
			name:"",
			ids:[],
			state:""
		}
	},
	validations: { //定义校验规则
		mission:{
			name:{
				required
			}
		}
	}, 
	methods: {
		query: function () {
			vm.reload();
		},
		getMissionList: function(event) {
			$.getJSON(baseURL + "sys/dict/all?type=missiontype&_"+$.now(), function(r){
				var missionList = [];
				for (var i = 0; i < r.data.length; i++) {
					missionList.push(r.data[i].value);
				}
				vm.missionList = missionList;
			});
		},
		add: function(){
			vm.$v.mission.$reset(); //进入新增页面时，将校验的触发状态清空($dirty=true -> false)
			vm.showList = false;
			vm.title = "新增";
			vm.mission = {name:null,remark:null,id:null};
		},
		update: function () {
			vm.$v.mission.$reset(); //进入新增页面时，将校验触发状态清空($dirty=true -> false)
			var id = getSelectedRow();
			if(id == null){
				return ;
			}
			vm.showList = false;
            vm.title = "修改";
            vm.getInfo(id);
		},
		getInfo: function(id){
			$.get(baseURL + "mission/detail?id="+id, function(r){
                vm.mission = r.data;
            });
		},
		saveOrUpdate: function (event) {
			if (!vm.validate()) { //在下方定义的validate方法
				return;
			}
			var url = vm.mission.id == null ? "mission/save" : "mission/update";
			$.ajax({
				type: vm.mission.id == null ? "POST" : "PUT",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.mission),
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
				postData:{'name': vm.q.name},
                page:page
            }).trigger("reloadGrid");
		},
		validate: function () {
			vm.$v.mission.$touch(); //提交时，不论是否已触发校验，都触发一下($dirty=false -> true)
			return !vm.$v.mission.$error;
		},
		converseStatus:function(status){
			vm.mission.ids=[];
			var id = getSelectedRow();
			if(id == null){
				return ;
			}
			vm.mission.ids.push(id);
			vm.mission.state=status;
			confirm('确定操作？', function(){
				$.ajax({
					type: "PUT",
				    url: baseURL + "mission/loopStateChange",
	                contentType: "application/json",
				    data: JSON.stringify(vm.mission),
				    success: function(r){
				    	vm.mission.ids=[];
				    	if(r.code === 200){
							alert('操作成功', function(index){
								vm.reload();
							});
						}else{
							alert(r.msg);
						}
					}
				});
			});
		}
	},
	created: function(){
		// 初始化进入执行方法
		this.getMissionList();
	}
});