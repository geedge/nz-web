<script>
$(function () {
    $("#jqGrid").jqGrid({
        url: baseURL + '/deteType/list',
        datatype: "json",
        colModel: [
            {label: 'ID', name: 'id', width: 30, key: true,hidden:true},
            {label: '<@spring.message "common.name"/>', name: 'name', sortable: true, width: 60},
            {
                label: '<@spring.message "deteType.method"/>',
                name: 'method',
                sortable: false,
                width: 60,
                formatter: function (value, options, rowData) {
                    return vm.deteMethodsArr[value - 1];
                }
            },
            {label: '<@spring.message "deteType.deteInterval"/>', name: 'deteInterval', sortable: false, width: 100},
            {label: '<@spring.message "deteType.version"/>', name: 'version', sortable: false, width: 100},
            {label: '<@spring.message "common.opTime"/>', name: 'opTime', sortable: true, width: 100},
            {label: '<@spring.message "common.remark"/>', name: 'remark', sortable: false, width: 80}
        ],
        viewrecords: true,
        height: 385,
        rowNum: 10,
        rowList: [10, 30, 50],
        rownumbers: true,
        rownumWidth: 25,
        autowidth: true,
        multiselect: true,
        pager: "#jqGridPager",
        jsonReader: {
            root: "data.list",
            page: "data.currPage",
            total: "data.totalPage",
            records: "data.totalCount"
        },
        prmNames: {
            page: "page",
            rows: "limit",
            order: "order"
        },
        gridComplete: function () {
            //隐藏grid底部滚动条
            $("#jqGrid").closest(".ui-jqgrid-bdiv").css({"overflow-x": "hidden"});
        }
    });
	var ps = new PerfectScrollbar('.meta-win_list');
});

var steps = ['<@spring.message "deteType"/>', '<@spring.message "deteTypeMeta"/>', '<@spring.message "deteTypeParam"/>'];

