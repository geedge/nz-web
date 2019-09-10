<script>
$(function () {

});

var group_ztree = { // setting
    view: {
        addHoverDom: addHoverDom,
        removeHoverDom: removeHoverDom,
        selectedMulti: false
    },
    edit: {
        enable: true,
        removeTitle: '删除',
        renameTitle: '修改'
    },
    data: {
        simpleData: {
            enable: true,
            idKey: "id",
            pIdKey: "pid",
            rootPId: 0
        }
    },
    check: {
        enable: false,
        nocheckInherit: true
    },
    callback: {
        beforeRemove: beforeRemove,         //点击删除时触发，用来提示用户是否确定删除（可以根据返回值 true|false 确定是否可以删除）
        beforeEditName: beforeEditName,     //点击编辑时触发，用来判断该节点是否能编辑
        // beforeRename: beforeRename,         //编辑结束时触发，用来验证输入的数据是否符合要求(也是根据返回值 true|false 确定是否可以编辑完成)
        beforeDrag: beforeDrag,             //用户禁止拖动节点
        onClick: clickNode                  //点击节点触发的事件
    }
};

// 删除
function beforeRemove(treeId, treeNode) {
    confirm("删除会删除关联节点,确定删除吗?", function () {
        var form = new FormData();
        form.append("id", treeNode.id);
        $.ajax({
            type: "DELETE",
            url: baseURL + "nodegroup/delete",
            processData: false,
            contentType: false,
            mimeType: "multipart/form-data",
            data: form,
            success: function (r) {
                if (r.code == 200) {
                    alert('操作成功', function (index) {
                        vm.reload();
                        location.reload();
                    });
                } else {
                    alert(r.msg);
                    location.reload();
                }
            }
        });
    });
    return false;
}

// 修改
function beforeEditName(treeId, treeNode) {
    vm.$v.nodeGroup.$reset();
    $.ajax({
        type: "GET",
        url: baseURL + "nodegroup/detail?id=" + treeNode.id,
        async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
        dataType: "json",
        success: function (r) {
            vm.nodeGroup = r.data;
        }
    });
    vm.getParentSystem(treeNode.systemId);
    open("修改");
    vm.nodeGroup.parentGroup = treeNode.getParentNode() == null ? {name: '根节点', id: 0} : treeNode.getParentNode();
    return false;
}

function clickNode(e, treeId, treeNode) {
    vue.nodeGroup = treeNode;
    vue.query();
    return false;
}

function beforeDrag(treeId, treeNodes) {
    return false;
}

function removeHoverDom(treeId, treeNode) {
    $("#addBtn_" + treeNode.tId).unbind().remove();
}

// 控制添加按钮和添加功能
function addHoverDom(treeId, treeNode) {
    var sObj = $("#" + treeNode.tId + "_span");
    if (treeNode.editNameFlag || $("#addBtn_" + treeNode.tId).length > 0 || treeNode.allowSub == 0) { // 可以让不可添加的按钮隐藏
        return;
    }
    var addStr = "<span class='button add' id='addBtn_" + treeNode.tId + "' title='添加子节点' onfocus='this.blur();'></span>";
    sObj.append(addStr);
    var btn = $("#addBtn_" + treeNode.tId);
    if (btn) btn.bind("click", function () {

        vm.$v.nodeGroup.$reset();
        // 获取节点组的业务系统 作为子节点组的业务系统
        vm.getParentSystem(treeNode.systemId);
        vm.showForm = true;
        vm.nodeGroup = {
            name: null,
            id: null,
            systemId: null,
            pid: null,
            remark: null,
            parentGroup: null
        };
        vm.nodeGroup.parentGroup = treeNode;
        vm.count = 1;
        open("新增");
    });
}

// 弹出框
function open(title) {
    layer.open({
        area: ['800px', '600px'],
        shade: 0,
        title: title,
        type: 1,
        fixed: false,
        maxmin: false,
        content: $("#form")
    });
}


