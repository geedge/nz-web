<script>
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'mission/list',
        datatype: "json",
        colModel: [		
            { label: 'ID', name: 'id', width: 30, key: true,hidden:true },
            { label: '任务名称', name: 'name', sortable: false, width: 60 },
			{ label: '任务类型', name: 'type', sortable: false, width: 60,formatter:function(value,options,rowData){return vm.missionTypeList[value-1].value} },
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

Vue.component("select-panel", pannel);
Vue.component("left-tree", znodeTree);
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
            name: null,
            type: null
		},
		showList: true,
		missionParam :true,
		title: null,
		mission:{
			id:"",
			name:"",
			ids:[],
			state:"",
			type:1,
			viewLevel:3,
			commandType:0,
			commandEnname:null,
			isLoop:0,
			loopInterval:null,
			param:{},
			userGroup:null,
			nodeGroupIds:[],
			nodeIds:[]
		},
		radio: '1',
		missionTypeList:[],
		commandType:[],
		viewLevel:[],
		userGroups:[],
		commandEnnameTypeList1:[],
		commandEnnameTypeList2:[],
		commandEnnameTypeList3:[],
		files:[],
		paramForExecuteInstruct:{
			pidFiles:[]
		},
		paramForExecuteInstructs:{},
		missionLoopEntity:{
			endTime:null,
			startTime:null
		},
		backup:{
			backUp:"",
			backUpTo:"",
			isAbs:0,
			exceptTmpStr:""
		},
		isUpgradeBackup:0,
		paramForUpgrade:{
            backups:[],
            cover:"",
            deleteTmpStr:'',
            groupName:"",
            isAbs:0,
            isCreateCover:0,
            permisson:"",
            username:""
        },
		paramForUpgrades:[],
		
		
		uncheckGroup: null, //标记左侧树取消选中的节点，用于左右联动
		nodeGroupsChecked: [], //选中的节点组
        nodeInfosChecked: [], //选中的节点
        showNodeSelect:false,
        nodeGroups: [],//select-panel 组件左侧树 填充数据
        nodeInfoData: [], //select-panel 组件右侧列表 填充数据
        columnModel: [    //select-panel 组件右侧列表 显示列设置  api doc：http://doc.huangsw.com/vue-easytable/app.html#/table/api
            {width: 60, titleAlign: 'center', columnAlign: 'center', type: 'selection', isFrozen: true},
            {field: 'name', title: '名称', width: 80, titleAlign: 'center', columnAlign: 'center', isResize: true},
            {field: 'ip', title: 'IP', width: 150, titleAlign: 'center', columnAlign: 'center', isResize: true},
            {
                field: 'type',
                title: '节点类型',
                width: 80,
                titleAlign: 'center',
                columnAlign: 'center',
                isResize: true,
                formatter: function (rowData, rowIndex, pagingIndex, field) {
                    switch (rowData.type) {
                        case "1":
                            return "服务器";
                        case "2":
                            return "网元";
                        default:
                            return "<span style='color:red;'>未知节点类型</span>";
                    }
                }
            },
            {
                field: 'state',
                title: '节点类型',
                width: 80,
                titleAlign: 'center',
                columnAlign: 'center',
                isResize: true,
                formatter: function (rowData, rowIndex, pagingIndex, field) {
                    switch (rowData.state) {
                        case "1":
                            return "上线";
                        case "2":
                            return "<span style='color:red;'>下线</span>";
                        default:
                            return "<span style='color:red;'>未知节点状态</span>";
                    }
                }
            }
        ]
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
				var missionTypeList = [];
				for (var i = 0; i < r.data.length; i++) {
					missionTypeList.push(r.data[i]);
				}
				vm.missionTypeList = missionTypeList;
			});
		},
		getcommandType: function(event) {
			$.getJSON(baseURL + "sys/dict/all?type=commandType&_"+$.now(), function(r){
				var commandType = [];
				for (var i = 0; i < r.data.length; i++) {
					commandType.push(r.data[i]);
				}
				vm.commandType = commandType;
			});
		},
		getCommandEnnameTypeList1: function(event) {
			$.getJSON(baseURL + "sys/dict/all?type=commandEnnameType1&_"+$.now(), function(r){
				var commandEnnameTypeList1 = [];
				for (var i = 0; i < r.data.length; i++) {
					commandEnnameTypeList1.push(r.data[i]);
				}
				vm.commandEnnameTypeList1 = commandEnnameTypeList1;
			});
		},
		getCommandEnnameTypeList2: function(event) {
			$.getJSON(baseURL + "sys/dict/all?type=commandEnnameType2&_"+$.now(), function(r){
				var commandEnnameTypeList2 = [];
				for (var i = 0; i < r.data.length; i++) {
					commandEnnameTypeList2.push(r.data[i]);
				}
				vm.commandEnnameTypeList2 = commandEnnameTypeList2;
			});
		},
		getCommandEnnameTypeList3: function(event) {
			$.getJSON(baseURL + "sys/dict/all?type=commandEnnameType3&_"+$.now(), function(r){
				var commandEnnameTypeList3 = [];
				for (var i = 0; i < r.data.length; i++) {
					commandEnnameTypeList3.push(r.data[i]);
				}
				vm.commandEnnameTypeList3 = commandEnnameTypeList3;
			});
		},
		getViewLevel:function(){
			$.getJSON(baseURL + "sys/dict/all?type=viewLevel&_"+$.now(), function(r){
				var viewLevel = [];
				for (var i = 0; i < r.data.length; i++) {
					viewLevel.push(r.data[i]);
				}
				vm.viewLevel = viewLevel;
			});
		},
		getUserGroups:function(){
			$.getJSON(baseURL + "sys/user/info?"+$.now(), function(r){
				var userGroups = [];
				for (var i = 0; i < r.user.usergroups.length; i++) {
					userGroups.push(r.user.usergroups[i]);
				}
				vm.userGroups = userGroups;
			});
		},
		add: function(){
			vm.$v.mission.$reset(); //进入新增页面时，将校验的触发状态清空($dirty=true -> false)
			vm.showList = false;
			vm.missionParam = false;
			vm.title = "新增";
			vm.missionLoopEntity={startTime:'',endTime:''}
			this.dataReset();
		},
		dataReset:function(){
			vm.mission = {
					name:null,
					remark:null,
					id:null,
					viewLevel:3,
					type:1,
					commandType:1,
					commandEnname:null,
					isLoop:0,
					loopInterval:null,
					param:{},
					viewerId:null,
					nodeGroupIds:[],
					nodeIds:[]
				};
			//清空界面数据
			vm.$refs.filePush.dataReset();
			vm.$refs.upgradeKillProcess.dataReset();
			vm.$refs.singleExec.dataReset();
			vm.$refs.upgradeExecStart.dataReset();
			vm.$refs.upgradeBackup.dataReset();
			vm.$refs.upgradeRecover.dataReset();
			vm.$refs.upgradeUpdate.dataReset();
			vm.$refs.upgradeServer.dataReset();
			vm.nodeInfoData = [];
            vm.nodeGroupsChecked = [];
            vm.nodeInfosChecked = [];
            vm.isUpgradeBackup=0;
            vm.paramForUpgrade={
                backups:[],
                cover:"",
                deleteTmpStr:'',
                groupName:"",
                isAbs:0,
                isCreateCover:0,
                permisson:"",
                username:""
            };
            vm.backup={
    			backUp:"",
    			backUpTo:"",
    			isAbs:0,
    			exceptTmpStr:""
    		};
    		vm.paramForUpgrades=[];
			
		},
		update: function () {
			vm.$v.mission.$reset(); //进入新增页面时，将校验触发状态清空($dirty=true -> false)
			var id = getSelectedRow();
			if(id == null){
				return ;
			}
			vm.showList = false;
			vm.missionParam = true;
            vm.title = "修改";
            vm.getInfo(id);
		},
		getInfo: function(id){
			$.get(baseURL + "mission/detail?id="+id, function(r){
                vm.mission = r.data;
            });
		},
		getFile(event) {
			this.file = event.target.files[0];
		},
		saveOrUpdate: function (event) {
			console.log(this.mission)
			console.log("任务参数")
			if (!vm.validate()) { //在下方定义的validate方法
				return;
			}
			//处理节点组 节点相关数据
            vm.formatMissionData();
			event.preventDefault();
            let formData = new FormData();
            //处理批量文件上传 
            if(this.mission.type==1){
            	this.files.forEach(function(o){
            		formData.append("files",o);
            	});
            }else{
            	//针对任务为指令的处理各种类型任务的参数
                this.mission.param.commandEnname=this.mission.commandEnname.code;
                formData.append("commandEnname",this.mission.commandEnname.code);
                formData.append("commandType",this.mission.commandType);
            	//处理任务为指令的周期参数
                if(this.mission.isLoop && this.mission.type==2){
                	formData.append("isLoop",this.mission.isLoop);
                	formData.append("loopInterval",this.mission.isLoop);
                	this.mission.param.missionLoopEntity=this.missionLoopEntity;
                }
            }
            
            //判断用户选择是否为用户组选项
            if(this.mission.viewLevel==1){
            	formData.append("viewerId",this.mission.viewerId.id);
            }
            if(this.mission.commandEnname!=null&&this.mission.commandEnname!=undefined){
	            //组装NC升级部署param参数
	            if(this.mission.commandEnname.code=='upgrade_agent'){
	            	if(this.isUpgradeBackup==1){
	            		this.paramForUpgrade.backups.push(this.backup);
	            	}
	            	this.mission.param.paramForUpgrade=this.paramForUpgrade;
	            	formData.append("files",this.file);
	            }
	            //组装DC升级部署或第三方升级部署的文件上传
	            if((this.mission.commandEnname.code=='upgrade_server')||(this.mission.commandEnname.code=='upgrade_other')){
	            	this.files.forEach(function(o){
	            		formData.append("files",o);
	            	});
	            }
            }    
            formData.append("name",this.mission.name);
            formData.append("param",JSON.stringify(this.mission.param));
            formData.append("nodeGroupIds",this.mission.nodeGroupIds);
            formData.append("nodeIds",this.mission.nodeIds);
            formData.append("type",this.mission.type);
            formData.append("viewLevel",this.mission.viewLevel);
			var url = vm.mission.id == null ? "mission/save" : "mission/update";
			$.ajax({
				type: vm.mission.id == null ? "POST" : "PUT",
			    url: baseURL + url,
			    contentType:false,
			    processData:false,
			    data:formData,
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
		changeStartTime:function(){
			this.missionLoopEntity.startTime = $("#beginDate").val();  
		},
		changeEndTime:function(){
			this.missionLoopEntity.endTime = $("#endDate").val();  
		},
		reload: function (event) {
			vm.showList = true;
			vm.missionParam = true;
			var page = $("#jqGrid").jqGrid('getGridParam','page');
			$("#jqGrid").jqGrid('setGridParam',{
				postData:
				{'name': vm.q.name,
				 'type': vm.q.type==null ? null :vm.q.type.code
				},
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
		},
		updateCommandEnname:function(){
			vm.mission.commandEnname=null;
		},
		formatMissionData: function () {
			console.log("处理任务节点相关")
            if(vm.mission.nodeGroupIds==null||vm.mission.nodeGroupIds==undefined){
                vm.mission.nodeGroupIds=[];
            }
            if(vm.mission.nodeIds==null||vm.mission.nodeIds==undefined){
                vm.mission.nodeIds=[];
            }
            $.each(vm.nodeGroupsChecked, function (i, v) {
                vm.mission.nodeGroupIds.push(v.id);
            })
            $.each(vm.nodeInfosChecked, function (i, v) {
                vm.mission.nodeIds.push(v.uuid);
            })
        },
		openNodePannel: function () {
			console.log("openPanel");
            vm.showNodeSelect = true;
            var index = layer.open({
                area: ['50%', '100%'],
                shade: 0,
                title: "节点/节点组",
                type: 1,
                maxmin: false,
                scrollbar: false,
                offset: 'r',
                closeBtn: 1,
                move: false,
                content: $("#layerView")
            });
        },
		/*columnClassChange: function (rowIndex, columnName, rowData) {
            if (rowData.state == '2') { //下线节点
                rowData._disabled = true;
                return "table-row-disabled";
            } else {
                if (vm.deteSet.typeId.method == "2") { //snmp 监测 节点类型 网元
                    if (rowData.type == '1') { //服务器
                        rowData._disabled = true;
                        return "table-row-disabled";
                    }
                } else {//snmp 监测 节点类型 服务器
                    if (rowData.type == '2') {//网元
                        rowData._disabled = true;
                        return "table-row-disabled";
                    }
                }
            }
            rowData._disabled=false;
            return "";
        },*/
        unCheckNodeInfo: function (index) {
            var delNode = JSON.parse(JSON.stringify(vm.nodeInfosChecked[index]));
            var delIndex = -1;
            $.each(vm.nodeInfoData, function (i, v) {
                for (var j = 0; j < vm.nodeInfosChecked.length; j++) {
                    var checkedNode = vm.nodeInfosChecked[j];
                    if (checkedNode.uuid == v.uuid) {
                        v._checked = true;
                    }
                }

                if (delNode.uuid == v.uuid) {
                    delIndex = i;
                }
            })
            vm.nodeInfosChecked.splice(index, 1);
            delNode._checked = false;
            // delNode._disabled=false;
            vm.nodeInfoData.splice(delIndex, 1, delNode);
        },
		updateCommandEnname:function(){
			vm.mission.param={};
			vm.mission.commandEnname=null;
		},
		getNodeList: function (nodeGroup) {
	        $.getJSON(baseURL + "node/list?groupId=" + nodeGroup.id+"&sidx=state&order=asc", function (r) {
	            vm.nodeFreshPoint = vm.nodeInfosChecked.length;
	            $.each(r.data.list, function (i, newNodeInfo) {
	                for (var j = 0; j < vm.nodeInfosChecked.length; j++) {
	                    var nodeInfo = vm.nodeInfosChecked[j];
	                    if (newNodeInfo.uuid == nodeInfo.uuid) {
	                        newNodeInfo._checked = true;
	                        break;
	                    }
	                }
	            })

	            vm.nodeInfoData = r.data.list;
	        })
	    },
	    unCheckGroup: function (index) {
	        vm.uncheckGroup = vm.nodeGroupsChecked[index];
	        vm.nodeGroupsChecked.splice(index, 1);
	    },
	    checkNodeGroup: function (nodeGroup) {
            if (nodeGroup.checked) {
                var fullPath = printNodeGroupName(nodeGroup);
                nodeGroup.fullPath = fullPath;
                vm.nodeGroupsChecked.push(nodeGroup)
            } else {
                $.each(vm.nodeGroupsChecked, function (i, v) {
                    if (v.id == nodeGroup.id) {
                        vm.nodeGroupsChecked.splice(i, 1);
                    }
                })
            }

        },
        checkNodeInfos: function (nodeInfos) {
            if (vm.nodeFreshPoint > 0) {
                vm.nodeInfosChecked.splice(vm.nodeFreshPoint);
                $.each(nodeInfos, function (i, v) {
                    var flag = true;
                    var index = -1;
                    for (var j = 0; j < vm.nodeInfosChecked.length; j++) {
                        var checkedNode = vm.nodeInfosChecked[j];
                        if (checkedNode.uuid == v.uuid) {
                            flag = false;
                            index = j;
                            break;
                        }
                    }

                    if (flag) {
                        vm.nodeInfosChecked.push(v);
                    } else {
                        console.log("节点存在" + (index + 1) + "号位置")
                    }
                })

            } else {
                vm.nodeInfosChecked = nodeInfos;
            }
        },
        init: function (systemId) {
            $.ajax({
                url: baseURL + "/nodegroup/list",
                type: "GET",
                async: false,
                success: function (r) {
                    if (200 == r.code) {
                        $.each(r.data.list, function (i, v) {
                            if (systemId == v.id) {
                                vm.nodeGroups = v.nodegroups;
                            }
                        });
                    }
                }
            })
        },
        getCurSystem: function () {
            $.getJSON(baseURL + "system/detail", function (r) {
                vm.curSystem = r.data;
                vm.init(vm.curSystem.id);
            });
        },
		
		
		
	},
	created: function(){
		// 初始化进入执行方法
		this.getMissionList();
		this.getViewLevel();
		this.getcommandType();
		this.getCommandEnnameTypeList1();
		this.getCommandEnnameTypeList2();
		this.getCommandEnnameTypeList3();
		this.getUserGroups();
		this.getCurSystem();
	}
});
</script>