<script>
Vue.use(window.vuelidate.default);
var required = window.validators.required;
var ipAddress = window.validators.ipAddress;

//分配节点组件
var nodeRelComponent = Vue.extend({
	template: "#nodeRelTemplate", //位于/modules/common/nodeRel.html
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
			height: top.$(".content").height()-250, //动态生成box的高度
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
			selectedIPEndSizeValidation: false,     // IPEnd 比 IPStart 小 提示标识   selected
			selectableIPEndSizeValidation: false,   // IPEnd 比 IPStart 小 提示标识   selectable
			selectedUuids: [],					    // 选中的uuids   selected
			selectableUuids: []						// 选中的uuids   selectable
		}
	},
	validations: {
		selectedIPStart: {
			ipAddress, required
		},
		selectedIPEnd: {
			ipAddress, required
		},
		selectableIPStart: {
			ipAddress, required
		},
		selectableIPEnd: {
			ipAddress, required
		}
	},
	methods: {
		check: function(node, flag) { //节点列表的点击事件
			var temp = this;
			var uuid = node.uuid;
			// 判断是否选中
			if (!node.checkedBox1) {
				if (flag == 'selected') {
					// 在选中数组中添加
					this.selectedUuids.push(uuid);
					// 选中
					this.selectedNodes.some(function (item, index) {
						if (item.uuid == uuid) {
							temp.$set(item, 'checkedBox1', true);
							temp.selectedNodes.splice(index, 1, item);
							return true;
						}
					});
				} else {
					// 在选中数组中添加
					this.selectableUuids.push(uuid);
					// 选中
					this.selectableNodes.some(function (item, index) {
						if (item.uuid == uuid) {
							temp.$set(item, 'checkedBox1', true);
							temp.selectableNodes.splice(index, 1, item);
							return true;
						}
					});
				}
			} else {
				if (flag == 'selected') {
					// 在选中数组中删除
					this.selectedUuids.splice(this.selectedUuids.indexOf(uuid),1);
					// 取消选中
					this.selectedNodes.some(function (item, index) {
						if (item.uuid == uuid) {
							temp.$set(item, 'checkedBox1', false);
							temp.selectedNodes.splice(index, 1, item);
							return true;
						}
					});
				} else {
					// 在选中数组中删除
					this.selectableUuids.splice(this.selectableUuids.indexOf(uuid),1);
					// 取消选中
					this.selectableNodes.some(function (item, index) {
						if (item.uuid == uuid) {
							temp.$set(item, 'checkedBox1', false);
							temp.selectableNodes.splice(index, 1, item);
							return true;
						}
					});
				}
			}
			// 计算选中数量和总数量
			this.countControll();
		},
		checkAll: function(event, flag) {
			var temp = this;
			// 全选
			if (!$(event.currentTarget).hasClass("checkbox_true_part")) {
				if (flag == 'selected') {
					// 先置空
					this.selectedUuids = [];
					// 全选 添加uuids
					this.selectedNodes.forEach(function (item) {
						temp.$set(item, 'checkedBox1', true);
						temp.selectedUuids.push(item.uuid);
					})
				} else {
					// 先置空
					this.selectableUuids = [];
					// 全选 添加uuids
					this.selectableNodes.forEach(function (item) {
						temp.$set(item, 'checkedBox1', true);
						temp.selectableUuids.push(item.uuid);
					})
				}
			} else { // 全不选
				if (flag == 'selected') {
					// 置空
					this.selectedUuids = [];
					// 取消全选
					this.selectedNodes.forEach(function (item) {
						temp.$set(item, 'checkedBox1', false);
					})
				} else {
					// 置空
					this.selectableUuids = [];
					// 取消全选
					this.selectableNodes.forEach(function (item) {
						temp.$set(item, 'checkedBox1', false);
					})
				}
			}
			// 计算选中数量和总数量
			this.countControll();
		},
		countControll: function() { // 更新已选数量和总数
			this.selectedCount = this.selectedUuids.length;
			this.selectableCount = this.selectableUuids.length;
			this.selectedTotalCount = this.selectedNodes.length;
			this.selectableTotalCount = this.selectableNodes.length;
		},
		nodeRelChange: function(flag, event) { // 节点分配状态更改
			if (flag == 0) { // 取消分配
				var addToSelectableNodes = [];
				for (var i = 0; i < this.selectedUuids.length; i++) {
					var uuid = this.selectedUuids[i];
					for (var j = this.selectedNodes.length-1; j >= 0; j--) {
						if (this.selectedNodes[j].uuid == uuid) {
							var addToSelectableNode = this.selectedNodes[j];
							// 取消分配后取消选中状态
							addToSelectableNode.checkedBox1 = false;
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
				// 重置selectuuids
				this.selectedUuids = [];

				this.selectableNodes = addToSelectableNodes.concat(this.selectableNodes);
				this.tempSelectableNodes = addToSelectableNodes.concat(this.tempSelectableNodes);
				this.sortRow(0, this.selectableNodes);
				this.sortRow(1, this.tempSelectableNodes);
			} else if (flag == 1) { // 可分配
				var addToSelectedNodes = [];
				for (var i = 0; i < this.selectableUuids.length; i++) {
					var uuid = this.selectableUuids[i];
					for (var j = this.selectableNodes.length-1; j >= 0; j--) {
						if (this.selectableNodes[j].uuid == uuid) {
							var addToSelectedNode = this.selectableNodes[j];
							// 分配过去取消选中
							addToSelectedNode.checkedBox1 = false;
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
				// 重置selectableUuids
				this.selectableUuids = [];

				this.selectedNodes = addToSelectedNodes.concat(this.selectedNodes);
				this.tempSelectedNodes = addToSelectedNodes.concat(this.tempSelectedNodes);
				this.sortRow(2, this.selectedNodes);
				this.sortRow(3, this.tempSelectedNodes);
			}
			// 已选数量和总数量控制
			this.countControll();
			// 将数据传递给父组件
			this.$emit("node-change", [this.selectedNodesAdd, this.selectableNodesAdd, this.tempSelectedNodes.length]);
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
		searchIp: function(event) { // ip搜索
			var matchNodes = [];
			if ($(event.currentTarget).parent().parent().parent().find("#selected-box").length > 0) { // 已分配
				// 搜索
				var input = $(event.currentTarget).parent().parent().find("[active=true]");
				if (input.length > 1) { // 范围搜索
					// 校验IP
					if (!this.validateSelectedStartAndEndIP()) {
						this.showSelectedIPStartWarningMsg();
						this.showSelectedIPEndWarningMsg();
						return;
					}
					var ipStart = $(input[0]).val();
					var ipEnd = $(input[1]).val();
					var ipStartLong = ipToNumber(ipStart);
					var ipEndLong = ipToNumber(ipEnd);
					if (ipStartLong > ipEndLong) {
						this.selectedIPEndSizeValidation = true;
						return;
					}
					this.selectedNodes = [];
					for (var i = 0; i < this.tempSelectedNodes.length; i++) {
						if (ipStartLong <= this.tempSelectedNodes[i].ipLong && ipEndLong >= this.tempSelectedNodes[i].ipLong) {
							this.selectedNodes.push(this.tempSelectedNodes[i]);
						}
					}
				} else if (input.length == 1) { // 精确搜索
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
				// 更新 selectedUuid  选中的依旧选中
				var temp = this;
				this.selectedUuids = [];
				this.selectedNodes.forEach(function (item) {
					if (item.checkedBox1) { // 搜索之后当前选中状态
						temp.$set(item, 'checkedBox1', true);
						temp.selectedUuids.push(item.uuid);
					}
				})
			} else if ($(event.currentTarget).parent().parent().parent().find("#selectable-box").length > 0) { // 可分配
				// 搜索
				var input = $(event.currentTarget).parent().parent().find("[active=true]");
				if (input.length > 1) { // 范围搜索
					// 校验IP
					if (!this.validateSelectableStartAndEndIP()) {
						this.showSelectableIPStartWarningMsg();
						this.showSelectableIPEndWarningMsg();
						return;
					}
					var ipStart = $(input[0]).val();
					var ipEnd = $(input[1]).val();
					var ipStartLong = ipToNumber(ipStart);
					var ipEndLong = ipToNumber(ipEnd);
					if (ipStartLong > ipEndLong) {
						this.selectableIPEndSizeValidation = true;
						return;
					}
					this.selectableNodes = [];
					for (var i = 0; i < this.tempSelectableNodes.length; i++) {
						if (ipStartLong <= this.tempSelectableNodes[i].ipLong && ipEndLong >= this.tempSelectableNodes[i].ipLong) {
							this.selectableNodes.push(this.tempSelectableNodes[i]);
						}
					}
				} else if (input.length == 1) { // 精确搜索
					if (!this.selectableIP) {
						this.selectableNodes = this.tempSelectableNodes;
					} else {
						this.selectableNodes = [];
						for (var i = 0; i < this.tempSelectableNodes.length; i++) {
							if (this.tempSelectableNodes[i].ip.indexOf(this.selectableIP) > -1) {
								this.selectableNodes.push(this.tempSelectableNodes[i]);
							}
						}
					}
				}
				// 更新 selectedUuid  选中的依旧选中
				var temp = this;
				this.selectableUuids = [];
				this.selectableNodes.forEach(function (item) {
					if (item.checkedBox1) { // 搜索之后当前选中状态
						temp.$set(item, 'checkedBox1', true);
						temp.selectableUuids.push(item.uuid);
					}
				})
			}

			// 已选数量和总数量控制
			this.$nextTick(()=>{
				this.countControll();
			});
		},
		searchReset: function(event) { //搜索条件重置
			if ($(event.currentTarget).parent().parent().parent().find("#selected-box").length > 0) { //已分配
				this.selectedIP = '';
				this.selectedIPStart = '';
				this.selectedIPEnd = '';

				this.selectedIPEndSizeValidation = this.selectedRange  = false;

				this.$v.selectedIPStart.$reset();
				this.$v.selectedIPEnd.$reset();
				this.$v.selectedIP.$reset();
			} else {
				this.selectableIP = '';
				this.selectableIPStart = '';
				this.selectableIPEnd = '';

				this.selectableIPEndSizeValidation = this.selectableRange = false;

				this.$v.selectableIPStart.$reset();
				this.$v.selectableIPEnd.$reset();
				this.$v.selectableIP.$reset();
			}
		},
		nodeReset: function() { //重置此次操作
			this.tempSelectedNodes = this.selectedNodes = JSON.parse(JSON.stringify(this.initSelectedNodes));
			this.tempSelectableNodes = this.selectableNodes = JSON.parse(JSON.stringify(this.initSelectableNodes));

			var temp = this;
			// 以前选中的恢复选中
			this.selectedNodes.forEach(function (item, index) {
				if (temp.selectedUuids.indexOf(item.uuid) != -1) {
					temp.$set(item, 'checkedBox1', true);
				}
			})
			// 以前选中的恢复选中
			this.selectableNodes.forEach(function (item, index) {
				if (temp.selectableUuids.indexOf(item.uuid) != -1) {
					temp.$set(item, 'checkedBox1', true);
				}
			})

			this.selectedNodesAdd = [];
			this.selectableNodesAdd = [];
			this.$emit("node-change", [this.selectedNodesAdd, this.selectableNodesAdd, this.tempSelectedNodes.length]);
			this.countControll();
		},
		resetSelectedCount: function() {
			// 重置选中数量
			this.selectedUuids = [];
			this.selectableUuids = [];
		},
		sortRow: function(flag, nodes) { //列表重新排序，将新增的项置顶
			var newNodes = [];
			for (var i = nodes.length-1; i >= 0; i--) {
				if (nodes[i].newNode) {
					newNodes.unshift(nodes[i]);
					nodes.splice(i, 1);
				}
			}
			if (flag == 0) {
				this.selectableNodes = newNodes.concat(nodes);
			} else if (flag == 1) {
				this.tempSelectableNodes = newNodes.concat(nodes);
			} else if (flag == 2) {
				this.selectedNodes = newNodes.concat(nodes);
			} else if (flag == 3) {
				this.tempSelectedNodes = newNodes.concat(nodes);
			}
		},
		validateSelectedStartAndEndIP: function() {
			this.$v.selectedIPStart.$touch();
			this.$v.selectedIPEnd.$touch();
			return !this.$v.selectedIPStart.$error && !this.$v.selectedIPEnd.$error && this.selectedIPEndSizeValidation;
		},
		validateSelectedIP: function() {
			this.$v.selectedIP.$touch();
			return !this.$v.selectedIP.$error;
		},
		validateSelectableStartAndEndIP: function() {
			this.$v.selectableIPStart.$touch();
			this.$v.selectableIPEnd.$touch();
			return !this.$v.selectableIPStart.$error && !this.$v.selectableIPEnd.$error && this.selectableIPEndSizeValidation;
		},
		validateSelectableIP: function() {
			this.$v.selectableIP.$touch();
			return !this.$v.selectableIP.$error;
		},
		showSelectedIPStartWarningMsg: function() {
			if(this.$v.selectedIPStart.$anyError){
				var msg = '';
				if (this.$v.selectedIPStart.$dirty&&!this.$v.selectedIPStart.required) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.required'/></div>"
				}
				if (this.$v.selectedIPStart.$dirty&&!this.$v.selectedIPStart.ipAddress) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.node.ipFormat'/></div>"
				}
				layer.tips(msg, "#selectedIPStart",{
					tips:[1, "white"],
					tipsMore:true
				});
			}
		},
		showSelectedIPEndWarningMsg: function() {
			if (this.$v.selectedIPEnd.$anyError || !this.selectedIPEndSizeValidation) {
				var msg = '';
				if (this.$v.selectedIPEnd.$dirty&&!this.$v.selectedIPEnd.required) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.required'/></div>"
				}
				if (this.$v.selectedIPEnd.$dirty&&!this.$v.selectedIPEnd.ipAddress) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.node.ipFormat'/></div>"
				}
				if (msg == '' && !this.selectedIPEndSizeValidation) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.IpEndLessThanIpStart'/></div>"
				}
				layer.tips(msg, "#selectedIPEnd",{
					tips:[1, "white"],
					tipsMore:true
				});
			}
		},
		showSelectedIPWarningMsg: function() {
			if (this.$v.selectedIP.$anyError) {
				var msg = '';
				if (this.$v.selectedIP.$dirty&&!this.$v.selectedIP.required) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.required'/></div>"
				}
				if (this.$v.selectedIP.$dirty&&!this.$v.selectedIP.ipAddress) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.node.ipFormat'/></div>"
				}
				layer.tips(msg, "#selectedIP",{
					tips:[1, "white"],
					tipsMore:true
				});
			}
		},
		showSelectableIPStartWarningMsg: function() {
			if(this.$v.selectableIPStart.$anyError){
				var msg = '';
				if (this.$v.selectableIPStart.$dirty&&!this.$v.selectableIPStart.required) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.required'/></div>"
				}
				if (this.$v.selectableIPStart.$dirty&&!this.$v.selectableIPStart.ipAddress) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.node.ipFormat'/></div>"
				}
				layer.tips(msg, "#selectableIPStart",{
					tips:[1, "white"],
					tipsMore:true
				});
			}
		},
		showSelectableIPEndWarningMsg: function() {
			if (this.$v.selectableIPEnd.$anyError || !this.selectableIPEndSizeValidation) {
				var msg = '';
				if (this.$v.selectableIPEnd.$dirty&&!this.$v.selectableIPEnd.required) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.required'/></div>"
				}
				if (this.$v.selectableIPEnd.$dirty&&!this.$v.selectableIPEnd.ipAddress) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.node.ipFormat'/></div>"
				}
				if (msg == '' && !this.selectableIPEndSizeValidation) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.IpEndLessThanIpStart'/></div>"
				}
				layer.tips(msg, "#selectableIPEnd",{
					tips:[1, "white"],
					tipsMore:true
				});
			}
		},
		showSelectableIPWarningMsg: function() {
			if (this.$v.selectableIP.$anyError) {
				var msg = '';
				if (this.$v.selectableIP.$dirty&&!this.$v.selectableIP.required) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.common.required'/></div>"
				}
				if (this.$v.selectableIP.$dirty&&!this.$v.selectableIP.ipAddress) {
					msg = "<div class=\"form-control_error-msg\"><@spring.message 'validate.node.ipFormat'/></div>"
				}
				layer.tips(msg, "#selectableIP",{
					tips:[1, "white"],
					tipsMore:true
				});
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
			//init数据拷贝，保证init不改变
			this.tempSelectedNodes = this.selectedNodes = JSON.parse(JSON.stringify(n));
			this.selectedTotalCount = n.length;
		},
		initSelectableNodes: function(n, o) {
			this.tempSelectableNodes = this.selectableNodes = JSON.parse(JSON.stringify(n));
			this.selectableTotalCount = n.length;
		},
		selectedIPEnd: function(n, o) {
			var ipStartLong = ipToNumber(this.selectedIPStart);
			var ipEndLong = ipToNumber(n);
			if (ipEndLong >= ipStartLong) {
				this.selectedIPEndSizeValidation = true;
			}else {
				this.selectedIPEndSizeValidation = false;
			}
		},
		selectableIPEnd: function(n, o) {
			var ipStartLong = ipToNumber(this.selectableIPStart);
			var ipEndLong = ipToNumber(n);
			if (ipEndLong >= ipStartLong) {
				this.selectableIPEndSizeValidation = true;
			}else {
				this.selectableIPEndSizeValidation = false;
			}
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