Vue.use(window.vuelidate.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-v-values
var required = window.validators.required;
var helpers = window.validators.helpers;

// 名称校验
var nameCheck = function (value, nodeGroup) {
    if (helpers.req(value)) {
        var flag = true;
        var params = "?name=" + value + "&pid=" + vm.nodeGroup.parentGroup.id;
        if (nodeGroup.id) {
            params += "&id=" + nodeGroup.id;
        }
        if (nodeGroup.parentGroup.id == 0) { // 添加的是根节点 则参数需要传业务ID
            params += "&systemId=" + vm.system.id;
        }
        $.ajax({
            type: "GET",
            url: baseURL + "nodegroup/groupNameCheck" + params,
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

var data = {
    showForm: false,
    systems: [],
    nodeGroup: {
        name: null,
        id: null,
        systemId: null,
        pid: null,
        remark: null,
        parentGroup: {
            id: null,
            name: null
        }
    },
    system: {
        id: null,
        name: null
    },
    count: 1
};

var vm = new Vue({
    el: '#app',
    data: this.data,
    validations: {
        nodeGroup: {
            name: {required, nameCheck}
        }
    },
    methods: {
        showTree: function (item) {
            $("#" + item.id).toggle(500);
        },
        saveOrUpdate: function () {
            if (!this.validate()) { //在下方定义的validate方法
                return;
            }
            vm.count++;
            var url;
            if (vm.nodeGroup.id == null) {
                url = "nodegroup/save";
            } else {
                url = "nodegroup/update";
            }
            vm.$set(vm.nodeGroup, "pid", vm.nodeGroup.parentGroup.id);
            vm.$set(vm.nodeGroup, "systemId", vm.system == null ? null : vm.system.id);
            if (vm.nodeGroup.id) {
                vm.nodeGroup.pid = vm.nodeGroup.pids = vm.nodeGroup.systemId = null;
            }
            console.log(vm.nodeGroup)
            $.ajax({
                type: vm.nodeGroup.id == null ? "POST" : "PUT",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.nodeGroup),
                success: function (r) {
                    if (r.code === 200) {
                        alert('操作成功', function (index) {
                            vm.reload();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        validate: function () {
            vm.$v.nodeGroup.$touch();
            return !vm.$v.nodeGroup.$error;
        },
        reload: function () {
            if (vm.count == 1) {
                layer.closeAll();
            } else {
                location.reload();
            }
        },
        add: function (system) {
            // 此添加为根节点
            vm.$v.nodeGroup.$reset();
            // 获取当前权限
            vm.system = system;
            vm.showForm = true;
            vm.nodeGroup = {
                name: null,
                id: null,
                systemId: null,
                pid: null,
                remark: null,
                parentGroup: null
            };
            vm.nodeGroup.parentGroup = {name: '根节点', id: 0};
            vm.count = 1;
            open("新增");
        },
        getParentSystem: function (systemId) { // 获取父节点的业务系统
            $.ajax({
                type: "GET",
                url: baseURL + "/system/detail?id=" + systemId,
                async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
                dataType: "json",
                success: function (r) {
                    vm.system = r.data;
                }
            });
        }
    },
    created: function () {
        // 获取菜单数据
        $.ajax({
            type: "GET",
            url: baseURL + "nodegroup/list",
            async: false, //关闭异步，否则会在执行完ajax前就跳到下一段代码
            dataType: "json",
            success: function (r) {
                data.systems = r.data.list;
            }
        });
    },
    mounted: function () {
        // 页面挂载后 创建ztree对象,构建树结构
        if (data.systems.length > 1) {
            data.systems.forEach(item => {
                var treeObj;
                treeObj = $.fn.zTree.init($("#" + item.id), group_ztree, item.nodegroups);
                treeObj.expandAll(true);
                // 开始隐藏
                $("#" + item.id).hide();
            });
        } else {
            var treeObj = $.fn.zTree.init($("#" + data.systems[0].id), group_ztree, data.systems[0].nodegroups);
            treeObj.expandAll(true);
        }
    }
});
</script>