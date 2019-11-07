<script>
//分配节点组件
var nodeRelComponent = Vue.extend({
	template: "#nodeRelTemplate",
	props: {
		initSelectedNodes: { //已分配节点
            type: Array,
            required: true
        },
		initSelectableNodes: { //可分配节点
            type: Array,
            required: true
        }
	},
	data: function () {
		return {
			height: top.$(".content").height()-220, //动态生成box的高度
			selectedIP: '',
			selectedIPStart: '',
			selectedIPEnd: '',
			selectableIP: '',
			selectableIPStart: '',
			selectableIPEnd: '',
			selectedCount: 0, //已分配列表中勾选的数量
			selectableCount: 0, //可分配列表中勾选的数量
			selectedTotalCount: 0, //已分配总数
			selectableTotalCount: 0, //可分配总数
			selectedNodes: [],
			selectableNodes: [],
			tempSelectedNodes: [],
			tempSelectableNodes: [],
			selectedNodesAdd: [], //新增的已分配节点，uuid数组，例如:[10,11,12]
			selectableNodesAdd: [], //新增的可分配节点（从已分配节点里删除的）
			selectedRange: false, //已分配搜索栏是搜索范围还是指定ip
			selectableRange: false, //可分配搜索栏是搜索范围还是指定ip
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
		countControll: function() { //更新已选数量和总数
			this.selectedCount = $("#selected-box").find(".success").length;
			this.selectableCount = $("#selectable-box").find(".success").length;
			this.selectedTotalCount = this.selectedNodes.length;
			this.selectableTotalCount = this.selectableNodes.length;
		},
		nodeRelChange: function(flag, event) { //节点分配状态更改
			if (flag == 0) { //取消分配
				var willChangeNodeDom = $("#selected-box").find(".success"); //所有将要改变的节点
				var addToSelectableNodes = [];
				for (var i = 0; i < willChangeNodeDom.length; i++) {
					var uuid = $(willChangeNodeDom[i]).attr("uuid");
					for (var j = this.selectedNodes.length-1; j >= 0; j--) {
						if (this.selectedNodes[j].uuid == uuid) {
							var addToSelectableNode = this.selectedNodes[j];
							if (addToSelectableNode.newNode) {
								for (var k = this.selectedNodesAdd.length-1; k >= 0; k--) {
									if (this.selectedNodesAdd[k] == addToSelectableNode.uuid) {
										this.selectedNodesAdd.splice(k, 1);
									}
								}
								addToSelectableNode.newNode = false;
							} else {
								this.selectableNodesAdd.push(addToSelectableNode.uuid);
								addToSelectableNode.newNode = true;
							}
							addToSelectableNodes.push(addToSelectableNode);
							this.selectedNodes.splice(j, 1);
							for (var k = this.tempSelectedNodes.length-1; k >= 0; k--) {
								if (this.tempSelectedNodes[k].uuid == uuid) {
									this.tempSelectedNodes.splice(k, 1);
									break;
								}
							}
							break;
						}
					}
				}
				this.selectableNodes = addToSelectableNodes.concat(this.selectableNodes);
				this.tempSelectableNodes = addToSelectableNodes.concat(this.tempSelectableNodes);
			} else if (flag == 1) { //分配
				var willChangeNodeDom = $("#selectable-box").find(".success"); //所有将要改变的节点
				var addToSelectedNodes = [];
				for (var i = 0; i < willChangeNodeDom.length; i++) {
					var uuid = $(willChangeNodeDom[i]).attr("uuid");
					for (var j = this.selectableNodes.length-1; j >= 0; j--) {
						if (this.selectableNodes[j].uuid == uuid) {
							var addToSelectedNode = this.selectableNodes[j];
							if (addToSelectedNode.newNode) {
								for (var k = this.selectableNodesAdd.length-1; k >= 0; k--) {
									if (this.selectableNodesAdd[k] == addToSelectedNode.uuid) {
										this.selectableNodesAdd.splice(k, 1);
									}
								}
								addToSelectedNode.newNode = false;
							} else {
								this.selectedNodesAdd.push(addToSelectedNode.uuid);
								addToSelectedNode.newNode = true;
							}
							addToSelectedNodes.push(addToSelectedNode);
							this.selectableNodes.splice(j, 1);
							for (var k = this.tempSelectableNodes.length-1; k >= 0; k--) {
								if (this.tempSelectableNodes[k].uuid == uuid) {
									this.tempSelectableNodes.splice(k, 1);
									break;
								}
							}
							break;
						}
					}
				}
				this.selectedNodes = addToSelectedNodes.concat(this.selectedNodes);
				this.tempSelectedNodes = addToSelectedNodes.concat(this.tempSelectedNodes);
			}
			$(".checkbox_true_part").removeClass("checkbox_true_part").addClass("checkbox_false_full");
			$(".checkbox_false_part").removeClass("checkbox_false_part").addClass("checkbox_false_full");
			$(".checkbox_true_full").removeClass("checkbox_true_full").addClass("checkbox_false_full");
			$(".success").removeClass("success");
			
			//已选数量和总数量控制
			this.countControll();
			//更新序号
			this.refreshRowIndex();
			//将数据传递给父组件
			this.$emit("node-change", [this.selectedNodesAdd, this.selectableNodesAdd, this.tempSelectedNodes.length]);
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
		},
		inputTouch: function(position, event) { //搜索框点击事件
			if (position == 0) { //范围搜索
				$(event.currentTarget).parent().find("input").css("color", "#555");
				$(event.currentTarget).parent().next().find("input").css("color", "lightgray");
				if ($(event.currentTarget).parent().parent().parent().find("#selected-box").length > 0) {
					this.selectedRange = true;
				} else if ($(event.currentTarget).parent().parent().parent().find("#selectable-box").length > 0) {
					this.selectableRange = true;
				}
			} else if (position == 1) { //精确搜索
				$(event.currentTarget).css("color", "#555");
				$(event.currentTarget).parent().prev().find("input").css("color", "lightgray");
				if ($(event.currentTarget).parent().parent().parent().find("#selected-box").length > 0) {
					this.selectedRange = false;
				} else if ($(event.currentTarget).parent().parent().parent().find("#selectable-box").length > 0) {
					this.selectableRange = false;
				}
			}
		},
		searchIp: function(event) { //ip搜索
			//TODO 校验
			
			var matchNodes = [];
			if ($(event.currentTarget).parent().parent().find("#selected-box").length > 0) { //已分配
				//搜索
				var input = $(event.currentTarget).parent().find("[active=true]");
				if (input.length > 1) { //范围搜索
					var ipStart = $(input[0]).val();
					var ipEnd = $(input[1]).val();
					var ipStartLong = ipToNumber(ipStart);
					var ipEndLong = ipToNumber(ipEnd);
					this.selectedNodes = [];
					for (var i = 0; i < this.tempSelectedNodes.length; i++) {
						if (ipStartLong <= this.tempSelectedNodes[i].ipLong && ipEndLong >= this.tempSelectedNodes[i].ipLong) {
							this.selectedNodes.push(this.tempSelectedNodes[i]);
						}
					}
				} else if (input.length == 1) { //精确搜索
					if (!this.selectedIP) {
						this.selectedNodes = this.tempSelectedNodes;
					} else {
						this.selectedNodes = [];
						for (var i = 0; i < this.tempSelectedNodes.length; i++) {
							if (this.tempSelectedNodes[i].ip.indexOf(this.selectedIP) > -1) {
								this.selectedNodes.push(this.tempSelectedNodes[i]);
							}
						}
					}
				}
			}
			//已选数量和总数量控制
			this.countControll();
			//更新序号
			this.refreshRowIndex();
		},
		reset: function(event) {
			if ($(event.currentTarget).parent().parent().parent().find("#selected-box").length > 0) { //已分配
				this.selectedIP = '';
				this.selectedIPStart = '';
				this.selectedIPEnd = '';
			} else {
				this.selectableIP = '';
				this.selectableIPStart = '';
				this.selectableIPEnd = '';
			}
		}
	},
	mounted: function() {
		//box内滚动条
		var ps1 = new PerfectScrollbar('#selected-box');
		var ps2 = new PerfectScrollbar('#selectable-box');
	},
	watch: {
		initSelectedNodes: function(n, o) {
			this.tempSelectedNodes = n;
			this.selectedNodes = n;
			this.selectedTotalCount = n.length;
		},
		initSelectableNodes: function(n, o) {
			this.tempSelectableNodes = n;
			this.selectableNodes = n;
			this.selectableTotalCount = n.length;
		}
	}
})
Vue.component('node-rel-component', nodeRelComponent);
function ipToNumber(ip) { 
	var num = 0; 
	if(ip == "") { 
		return num; 
	}   
	var aNum = ip.split(".");  
	if(aNum.length != 4) { 
		return num; 
	}   
	num += parseInt(aNum[0]) << 24; 
	num += parseInt(aNum[1]) << 16; 
	num += parseInt(aNum[2]) << 8; 
	num += parseInt(aNum[3]) << 0; 
	num = num >>> 0;
	return num;  
}   
	   
function numberToIp(number) {   
	var ip = ""; 
	if(number <= 0) { 
		return ip; 
	} 
	var ip3 = (number << 0 ) >>> 24; 
	var ip2 = (number << 8 ) >>> 24; 
	var ip1 = (number << 16) >>> 24; 
	var ip0 = (number << 24) >>> 24 
		    
	ip += ip3 + "." + ip2 + "." + ip1 + "." + ip0; 
		    
	return ip;   
}
</script>