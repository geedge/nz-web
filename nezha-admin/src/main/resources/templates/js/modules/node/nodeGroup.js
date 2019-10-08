<script>
    //  定义为 nodegroup 的私有组件
let groupTree = Vue.component('groupTree', {
    template: "#treeTemplate",
    props: {
        znodes: {
            type: Array
        },
        selectedNodeId: "",
        node: null
    },
    data() {
        return {
            selectedNode: {},
            setting: {
                view: {
                    addHoverDom: this.addHoverDom,
                    removeHoverDom: this.removeHoverDom,
                    showIcon: false
                },
                edit: {
                    enable: true,
                    removeTitle: "<@spring.message 'common.delete'/>",
                    renameTitle: "<@spring.message 'common.edit'/>"
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
                    beforeRemove: this.beforeRemove,         // 点击删除时触发，用来提示用户是否确定删除（可以根据返回值 true|false 确定是否可以删除）
                    beforeEditName: this.beforeEditName,     // 点击编辑时触发，用来判断该节点是否能编辑
                    onClick: this.clickNode                  // 点击节点触发的事件
                }
            }
        };
    },
    methods: {
        // 初始化ztree
        init() {
            var tree = $.fn.zTree.init($("#treeDemo" + this.node.id), this.setting, this.znodes);
            tree.expandAll(true);
        },
        // 删除
        beforeRemove: function (treeId, treeNode) {
            var parent = this.$parent;
            confirm("<@spring.message 'common.confirmDelete'/>", function () {
                parent.delete(treeNode);
            });
            return false;
        },
        // 修改
        beforeEditName: function (treeId, treeNode) {
            this.$parent.update(treeNode);
            return false;
        },
        // 点击节点组
        clickNode: function (e, treeId, treeNode) {
            this.$parent.getNodeInfos(treeNode);
            return false;
        },
        // 鼠标划过展示功能按钮
        addHoverDom: function (treeId, treeNode) {
            //var node = this.node;
            var parent = this.$parent;
            var sObj = $("#" + treeNode.tId + "_span");
            if (treeNode.editNameFlag || $("#addBtn_" + treeNode.tId).length > 0 || treeNode.allowSub == 0) { // 可以让不可添加的按钮隐藏
                return;
            }
            // 添加子节点组
            var addStr = "<span class='button add' id='addBtn_" + treeNode.tId + "' title='<@spring.message 'common.add'/><@spring.message 'nodegroup.child.group'/>' onfocus='this.blur();'></span>";
            sObj.append(addStr);
            var btn = $("#addBtn_" + treeNode.tId);
            if (btn) btn.bind("click", function () {
                //vm.$refs.gtree[node.id].open(treeNode);
                parent.add(treeNode, false);
                return false; // 结束方法
            });
        },
        open: function (treeNode) {
            this.$parent.add(treeNode, false);
        },
        // 点击业务系统旁边的添加按钮 添加根节点组
        addRootGroup: function () {
            // 将数据传至父组件 也就是外面的vm 实例中进行添加操作
            this.$emit('addrootgroup', this.node, true);
        },
        // 鼠标移除删除添加按钮
        removeHoverDom: function (treeId, treeNode) {
            $("#addBtn_" + treeNode.tId).unbind().remove();
        },
        // 一下是 展示/样式相关方法
        showTree: function (item) {
            $("#box" + item.id).toggle(200);
            if ($("#icon" + item.id).hasClass("mytreeicon-open")) {
                $("#icon" + item.id).removeClass("mytreeicon-open").addClass("mytreeicon-close");
            } else {
                $("#icon" + item.id).removeClass("mytreeicon-close").addClass("mytreeicon-open");
            }
        },
        showBtn: function (item) {
            $("#icon2" + item.id).css("display", "inline-block");
        },
        hideBtn: function (item) {
            $("#icon2" + item.id).hide();
        },
        showBtn2: function (item) {
            $("#icon2" + item.id).removeClass("mytreeicon-add_unact").addClass("mytreeicon-add_act");
        },
        hideBtn2: function (item) {
            $("#icon2" + item.id).removeClass("mytreeicon-add_act").addClass("mytreeicon-add_unact");
        },
    },
    watch: {
        // 监听树节点的变化更新树
        znodes: function (val) {
            this.init();
        },
    },
    mounted() {
        this.init();
    }
});

