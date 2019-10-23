<script>
//注册select组件
Vue.component('multiselect', window.VueMultiselect.default);
//引用vuelidate组件
Vue.use(window.vuelidate.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-v-values
var required = window.validators.required;
var helpers = window.validators.helpers;
var integer = window.validators.integer;

Vue.component("label-input",{
    template:"#labelInput",
    props:{
    	defVal: {
    		type: String
    	},
        label:{
            type:String
        },
        value:{
            type:String
        }
    }

});

function getSysDicArr(dtype){
    var container=[];
    $.ajax({
        url:baseURL+"sys/dict/all?type="+dtype+"&_" + $.now(),
        async:false,
        type:"GET",
        success:function(r){
            for (var i = 0; i < r.data.length; i++) {
                var m = {"code": r.data[i].code, "value": r.data[i].value}
                container.push(m.value);
            }
        },
        error : function(result) {
            console.error("error-->"+JSON.stringify(result));
        }
    })
    return container;
}

function getSysDicMap(dtype){
    var container=[];
    $.ajax({
        url:baseURL+"sys/dict/all?type="+dtype+"&_" + $.now(),
        async:false,
        type:"GET",
        success:function(r){
            for (var i = 0; i < r.data.length; i++) {
                var m = {"code": r.data[i].code, "value": r.data[i].value}
                container.push(m);
            }
        },
        error : function(result) {
            console.error("error-->"+JSON.stringify(result));
        }
    })
    return container;
}

function printNodeGroupName (nodeGroup){
    var fullPath="";
    var func=function(nodeGroup){
        var pid=nodeGroup.pid;
        if(pid != 0){
            $.each(vm.nodeGroups,function(i,v){
                if(pid == v.id){
                    fullPath += printNodeGroupName(v)+"/"+nodeGroup.name;
                }
            })
        }else{
            fullPath+=nodeGroup.name;
        }

    }
    func(nodeGroup);
    return fullPath;
}
</script>