/*自定义校验规则*/
var nameRepeat = function (name, deteType) {
    if (helpers.req(name) && helpers.req(deteType)) {
        var params = "?name=" + name;
        if (deteType.id) {
            params += "&id=" + deteType.id;
        }
        var flag = true;
        $.ajax({
            type: "get",
            url: baseURL + "/deteType/nameCheck" + params,
            async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
            dataType: "json",
            success: function (r) {
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
    el: '#deteType',
    data: {
        yes: true,
        no: false,
        deteType: {
            id: null,
            name: '',
            viewLevel: 2,
            method: 1,
            deteInterval: 60,
            content: '',
            i18nCode: '',
            remark: '',
            deteTypeMetas: [
                {
                    id: null,
                    name: '',
                    fieldType: null,
                    dataType: null,
                    dataLenth: null,
                    oid: '',
                    unit: '',
                    dicType: '',
                    remark: '',
                    delFlag: '0'
                }
            ],
            deteTypeParams: [
                {
                    id: null,
                    name: '',
                    defVal: '',
                    remark: '',
                    delFlag: '0'
                }
            ]
        },
        params: {
            name: ''
        },
        showList: true,
        title: 'title',
        curStep: 0,
        metaCount: 1,
        paramCount: 1,
        deteMethods: [],//监测方式对象数组，getDeteMethod()中初始化,用于表单
        deteMethod: {}, //接收监测方式对象，在watch中对method 参数赋值
        deteMethodsArr: [], //监测方式数组，getDeteMethod()中初始化,用于列表展示
        fieldTypes: [],//字段类型数组
        fieldType: [],
        dataTypes: [],//数据类型数组
        dataType: [], //数据类型对象
        dicTypes: [],//mapping type数组
        validation: {
            meta: [false],
            param: [false]
        }
    },
    validations: function () {
        if (this.deteType.method == 2) {//snmp 监测
            return { //定义校验规则
                deteType: {
                    name: {
                        required,
                        nameRepeat
                    },
                    method: {},
                   /* deteInterval: {
                        integer
                    },*/
                    content: {},
                    i18nCode: {required},
                    remark: {},
                    deteTypeMetas: {
                        $each: {
                            name: {
                                required
                            },
                            fieldType: {
                                required
                            },
                            dataType: {
                                required
                            },
                            dataLenth: {
                                required,
                                integer
                            },
                            oid: {
                                required
                            },
                            unit: {
                                required
                            },
                            dicType: {},
                            index: {}
                        }
                    },
                    deteTypeParams: {
                        $each: {
                            name: {
                                required
                            },
                            defVal: {
                                required
                            },
                            remark: '',
                            delFlag: '0'
                        }
                    }
                }
            }
        } else {
            return { //定义校验规则
                deteType: {

                    name: {
                        required,
                        nameRepeat
                    },
                    method: {},
                    deteInterval: {
                        integer
                    },
                    content: {},
                    i18nCode: {required},
                    remark: {},
                    deteTypeMetas: {
                        $each: {
                            name: {
                                required
                            },
                            fieldType: {
                                required
                            },
                            dataType: {
                                required
                            },
                            dataLenth: {
                                required,
                                integer
                            },
                            oid: {},
                            unit: {required},
                            dicType: {}
                        }
                    },
                    deteTypeParams: {
                        $each: {
                            name: {
                                required
                            },
                            defVal: {
                                required
                            },
                            remark: ''
                        }
                    }
                }
            }
        }
    },

    created: function () {
        // this.nameCheckFunc = _.debounce(this.nameCheck, 500);
        this.getDeteMethod();
        this.getFieldTypes();
        this.getDataTypes();
        this.getDicTypes();
        this.init();

    },
    methods: {
        init: function () {
            var step = this.curStep;
            this.title = steps[step];
            $("#preStep")[0].disabled = true;
        },
        query: function () {
            vm.showList = true;
            var page = $("#jqGrid").jqGrid('getGridParam', 'page');
            $("#jqGrid").jqGrid('setGridParam', {
                postData: {'name': vm.params.name},
                page: 1
            }).trigger("reloadGrid");
        },
        reload: function () {
            vm.query();
        },
        add: function () {
            // vm.$v.deteType.name.$reset();
            // vm.$v.deteType.method.$reset();
            // vm.$v.deteType.deteInterval.$reset();
            // vm.$v.deteType.content.$reset();
            // vm.$v.deteType.i18nCode.$reset();
            // vm.$v.deteType.remark.$reset();
            // /*遍历touch 对数组调用touch会栈溢出*/
            // for (var i = 0; i < vm.$v.deteType.deteTypeMetas.length; i++) {
            //
            //     vm.$v.deteType.deteTypeMetas.$each[i].$reset();
            // }
            //
            // for (var i = 0; i < vm.$v.deteType.deteTypeParams.length; i++) {
            //     vm.$v.deteType.deteTypeParams.$each[i].$reset();
            // }
            vm.$v.deteType.$reset();
            vm.showList = false;
            vm.curStep = 0;
            var deteType = {
                id: null,
                name: '',
                viewLevel: 2,
                method: 1,
                deteInterval: 60,
                content: '',
                i18nCode: '',
                remark: '',
                deteTypeMetas: [
                    {
                        id: null,
                        name: '',
                        fieldType: null,
                        dataType: null,
                        dataLenth: null,
                        oid: '',
                        unit: '',
                        dicType: '',
                        remark: '',
                        delFlag: '0'
                    }
                ],
                deteTypeParams: [
                    {
                        id: null,
                        name: '',
                        defVal: '',
                        remark: '',
                        delFlag: '0'
                    }
                ]
            }

            vm.deteType = deteType;
        },
        update: function () {
            // vm.$v.deteType.name.$reset();
            // vm.$v.deteType.method.$reset();
            // vm.$v.deteType.deteInterval.$reset();
            // vm.$v.deteType.content.$reset();
            // vm.$v.deteType.i18nCode.$reset();
            // vm.$v.deteType.remark.$reset();
            // /*遍历touch 对数组调用touch会栈溢出*/
            // for (var i = 0; i < vm.$v.deteType.deteTypeMetas.length; i++) {
            //
            //     vm.$v.deteType.deteTypeMetas.$each[i].$reset();
            // }
            //
            // for (var i = 0; i < vm.$v.deteType.deteTypeParams.length; i++) {
            //     vm.$v.deteType.deteTypeParams.$each[i].$reset();
            // }
            vm.$v.deteType.$reset();
            var id = getSelectedRow();
            if (id == null) {
                return;
            }
            vm.showList = false;
            vm.curStep = 0;
            vm.getInfo(id);


        },
        getInfo: function (id) {
            $.get(baseURL + "deteType/detail?id=" + id, function (r) {
                vm.deteType = r.data;
                vm.metaCount = r.data.deteTypeMetas.length;
                vm.paramCount = r.data.deteTypeParams.length;
                vm.formatDeteTypeToObject();
            });
        },
        saveOrUpdate: function () {

            if (vm.validate()) { //在下方定义的validate方法
                return;
            }
            var cloneStr=JSON.stringify(vm.deteType);
            var url = vm.deteType.id == null ? "deteType/save" : "deteType/update";
            var method = vm.deteType.id == null ? "POST" : "PUT";

            //处理json数据
            vm.formatDeteTypeToSimple();


            $.ajax({
                type: method,
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.deteType),
                success: function (r) {
                    if (r.code === 200) {
                        alert('<@spring.message "common.success"/>', function (index) {
                            vm.reload();
                        });
                    } else {
                        var cloneObj=eval("(" + cloneStr + ")");
                        vm.deteType=cloneObj;
                        alert(r.msg);
                    }
                }
            });


        },
        del: function (event) {
            var ids = getSelectedRows();
            if (ids == null) {
                return;
            }
            confirm("<@spring.message 'common.confirmDelete'/>", function () {
                $.ajax({
                    type: "DELETE",
                    url: baseURL + "/deteType/delete",
                    contentType: "application/json",
                    data: JSON.stringify(ids),
                    success: function (r) {
                        if (r.code == 200) {
                            alert('<@spring.message "common.success"/>', function (index) {
                                vm.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });

            })
        },
        validate: function () {//提交时的校验
            var metaFlag = false;
            var paramFlag = false;
            for (var i = 0; i < vm.validation.meta.length; i++) {
                if (vm.validation.meta[i]) {
                    metaFlag = true;
                    break;
                }
            }

            for (var i = 0; i < vm.validation.param.length; i++) {
                if (vm.validation.param[i]) {
                    paramFlag = true;
                    break;
                }
            }
            // vm.$v.deteType.name.$touch();
            // vm.$v.deteType.method.$touch();
            // vm.$v.deteType.deteInterval.$touch();
            // vm.$v.deteType.content.$touch();
            // vm.$v.deteType.i18nCode.$touch();
            // vm.$v.deteType.remark.$touch();
            // /*遍历touch 对数组调用touch会栈溢出*/
            // for (var i = 0; i < vm.$v.deteType.deteTypeMetas.length; i++) {
            //
            //     vm.$v.deteType.deteTypeMetas.$each[i].$touch();
            // }
            //
            // for (var i = 0; i < vm.$v.deteType.deteTypeParams.length; i++) {
            //     vm.$v.deteType.deteTypeParams.$each[i].$touch();
            // }
            vm.$v.deteType.$touch();
            return vm.$v.deteType.$error || metaFlag || paramFlag;
        },
        getDeteMethod: function () {
            $.getJSON(baseURL + "sys/dict/all?type=dtmethond&_" + $.now(), function (r) {
                var deteMethods = [];
                var deteMethodsArr = [];
                for (var i = 0; i < r.data.length; i++) {
                	if (r.data[i].code == 3) { //跳过“内置”类型
                		continue;
                	}
                    var m = {"code": r.data[i].code, "value": r.data[i].value}
                    deteMethods.push(m);
                    deteMethodsArr.push(r.data[i].value);
                    if ("1" == r.data[i].code) { //默认为脚本
                        vm.deteMethod = m;
                    }
                }
                vm.deteMethods = deteMethods;
                vm.deteMethodsArr = deteMethodsArr;
            });
        },
        getFieldTypes: function () {
            $.getJSON(baseURL + "sys/dict/all?type=fieldType&_" + $.now(), function (r) {
                for (var i = 0; i < r.data.length; i++) {
                    var m = {"code": r.data[i].code, "value": r.data[i].value}
                    vm.fieldTypes.push(m);
                }
            });
        },
        getDataTypes: function () {
            $.getJSON(baseURL + "sys/dict/all?type=dataType&_" + $.now(), function (r) {
                for (var i = 0; i < r.data.length; i++) {
                    var m = {"code": r.data[i].code, "value": r.data[i].value}
                    vm.dataTypes.push(m);
                }
            });
        },
        getDicTypes: function () {
            $.getJSON(baseURL + "deteType/dicList?limit=999&_" + $.now(), function (r) {
                for (var i = 0; i < r.data.list.length; i++) {
                    vm.dicTypes.push(r.data.list[i].type)
                }
            });
        },
        preStep: function () {
            this.curStep -= 1;
            if (this.curStep <= 0) {
                $("#preStep")[0].disabled = true;
                $("#nextStep")[0].disabled=false;

            }else if((0<this.curStep && this.curStep<steps.length-1)){
                $("#nextStep")[0].disabled = false;
                $("#preStep")[0].disabled=false;
            }else {
                $("#preStep")[0].disabled = false;
            }
        },
        nextStep: function () {
            this.curStep += 1;
            if (this.curStep >= steps.length - 1) {
                $("#nextStep")[0].disabled = true;
                $("#preStep")[0].disabled=false;
            } else if(0<this.curStep && this.curStep<steps.length-1) {
                $("#nextStep")[0].disabled = false;
                $("#preStep")[0].disabled=false;
            }else{
                $("#nextStep")[0].disabled = false;
            }
        },
        addMetaView: function () {
            var mata = {
                id: null,
                name: '',
                fieldType: null,
                dataType: null,
                dataLenth: null,
                oid: '',
                unit: '',
                dicType: '',
                remark: '',
                delFlag: '0'
            }
            Vue.set(this.deteType.deteTypeMetas, this.metaCount, mata);
            this.metaCount += 1;
        },
        delMetaView: function (index) {
            if (this.deteType.deteTypeMetas[index].id == null) {
                Vue.delete(this.deteType.deteTypeMetas, index);
                this.metaCount -= 1;
            } else {
                this.deteType.deteTypeMetas[index].delFlag = '1';
            }

        },
        addParamView: function () {
            var param = {
                id: null,
                name: '',
                defVal: '',
                remark: '',
                delFlag: '0'
            }


            Vue.set(this.deteType.deteTypeParams, this.paramCount, param);
            this.paramCount += 1;
        },
        delParamView: function (index) {
            if (this.deteType.deteTypeParams[index].id == null) {
                Vue.delete(this.deteType.deteTypeParams, index);
                this.paramCount -= 1;
            } else {
                this.deteType.deteTypeParams[index].delFlag = '1';
            }
        },
        formatDeteTypeToSimple: function () {
            $.each(vm.deteType.deteTypeMetas, function (i, v) {
                vm.deteType.deteTypeMetas[i].fieldType = v.fieldType.code;
                vm.deteType.deteTypeMetas[i].dataType = v.dataType.code;
            });
        },
        formatDeteTypeToObject: function () {
            $.each(vm.deteType.deteTypeMetas, function (index, value) {
                for (var i = 0; i < vm.fieldTypes.length; i++) {
                    if (vm.fieldTypes[i].code == value.fieldType) {
                        vm.deteType.deteTypeMetas[index].fieldType = vm.fieldTypes[i];
                    }
                }

                for (var i = 0; i < vm.dataTypes.length; i++) {
                    if (vm.dataTypes[i].code == value.dataType) {
                        vm.deteType.deteTypeMetas[index].dataType = vm.dataTypes[i];
                    }
                }
            });
        },
        metaNameRepeat: function (index) {
            var name = vm.deteType.deteTypeMetas[index].name;
            if (name == null || name == "") {
                return;
            }
            var flag = false;
            for (var i = 0; i < vm.deteType.deteTypeMetas.length; i++) {
                if (index == i) {
                    continue;
                }
                var tempName = vm.deteType.deteTypeMetas[i].name;

                if (name == tempName) {
                    flag = true;
                    vm.validation.meta[index] = flag;
                    return;
                }
                flag = false;
            }
            vm.validation.meta[index] = flag;
        },
        paramNameRepeat: function (index) {
            var name = vm.deteType.deteTypeParams[index].name;
            if (name == null || name == "") {
                return;
            }
            var flag = false;
            for (var i = 0; i < vm.deteType.deteTypeParams.length; i++) {
                if (index == i) {
                    continue;
                }
                var tempName = vm.deteType.deteTypeParams[i].name;

                if (name == tempName) {
                    flag = true;
                    vm.validation.param[index] = flag;
                    return;
                }
                flag = false;
            }
            vm.validation.param[index] = flag;
        },
        openMetaWindow: function(metaId) {
        	var index = layer.open({
                area: ['600px', '100%'],
                shade: 0.02,
                title: "元数据",
                type: 1,
                maxmin: false,
                scrollbar: false,
                offset: 'r',
                closeBtn: 1,
                move: false,
                content: $("#metaWindow"),
                success:function(){
                    
                }
            });
        },
        delMeta: function(event) {
        	//这里可以改用vue来控制
        	$(event.currentTarget).css("display", "none")
        	$(event.currentTarget).next().css("display", "inline-block");
        	$(".meta-detail").css("color", "lightgray");
        	$(".test").css("text-decoration", "line-through");
        	$(".meta-detail .btn-primary").prop("disabled", true);
        },
        recMeta: function(event) {
        	$(event.currentTarget).css("display", "none")
        	$(event.currentTarget).prev().css("display", "inline-block");
        	$(".meta-detail").css("color", "#333333");
        	$(".test").css("text-decoration", "none");
        	$(".meta-detail .btn-primary").prop("disabled", false);
        },
        addMeta: function() {
        	$(".meta-win_item").removeClass("meta-win_item_active");
        	var hhh = '<div class="meta-win_item meta-win_item_active">' +
        		'<div style="font-size:14px;line-height:30px;">随输入名称联动改变</div>' +
        		'<span style="color: red;position:absolute;top:2px;left:2px;" class="myicon-new"></span>' +
        		'<div style="text-align:right;"><span class="add-box_tag1"></span><span class="add-box_tag2"></span></div>' +
            			'</div>'
        	$(event.currentTarget).parent().before(hhh);
        }
    },
    watch: {
        deteMethod: function () {
            this.deteType.method = this.deteMethod.code;
        },
        curStep: function () {
            this.title = steps[this.curStep]
            if(this.curStep==0){
                $("#preStep")[0].disabled=true;
                $("#nextStep")[0].disabled=false;
            }
        }

    }
});
</script>