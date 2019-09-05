<script>
//生成菜单
var menuItem = Vue.extend({
    name: 'menu-item',
    props:{item:{}},
    template:[
        '<li>',
        '	<a v-if="item.type === 0" href="javascript:;">',
        '		<i v-if="item.icon != null" :class="item.icon"></i>',
        '		<span>{{item.name}}</span>',
        '		<i class="fa fa-angle-left pull-right"></i>',
        '	</a>',
        '	<ul v-if="item.type === 0" class="treeview-menu">',
        '		<menu-item :item="item" v-for="item in item.list"></menu-item>',
        '	</ul>',

        '	<a v-if="item.type === 1 && item.parentId === 0" :href="\'#\'+item.url">',
        '		<i v-if="item.icon != null" :class="item.icon"></i>',
        '		<span>{{item.name}}</span>',
        '	</a>',

        '	<a v-if="item.type === 1 && item.parentId != 0" :href="\'#\'+item.url"><i v-if="item.icon != null" :class="item.icon"></i><i v-else class="fa fa-circle-o"></i> {{item.name}}</a>',
        '</li>'
    ].join('')
});

//iframe自适应
$(window).on('resize', function() {
	var $content = $('.content');
	$content.height($(this).height() - 154);
	$content.find('iframe').each(function() {
		$(this).height($content.height());
	});
}).resize();

//注册菜单组件
Vue.component('menuItem',menuItem);

var vm = new Vue({
	el:'#rrapp',
	data:{
		user:{},
		menuList:{},
		currSystem: {}, //当前业务系统
		systems:{}, //用户可切换的业务系统列表
		main:"main.html",
		password:'',
		newPassword:'',
        navTitle:"控制台",
        changeSystemShow: false, //是否显示业务系统列表
        changeLangShow: false, //是否显示语言列表
        systemBoxWidth: null, //业务系统列表dom的宽度
        langList: [],
        currLang: {}
	},
	methods: {
		getMenuList: function (event) {
			$.getJSON("sys/menu/nav?_"+$.now(), function(r){
				vm.menuList = r.menuList;
			});
		},
		getUser: function(){
			$.getJSON("sys/user/info?_"+$.now(), function(r){
				vm.user = r.user;
			});
		},
		getCurrSystem: function() {
			$.getJSON("system/detail", function(r){
				vm.currSystem = r.data;
			});
		},
		getSystems: function() {
			$.getJSON("system/listByUser?_"+$.now(), function(r){
				vm.systems = r.data;
			});
		},
		updatePassword: function(){
			layer.open({
				type: 1,
				skin: 'layui-layer-molv',
				title: "修改密码",
				area: ['550px', '270px'],
				shadeClose: false,
				content: jQuery("#passwordLayer"),
				btn: ['修改','取消'],
				btn1: function (index) {
					var data = "password="+vm.password+"&newPassword="+vm.newPassword;
					$.ajax({
						type: "POST",
					    url: "sys/user/password",
					    data: data,
					    dataType: "json",
					    success: function(result){
							if(result.code == 200){
								layer.close(index);
								layer.alert('修改成功', function(index){
									location.reload();
								});
							}else{
								layer.alert(result.msg);
							}
						}
					});
	            }
			});
		},
		toChangeSystem: function(event) {
			vm.changeSystemShow = !(vm.changeSystemShow); 
			vm.changeLangShow = false;
		},
		toChangeLang: function() {
			vm.changeLangShow = !(vm.changeLangShow); 
			vm.changeSystemShow = false;
		},
		getLangList: function(event) { //查字典获取语言种类
			$.getJSON("sys/i18n/getLangList2", function(r){
				vm.langList = r.data;
			});
		},
		changeSystem: function(systemId) {
			$.getJSON("system/changeSystem?id=" + systemId, function(r){
				if (r.code == '200') {
					location.reload();
				} else {
					alert(r.msg);
				}
			});
		},
		changeLang: function(lang) {
			location.href = "?_lang=" + lang;
		},
		getCurrLang: function() {
			$.getJSON("sys/i18n/getCurrLang", function(r){
				if (r.code == '200') {
					vm.currLang = r.data;
				}
			});
		},
        donate: function() {
            layer.open({
                type: 2,
                title: false,
                area: ['806px', '467px'],
                closeBtn: 1,
                shadeClose: false,
                content: ['http://cdn.nezha.io/donate.jpg', 'no']
            });
        },
        logout: function() {
        	$.get("${request.contextPath}/logout");
        	location.reload();
        }
	},
	created: function(){
		this.getMenuList();
		this.getUser();
		this.getSystems();
		this.getCurrSystem();
		this.getLangList();
		this.getCurrLang();
	},
	updated: function(){
		//路由
		var router = new Router();
		routerList(router, vm.menuList);
		router.start();
	}
});



function routerList(router, menuList){
	for(var key in menuList){
		var menu = menuList[key];
		if(menu.type == 0){
			routerList(router, menu.list);
		}else if(menu.type == 1){
			router.add('#'+menu.url, function() {
				var url = window.location.hash;
				
				//替换iframe的url
			    //vm.main = url.replace('#', '');
				goNext(url.replace('#', ''));
			    
			    //导航菜单展开
			    $(".treeview-menu li").removeClass("active");
			    $("a[href='"+url+"']").parents("li").addClass("active");
			    
			    vm.navTitle = $("a[href='"+url+"']").text();
			    
			    window.location.hash = url + "?t=" + new Date().getTime();
			});
		}
	}
}

function goNext(url) {
    vm.main = url;
    $("#childIframe")[0].contentWindow.location.reload(true);
}
</script>