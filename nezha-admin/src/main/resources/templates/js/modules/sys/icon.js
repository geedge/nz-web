<script>
var data = {
	q: {
		iconName: null   // 搜索的名称
	},
	icons: [],			 // 全部的图标
	selectedIcon: null   // 选中的图标
};

var iconVm = new Vue({
	el: '#wrap',
	data: this.data,
	methods: {
		searchIcons: function () {
			// 搜索之前先展开所有
			$("section").css('display','block');
			$(".fontawesome-icon-list div").css('display','block');

			// 如果没有搜索条件则展开所有后直接返回
			if (iconVm.q.iconName == null || iconVm.q.iconName == "") {
				return;
			}

			// 查到的集合 (要展示的集合)
			var searchedIcons = [];
			iconVm.icons.forEach(function (item) {
				if (item.indexOf(iconVm.q.iconName) == 0) {
					searchedIcons.push(item);
				}
			})

			// 没有查到的集合 (要隐藏的集合)
			var otherIcons = iconVm.icons.filter(function (item) {
				if (searchedIcons.indexOf(item) == -1) {
					$(".fa-"+item).parent().parent().hide();
					return item;
				}
			});

			var allIconList = $(".fontawesome-icon-list");
			for (var i = 0; i < allIconList.length; i++) {
				var count = 0;
				var childrens = $(allIconList[i]).children();
				var allLenght = childrens.length;
				for (var j = 0; j < allLenght ; j++) {
					if ($(childrens[j]).is(":hidden")) {
						count++;
					}
				}
				// 如果下方都没有 则隐藏整体
				if (count == allLenght) {
					$(allIconList[i]).parent().hide();
				}
			}
		},
		handleClick: function(event) {
			var paths = event.path;
			for (var i = 0; i < paths.length; i++) {
				if ($(paths[i]).hasClass('fa-hover')) {
					iconVm.selectedIcon = $(paths[i]).text().trim();
					var thatIcon = window.parent.document.getElementById("icon");
					thatIcon.setAttribute('class','fa fa-' + iconVm.selectedIcon + '');
					// window.localStorage.setItem("icon", 'fa fa-'+iconVm.selectedIcon);
					break;
				}
			}
		}
	},
	created: function() {
		var iconStr = $("#icons section div div a").text();
		data.icons = iconStr.split(" ");

		data.icons = data.icons.filter(function (item) {
			if (item.length > 0 && item.indexOf("/r") == -1 && item.indexOf("/n") == -1) {
				return item;
			}
		});
	},
	mounted () {
		window.addEventListener('click',this.handleClick)
	},
	destroyed () {
		window.removeEventListener('click',this.handleClick)
	}
});
</script>