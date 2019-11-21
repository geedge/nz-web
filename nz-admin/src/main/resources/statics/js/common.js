//jqGrid的配置信息
$.jgrid.defaults.width = 1000;
$.jgrid.defaults.responsive = true;
$.jgrid.defaults.styleUI = 'Bootstrap';

var baseURL = "../../";

//工具集合Tools
window.T = {};

// 获取请求参数
// 使用示例
// location.href = http://localhost:8080/index.html?id=123
// T.p('id') --> 123;
var url = function(name) {
	var reg = new RegExp("(^|&)"+ name +"=([^&]*)(&|$)");
	var r = window.location.search.substr(1).match(reg);
	if(r!=null)return  unescape(r[2]); return null;
};
T.p = url;

//全局配置
$.ajaxSetup({
	dataType: "json",
	cache: false
});

//重写alert
window.alert = function(msg, callback){
	parent.layer.alert(msg, function(index){
		parent.layer.close(index);
		if(typeof(callback) === "function"){
			callback("ok");
		}
	});
}

//重写confirm式样框
window.confirm = function(msg, callback){
	parent.layer.confirm(msg, {btn: ['确定','取消']},
	function(){//确定事件
		if(typeof(callback) === "function"){
			callback("ok");
		}
	});
}

//选择一条记录
function getSelectedRow() {
    var grid = $("#jqGrid");
    var rowKey = grid.getGridParam("selrow");
    if(!rowKey){
    	//alert("请选择一条记录");
    	return ;
    }
    
    var selectedIDs = grid.getGridParam("selarrrow");
    if(selectedIDs.length > 1){
    	//alert("只能选择一条记录");
    	return ;
    }
    
    return selectedIDs[0];
}

//获取当前选中行的属性
function getParam(name) {
	var id = getSelectedRow();
	var rowDatas = $("#jqGrid").getRowData(id);
	return rowDatas[name];
}

//选择多条记录
function getSelectedRows() {
    var grid = $("#jqGrid");
    var rowKey = grid.getGridParam("selrow");
    if(!rowKey){
    	//alert("请选择一条记录");
    	return ;
    }
    
    return grid.getGridParam("selarrrow");
}

//判断是否为空
function isBlank(value) {
    return !value || !/\S/.test(value)
}

//控制工具栏按钮的可用状态
function toolsBtnHandle() {
	var checkedNum = $(".cbox:checked").not("#cb_jqGrid").length;//表格中选中状态的行的数量
	if (checkedNum == 0) {
		vm.single = vm.multi = true;
	} else if (checkedNum == 1) {
		vm.single = vm.multi = false;
	} else if (checkedNum > 1) {
		vm.single = true;
		vm.multi = false;
	}
}

/*======================时间验证方法start===========================*/
//startTimeSelector:输入框开始时间选择器; 
//endTimeSelector:输入框结束时间选择器; 
//granule:时间粒度(数字);
//unit:时间粒度单位(h-小时,d-天,w-周,m-月);
//formate：时间格式;默认格式：yyyy-MM-dd hh:mm:ss
//future:选择的时间是否可以为未来时间 true or false 默认false(不能为未来时间)
function setEndTime(startTimeSelector,endTimeSelector,granule,unit,formate,future){
	
	/*if(strIsBlank(granule)||strIsBlank(unit)){
		console.log("时间粒度或时间单位为空");
		return;
	}*/

	if(strIsBlank(startTimeSelector)||strIsBlank(endTimeSelector)){
		console.log("时间输入框选择器为空");
		return;
	}

	if(strIsBlank(formate)){
		formate="yyyy-MM-dd hh:mm:ss"
	}
	if(future==null || future==undefined){
		future=false;
	}
	var startTimeVal=$(startTimeSelector).val();
	var endTimeVal=$(endTimeSelector).val();

	var startTime;
	var endTime;

	if(!strIsBlank(endTimeVal)){
		endTime=new Date(endTimeVal);
		var currentTime=new Date().getTime();
		if(!future&&endTime.getTime()>currentTime){ //endTime不能是未来时间
			endTime=new Date();
		}
		if(strIsBlank(startTimeVal)){
			startTime=new Date();
		}else{//startTime和endTime都不为空
			startTime=new Date(startTimeVal);
			if(endTime.getTime()-startTime.getTime()<=0){//startTime不在时间粒度之内
				startTime=new Date(endTime.getTime());
			}
		}
		
	}else{
		if(strIsBlank(startTimeVal)){//startTime and endTime 都为空
			endTime=new Date();
			startTime=new Date(endTime.getTime());
		}else{//startTime 不为空，endTime为空
			setStartTime(startTimeSelector,endTimeSelector,granule,unit,formate,future);
			return;
		}
	}

	$(startTimeSelector).val(dateFtt(formate,startTime));
	$(endTimeSelector).val(dateFtt(formate,endTime));	
}

