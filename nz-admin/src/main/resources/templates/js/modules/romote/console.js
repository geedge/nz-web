<script>
   var terminal=Vue.extend({
           template:"#nzConsole",
           components: {

           },
           props: {
               accountId:{
                 type:Number,
                 required:true
               },
               token:{
                    type:String,
                   required:true
               },
               isNew:{
                   type:Boolean,
                   required:true
               },
               name:{
                 type:String,
                 required:true
               },
               index:{
                   type:Number
               },
               hideAlldom:{
                   type:Function,
                   required:true
               }
           },
           data: function () {
               return {
                   terminalWin:null,
                   domContainerSelector:"",
                   domheadSelector:"",

               }
           },
           validations:{

           },
           created:function(){

           },
           methods: {
                init:function(){
                    var temp=this;
                    temp.domContainerSelector="#terminal-container-"+temp.accountId+"-"+temp.token;
                    temp.domheadSelector="#terminal-head-"+temp.accountId+"-"+temp.token;
                    temp.terminalWin=new JobXTerm(temp.token,null,temp.domContainerSelector);
                    $(temp.domContainerSelector).appendTo($("#terminal-context"));
                    $(temp.domheadSelector).unbind("click").bind("click",temp.activeSelf)

                    if(typeof temp.isNew != "undefined"&&temp.isNew){
                        // $(temp.domContainerSelector).get(0).style.display="none";
                        temp.activeSelf();
                    }


                },
               activeSelf:function(){
                   var temp=this;
                   $(".terminal-head").removeClass("active");
                   temp.hideAlldom();
                   $(temp.domheadSelector).addClass("active");
                   $(temp.domContainerSelector).get(0).style.display="block";
                   temp.$emit("sign-current-win",temp.token);
               },
               hideSelf:function(){
                    var temp=this;
                    try{
                        $(temp.domheadSelector).removeClass("active");
                        $(temp.domContainerSelector).get(0).style.display="none";
                    }catch(e){
                        console.log("errorID-->"+temp.domheadSelector)
                    }

               },
               closeSelf:function(event){
                   event.stopPropagation();
                   this.terminalWin.socket.onclose();
                    // $(this.domContainerSelector).remove();
                    // $(this.domheadSelector).remove();

                   this.$emit("close-win",this.index,this.token);
               }

           },
           computed: {

           },
           watch: {
           },
           mounted: function () {
                this.init();
           }
       })
        Vue.component("nz-terminal",terminal)
       var vm=new Vue({
            el:"#terminalwin",
           data:{
                terminals:[],
               sendAllCmd:"",
               curToken:''
           },
           created:function(){
                var temp=this;
               window.addEventListener("storage", function (e) {
                   if(e.key == 'terminals'){
                       temp.terminals=JSON.parse(e.newValue);
                   }
               });
               window.onbeforeunload = function (e){
                   localStorage.setItem("pageCreated",false);
                   for (var i in temp.terminals){
                       var terminal=temp.terminals;
                       temp.closeWinWithToken(i,terminal.token);
                   }
                   temp.terminals=[]; //会导致刷新页面 控制台也丢失
                   localStorage.setItem("terminals",JSON.stringify(temp.terminals))
                   return true;
               }
               temp.terminals=JSON.parse(localStorage.getItem("terminals"));

           },
           methods:{
                init:function(){
                    var temp=this;
                    $('#file').fileinput({
                        language: 'zh',
                        showPreview : true,
                        browseOnZoneClick:false,
                        uploadUrl : baseURL+'/account/upload.do',
                        removeLabel : "删除",
                        showCaption: true, //是否显示标题,
                        dropZoneEnabled:true,
                        dropZoneTitle:"拖拽文件到这里来上传...",
                        resizeImage: false,
                        previewFileIcon: "<i class='glyphicon glyphicon-king'></i>",
                        initialCaption: "请选择要上传的文件",
                        maxFileSize:1024*1024*100,//文件最大100M
                        allowedFileExtensions : null,
                        elErrorContainer: '#errorBlock',
                        uploadExtraData: function() {
                            var obj = {};
                            obj.token=temp.curToken;
                            obj.path = $("#path").val();
                            return obj;
                        }
                    }).on("fileuploaded", function(event, data) {
                        if (!data.response) {
                            alert('文件格式类型不正确');
                        }else if (data.response.success == "false") {
                            window.setTimeout(function () {
                                $("#upload_push").find(".progress-bar-success").addClass("progress-bar-danger").removeClass("progress-bar-success");
                                $(".file-actions").find(".glyphicon-ok-sign").addClass("text-danger glyphicon-remove-sign").removeClass("glyphicon-ok-sign text-success");
                                var uperrorhtml =
                                    '<span class="close kv-error-close" onclick="colseUpError()">×</span>' +
                                    '<ul>' +
                                    '<li><b>上传失败: (可能有以下原因)</b></li>' +
                                    '<li><b>1): </b>文件大小太大,上传失败</li>' +
                                    '<li><b>2): </b>上传路径是当前路径,且控制台正在实时输出日志(无法获取当前路径)</li>' +
                                    '</ul>';
                                $(".file-error-message").css({"margin":"8px 0px"});
                                $("#errorBlock").html(uperrorhtml).show();
                            },300);
                        }
                    });
                },
               upload:function(){
                   $("#upload_push").modal('show');
                   $(".fileinput-remove-button").click();
                   $("#path").val('');
               },
               hideAllDom:function(){
                   var temp=this;
                   var componets=temp.$refs.nzTerminal;
                   for (var i in componets){
                       var componet=componets[i];
                       componet.hideSelf();
                   }
               },
               signCurrentWin:function(token){
                    this.curToken=token;
               },
               closeWinWithToken:function(index,token){
                    var temp=this;
                   var len=temp.terminals.length;
                   if(len>1){ //避免弹两次询问窗口
                       // confirm("确定关闭窗口吗？",function(){
                       //     temp.closeAjax(index,token);
                       //     layer.closeAll("dialog");
                       // })
                       temp.terminals.splice(index,1);
                       localStorage.setItem("terminals",JSON.stringify(temp.terminals));
                   }else{
                       // temp.closeAjax(index,token);
                       window.close();
                   }

               },
               reopen:function(){
                    var temp=this;
                    $.ajax({
                        url:baseURL+"/account/reopen.do",
                        type:"post",
                        data:{"token":temp.curToken},
                        success:function(){

                        }
                    })
               },
               closeAjax:function(index,token){
                    var temp=this;
                   $.ajax({
                       url:baseURL+"/terminal/close.do",
                       type:"POST",
                       data:{"tokens":token},
                       success:function(r){
                           console.log("close result")
                           console.log(r);
                           if(r.code == "200"){
                                var len=temp.terminals.length;

                               temp.terminals.splice(index,1);
                               localStorage.setItem("terminals",JSON.stringify(temp.terminals));
                               if(len<=1){
                                   window.close();
                               }
                           }else{
                               alert("关闭失败")
                           }
                       }
                   })
               }
           },
           mounted:function(){
                this.init();
           }
       })
</script>