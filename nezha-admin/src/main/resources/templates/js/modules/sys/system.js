<script>
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'system/list',
        datatype: "json",
        colModel: [		
            { label: 'ID', name: 'id', width: 30, key: true, hidden: true },
            { label: '<@spring.message "common.name"/>', name: 'name', sortable: false },
			{ label: '<@spring.message "common.createTime"/>', name: 'createDate', sortable: false },
			{ label: '<@spring.message "common.remark"/>', name: 'remarks', width: 80 },
			{ label: 'builtIn', name: 'buildIn', hidden: true }
        ],
		viewrecords: true,
		height: top.$(".content").height()-130,
        rowNum: 15,
		rowList : [15,30,50],
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
        	
	        /*buildin=1的记录禁用复选框*/
	        //1.先将对应记录的checkbox设为disabled
        	var dataRows = $("#jqGrid").find(".jqgrow");
        	$.each(dataRows, function(index) {
        		var buildIn = $(this).find("td").get(6);//buildIn字段所在列
        		if ($(buildIn).text() == "1") {
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
                toolsBtnHandle();
            }
        },
        onSelectRow: function(rowid,status) {
        	toolsBtnHandle();
        }
    });
});

//分配节点组件
var nodeRelComponent = Vue.extend({
	template: "#nodeRelTemplate",
	props: {
		selectedNodes: { //已分配节点
            type: Array,
            required: true
        },
		selectableNodes: { //可分配节点
            type: Array,
            required: true
        },
        initSelectedTotalCount: Number,
        initSelectableTotalCount: Number
	},
	data: function () {
		return {
			height: top.$(".content").height()-220,
			selectedIP: '',
			selectedIPStart: '',
			selectedIPEnd: '',
			selectableIP: '',
			selectableIPStart: '',
			selectableIPEnd: '',
			selectedCount: 0,
			selectableCount: 0,
			selectedTotalCount: null,
			selectableTotalCount: null,
			selectedNodesAdd: [], //新增的已分配节点
			selectableNodesAdd: [], //新增的可分配节点（从已分配节点里删除的）
			colModel: [
	            {name: "IP", field: "ip", width: 120},
	            {name: "类型", field: "type", width: 70,formatter:function(rowData){
	            	return rowData["type"]=='1'?"服务器":"网元";
	            }},
	            {name: "状态", field: "state", width: 60,formatter:function(rowData){
	            	return rowData["state"]=='1'?"在线":"<span style='color:#aaa;'>下线</span>";
	            }}
	        ],
		}
	},
	methods: {
		check: function(event, tr) { //节点列表的点击事件
			if (!tr) {
				var tr = event.currentTarget; //绑定了点击事件的tr
			}
			//列表中的checkbox是否打钩控制
			if ($(tr).find(".r-checkbox").hasClass("checkbox_false_full")) {
				$(tr).find(".r-checkbox").removeClass("checkbox_false_full").addClass("checkbox_true_full");
				$(tr).addClass("success");
			} else if ($(tr).find(".r-checkbox").hasClass("checkbox_true_full")) {
				$(tr).find(".r-checkbox").removeClass("checkbox_true_full").addClass("checkbox_false_full");
				$(tr).removeClass("success");
			}
			//已选数量控制
			this.countControll();
			//tr背景色、全选框控制
			if ($(tr).parent().find(".success").length == 0) {
				$(tr).parent().prev().find(".r-checkbox").removeClass("checkbox_false_part").removeClass("checkbox_true_part").addClass("checkbox_false_full");
			} else if ($(tr).parent().find(".success").length < $(tr).parent().find("tr").length) {
				$(tr).parent().prev().find(".r-checkbox").removeClass("checkbox_false_full").removeClass("checkbox_true_part").addClass("checkbox_false_part");
			} else if ($(tr).parent().find(".success").length == $(tr).parent().find("tr").length) {
				$(tr).parent().prev().find(".r-checkbox").removeClass("checkbox_false_full").removeClass("checkbox_false_part").addClass("checkbox_true_part");
			}
		},
		checkAll: function(event) {
			//全选框控制
			if (!$(event.currentTarget).hasClass("checkbox_true_part")) {
				$(event.currentTarget).removeClass("checkbox_false_full").removeClass("checkbox_false_part").addClass("checkbox_true_part");
				$(event.currentTarget).parent().parent().parent().next().find("tr").removeClass("success").addClass("success");
				$(event.currentTarget).parent().parent().parent().next().find(".r-checkbox").removeClass("checkbox_false_full").removeClass("checkbox_true_full").addClass("checkbox_true_full");
			} else {
				$(event.currentTarget).removeClass("checkbox_true_part").addClass("checkbox_false_full");
				$(event.currentTarget).parent().parent().parent().next().find("tr").removeClass("success");
				$(event.currentTarget).parent().parent().parent().next().find(".r-checkbox").removeClass("checkbox_true_full").addClass("checkbox_false_full");
			}
			//已选数量控制
			this.countControll();
		},
		countControll: function() { //更新已选数量和总数的数字
			this.selectedCount = $("#selected-box").find(".success").length;
			this.selectableCount = $("#selectable-box").find(".success").length;
			this.selectedTotalCount = $("#selected-box").find("tbody").find("tr").length;
			this.selectableTotalCount = $("#selectable-box").find("tbody").find("tr").length;
		},
		nodeRelChange: function(flag, event) { //节点分配状态更改
			if (flag == 0) { //取消分配
				var fromSelector = "selected";
				var toSelector = "selectable";
			} else if (flag == 1) { //分配
				var fromSelector = "selectable";
				var toSelector = "selected";
			}
			$("#" + fromSelector + "-box").find("thead").find(".r-checkbox").removeClass("checkbox_false_part").removeClass("checkbox_true_part").addClass("checkbox_false_full");
			var willChangeNode = $("#" + fromSelector + "-box").find(".success"); //所有将要改变的节点
			this.copyNode($("#" + toSelector + "-box").find("tbody"), willChangeNode);//复制dom
			$("#" + fromSelector + "-box").find(".success").remove(); //复制后删除自身
			var changeNode = $("#" + toSelector + "-box").find(".success." + fromSelector); //改变后的节点所在tr
			var tmp = [];
			for (var i = 0; i < changeNode.length; i++) {
				this.check(null, changeNode[i]);
				$(changeNode[i]).removeClass(fromSelector).addClass(toSelector).removeClass("success");
				if ($(changeNode[i]).hasClass("new-node")) {
					$(changeNode[i]).removeClass("new-node");
				} else {
					tmp.push(changeNode[i]);
					$(changeNode[i]).remove();
				}
			}
			for (var i = tmp.length-1; i >= 0; i--) {
				$(tmp[i]).addClass("new-node");
				$("#" + toSelector + "-box").find("tbody").prepend(tmp[i]);
			}
			//同步数组数据
			this.selectedNodesAdd = [];
			this.selectableNodesAdd = [];
			var vmTemp = this;
			$("#selected-box").find(".new-node").each(function(index) {
				vmTemp.selectedNodesAdd.push($(this).attr("uuid"));
			});
			$("#selectable-box").find(".new-node").each(function(index) {
				vmTemp.selectableNodesAdd.push($(this).attr("uuid"));
			});
			//已选数量和总数量控制
			this.countControll();
			//更新序号
			this.refreshRowIndex();
			//将数据传递给父组件
			this.$emit("node-change", [this.selectedNodesAdd, this.selectableNodesAdd]);
		},
		copyNode: function(container, nodes) { //复制节点dom
			var trs = $(container).find("tr");
			if (trs.length == 0) {
				container.append(nodes);
			} else {
				for (var i = 0; i < trs.length; i++) {
					if (!$(trs[i]).hasClass("new-node")) {
						$(trs[i]).before(nodes);
						break;
					}
				}
			}
		},
		refreshRowIndex: function() { //更新序号
			var selectedTrs = $("#selected-box").find("tbody").find("tr");
			var selectableTrs = $("#selectable-box").find("tbody").find("tr");
			var objects = [selectedTrs, selectableTrs];
			for (var i = 0; i < objects.length; i++) {
				for (var j = 0; j < objects[i].length; j++) {
					$(objects[i][j]).children(":first").text(j+1);
				}
			}
		}
	},
	mounted: function() {
		//box内滚动条
		var ps1 = new PerfectScrollbar('#selected-box');
		var ps2 = new PerfectScrollbar('#selectable-box');
	}
})
Vue.component('node-rel-component', nodeRelComponent)

