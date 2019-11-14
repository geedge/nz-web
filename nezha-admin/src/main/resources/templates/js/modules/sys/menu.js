<script>

var setting = {
    data: {
        simpleData: {
            enable: true,
            idKey: "menuId",
            pIdKey: "parentId",
            rootPId: -1
        },
        key: {
            url:"nourl"
        }
    }
};
var ztree;


var data = {
    showList: true,
    title: null,
    menu: {
        parentName: null,
        parentId: 0,
        type: 1,
        orderNum: 0,
        icon: null,
        name: null,
        url: null
    }
}

Vue.use(window.vuelidate.default);
var required = window.validators.required;

var urlRequired = function (value, menu) {
    if (menu.type === 1) {
        var flag = true;
        if (isBlank(value)) {
            flag = false;
        }
        return flag;
    } else {
        return true;
    }
}

var vm = new Vue({
    el:'#rrapp',
    data: this.data,
    validations: {
        menu: {
            name: {
                required
            },
            url: {
                urlRequired
            }
        }
    },
    methods: {
        getMenu: function(menuId){
            //加载菜单树
            $.get(baseURL + "sys/menu/select", function(r){
                ztree = $.fn.zTree.init($("#menuTree"), setting, r.menuList);
                var node = ztree.getNodeByParam("menuId", vm.menu.parentId);
                ztree.selectNode(node);

                vm.menu.parentName = node.name;
            })
        },
        add: function(){
            vm.$v.menu.$reset();
            vm.showList = false;
            vm.title = "<@spring.message 'common.add'/>";
            vm.menu = {
                parentName: null,
                parentId: 0,
                type: 1,
                orderNum: 0,
                icon: null,
                name: null,
                url: null
            };
            vm.getMenu();
        },
        update: function () {
            vm.$v.menu.$reset();
            var menuId = getMenuId();
            if(menuId == null){
                return ;
            }
            $.get(baseURL + "sys/menu/info/"+menuId, function(r){
                vm.showList = false;
                vm.title = "<@spring.message 'common.edit'/>";
                vm.menu = r.menu;

                // 处理图标
                if (vm.menu.icon != null && vm.menu.icon != '') {
                    $("#icon").attr('class', vm.menu.icon);
                } else {
                    $("#icon").attr('class', null);
                }

                vm.getMenu();
            });
        },
        del: function () {
            var menuId = getMenuId();
            if(menuId == null){
                return ;
            }

            confirm("<@spring.message 'common.confirmDelete'/>", function(){
                $.ajax({
                    type: "POST",
                    url: baseURL + "sys/menu/delete",
                    data: "menuId=" + menuId,
                    success: function(r){
                        if(r.code === 200){
                            alert("<@spring.message 'common.success'/>", function(){
                                vm.reload();
                            });
                        }else{
                            alert(r.msg);
                        }
                    }
                });
            });
        },
        saveOrUpdate: function () {
            if (!this.validator()) {
                return;
            }

            if ($("#icon").attr('class') != undefined) {
                vm.menu.icon = $("#icon").attr('class').trim();
            } else {
                vm.menu.icon = null;
            }

            var url = vm.menu.menuId == null ? "sys/menu/save" : "sys/menu/update";
            $.ajax({
                type: "POST",
                url:  baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.menu),
                success: function(r){
                    if(r.code === 200){
                        alert("<@spring.message 'common.success'/>", function(){
                            vm.reload();
                        });
                    }else{
                        alert(r.msg);
                    }
                }
            });
        },
        menuTree: function(){
            layer.open({
                type: 1,
                offset: '50px',
                skin: 'layui-layer-molv',
                title: "<@spring.message 'common.pleaseSelect'/><@spring.message 'menu.menu'/>",
                area: ['300px', '450px'],
                shade: 0,
                shadeClose: false,
                content: jQuery("#menuLayer"),
                btn: ["<@spring.message 'common.submit'/>", "<@spring.message 'common.back'/>"],
                btn1: function (index) {
                    var node = ztree.getSelectedNodes();
                    //选择上级菜单
                    vm.menu.parentId = node[0].menuId;
                    vm.menu.parentName = node[0].name;

                    layer.close(index);
                }
            });
        },
        reload: function () {
            vm.showList = true;
            Menu.table.refresh();
            $("#icon").attr('class', '');
        },
        validator: function () {
            vm.$v.menu.$touch();
            return !vm.$v.menu.$error;
        },
        getIcon: function() { // 获取图标
            var currentUrl = window.location.href;
            var a = currentUrl.split('/');
            currentUrl = currentUrl.substring(0,currentUrl.indexOf(a[a.length-1]));
            var index = layer.open({
                area: ['75%', '100%'],
                shade: 0.01,
                title: "<@spring.message 'menu.getIcon'/>", // 获取图标
                type: 2,
                fixed: false,
                maxmin: false,
                scrollbar: false,
                offset: 'r',
                closeBtn: 1,
                move: false,
                content: currentUrl+"icons.html"
            });
        },
        cancelIcon: function () {
            vm.menu.icon = '';
            $("#icon").attr('class', '');
        }
    }
});


var Menu = {
    id: "menuTable",
    table: null,
    layerIndex: -1
};

/**
 * 初始化表格的列
 */
Menu.initColumn = function () {
    var columns = [
        {field: 'selectItem', radio: true},
        {title: 'ID', field: 'menuId', visible: false, align: 'center', valign: 'middle', width: '80px'},
        {title: "<@spring.message 'common.name'/>", field: 'name', align: 'center', valign: 'middle', sortable: true, width: '180px'},
        {title: "<@spring.message 'menu.parent'/>", field: 'parentName', align: 'center', valign: 'middle', sortable: true, width: '100px'},
        {title: "<@spring.message 'menu.icon'/>", field: 'icon', align: 'center', valign: 'middle', sortable: true, width: '80px', formatter: function(item, index){
            return item.icon == null ? '' : '<i class="'+item.icon+' fa-lg"></i>';
        }},
        {title: "<@spring.message 'menu.type'/>", field: 'type', align: 'center', valign: 'middle', sortable: true, width: '100px', formatter: function(item, index){
            if(item.type === 0){
                return "<span class='label label-primary'><@spring.message 'menu.directory'/></span>";
            }
            if(item.type === 1){
                return "<span class='label label-success'><@spring.message 'menu.menu'/></span>";
            }
            if(item.type === 2){
                return "<span class='label label-warning'><@spring.message 'common.button'/></span>";
            }
        }},
        {title: "<@spring.message 'menu.sortNumber'/>", field: 'orderNum', align: 'center', valign: 'middle', sortable: true, width: '100px'},
        {title: "URL", field: 'URL', align: 'center', valign: 'middle', sortable: true, width: '160px'},
        {title: "<@spring.message 'menu.authority.flag'/>", field: 'perms', align: 'center', valign: 'middle', sortable: true}]
    return columns;
};


function getMenuId () {
    var selected = $('#menuTable').bootstrapTreeTable('getSelections');
    if (selected.length == 0) {
        alert("<@spring.message 'common.selectOne'/>"); // 请选择一条记录
        return null;
    } else {
        return selected[0].id;
    }
}


$(function () {
    var colunms = Menu.initColumn();
    var table = new TreeTable(Menu.id, baseURL + "sys/menu/list", colunms);
    table.setExpandColumn(2);
    table.setIdField("menuId");
    table.setCodeField("menuId");
    table.setParentCodeField("parentId");
    table.setExpandAll(false);
    table.init();
    Menu.table = table;
});
</script>
