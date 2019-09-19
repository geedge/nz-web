<script>
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + 'sys/i18n/list',
        datatype: "json",
        colModel: [		
            { label: 'ID', name: 'id', width: 30, key: true, hidden: true },
            { label: 'Code', name: 'code', width: 60 },
			{ label: '<@spring.message "common.language"/>', name: 'lang', sortable: false, width: 60 },
			{ label: 'Value', name: 'value', width: 100, sortable: false },
			{ label: '<@spring.message "common.remark"/>', name: 'remark', width: 80, sortable: false }
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
        }
    });
});
//注册select组件
Vue.component('multiselect', window.VueMultiselect.default);
//引用vuelidate组件
Vue.use(window.vuelidate.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-builtin-validators
var required = window.validators.required;
var helpers = window.validators.helpers;//引入自定义校验规则助手 helpers
//自定义规则，校验i18n的lang+code组合不能重复;第一个参数是当前字段值，第二个参数是当前校验的整个对象(此处是vm.data.i18n)
var checkI18n = function(value, i18n) {
	if (helpers.req(i18n.lang) && helpers.req(value)) {
		var params = "?lang=" + i18n.lang + "&code=" + value;
		if (i18n.id) {
			params = params + "&id=" + i18n.id;
		}
		var flag = true;
		$.ajax({
			type: "get",
			url: baseURL + "sys/i18n/check" + params,
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
		q:{
            code: null,
            lang: null,
            value: null
		},
		showList: true,
		title: null,
		i18n: {
			code: "",
			lang: "",
			value: "",
			remark: "",
			id: null
		},
		langList: []
	},
	validations: { //定义校验规则
		i18n: {
			code: {
				required,
				checkI18n
			},
			lang: {
				required
			},
			value: {
				required
			}
		}
	}, 
	methods: {
		query: function () {
			vm.reload();
		},
		getLangList: function(event) { //查字典获取语言种类
			$.getJSON(baseURL + "sys/i18n/getLangList", function(r){
				var langList = [];
				for (var i = 0; i < r.data.length; i++) {
					langList.push(r.data[i]);
				}
				vm.langList = langList;
			});
		},
		add: function(){
			vm.$v.i18n.$reset(); //进入新增页面时，将校验的触发状态清空($dirty=true -> false)
			vm.showList = false;
			vm.title = "<@spring.message 'common.add'/>";
			vm.i18n = {code:null,lang:null,value:null,remark:null,id:null};
		},
		update: function () {
			vm.$v.i18n.$reset(); //进入新增页面时，将校验触发状态清空($dirty=true -> false)
			var id = getSelectedRow();
			if(id == null){
				return ;
			}
			vm.showList = false;
            vm.title = "<@spring.message 'common.edit'/>";
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
			confirm("<@spring.message 'common.confirmDelete'/>", function(){
				$.ajax({
					type: "DELETE",
				    url: baseURL + "sys/i18n/delete",
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
			var url = vm.i18n.id == null ? "sys/i18n/save" : "sys/i18n/update";
			$.ajax({
				type: vm.i18n.id == null ? "POST" : "PUT",
			    url: baseURL + url,
                contentType: "application/json",
			    data: JSON.stringify(vm.i18n),
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
                postData:{'code': vm.q.code, 'lang': vm.q.lang, 'value': vm.q.value},
                page:1
            }).trigger("reloadGrid");
		},
		refresh: function () {
			$.get(baseURL + "sys/i18n/clearCache", function(r){
				parent.location.reload();
            });
		},
		validate: function () {//提交时的校验
			vm.$v.i18n.$touch(); //提交时，不论是否已触发校验，都触发一下($dirty=false -> true)
			return !vm.$v.i18n.$error;
		}
	},
	created: function(){
		this.getLangList();
	}
});
</script>