//引用vuelidate组件
Vue.use(window.vuelidate.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-builtin-validators
var required = window.validators.required;
var helpers = window.validators.helpers;//引入自定义校验规则助手 helpers
//自定义规则，校验业务系统名称不能重复;
var checkName = function(value, system) {
	if (helpers.req(system.name) && helpers.req(value)) {
		var params = "?name=" + system.name;
		if (system.id) {
			params = params + "&id=" + system.id;
		}
		var flag = true;
		$.ajax({
			type: "get",
			url: baseURL + "system/nameCheck" + params,
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
		//single和multi分别控制工具栏的单选、多选按钮的可点击状态
		single: true,
		multi: true,
		
		showList: true,
		title: null,
		system: {
			name: "",
			remarks: "",
			id: null,
			usergroupIds: [],
			usergroups: [],
			addUuids: [],
			removeUuids: [],
			nodeNum: 0
		},
		usergroups: [],
		allNodes: [], //所有节点
        selectedNodes: [], //已选节点
		selectableNodes: [], //可选节点
		//控制全选框样式
		selectedCheckAll: false, //是否全选
		selectedCheckNone: true, //是否全不选
		selectableCheckAll: false,
		selectableCheckNone: true
	},
	validations: { //定义校验规则
		system: {
			name: {
				required,
				checkName
			}
		}
	},
	methods: {
		add: function(){
			vm.$v.system.$reset(); //进入新增页面时，将校验的触发状态清空($dirty=true -> false)
			vm.showList = false;
			vm.title = "<@spring.message 'common.add'/>";
			vm.system = {name:null,remarks:null,id:null,usergroupIds:[],usergroups: []};
			vm.selectableNodes = vm.allNodes;
		},
		update: function () {
			vm.$v.system.$reset(); //进入新增页面时，将校验触发状态清空($dirty=true -> false)
			var id = getSelectedRow();
			if(id == null){
				return ;
			}
			vm.showList = false;
            vm.title = "<@spring.message 'common.edit'/>";
            vm.getInfo(id);
		},
		getInfo: function(id){
			$.get(baseURL + "system/detail?id=" + id, function(r){
                vm.system = r.data;
                vm.system.usergroupIds = [];
                for (var i = 0; i < vm.system.usergroups.length; i++) {
                	vm.system.usergroupIds[i] = vm.system.usergroups[i].id;
                }
            });
			var vm_tmp = this;
			$.ajax({
	            url: baseURL + "/system/nodes?id=" + id,
	            type: "GET",
	            async: false,
	            success: function (r) {
	            	vm_tmp.selectedNodes = r.data.list;
	            	vm_tmp.selectableNodes = r.data.list.concat(vm_tmp.allNodes).filter(function (v) {
                		var tempFlag1 = true;
                		var tempFlag2 = true;
                		var comparisonUuid = v.uuid;
                		for (var ii = 0 ; ii < r.data.list.length; ii++) {
                			if (r.data.list[ii].uuid == comparisonUuid) {
                				tempFlag1 = false;
                			}
                		}
                		for (var ii = 0 ; ii < vm_tmp.allNodes.length; ii++) {
                			if (vm_tmp.allNodes[ii].uuid == comparisonUuid) {
                				tempFlag2 = false;
                			}
                		}
                        return tempFlag1 || tempFlag2;
                    });
	            }
			});
		},
		getAllUsergroups: function(id) {
			$.get(baseURL + "sys/usergroup/list?limit=999", function(r){
                vm.usergroups = r.data.list;
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
				    url: baseURL + "system/delete",
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
			var url = vm.system.id == null ? "system/save" : "system/update";
			$.ajax({
				type: vm.system.id == null ? "POST" : "PUT",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.system),
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
		nodeRel: function (event) {
			layer.open({
                area: ['740px', '100%'],
                shade: 0.01,
                title: "分配节点",
                type: 1,
                maxmin: false,
                scrollbar: false,
                offset: 'r',
                closeBtn: 1,
                move: false,
                content: $(".nodeRel")
            });
		},
		nodeChange: function(data) {
			this.system.addUuids = data[0];
			this.system.removeUuids = data[1];
			this.system.nodeNum = $("#selected-box").find("tbody").find("tr").length;
		},
		reload: function (event) {
			vm.single = vm.multi = true;
			vm.showList = true;
			var page = $("#jqGrid").jqGrid('getGridParam','page');
			$("#jqGrid").jqGrid('setGridParam',{ 
                page:page
            }).trigger("reloadGrid");
		},
		selectableNodesReload: function (event) {
			var page = parent.$("#selectable").jqGrid('getGridParam','page');
			parent.$("#selectable").jqGrid('setGridParam',{ 
                postData:{'id':1},
                page:1
            }).trigger("reloadGrid");
		},
		validate: function () {//提交时的校验
			vm.$v.system.$touch(); //提交时，不论是否已触发校验，都触发一下($dirty=false -> true)
			return !vm.$v.system.$error;
		},
		getAllNodes: function() {
			var vm_temp = this;
			$.ajax({
                url: baseURL + "/node/list?limit=9999&page=1",
                type: "GET",
                async: false,
                success: function (r) {
                	vm_temp.allNodes = r.data.list;
                }
			});
		}
	},
	created: function() {
		this.getAllUsergroups();
		this.getAllNodes();
	},
	mounted: function() {
		
	}
});

</script>