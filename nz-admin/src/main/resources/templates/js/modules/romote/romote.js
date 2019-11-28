<script>
    var vm = new Vue({
        el:"#terminalWindow",
        data:{
            terminals:[],
            pageCreated:false
        },
        created:function(){
            var temp=this;
            window.addEventListener("setItemEvent", function(e) {
                if(e.key == "pageCreated"){
                    console.log("pageCreated1-->"+e.newValue)
                    temp.pageCreated=e.newValue;
                }
            });
            window.addEventListener("storage", function (e) {
                if(e.key == 'pageCreated'){
                    console.log("pageCreated2-->"+e.newValue)
                    temp.pageCreated=e.newValue;
                }
                if(e.key == 'terminals'){
                    temp.terminals=JSON.parse(e.newValue)
                }
                // localStorage.clear();
            });
        },
        methods:{
            openTerminal:function(id){
                var temp=this;
                $.ajax({
                    url:baseURL+"/account/ssh.do",
                    type: "post",
                    data: {"id":id},
                    success:function(json){

                        if(json&&json.toString().indexOf("login")>-1){
                            window.location.href=baseURL;
                        }
                        console.log(json)
                        if ( json.data.status != "success" ) {
                            if (json.data.status == "authfail" || json.data.status == "keyauthfail") {
                                alert("用户名密码错误,登录失败");
                            } else if (json.data.status == "hostfail") {
                                alert("DNS解析失败");
                            } else if (json.data.status == "genericfail") {
                                alert("连接失败请重试");
                            }
                        } else {
                            confirm("登录成功，确定打开终端吗？",function(index){
                                // temp.showTerminal=true;
                                for (var i in temp.terminals){
                                    var terminal=temp.terminals[i];
                                    terminal.isNew=false;
                                }
                                temp.terminals.push({"token":json.data.token,"name":json.data.name,"accountId":id,"isNew":true})
                                localStorage.clear();
                                localStorage.setItem("terminals",JSON.stringify(temp.terminals))
                                if(!temp.pageCreated||"false"==temp.pageCreated){
                                    console.log('open')
                                    window.open("./console.html","_blank")
                                    localStorage.setItem("pageCreated",true);
                                }
                                layer.closeAll("dialog");
                            })
                        }
                    }
                })

            },

        }


    })
</script>