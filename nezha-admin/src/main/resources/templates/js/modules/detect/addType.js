<script>
$(function(){
    var ps1 = new PerfectScrollbar('.meta-win_list');
    // var ps2 = new PerfectScrollbar('.add-param-box_rows');
    var ps3 = new PerfectScrollbar('.add-box_body_160');
})
Vue.use(window.vuelidate.default);
Vue.component('multiselect', window.VueMultiselect.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-v-values
var required = window.validators.required;
var helpers = window.validators.helpers;
var integer = window.validators.integer;

Vue.directive('id', function (el, binding, vnode) {
        el.id = binding.arg + "-" + binding.value;
    }
)


/*自定义校验规则*/
//监测类别名称重复性验证
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
//统一页面内 名称是否重复 params：数组 index当前检验的元素的下标（排除自己）
var inPageNameRepeat=function(params,index){
    return helpers.withParams({type:inPageNameRepeat},function(value){
        if(helpers.req(value)){
            for (let i in params){
                if(i!=index){
                    var param=params[i];
                    if(helpers.req(param.name)){
                        if(param.name==value){
                            return false;
                        }
                    }

                }
            }
        }
        return true;
    })
}

var meta=Vue.extend({
    template:"#detectMata",
    components: {},
    props: {
        deteMeta:{
            type:Object,
            required:true
        },
        deteMetas:{
            type:Array,
            required:true
        },
        index:{
            type:Number,
            required:true
        },
        deteMethod:{
            type:Object,
            required:true
        },
        fieldTypes:{
            type:Array,
            required:true
        },
        dataTypes:{
            type:Array,
            required:true
        },
        dicTypes:{
            type:Array,
            required:true
        },
        delMeta:{
            type:Function,
            required:true
        }
    },
    data: function () {
        return {
            metaDomIdKey:"r-meta-id",
            metaDetailDomIdKey:"r-meta-detail-id",
            metaDomSelector:"",
            metaDetailDomSelector:"",
            metaDom:null,
            metaDetailDom:null,
            nameRepeat:false
        }
    },
    validations:function(){
        var temp=this;
        if("2"==this.deteMethod.code){
            return {
                deteMeta:{
                    name: {
                        required,
                        inPageNameRepeat:inPageNameRepeat(temp.deteMetas,temp.index)
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
                    remark: {}
                }
            }
        }else{
            return {
                deteMeta:{
                    name: {
                        required,
                        inPageNameRepeat:inPageNameRepeat(temp.deteMetas,temp.index)
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
                    unit: {
                        required
                    },
                    dicType: {},
                    remark: {}
                }
            }
        }


    },
    created:function(){

    },
    methods: {
        init:function(){
            var temp=this;
            temp.metaDomSelector="#"+temp.metaDomIdKey+"-"+temp.index;
            temp.metaDetailDomSelector="#"+temp.metaDetailDomIdKey+"-"+temp.index;
            temp.metaDom=$(temp.metaDomSelector);
            temp.metaDetailDom=$(temp.metaDetailDomSelector);
            temp.metaDom.off("click").on("click",temp.metaDomClick)
            temp.metaDetailDom.appendTo($("#meta-details"));
            if(temp.deteMeta.bestNew!=undefined&&temp.deteMeta.bestNew!=null&&temp.deteMeta.bestNew){
                temp.metaDomClick();
            }
            if(temp.deteMeta.delFlag=='1'){
                temp.addShadow();
            }
        },
        afterHide:function(e){

        },
        addMeta:function(){

        },
        metaDomClick:function(){
            var temp=this;
            $(".meta-win_item").removeClass("meta-win_item_active");
            temp.metaDom.addClass("meta-win_item_active");
            $(".r-dete-meta-detail").hide();
            temp.$emit('swap-meta',temp.index);
            temp.metaDetailDom.show();
            temp.$emit("up-meta",temp.deteMeta)
        },
        delSelf:function(index){
            var temp=this;
            if(temp.deteMeta.new){
                temp.delMeta(index);
                temp.metaDetailDom.remove();
            }else{
                temp.deteMeta.delFlag='1';
                temp.addShadow();
            }

        },
        recSelf:function(){
            this.deteMeta.delFlag='0';
            this.removeShadow();
        },
        validate:function(errContainer){
            var temp=this;
            temp.$v.deteMeta.$touch();
            temp.deteMeta.passFlag=!temp.$v.deteMeta.$error;
            if(errContainer!=undefined&&errContainer instanceof Array){
                errContainer.push(temp.$v.deteMeta.$error);
            }
        },
        addShadow:function(){
            $(this.metaDetailDomSelector+">.shadow").show();
        },
        removeShadow:function(){
            $(this.metaDetailDomSelector+">.shadow").hide();
        },
        mouseAt: function(id) {
            layer.tips($("#r-dictype-option-detail-"+id+'-'+this.index).html(),"#r-dictype-option-"+id+'-'+this.index,{
                tips: [4, "#41B883"],
                closeBtn: 0,
                shade: 0
            });
        },
        mouseout: function(){
            layer.closeAll("tips");
        },

    },
    computed: {},
    watch: {
        '$v.deteMeta': {
            handler() {
                var temp=this;
                var v=temp.$v.deteMeta;
                if(v.name.$anyError){
                    temp.deteMeta.passFlag=false;
                    return ;
                }
                if(v.fieldType.$anyError){
                    temp.deteMeta.passFlag=false;
                    return ;
                }
                if(v.dataType.$anyError){
                    temp.deteMeta.passFlag=false;
                    return ;
                }
                if(v.dataLenth.$anyError){
                    temp.deteMeta.passFlag=false;
                    return ;
                }
                if(v.oid.$anyError){
                    temp.deteMeta.passFlag=false;
                    return ;
                }
                if(v.unit.$anyError){
                    temp.deteMeta.passFlag=false;
                    return ;
                }
                if(v.dicType.$anyError){
                    temp.deteMeta.passFlag=false;
                    return ;
                }
                if(v.remark.$anyError){
                    temp.deteMeta.passFlag=false;
                    return ;
                }

                temp.deteMeta.passFlag=true;
            },
            deep: true
        },
        'deteMeta.delFlag':function(){
            if('1'==this.deteMeta.delFlag){
                this.delSelf(this.index)
            }
        },
        // 'deteMeta.name':function(){
        //     this.$emit('name-check',this.deteMeta.name);
        // }
    },
    mounted: function () {
        this.init();
    }
})

var addMeta=Vue.extend({
    template:"#addDetectMata",
    components: {
        "detect-meta":meta
    },
    props: {
        deteMetas:{
            type:Array,
            required:true
        },
        deteMethod:{
            type:Object,
            required
        },
        fieldTypes:{
            type:Array,
            required:true
        },
        dataTypes:{
            type:Array,
            required:true
        },
        dicTypes:{
            type:Array,
            required:true
        }
    },
    data: function () {
        return {
            counter:1,
            reset:true,
            beforeIndex:null
        }
    },
    validations:function(){


    },
    created:function(){

    },
    methods: {
        addMeta:function(){

            var name='TEMP-'+this.counter;
            var meta={
                id: null,
                name: name,
                fieldType: {
                    code:"3",
                    value:"TEMP"
                },
                dataType: {
                    code:'3',
                    value:"STRING"
                },
                dataLenth: 100,
                oid: '',
                unit: '',
                dicType: '',
                remark: '',
                delFlag: '0',
                new:true,
                bestNew:true,
                passFlag:true
            };

            for (let meta of this.deteMetas){
                meta.bestNew=false;
            }

            this.deteMetas.push(meta);
            this.counter++;
        },
        delMeta:function(index){
            this.deteMetas.splice(index,1);
            $("#meta-details").html("");
            this.resetDom();
        },
        getOpMeta:function(deteMeta){
            var temp=this;
            temp.opDeteMeta=deteMeta;
        },
        resetDom: function () {//强制刷新dom
            var temp = this;
            temp.reset = false;
            temp.$nextTick(() => {
                temp.reset = true
            });
        },
        swapMeta:function(index){
            var temp=this;
            /*校验上一个*/
            if(typeof(temp.beforeIndex)=='number'){
                var componet=temp.$refs['r-detect-meta'][temp.beforeIndex];
                if(componet!=undefined){
                    componet.validate();
                }

            }

            if(typeof(index)=='number'){
                temp.beforeIndex=index;
            }
            /*检验出当前点击的全部*/
            // var componets=temp.$refs['r-detect-meta'];
            // for (let i in componets){
            //     if(i==index){
            //         continue;
            //     }
            //     var componet=componets[i];
            //     componet.validate();
            // }

        },

    },
    computed: {},
    watch: {
        // rowData: {
        //     handler() {
        //         this.init();
        //     },
        //     deep: true
        // }
    },
    mounted: function () {
    }
})

var addParam=Vue.extend({
    template:"#addDetectParam",
    components: {

    },
    props: {
        deteParam:{
            type:Object,
            required:true
        },
        index:{
            type:Number,
            required:true
        },
        deteParams:{
            type:Array,
            required:true
        }
    },
    data: function () {
        return {
            editSwitch:false,
            listPreKey:'r-detect-param-list',
            editPreKey:'r-detect-param-edit',
            listSelector:'',
            editSelector:'',
            listDom:null,
            editDom:null
        }
    },
    validations:function(){
        var temp=this;
        return {
            deteParam:{
                name:{
                    required,
                    inPageNameRepeat:inPageNameRepeat(temp.deteParams,temp.index)
                },
                defVal:{
                    required
                }
            }
        }


    },
    created:function(){

    },
    methods: {
        init:function(){
            var temp=this;
            temp.listSelector="#"+temp.listPreKey+"-"+temp.index;
            temp.editSelector="#"+temp.editPreKey+"-"+temp.index;
            temp.listDom=$(temp.listSelector);
            temp.editDom=$(temp.editSelector);
            if(temp.deteParam.new!=undefined&&temp.deteParam.new!=null&&temp.deteParam.new){
                temp.editSwitch=true;
                temp.listDom.parent().attr("draggable",'false');
            }
            //解决火狐弹出新选项卡的问题
            $("#r-dete-param-container").bind('drop',function(event){
                event.preventDefault();
                event.stopPropagation();
            });
            temp.listDom.parent().bind("dragstart",temp.dragStartFunc);
            temp.listDom.parent().bind("dragend",temp.dragEndFunc);
            temp.listDom.parent().bind("dragenter",temp.dragEnterFunc);
            temp.listDom.parent().bind("dragover",temp.dragOverFunc);
            temp.listDom.parent().bind("dragleave",temp.dragLeaveFunc);
            temp.listDom.parent().bind("drop",temp.dropFunc);


        },
        dragStartFunc:function(event){//拖拽开始时将被拖拽元素透明度设置为50%
            this.editSwitch=false;
            var event = event.originalEvent;
            event.target.style.opacity = .5;
            var index = $(event.target).attr('index');
            //firefox 必须添加
            event.dataTransfer.setData("index",index);
        },
        dragEndFunc:function(event){//拖拽之后恢复被拖拽元素的透明度
            var event = event.originalEvent;
            event.target.style.opacity = 1;
        },
        dragEnterFunc:function(event){//将元素拖入当前元素
            var event = event.originalEvent;
            event.target.style.backgroundColor='#eeeeee';
        },
        dragOverFunc:function(event){//google chrome,opera需要添加
            event.originalEvent.preventDefault();
        },
        dragLeaveFunc:function(event){//将元素拖离当前元素
            var event = event.originalEvent;
            event.target.style.backgroundColor='';
        },
        dropFunc:function(event){//将元素释放到当前元素中
            var event = event.originalEvent;
            var index = event.dataTransfer.getData("index");
            //重置背景色
            event.target.style.backgroundColor='';

            this.$emit("ask-for-swap",index,this.index);

            event.preventDefault();
            event.stopPropagation();
        },
        editSelf:function(){
            var temp=this;
            if(temp.deteParam.delFlag=='0'){
                temp.editSwitch=true;
                temp.listDom.parent().attr("draggable",'false');
            }else{
                return false;
            }

        },
        delSelf:function(e){
            var temp=this;
            var p=temp.deteParam;
            if(p.new!=undefined&&p.new!=null&&p.new){
                temp.$emit("del-param",this.index);
            }else{
                $("#add-box_op_edit_"+temp.index).bind('mouseover',function(event){
                    event.target.style.cursor='not-allowed';
                });
                p.delFlag='1';
            }

        },
        recSelf:function(e){
            var temp=this;
            temp.listDom.removeClass('text-disabled');
            $("#add-box_op_edit_"+temp.index).bind('mouseover',function(event){
                event.target.style.cursor='pointer';
            });
            temp.deteParam.delFlag='0';
        },
        submitSelf:function(){
            var temp=this;



            if(temp.validate()){
                temp.showNameWarningMsg();
                temp.showDefValWarningMsg();
                return ;
            }

            temp.editSwitch=false;
            temp.listDom.parent().attr("draggable",'true');

        },
        validate:function(errContainer){
            var temp=this;
            temp.$v.deteParam.$touch();
            if(errContainer!=undefined&&errContainer instanceof Array){
                errContainer.push(temp.$v.deteParam.$error);
            }

            return temp.$v.deteParam.$error;
        },
        showNameWarningMsg:function(){
            var temp =this;
            if(temp.$v.deteParam.name.$anyError){
                $("#r-param-name-input-"+temp.index).blur();
                var msg='';
                if(temp.$v.deteParam.name.$dirty&&!temp.$v.deteParam.name.required){
                    msg="<div class=\"form-control_error-msg\">必填项</div>"
                }
                if(temp.$v.deteParam.name.$dirty&&!temp.$v.deteParam.name.inPageNameRepeat){
                    msg="<div class=\"form-control_error-msg\">名称重复</div>"
                }
                if(temp.editSwitch){
                    layer.tips(msg, "#r-param-name-input-"+temp.index,{
                        tips:[1, "white"],
                        tipsMore:true
                    });
                }


            }

        },
        showDefValWarningMsg:function(){
            var temp=this;
            if(temp.$v.deteParam.defVal.$anyError){
                $("#r-param-defval-input-"+this.index).blur();
                var msg='';
                if(temp.$v.deteParam.defVal.$dirty&&!temp.$v.deteParam.defVal.required){
                    msg="<div class=\"form-control_error-msg\">必填项</div>"
                }
                if(temp.editSwitch){
                    layer.tips(msg, "#r-param-defval-input-"+temp.index,{
                        tips:[1, "white"],
                        tipsMore:true
                    });
                }

            }
        }

    },
    computed: {},
    watch: {
        "$v.deteParam.name": {
            handler() {
                this.showNameWarningMsg();
            },
            deep: true
        },
        "$v.deteParam.defVal": {
            handler() {
                this.showDefValWarningMsg();
            },
            deep: true
        }
    },
    mounted: function () {
        this.init();
    }
})
var addType=Vue.extend({
    template:"#addTypeTemplate",
    components: {
        "nz-add-meta":addMeta,
        "nz-add-param":addParam
    },
    props: {
        typeId:{

        },
        parentLayerId:{

        },
        reloadFunc:{
            type:Function,
            required:true
        }
    },
    data: function () {
        return {
            title:"监测类别",
            deteType:{
                id: null,
                name: '',
                viewLevel: 2,
                method: 1,
                deteInterval: 60,
                content: '',
                i18nCode: '',
                remark: '',
                deteTypeMetas: [],
                deteTypeParams: []
            },
            deteMethods:[],
            defaultMethod:{},
            fieldTypes:[],
            dataTypes:[],
            dicTypes:[],
            metaReset:true,
            paramReset:true,
            metaRequired:true,
            paramRequired:true,
            typeNotLayer: true
        }
    },
    validations:{

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
            i18nCode: {

            },
            remark: {}
        }

    },
    created:function(){
        this.init();
    },
    methods: {
        init:function(){
            var temp=this;
            temp.getDeteMethods();
            temp.getFieldTypes();
            temp.getDataTypes();
            temp.getDicTypes();
            temp.initDeteType();
            if (temp.$parent.typeNotLayer == false) {
            	temp.typeNotLayer = false;
            }
        },
        initDeteType:function(){
            var temp=this;
            console.log("typeId-->"+temp.typeId)
            if(temp.typeId==null){
               temp.deteType={
                   id: null,
                   name: '',
                   viewLevel: 2,
                   method: 1,
                   deteInterval: 60,
                   content: '',
                   i18nCode: '',
                   remark: '',
                   deteTypeMetas: [],
                   deteTypeParams: []
               }
            }else{
                temp.getTypeInfo();
            }
        },
        getTypeInfo: function () {
            var temp=this;
            $.get(baseURL + "deteType/detail?id=" + temp.typeId, function (r) {
                temp.deteType = r.data;
                temp.formatDeteTypeToObject();
            });
        },
        getDeteMethods:function(){
            var temp=this;
            $.getJSON(baseURL + "sys/dict/all?type=dtmethond&_" + $.now(), function (r) {
                for (let dic of r.data) {
                    var m={
                        code:dic.code,
                        value:dic.value,
                    }
                    if("1"==dic.code){
                        temp.defaultMethod=m;
                    }
                    temp.deteMethods.push(m);
                }
            });
        },
        openMetaWindow: function(i) {
            var temp=this;
            var parentLayer=null;
            if(temp.parentLayerId!=undefined&&temp.parentLayerId!=null) {
                $("#metaWindow").appendTo($('body'))
                parentLayer= $("#layui-layer" + temp.parentLayerId).get(0);
            }
            var metaComponets=null;
            $(parentLayer).animate({right: "600px"}, "normal");
            var index = layer.open({
                area: ['600px', '100%'],
                shade: 0.01,
                title: "元数据",
                type: 1,
                maxmin: false,
                scrollbar: false,
                offset: 'r',
                closeBtn: 1,
                move: false,
                content: $("#metaWindow"),
                zIndex: layer.zIndex,
                success: function(layero,index){
                    metaComponets=temp.$refs.addMetaWindow.$refs['r-detect-meta'];
                    if(temp.parentLayerId!=undefined&&temp.parentLayerId!=null){
                        $(parentLayer).animate({right: "600px"});
                    }
                },
                cancel:function(){
                    metaComponets=temp.$refs.addMetaWindow.$refs['r-detect-meta'];
                    if(metaComponets!=null&&metaComponets!=undefined&&metaComponets.length>0){
                        for(let componet of metaComponets){
                            componet.validate();
                        }
                    }

                    if(temp.parentLayerId!=undefined&&temp.parentLayerId!=null){
                    	$(parentLayer).animate({right: 0}, "normal");
                    }
                },
                end: function() {
                	$("html").css("overflow-y", "hidden");
                }
            });
            if(typeof(i)=="number"){
                metaComponets[i].metaDomClick();
            }


        },
        getFieldTypes:function(){
            var temp=this;
            $.getJSON(baseURL + "sys/dict/all?type=fieldType&_" + $.now(), function (r) {
                for (var i = 0; i < r.data.length; i++) {
                    var m = {"code": r.data[i].code, "value": r.data[i].value,"remark":r.data[i].remark}
                    temp.fieldTypes.push(m);
                }
            });
        },
        getDataTypes: function () {
            var temp=this;
            $.getJSON(baseURL + "sys/dict/all?type=dataType&_" + $.now(), function (r) {
                for (var i = 0; i < r.data.length; i++) {
                    var m = {"code": r.data[i].code, "value": r.data[i].value,"remark":r.data[i].remark}
                    temp.dataTypes.push(m);
                }
            });
        },
        getDicTypes: function () {
            var temp=this;
            $.getJSON(baseURL + "deteType/dicList?limit=999&_" + $.now(), function (r) {
                for (var i = 0; i < r.data.list.length; i++) {
                    temp.dicTypes.push(r.data.list[i])
                }
            });
        },
        spliteName:function(name){
            if(name.length>20){
                return name.substr(0,19)+"..."
            }else{
                return name;
            }
        },
        formatDeteTypeToSimple: function () {
            var temp=this;
            $.each(temp.deteType.deteTypeMetas, function (i, v) {
                temp.deteType.deteTypeMetas[i].fieldType = v.fieldType.code;
                temp.deteType.deteTypeMetas[i].dataType = v.dataType.code;
                temp.deteType.deteTypeMetas[i].dicType=v.dicType.type;
            });
        },
        formatDeteTypeToObject: function () {
            var temp=this;
            $.each(temp.deteType.deteTypeMetas, function (index, value) {
                for (var i = 0; i < temp.fieldTypes.length; i++) {
                    if (temp.fieldTypes[i].code == value.fieldType) {
                        temp.deteType.deteTypeMetas[index].fieldType = temp.fieldTypes[i];
                    }
                }

                for (var i = 0; i < temp.dataTypes.length; i++) {
                    if (temp.dataTypes[i].code == value.dataType) {
                        temp.deteType.deteTypeMetas[index].dataType = temp.dataTypes[i];
                    }
                }
            });
        },
        delMeta:function(index){
            var temp=this;
            var meta=temp.deteType.deteTypeMetas[index];
            if(meta.new!=undefined&&meta.name!=null&&meta.new){
                temp.deteType.deteTypeMetas.splice(index,1);
            }else{
                meta.delFlag='1';
            }
        },
        swapParam:function(srcIndex,destIndex){
            var temp=this;
            var srcParam=temp.deteType.deteTypeParams[srcIndex];
            var destParam=temp.deteType.deteTypeParams[destIndex];

            temp.deteType.deteTypeParams.splice(srcIndex,1,destParam)
            temp.deteType.deteTypeParams.splice(destIndex,1,srcParam)

        },
        addParam:function(){
            var temp=this;
            var param={
                id:null,
                name:'',
                defVal:'',
                delFlag:'0',
                new:true
            }
            temp.deteType.deteTypeParams.push(param);
        },
        delParam:function(index){
            var temp=this;
            temp.deteType.deteTypeParams.splice(index,1);
            temp.resetParamDom();
        },
        resetMetaDom: function () {//强制刷新dom
            var temp = this;
            temp.metaReset = false;
            temp.$nextTick(() => {
                temp.metaReset = true
            });
        },
        resetParamDom:function(){
            var temp = this;
            temp.paramReset = false;
            temp.$nextTick(() => {
                temp.paramReset = true
            });
        },
        saveOrUpdate:function(){
            var temp=this;
            if(temp.validate()){
                return ;
            }

            var cloneStr=JSON.stringify(temp.deteType);
            var url = temp.deteType.id == null ? "deteType/save" : "deteType/update";
            var method = temp.deteType.id == null ? "POST" : "PUT";

            //处理json数据
            temp.formatDeteTypeToSimple();
            console.log(temp.deteType)

            $.ajax({
                type: method,
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(temp.deteType),
                success: function (r) {
                    if (r.code === 200) {
                        temp.reload();
                    } else {
                        var cloneObj=eval("(" + cloneStr + ")");
                        temp.deteType=cloneObj;
                        layer.msg(r.msg);
                    }
                }
            });
        },
        reload:function(){
            this.reloadFunc();
        },
        validate:function(){
            var temp=this;
            temp.$v.deteType.$touch();
            var typeError=temp.$v.deteType.$error;

            var metaErrorArr=[]
            var metaComponets=temp.$refs.addMetaWindow.$refs['r-detect-meta'];
            if(metaComponets!=null&&metaComponets!=undefined&&metaComponets.length>0){
                for(let componet of metaComponets){
                    componet.validate(metaErrorArr);
                }
            }
            var paramErrorArr=[];
            var paramComponets=temp.$refs['r-detect-param'];
            if(paramComponets!=null&&paramComponets!=undefined&&paramComponets.length>0){
                for(let componet of paramComponets){
                    componet.validate(paramErrorArr);
                }
            }
            var metaError=false;
            if(metaErrorArr.length<=0){
                temp.metaRequired=false;
                metaError=true;
            }else{
                temp.metaRequired=true;
                for (let err of metaErrorArr){
                    if(err){
                        metaError=true;
                        break;
                    }
                }
            }

            var paramError=false;
            if(paramErrorArr.length<=0){
                temp.paramRequired=false;
                paramError=true;
            }else{
                temp.paramRequired=true;
                for (let err of paramErrorArr){
                    if(err){
                        paramError=true;
                        break;
                    }
                }
            }
            console.log(typeError+"|"+metaError+"|"+paramError);
            return typeError||metaError||paramError;
        }
    },
    computed: {

    },
    watch: {
        typeId:function(){
            this.getTypeInfo();
        }
    },
    mounted: function () {

    }
})

</script>