//startTimeSelector:输入框开始时间选择器; 
//endTimeSelector:输入框结束时间选择器; 
//granule:时间粒度(数字);
//unit:时间粒度单位(h-小时,d-天,w-周,m-月);
//formate：时间格式;默认格式：yyyy-MM-dd hh:mm:ss
//future:选择的时间是否可以为未来时间 true or false 默认false(不能为未来时间)
function setStartTime(startTimeSelector,endTimeSelector,granule,unit,formate,future){
	
	/*if(strIsBlank(granule)||strIsBlank(unit)){
		//console.log("时间粒度或时间单位为空");
		return;
	}*/

	if(strIsBlank(startTimeSelector)||strIsBlank(endTimeSelector)){
		//console.log("时间输入框选择器为空");
		return;
	}

	if(strIsBlank(formate)){
		formate="yyyy-MM-dd hh:mm:ss"
	}

	if(future==null || future==undefined){
		future=false;
	}
	var startTimeVal=$(startTimeSelector).val();
	var endTimeVal=$(endTimeSelector).val();

	var startTime;
	var endTime;
	
	if(!strIsBlank(startTimeVal)){
		startTime=new Date(startTimeVal);
		var currentTime=new Date().getTime();
		if(!future&&startTime.getTime()>currentTime){//startTime是一个超过当前时间值
			startTime=new Date(currentTime);
		}
		if(strIsBlank(endTimeVal)){
			if(startTime.getTime()>currentTime){
				endTime=new Date();
			}else{
				endTime=new Date(startTime.getTime());
			}
		}else{//startTime和endTime都不为空
			endTime=new Date(endTimeVal);
			if(endTime.getTime()-startTime.getTime()<=0){//endTime不在时间粒度之内
				
				if(startTime.getTime()>currentTime){
					endTime=new Date();
					startTime=new Date(endTime.getTime()-5*60*1000);
				}else{
					endTime=new Date(startTime.getTime());
				}
			}
		}
	}else{
		if(strIsBlank(endTimeVal)){//startTime和endTime 都为空
			endTime=new Date();
			startTime=new Date(endTime.getTime());
			
		}else{
			setEndTime(startTimeSelector,endTimeSelector,granule,unit,formate,future);
			return;
		}
	}

	$(startTimeSelector).val(dateFtt(formate,startTime));
	$(endTimeSelector).val(dateFtt(formate,endTime));	
}

function strIsBlank(obj){
	if(obj==null || obj==undefined|| obj==''){
		return true;
	}else{
		return false;
	}
}

function dateFtt(fmt,date){ 
var o = {   
"M+" : date.getMonth()+1,                 //月份   
"d+" : date.getDate(),                    //日   
"h+" : date.getHours(),                   //小时   
"m+" : date.getMinutes(),                 //分   
"s+" : date.getSeconds(),                 //秒   
"q+" : Math.floor((date.getMonth()+3)/3), //季度   
"S"  : date.getMilliseconds()             //毫秒   
};   
if(/(y+)/.test(fmt))   
fmt=fmt.replace(RegExp.$1, (date.getFullYear()+"").substr(4 - RegExp.$1.length));   
for(var k in o)   
if(new RegExp("("+ k +")").test(fmt))   
fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));   
return fmt;   
}
/*======================时间验证方法end===========================*/

