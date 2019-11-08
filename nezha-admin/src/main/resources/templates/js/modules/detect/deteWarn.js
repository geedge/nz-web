<script>
var nameRepeat = function (name, deteWarn) {
    if (helpers.req(name) && helpers.req(deteWarn)) {
        var params = "?name=" + name;
        if (deteWarn.id) {
            params += "&id=" + deteWarn.id;
        }
        var flag = true;
        $.ajax({
            type: "get",
            url: baseURL + "/deteWarn/nameCheck" + params,
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
var warnComponet = Vue.extend({
    template:"#warnWindow",
    components:{
        'nz-table':rTable,
        "nz-fileupload":fileUpload
    },
    props:{
        cap:{
            type:String,
            required:true
        },
        setId:{
            type:Number,
            required:true
        },
        closeAllTab:{
            type:Function
        }
        // parentColLen:{
        //     type:Number,
        //     required:true
        // }
    },
    data: function(){
        var temp=this;
        return {
            single:true,
            multi:true,
            setWarns:[],
            setDetail:{},
            state:2,//1,告警个数 2，告警列表 3，编辑告警
            counter:0,
            colModel:[
                {name: "<@spring.message 'common.name'/>", field: "name", width: 120},
                {name: "<@spring.message 'deteWarn.warnLevel'/>", field: "warnLevel", width: 120,formatter:function(rowData){
                        for (let index in temp.warnLevels){
                            var warnLevel=temp.warnLevels[index];
                            if(rowData.warnLevel==warnLevel.code){
                                return warnLevel.value;
                            }
                        }
                        return 'undefinded';
                    }},
                {name: "<@spring.message 'detect.triggerType'/>", field: "triggerType", width: 120,formatter:function(rowData){
                        for (let index in temp.triggerTypes){
                            var triggerType=temp.triggerTypes[index];
                            if(rowData.triggerType==triggerType.code){
                                return triggerType.value;
                            }
                        }
                        return 'undefinded';
                    }},
                {name: "<@spring.message 'detect.warnMode'/>", field: "warnMode", width: 120,formatter:function(rowData){
                        for (let index in temp.warnModes){
                            var warnMode=temp.warnModes[index];
                            if(rowData.warnMode==warnMode.code){
                                return warnMode.value;
                            }
                        }
                        return 'undefinded';
                    }},
                {name: "<@spring.message 'deteWarn.callbackType'/>", field: "callbackType", width: 120,formatter:function(rowData){
                        for (let index in temp.callbackTypes){
                            var callbackType=temp.callbackTypes[index];
                            if(rowData.callbackType==callbackType.code){
                                return callbackType.value;
                            }
                        }
                        return 'undefinded';
                    }},

                {name: "<@spring.message 'common.opTime'/>", field: "opTime", width: 240},
                {name: "<@spring.message 'common.remark'/>", field: "remark", width: 240}
            ],
            headSpan:'warn-span-',
            headSpanSelector:'',
            bodyTab:'warn-tab-',
            bodyTabSelector:'',
            footDiv:'warn-div-',
            footDivSelector:'',
            overcoat:'',
            selectedWarns:[],
            deteWarn:{
                id:null,
                name:'',
                deteTypeId:null,
                deteSetIds:[],
                metaId:'',
                triggerType:'',
                warnMode:'',
                statType:'',
                symbol:'',
                threshold:'',
                tips:'',
                warnLevel:'',
                callbackType:'0',
                callbackContent:'',
                tempCallbackContent:'',//解决更新操作时，回调方式从脚本改为http的回显问题
                viewLevel:'',
                remark:''
            },
            deteMetas:[],
            triggerTypes:[],
            defaultTrigger:{},
            warnModes:[],
            defaultWarnMode:{},
            statTypes:[],
            defaultStatType:{},
            symbols:[],
            defaultSymbol:{},
            warnLevels:[],
            defaultWarnLevel:{},
            callbackTypes:[],
            defaultCallbackType:{},
            viewLevels:[],
            defaultViewLevel:{},
            uploadReset:true,
            file:null,
            acceptFile:".sh",
            hasOldFile:false,
            oldFile:{
                filePath:'',
                remark:'No File'
            }
        }

    },
    created: function () {
        this.initData();
    },
    validations:function() {
        var temp=this;
        var validate={
            name:{
                required,
                nameRepeat
            },
            tips:{
                required,
            },
            metaId:{
                required
            },
            threshold:{},
            callbackType:{},
            callbackContent:{},
            remark:{}
        }
        if(typeof temp.defaultTrigger!='undefined' && temp.defaultTrigger.code=='1'){
            validate.threshold={required}
        }
        if(typeof temp.deteWarn!='undefined' && typeof temp.deteWarn.callbackType!='undefined'&&temp.deteWarn.callbackType=='2'){
            validate.tempCallbackContent={required};
        }
        return {
            deteWarn:validate
        }
    },
    methods: {
        initData:function(){
            var temp=this;
            temp.loadWarns();
            temp.loadSetDetail();
            temp.loadTriggerTypes();
            temp.loadWarnModes();
            temp.loadStateTypes();
            temp.loadSymbols();
            temp.loadWarnLevels();
            temp.loadCallbackTypes();
            temp.loadViewLevels();
        },
        initDom:function(){
            var temp=this;
            temp.headSpanSelector='#'+temp.headSpan+temp.setId;
            temp.bodyTabSelector='#'+temp.bodyTab+temp.setId;

            $("#"+temp.cap).append($(temp.headSpanSelector));
            $(temp.headSpanSelector).bind('click',temp.clickSelf)
            $(temp.headSpanSelector).bind('mouseover',temp.mouseOverSelf)
            $(temp.headSpanSelector).bind('mouseout',temp.mouseOutSelf)

            var parentTr=$("#"+temp.cap).parents('tr');
            var colSpan=parentTr.children('td').length;
            temp.overcoat='constom-td-'+temp.setId;
            var emptyTr='<tr style="background-color: aliceblue;"><td colspan="'+colSpan+'" id="'+temp.overcoat+'"></td></tr>';
            parentTr.after($(emptyTr))

            $("#"+temp.overcoat).append($(temp.bodyTabSelector));
            temp.tabUp();

            temp.footDivSelector='#'+temp.footDiv+temp.setId;

        },
        reset:function(){
            var temp=this;
            temp.deteWarn={
                name:'',
                deteTypeId:this.setDetail.type.id,
                deteSetIds:[this.setId],
                metaId:'',
                triggerType:'',
                warnMode:'',
                statType:'',
                symbol:'',
                threshold:'',
                tips:'',
                warnLevel:'',
                callbackType:'0',
                callbackContent:'',
                tempCallbackContent:'',
                viewLevel:'',
                remark:''
            }
            temp.$v.deteWarn.$reset();
            temp.setDefaultValue();
        },
        loadWarns:function(){
            var temp=this;
            $.ajax({
                url: baseURL + "/deteWarn/list?setId="+temp.setId,
                type:'get',
                sync:false,
                timeout:10000,
                beforeSend:function(XMLHttpRequest){
                },
                success:function(data,textStatus){
                    temp.setWarns=data.data.list;
                },
                error:function(XMLHttpRequest,textStatus,errorThrown){
                }
            })
        },
        loadSetDetail:function(){
            var temp=this;
            $.ajax({
                url: baseURL + "/deteSet/detail?id="+temp.setId,
                type:'get',
                sync:false,
                timeout:10000,
                success:function(data,textStatus){
                    temp.setDetail=data.data;
                    temp.loadDeteMetas();
                }
            })
        },
        mouseOverSelf:function(){
            var temp=this;
            $("#"+temp.cap).get(0).style.cursor='pointer'
        },
        mouseOutSelf:function(){
            var temp=this;
            $("#"+temp.cap).get(0).style.cursor='default'
        },
        clickSelf:function(event) {
            event.stopPropagation()
            var temp = this;
            if(temp.counter%2==1){
                temp.tabDown();
            }else{
                temp.tabUp();
            }
        },
        tabDown:function(){
            var temp=this;
            if(temp.state==1){
                temp.touchBrothor();
                temp.state=2;
                $("#"+temp.overcoat).parent().show();
                temp.counter++;
            }

        },
        tabUp:function(){
            var temp=this;
            if(temp.state==2){
                temp.state=1;
                $("#"+temp.overcoat).parent().hide();
                temp.counter++;
            }

        },
        touchBrothor:function(){
            this.closeAllTab();
        },
        loadDeteMetas: function () {
            var temp=this;
            $.ajax({
                url: baseURL + "deteType/detail?id=" + temp.setDetail.type.id,
                type:'get',
                sync:false,
                success:function(r){
                    temp.deteMetas = r.data.deteTypeMetas;
                }
            })
        },
        loadTriggerTypes:function(){
            var temp=this;
            temp.triggerTypes=[];
            $.getJSON(baseURL + "sys/dict/all?type=triggerType&_" + $.now(), function (r) {
                for (let dic of r.data) {
                    var m={
                        code:dic.code,
                        value:dic.value,
                    }
                    // if("1"==dic.code){
                    //     temp.defaultTrigger=m;
                    // }
                    temp.triggerTypes.push(m);
                }
            })
        },
        loadWarnModes:function(){
            var temp=this;
            temp.warnModes=[];
            $.getJSON(baseURL + "sys/dict/all?type=warnMode&_" + $.now(), function (r) {
                for (let dic of r.data) {
                    var m={
                        code:dic.code,
                        value:dic.value,
                    }
                    // if("1"==dic.code){
                    //     temp.defaultWarnMode=m;
                    // }
                    temp.warnModes.push(m);
                }
            })
        },
        loadStateTypes:function(){
            var temp=this;
            temp.statTypes=[];
            $.getJSON(baseURL + "sys/dict/all?type=statType&_" + $.now(), function (r) {
                for (let dic of r.data) {
                    var m={
                        code:dic.code,
                        value:dic.value,
                    }
                    // if("1"==dic.code){
                    //     temp.defaultStatType=m;
                    // }
                    temp.statTypes.push(m);
                }
            });
        },
        loadSymbols:function(){
            var temp=this;
            temp.symbols=[];
            $.getJSON(baseURL + "sys/dict/all?type=symbol&_" + $.now(), function (r) {
                for (let dic of r.data) {
                    var m={
                        code:dic.code,
                        value:dic.value,
                    }
                    // if("1"==dic.code){
                    //     temp.defaultSymbol=m;
                    // }
                    temp.symbols.push(m);
                }
            });
        },
        loadWarnLevels:function(){
            var temp=this;
            temp.warnLevels=[];
            $.getJSON(baseURL + "sys/dict/all?type=warnLevel&_" + $.now(), function (r) {
                for (let dic of r.data) {
                    var m={
                        code:dic.code,
                        value:dic.value,
                    }
                    // if("3"==dic.code){
                    //     temp.defaultWarnLevel=m;
                    // }
                    temp.warnLevels.push(m);
                }
            });
        },
        loadCallbackTypes:function(){
            var temp=this;
            temp.callbackTypes=[];
            $.getJSON(baseURL + "sys/dict/all?type=callbackType&_" + $.now(), function (r) {
                for (let dic of r.data) {
                    var m={
                        code:dic.code,
                        value:dic.value,
                    }
                    // if("0"==dic.code){
                    //     temp.defaultCallbackType=m;
                    // }
                    temp.callbackTypes.push(m);
                }
            });
        },
        loadViewLevels:function(){
            var temp=this;
            temp.viewLevels=[];
            $.getJSON(baseURL + "sys/dict/all?type=viewLevel&_" + $.now(), function (r) {
                for (let dic of r.data) {
                    var m={
                        code:dic.code,
                        value:dic.value,
                    }
                    // if("1"==dic.code){
                    //     temp.defaultViewLevel=m;
                    // }
                    temp.viewLevels.push(m);
                }
            });
        },
        setDefaultValue:function(){
            var temp=this;
            for (let m of temp.triggerTypes){
                if('1'==m.code){
                    temp.defaultTrigger=m;
                }
            }
            for (let m of temp.warnModes){
                if('1'==m.code){
                    temp.defaultWarnMode=m;
                }
            }
            for (let m of temp.statTypes){
                if('1'==m.code){
                    temp.defaultStatType=m;
                }
            }
            for (let m of temp.symbols){
                if('1'==m.code){
                    temp.defaultSymbol=m;
                }
            }
            for (let m of temp.warnLevels){
                if('3'==m.code){
                    temp.defaultWarnLevel=m;
                }
            }
            for (let m of temp.callbackTypes){
                if('0'==m.code){
                    temp.defaultCallbackType=m;
                }
            }
            for (let m of temp.viewLevels){
                if('1'==m.code){
                    temp.defaultViewLevel=m;
                }
            }

        },
        add:function(){
            var temp=this;
            temp.reset();
            temp.openEditLayer();
            temp.resetFileUploadDom();
        },
        update:function(){
            var temp=this;
            temp.reset();

            temp.getWarnDetail();

            temp.openEditLayer();
            temp.resetFileUploadDom();
        },
        getWarnDetail:function(){
            var temp=this;
            var warn=temp.selectedWarns[0];
            $.ajax({
                type: 'get',
                url: baseURL + 'deteWarn/detail?id='+warn.id,
                contentType: "application/json",
                success: function (r) {
                    if (r.code === 200) {
                        temp.deteWarn=r.data;
                        temp.formatDeteSetToObject()
                    }
                }
            });
        },
        del:function(){
            var temp=this;
            var ids=[];
            for (let m  of temp.selectedWarns){
                ids.push(m.id);
            }
            if(ids.length<1){
                return ;
            }
            confirm("<@spring.message 'common.confirmDelete'/>", function (index) {
                $.ajax({
                    type: "DELETE",
                    url: baseURL + "/deteWarn/delete",
                    contentType: "application/json",
                    data: JSON.stringify(ids),
                    success: function (r) {
                        if (r.code == 200) {
                            alert('<@spring.message "common.success"/>', function (index) {
                                temp.reload();
                            });
                        } else {
                            alert(r.msg);
                        }
                    }
                });

            })
        },
        saveOrUpdate:function(){
            var temp=this;
            if(temp.validate()){
                return ;
            }
            var cloneStr = JSON.stringify(temp.deteWarn);

            var url = temp.deteWarn.id == null ? "deteWarn/save" : "deteWarn/update";
            var method = temp.deteWarn.id == null ? "POST" : "PUT";

            //处理json数据
            temp.formatDeteSetToSimple();

            let formData=new FormData();
            formData.append('deteWarnInfo',JSON.stringify(temp.deteWarn));
            formData.append('file',temp.file);

            $.ajax({
                type: method,
                url: baseURL + url,
                contentType: false,
                processData:false,
                data: formData,
                success: function (r) {
                    if (r.code === 200) {
                        alert('<@spring.message "common.success"/>', function (index) {
                            temp.reset();
                            temp.reload();
                        });
                    } else {
                        var cloneObj = eval("(" + cloneStr + ")");
                        temp.deteWarn = cloneObj;
                        alert(r.msg);
                    }
                }
            });

        },
        reload:function(){
            layer.closeAll('page');
            this.loadWarns();
        },
        validate:function(){
            var temp=this;
            temp.$v.deteWarn.$touch();
            var warnScripError=false;
            var uploadComponet=temp.$refs.warnScriptUpload;
            if(typeof uploadComponet != 'undefined'){
                uploadComponet.touch();
                warnScripError=uploadComponet.error;
            }
            console.log(temp.$v.deteWarn.$error+"|"+warnScripError)
            return temp.$v.deteWarn.$error||warnScripError;
        },
        formatDeteSetToSimple:function(){
            var temp=this;
            temp.deteWarn.metaId=temp.deteWarn.metaId.id;
            temp.deteWarn.warnMode=temp.defaultWarnMode.code;
            if(temp.defaultWarnMode.code=='2'){
                console.log('statType-->'+temp.defaultStatType.code)
                temp.deteWarn.statType=temp.defaultStatType.code;
            }

            temp.deteWarn.triggerType=temp.defaultTrigger.code;
            if(temp.defaultTrigger.code=='1'){//阈值触发
                temp.deteWarn.symbol=temp.defaultSymbol.code;
            }

            temp.deteWarn.warnLevel=temp.defaultWarnLevel.code;

            temp.deteWarn.callbackContent=temp.deteWarn.tempCallbackContent;
        },
        formatDeteSetToObject:function(){
            var temp=this;
            for (let m of temp.deteMetas){
                console.log(temp.deteWarn)
                if(temp.deteWarn.metaId==m.id){
                    temp.deteWarn.metaId=m;
                }
            }
            for (let m of temp.triggerTypes){
                if(temp.deteWarn.triggerType==m.code){
                    temp.defaultTrigger=m;
                }
            }
            for (let m of temp.warnModes){
                if(temp.deteWarn.warnMode==m.code){
                    temp.defaultWarnMode=m;
                }
            }
            for (let m of temp.statTypes){
                if(temp.deteWarn.statType==m.code){
                    temp.defaultStatType=m;
                }
            }
            for (let m of temp.symbols){
                if(temp.deteWarn.symbol==m.code){
                    temp.defaultSymbol=m;
                }
            }
            for (let m of temp.warnLevels){
                if(temp.deteWarn.warnLevel==m.code){
                    temp.defaultWarnLevel=m;
                }
            }
            for (let m of temp.callbackTypes){
                if(temp.deteWarn.callbackType==m.code){
                    temp.defaultCallbackType=m;
                }
            }
            for (let m of temp.viewLevels){
                if(temp.deteWarn.viewLevel==m.code){
                    temp.defaultViewLevel=m;
                }
            }

            if(temp.deteWarn.callbackType=='1'){
                temp.hasOldFile=true;
                if(typeof temp.deteWarn.callbackContent != 'undefined'&&temp.deteWarn.callbackContent!=null&&temp.deteWarn.callbackContent!=''){
                    try {
                        var file=JSON.parse(temp.deteWarn.callbackContent);
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
            if(temp.deteWarn.callbackType=='2'){
                temp.deteWarn.tempCallbackContent=temp.deteWarn.callbackContent;
            }

        },
        getSelectDatas:function(selected,unselected){
            var temp=this;
            temp.selectedWarns=selected;
        },

        openEditLayer:function(){
            var temp=this;
            var title=temp.setDetail.type.name+" > "+temp.setDetail.name+" > 告警";
            var index = layer.open({
                area: ['600px', '100%'],
                shade: 0.01,
                title: title,
                type: 1,
                maxmin: false,
                scrollbar: false,
                offset: 'r',
                closeBtn: 1,
                move: false,
                content: $(temp.footDivSelector),
                zIndex: layer.zIndex,
                success: function(layero,index){

                },
                cancel:function(){

                },
                end: function() {
                    $("html").css("overflow-y", "hidden");
                }
            });
        },
        resetFileUploadDom:function(){
            var temp = this;
            temp.uploadReset = false;
            temp.$nextTick(() => {
                temp.uploadReset = true
            });
        },
        destoryUploadComponet:function(){
            this.file=null;
            // this.acceptFile=".sh";
            // this.hasOldFile=false;
            // this.oldFile={
            //     filePath:'',
            //     remark:'No File'
            // }
        }
    },
    watch:{
        selectedWarns:function(){
            var temp=this;
            if(temp.selectedWarns.length>0){
                temp.multi=false;
                temp.single=false;
                if(temp.selectedWarns.length>1){
                    temp.single=true;
                }
            }else{
                temp.multi=true;
                temp.single=true;
            }
        }
        // viewers:{
        //     handler(){
        //     },
        //     deep:true
        // }
    },
    mounted() {
        this.initDom();
    }

})

var warnComponets=Vue.extend({
        template:'#warnWindows',
        props:{

        },
        components:{
            'nz-warn':warnComponet,
        },
        data:function(){
            return {
                containerInfos:new Map(),
                multi:true
            }
        },
        created: function () {
            var temp=this;
            temp.init();
        },
        validations: {

        },
        methods: {
            init:function(){
                var temp=this;
                $(".td-warn-win").each(function(index,e){
                    var setId=parseInt($(e).attr('setId'));
                    var id=$(e).attr('id');
                    temp.containerInfos.set(id,setId);
                });
            },
            closeTabs:function(){
                var temp=this;
                var componets=temp.$refs.nzwarn;
                for (let componet of componets){
                    componet.tabUp();
                }
            }
        },
        watch:{
            // viewers:{
            //     handler(){
            //     },
            //     deep:true
            // }
        },
        mounted() {

        }
    })



    </script>