var data = {
    ref: '',
    systems: [],
    nodegroups: [],
    showForm: false,
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
}

// 引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-v-values
Vue.use(window.vuelidate.default);
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
            async: false,
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
    el: "#app",
    data: this.data,
    validations: {
        nodeGroup: {
            name: {
                required, nameCheck
            }
        }
    },
    components: {
        groupTree // 私有组件 节点组页面左侧树结构
    },
    methods: {
        getNodeInfos: function (node) {
            vue.nodeGroup = node;
            vue.query();
        },
        add: function (node, isRoot) {
            vm.$v.nodeGroup.$reset();
            if (isRoot) { // 添加根节点组
                vm.showForm = true;
                // 获取当前权限
                vm.system = node;
                vm.nodeGroup = {
                    name: null,
                    id: null,
                    systemId: null,
                    pid: null,
                    remark: null,
                    parentGroup: null
                };
                vm.nodeGroup.parentGroup = {name: "<@spring.message 'nodegroup.root.group'/>", id: 0}; // 根节点组
            } else {
                // 获取节点组的业务系统 作为子节点组的业务系统
                vm.getParentSystem(node.systemId);
                vm.showForm = true;
                vm.nodeGroup = {
                    name: null,
                    id: null,
                    systemId: null,
                    pid: null,
                    remark: null,
                    parentGroup: null
                };
                vm.nodeGroup.parentGroup = node;
            }
            vm.count = 1;
            this.open("<@spring.message 'common.add'/>");
        },
        update: function (node) {
            vm.$v.nodeGroup.$reset();
            $.ajax({
                type: "GET",
                url: baseURL + "nodegroup/detail?id=" + node.id,
                async: false, // 关闭异步
                dataType: "json",
                success: function (r) {
                    vm.nodeGroup = r.data;
                }
            });
            vm.getParentSystem(node.systemId);
            vm.nodeGroup.parentGroup = node.getParentNode() == null ? {name: "<@spring.message 'nodegroup.root.group'/>", id: 0} : node.getParentNode();
            this.open("<@spring.message 'common.edit'/>");
        },
        saveOrUpdate: function () {
            if (!this.validate()) { // 在下方定义的validate方法
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
            $.ajax({
                type: vm.nodeGroup.id == null ? "POST" : "PUT",
                url: baseURL + url,
                contentType: "application/json",
                data: JSON.stringify(vm.nodeGroup),
                success: function (r) {
                    if (r.code === 200) {
                        alert("<@spring.message 'common.success'/>", function (index) {
                            vm.reload();
                        });
                    } else {
                        alert(r.msg);
                    }
                }
            });
        },
        delete: function (node) {
            var form = new FormData();
            form.append("id", node.id);
            $.ajax({
                type: "DELETE",
                url: baseURL + "nodegroup/delete",
                processData: false,
                contentType: false,
                mimeType: "multipart/form-data",
                data: form,
                success: function (r) {
                    if (r.code == 200) {
                        alert("<@spring.message 'common.success'/>", function (index) {
                            vm.reload();
                            location.reload();
                        });
                    } else {
                        alert(r.msg);
                        location.reload();
                    }
                }
            });
        },
        getParentSystem: function (systemId) { // 获取父节点的业务系统
            $.ajax({
                type: "GET",
                url: baseURL + "/system/detail?id=" + systemId,
                async: false,
                dataType: "json",
                success: function (r) {
                    vm.system = r.data;
                }
            });
        },
        open: function (title) {
            layer.open({
                area: ['600px', '400px'],
                shade: 0,
                title: title,
                type: 1,
                fixed: false,
                maxmin: false,
                content: $("#form") // 本页面的元素
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
        }
    },
    created: function () {
        // 获取菜单数据
        $.ajax({
            type: "GET",
            url: baseURL + "nodegroup/list",
            async: false,
            dataType: "json",
            success: function (r) {
                data.systems = r.data.list;
            }
        });
    },
    mounted() {
        // 如果是global 下隐藏树
        if (data.systems.length > 1) {
            data.systems.forEach(item => {
                // 开始隐藏
                $("#box" + item.id).hide();
            });
        }else{
            // 如果只是一条数据的话 默认展开 图标状态转换
            $("#icon" + data.systems[0].id).removeClass("mytreeicon-open").addClass("mytreeicon-close");
        }
    }
})
    </script>