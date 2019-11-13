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
            if(temp.deteMeta.delFlag=='1'||temp.deteMethod.code=='3'){
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
        mouseAt: function(id, event) {
            layer.tips($("#r-dictype-option-detail-"+id+'-'+this.index).html(), $(event.currentTarget).prev(), {
                tips: [4, "#41B883"],
                success: function(layi, i) {
                	$(layi).css("z-index", "99999999")
                }
            });
            /*layer.tips($("#r-dictype-option-detail-"+id+'-'+this.index).html(),"#r-dictype-option-"+id+'-'+iii,{
            	tips: [4, "#41B883"],
            	closeBtn: 0,
            	shade: 0
            });*/
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
            if(this.deteMethod.code=='3'){
                return ;
            }
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
        deteMethod:{
          type:Object,
          required:true
        },
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
            if(typeof temp.deteParam.new!='undefined'&&temp.deteParam.new!=null&&!temp.deteParam.isEdited){
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
            this.submitSelf();
            if(!this.$v.deteParam.$error){
                this.$emit("ask-for-swap",index,this.index);
            }

            event.preventDefault();
            event.stopPropagation();
        },
        editSelf:function(){
            var temp=this;
            if(temp.deteMethod.code=='3'){
                return ;
            }
            if(temp.deteParam.delFlag=='0'){
                temp.editSwitch=true;
                temp.listDom.parent().attr("draggable",'false');
            }else{
                return false;
            }

        },
        delSelf:function(e){
            var temp=this;
            if(temp.deteMethod.code=='3'){
                return ;
            }
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
            temp.deteParam.isEdited=true;
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
                    msg="<div class=\"form-control_error-msg\"><@spring.message 'validate.common.required'/></div>"
                }
                if(temp.$v.deteParam.name.$dirty&&!temp.$v.deteParam.name.inPageNameRepeat){
                    msg="<div class=\"form-control_error-msg\"><@spring.message 'validate.common.name.repeat'/></div>"
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
                    msg="<div class=\"form-control_error-msg\"><@spring.message 'validate.common.required'/></div>"
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
        "nz-add-param":addParam,
        "nz-fileupload":fileUpload
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
            title:"<@spring.message 'deteType'/>",
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
                deteTypeParams: [],
            },
            deteMethods:[],
            defaultMethod:{},
            innerMethod:null,
            fieldTypes:[],
            dataTypes:[],
            dicTypes:[],
            metaReset:true,
            paramReset:true,
            uploadReset:true,
            metaDirty:false,
            metaRequired:true,
            paramDirty:false,
            paramRequired:true,
            typeNotLayer: true,//判断是否是在layer中打开，
            isUpdate:false, //是否是更新操作，更新不容许修改监测方式
            file:null,
            acceptFile:".sh",
            hasOldFile:false,
            oldFile:{
                filePath:'',
                remark:'No File'
            },
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
            temp.resetType();
            if(temp.typeId==null){
                temp.isUpdate=false;
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
                   deteTypeParams: [],
               }
            }else{
                temp.isUpdate=true;
                temp.getTypeInfo();
            }

            temp.resetMetaDom();
            temp.resetParamDom();
            temp.resetFileUploadDom();
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
            temp.deteMethods=[];
            $.getJSON(baseURL + "sys/dict/all?type=dtmethond&_" + $.now(), function (r) {
                for (let dic of r.data) {

                    var m={
                        code:dic.code,
                        value:dic.value,
                    }
                    if('3'==dic.code){
                        temp.innerMethod=m;
                        continue;
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
            temp.metaDirty=true;
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
                title: "<@spring.message 'deteType.meta'/>",
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
                    if(!temp.parentLayerId){
                        $(parentLayer).animate({right: "600px"});
                    }

                    if(temp.deteType.deteTypeMetas.length<1){
                        temp.$refs.addMetaWindow.addMeta();
                    }
                },
                cancel:function(){
                    metaComponets=temp.$refs.addMetaWindow.$refs['r-detect-meta'];
                    if(typeof metaComponets!='undefined'&&metaComponets!=null&&metaComponets.length>0){
                        for(let componet of metaComponets){
                            componet.validate();
                        }
                    }

                    if(typeof temp.parentLayerId!='undefined'&&temp.parentLayerId!=null){
                    	$(parentLayer).animate({right: 0}, "normal");
                    }
                },
                end: function() {
                	if(temp.parentLayerId!=undefined&&temp.parentLayerId!=null) {
                		$("html").css("overflow-y", "hidden");
                	}
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
            temp.deteType.method=temp.defaultMethod.code;
            $.each(temp.deteType.deteTypeMetas, function (i, v) {
                temp.deteType.deteTypeMetas[i].fieldType = v.fieldType.code;
                temp.deteType.deteTypeMetas[i].dataType = v.dataType.code;
                if(typeof v.dicType!='undefined' && v.dicType!=null){
                    temp.deteType.deteTypeMetas[i].dicType=v.dicType.type;
                }
            });
        },
        formatDeteTypeToObject: function () {
            var temp=this;
            if(temp.deteType.method=='3'){
                temp.defaultMethod=temp.innerMethod;
            }else{
                for (let method of temp.deteMethods){
                    if(temp.deteType.method==method.code){
                        temp.defaultMethod=method;
                        break;
                    }
                }
            }

            $.each(temp.deteType.deteTypeMetas, function (index, value) {
                for (var i = 0; i < temp.fieldTypes.length; i++) {
                    if (temp.fieldTypes[i].code == value.fieldType) {
                        temp.deteType.deteTypeMetas[index].fieldType = temp.fieldTypes[i];
                        break;
                    }
                }

                for (var i = 0; i < temp.dataTypes.length; i++) {
                    if (temp.dataTypes[i].code == value.dataType) {
                        temp.deteType.deteTypeMetas[index].dataType = temp.dataTypes[i];
                        break;
                    }
                }

                for (var i = 0; i < temp.dicTypes.length; i++){
                    if(temp.dicTypes[i].type == value.dicType){
                        temp.deteType.deteTypeMetas[index].dicType=temp.dicTypes[i];
                        break;
                    }
                }
            });
            if(temp.deteType.method=='1'){
                temp.hasOldFile=true;
                if(typeof temp.deteType.content != 'undefined'&&temp.deteType.content!=null&&temp.deteType.content!=''){
                    try {
                        var file=JSON.parse(temp.deteType.content);
                        temp.oldFile.filePath=file.filePath;
                        temp.oldFile.remark=file.fileName;
                    }catch (e) {
                        temp.oldFile.filePath='';
                        temp.oldFile.remark='No File';
                    }

                }else{
                    temp.oldFile.filePath='';
                    temp.oldFile.remark='No File';
                }
            }else{
                temp.hasOldFile=false;
                temp.oldFile.filePath='';
                temp.oldFile.remark='No File';
            }
        },
        delMeta:function(index){
            if(this.deteType.method=='3'){
                return ;
            }
            var temp=this;
            temp.deteType.method=temp.defaultMethod.code;
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
            if(this.deteType.method=='3'){
                return ;
            }
            var temp=this;
            temp.paramDirty=true;

            var paramLen=temp.deteType.deteTypeParams.length;
            if(paramLen>0){
                for (var i=0;i<paramLen;i++){
                    temp.$refs['r-detect-param'][i].submitSelf();
                    if(temp.$refs['r-detect-param'][i].$v.deteParam.$error){
                        return;
                    }
                }
            }

            var param={
                id:null,
                name:'',
                defVal:'',
                delFlag:'0',
                new:true,
                isEdited:false
            }
            temp.deteType.deteTypeParams.push(param);
        },
        delParam:function(index){
            var temp=this;
            if(this.deteType.method=='3'){
                return ;
            }
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
        resetFileUploadDom:function(){
            var temp = this;
            temp.uploadReset = false;
            temp.$nextTick(() => {
                temp.uploadReset = true
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
            let formData=new FormData;
            formData.append('deteTypeInfo',JSON.stringify(temp.deteType))
            formData.append('file',temp.file);
            $.ajax({
                type: method,
                url: baseURL + url,
                // contentType: "application/json",
                contentType:false,
                processData:false,
                data:formData,
                success: function (r) {
                    if (r.code === 200) {
                        temp.reload();
                    } else {
                        var cloneObj=eval("(" + cloneStr + ")");
                        temp.deteType=cloneObj;
                        alert(r.msg);
                    }
                }
            });
        },
        reload:function(){
            this.reloadFunc();
        },
        validate:function(){
            var temp=this;
            temp.touchType();
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
            var uploadError=false;
            if(temp.deteType.method=='1'&&!temp.isUpdate) {
                var uploadComponet = temp.$refs.scriptUpload;
                if (typeof uploadComponet != "undefined") {
                    uploadError = uploadComponet.error;
                }
            }

            return typeError||metaError||paramError||uploadError;
        },
        touchType:function(){
            var temp=this;
            temp.$v.deteType.$touch();
            temp.metaDirty=true;
            temp.paramDirty=true;
            if(temp.deteType.method=='1'&&!temp.isUpdate){
                var uploadComponet=temp.$refs.scriptUpload;
                if(typeof uploadComponet != "undefined"){
                    uploadComponet.touch();
                }
            }

        },
        resetType:function(){
            var temp=this;
            temp.$v.deteType.$reset();
            temp.metaDirty=false;
            temp.paramDirty=false;
        },
        destoryUploadComponet:function(){
            this.file=null;
            this.acceptFile=".sh";
            this.hasOldFile=false;
            this.oldFile={
                filePath:'',
                remark:'No File'
            }
        }
    },
    computed: {

    },
    watch: {
        typeId:function(){
            this.initDeteType();
        },
        'deteType.method':{
            handler:function(){
                var temp=this;
                if(temp.deteType.method=='3'){
                    $('.input-control').each(function(i,v){
                        v.disabled='disabled';
                    })

                }
            },
            immediate: true
        },
        'deteType.deteTypeMetas':function(){
            if(this.deteType.deteTypeMetas.length<1){
                this.metaRequired=false;
            }else{
                this.metaRequired=true;
            }
        },
        'deteType.deteTypeParams':function(){
            if(this.deteType.deteTypeParams.length<1){
                this.paramRequired=false;
            }else{
                this.paramRequired=true;
            }
        }
    },
    mounted: function () {

    }
})

</script>