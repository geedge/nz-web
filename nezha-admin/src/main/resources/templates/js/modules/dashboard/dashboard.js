<script>
	$(function(){
		//初始化时间范围选择
		var rangesConf = {};
	    rangesConf['最近一小时'] = [moment().subtract(1, 'hours'), moment()];
	    rangesConf['今日'] = [moment().startOf('day'), moment().endOf('day')];
	    rangesConf['昨日'] = [moment().subtract(1, 'days').startOf('day'), moment().subtract(1, 'days').endOf('day')];
	    rangesConf['最近一月'] = [moment().startOf('month'), moment().endOf('month')];
	    rangesConf['上个月'] = [moment().subtract(1, 'months').startOf('month'), moment().subtract(1, 'months').endOf('month')];
	    rangesConf['最近一周'] = [moment().subtract(1, 'weeks').startOf('day'), moment().endOf('day')];
	    rangesConf['本月'] = [moment().subtract(1, 'months').startOf('day'), moment().endOf('day')];
		$('#filterTime').daterangepicker({
	        autoApply:false,
	        singleDatePicker:false,
	        showDropdowns:false,        // 是否显示年月选择条件
			timePicker: true, 			// 是否显示小时和分钟选择条件
			timePickerIncrement: 10, 	// 时间的增量，单位为分钟
	        timePicker24Hour : true,
	        opens : 'left', //日期选择框的弹出位置
			ranges: rangesConf,
	        locale : {
	            format: 'YYYY-MM-DD HH:mm:ss',
	            separator : ' - ',
	            customRangeLabel : '自定义' ,
	            applyLabel : '确定' ,
	            cancelLabel : '取消' ,
	            fromLabel : '起始时间' ,
	            toLabel : '结束时间' ,
	            daysOfWeek : '日,一,二,三,四,五,六'.split(',') ,        // '日', '一', '二', '三', '四', '五', '六'
	            monthNames : '一月,二月,三月,四月,五月,六月,七月,八月,九月,十月,十一月,十二月'.split(',') ,        // '一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'
	            firstDay : 1
	        },
	        startDate: rangesConf['今日'][0],
	        endDate: rangesConf['今日'][1]
		});
		
		// 初始化仪表盘拖拽
		var gridster = $(".gridster > ul").gridster({
	        widget_margins: [20, 20], // 模块的间距 [上下,左右]
	        widget_base_dimensions: [150, 150], //模块的宽高 [宽,高]
	        min_cols:6,     //设置最小列数
	        max_cols:10,     //设置最大列数
	        draggable: {
	        	handle: 'header' //模块内定义拖动的元素<header>，这里也支持jquery选择器，如"span.drag_handle"
	        }, 
	        serialize_params: function($w, wgd) {
	            return { chartId: $w[0].id, type:$w[0].type, x: wgd.row, y: wgd.col,  width: wgd.size_x*150, height: wgd.size_y*150 }
	         },
	        helper: 'clone',
	        resize: {
				enabled: true,
				max_size: [10, 20],
				min_size: [1, 1],
				start: function (e, ui, $widget) {
				},
                resize: function (e, ui, $widget) {
                },
                stop: function (e, ui, $widget) {
				}
			},
	    }).data('gridster');		
		
		var singleValue=10;
		var now = +new Date(2019, 9, 3);
		var oneDay = 24 * 3600 * 1000;
		var value = Math.random() * 1000;
		var datas=[];
		var series=[];
		function randomData() {
	 	    now = new Date(+now + oneDay);
	 	    value = value + Math.random() * 21 - 10;
	 	    return {
	 	        name: now.toString(),
	 	        value: [
	 	            [now.getFullYear(), now.getMonth() + 1, now.getDate()].join('/'),
	 	            Math.round(value)
	 	        ]
	 	    }
	 	}
	 	
	 	for (var i = 0; i < 50; i++) {
	 	    datas.push(randomData());
	 	}
	 	var shamData=
	    	{
	    		id:0,
	            name: 'series1',
	            type: 'line',
	            showSymbol: false,
	            hoverAnimation: false,
	            lineStyle:{
                    width:1
                },
                areaStyle:{
                    opacity:0
                },
	            data: datas
	        }
	 	series.push(shamData);
		/* 初始化点 线 柱图 */
        myChart = echarts.init(document.getElementById('chart'));

		var colors = ['#5793f3', '#d14a61', '#DC143C'];

		option = {
		    color: colors,
		    tooltip: {
		        trigger: 'item',
		        axisPointer: {
		            type: 'cross'
		        }
		    },
		    legend:{
		    	show:vm.chartStatistical.legend==1?true:false,
		    	x:vm.chartStatistical.legendXPosition.code?vm.chartStatistical.legendXPosition.code:"center",
		    	y:vm.chartStatistical.legendYPosition.code?vm.chartStatistical.legendYPosition.code:"top"
		    },
		    toolbox:{
                feature:{
                    dataZoom:{
                    	title:false
                    }
                }
            }, 
            dataZoom:[{
        　　　　			type:"inside"         //详细配置可见echarts官网
        　　			}],
		    grid: {
		        right: '20%'
		    },
		    xAxis: [
		        {
		        	name:vm.chartStatistical.xaxisName?vm.chartStatistical.xaxisName:"Time",
		        	type:'time'
		        }
		    ],
		    yAxis: [
		        {
		        	show:true,
		            type: 'value',
		            name: vm.chartStatistical.yaxesName?vm.chartStatistical.yaxesName:"Value",
		            position: 'left'
		        },
		        {
		        	show:false,
		            type: 'value',
		            name:vm.chartStatistical.yaxisName?vm.chartStatistical.yaxisName:"Value",
		            position: 'right'
		        }
		    ],
		    series: series
		};
		// 使用刚指定的配置项和数据显示图表。
        myChart.setOption(option);
		
        /* 初始化单值图  */
        singleChart=echarts.init(document.getElementById('singleChart'));
		var angle = [220, -40]
		singleOption = {
			    backgroundColor: '#fff',
			    tooltip: {
			        show: false,
			        formatter: "{a} <br/>{b} : {c}%"
			    },
			    series: [{
			        name: '最外层环仪表盘',
			        type: "gauge",
			        min: vm.chartSingle.minValue?vm.chartSingle.minValue:0,
			        max: vm.chartSingle.maxValue?vm.chartSingle.maxValue:100,
			        startAngle: 220,
			        endAngle: -40,
			        splitNumber: 20,
			        axisLine: {
			            lineStyle: {
			                color: [
			                    [1, "#c1c3c5"]
			                ],
			                width: 10,
			                opacity:vm.chartSingle.isGauge==1?1:0
			            }
			        },
			        axisTick: {
			            show:false,
			            lineStyle: {
			                color: "#fff",
			                width: 2,
			            },
			            length: 0,
			            splitNumber: 1
			        },
			        axisLabel: {
			            show:vm.chartSingle.isScale==1?true:false,
			            distance: -40,
			            textStyle: {
			                color: "#bbb"
			            },
			            rich: {
			                a: {
			                    color: '#fbe010'
			                },
			                b: {
			                    color: '#3fa7dc'
			                }
			            }
			        },
			        splitLine: {
			            show: vm.chartSingle.isScale==1?true:false,
			            length: 10,
			            lineStyle: {
			                color: '#fff',
			                width: 2
			            }
			        },
			        itemStyle: {
			            normal: {
			                color: "#818488",
			                shadowColor: 'rgba(0, 0, 0, 0.5)',
			                shadowBlur: 10
			            }
			        },
			        detail: {
			            formatter: function(v) {
			            	return singleFormat(v);
			            },
			            offsetCenter:vm.chartSingle.isGauge==1?[0, "50%"]:[0, "10%"],
			            textStyle: {
			                fontSize: vm.chartSingle.valueFontSize?vm.chartSingle.valueFontSize:50,
			                color: vm.chartSingle.colorValue?vm.chartSingle.colorValue:"#333"
			            },
			            rich:{
			                a:{
			                    fontSize:vm.chartSingle.isGauge==1?vm.chartSingle.valueFontSize:vm.chartSingle.prefixFontSize?vm.chartSingle.prefixFontSize:30,
			                    color:vm.chartSingle.colorPrefix?vm.chartSingle.colorPrefix:'#000',
			                    textShadowOffsetX:10
			                },
			                b:{
			                    fontSize:vm.chartSingle.isGauge==1?vm.chartSingle.valueFontSize:vm.chartSingle.suffixFontSize?vm.chartSingle.suffixFontSize:60,
			                    color:vm.chartSingle.colorSuffix?vm.chartSingle.colorSuffix:'#000'
			                }
			            }
			        },
			        title: {
			            show:false
			        },
			        pointer:{
			            show:vm.chartSingle.isGauge==1?true:false,
			            length: '85%'
			        },
			        data: [{
			            value: singleValue
			        }]
			    },{
			        name: "内环仪表盘四段颜色",
			        type: "gauge",
			        min: 0,
			        max: 100,
			        radius: '69%',
			        startAngle: 220,
			        endAngle: -40,
			        splitNumber: 10,
			        axisLine: {
			            lineStyle: {
			                color: [[0.6, '#fb5310'],[0.85, '#fbe010'],[1, "#00ff00"]],
			                width: 30,
			                opacity:vm.chartSingle.isThreshold==1?1:0
			            }
			        },
			        axisTick: {
			            show: vm.chartSingle.isThreshold==1?true:false
			        },
			        axisLabel: {
			            show: false
			        },
			        splitLine: {
			            show: vm.chartSingle.isThreshold==1?true:false,
			            length: 15,
			            lineStyle: {
			                color: '#fff',
			                width: 2
			            }
			        },
			        pointer: {
			            length: 0
			        },
			        detail: {
			            show: false
			        },
			    }]
			}
		singleChart.setOption(singleOption);
		/* 仪表盘对应配置修改  */
		function singleFormat(v){
			return '{a| '+vm.chartSingle.prefix+'} '+v+' {b| '+vm.chartSingle.suffix+'}';
		}
		
		// 是否为线图
	    $("#isLine").bootstrapSwitch("onSwitchChange",function(event,state){
	    	if(!state){
	        	option.series.forEach(function(value,key,arr){
			    	value.type='';
			    })
	        }else{
	        	$("#isScatter").bootstrapSwitch('state', false);
		        $("#isBar").bootstrapSwitch('state', false);
	        	option.series.forEach(function(value,key,arr){
			    	value.type='line';
			    })
			    vm.showType='line';
	        	vm.chartStatistical.points=0;
	        	vm.chartStatistical.bars=0;
	        	vm.chartStatistical.lines=1;
	        }
	    })
		
	    //是否为柱图
	    $("#isBar").bootstrapSwitch("onSwitchChange",function(event,state){
	    	if(!state){
	        	option.series.forEach(function(value,key,arr){
			    	value.type='';
			    })
	        }else{
	        	$("#isScatter").bootstrapSwitch('state', false);
		        $("#isLine").bootstrapSwitch('state', false);
	        	option.series.forEach(function(value,key,arr){
			    	value.type='bar';
			    })
			    vm.showType='bar';
	        	vm.chartStatistical.points=0;
	        	vm.chartStatistical.bars=1;
	        	vm.chartStatistical.lines=0;
	        }
	    })
	    
	    //是否为点图
	    $("#isScatter").bootstrapSwitch("onSwitchChange",function(event,state){
	    	if(!state){
	        	option.series.forEach(function(value,key,arr){
			    	value.type='';
			    })
	        }else{
		        $("#isLine").bootstrapSwitch('state', false);
		        $("#isBar").bootstrapSwitch('state', false);
	        	option.series.forEach(function(value,key,arr){
			    	value.type='scatter';
			    })
			    vm.showType='scatter';
	        	vm.chartStatistical.points=1;
	        	vm.chartStatistical.bars=0;
	        	vm.chartStatistical.lines=0;
	        }
	    })
	    
	    //左Y轴是否开启
	    $("#leftYOpen").bootstrapSwitch("onSwitchChange",function(event,state){
	    	if(!state){
	        	option.yAxis[0].show=false;
	        	vm.chartStatistical.yaxes=0;
	        	/*$('.leftY').each(function(){
	        		$(this).bootstrapSwitch('state', false);
	        	})*/
	        }else{
	        	option.yAxis[0].show=true;
	        	vm.chartStatistical.yaxes=1;
	        }
	    })
	    
	    //左Y轴是否开启
	    $("#rightYOpen").bootstrapSwitch("onSwitchChange",function(event,state){
	    	if(!state){
	        	option.yAxis[1].show=false;
	        	vm.chartStatistical.yaxis=0;
	        	/*$('.rightY').each(function(){
	        		$(this).bootstrapSwitch('state', false);
	        	})*/
	        }else{
	        	option.yAxis[1].show=true;
	        	vm.chartStatistical.yaxis=1;
	        }
	    })
	    
	    //X轴是否开启
	    $("#xOpen").bootstrapSwitch("onSwitchChange",function(event,state){
	    	if(!state){
	        	option.xAxis[0].show=false;
	        	vm.chartStatistical.xaxis=0;
	        	/*$('.xOption').each(function(){
	        		$(this).bootstrapSwitch('state', false);
	        	})*/
	        }else{
	        	option.xAxis[0].show=true;
	        	vm.chartStatistical.xaxis=1;
	        }
	    })
	    
	    //legend是否展示
		$("#legendShow").bootstrapSwitch("onSwitchChange",function(event,state){
			if(!state){
	        	option.legend.show=false;
	        	vm.chartStatistical.legend=0;
	        }else{
	        	option.legend.show=true;
	        	vm.chartStatistical.legend=1;
	        }
	    })
	    
	    //分页是否使用
	    /*$("#isPaging").bootstrapSwitch("onSwitchChange",function(event,state){
	        if(!state){
	        }else{
	        }
	    })*/
	    
	    //是否启用仪表盘
	    $("#isGauge").bootstrapSwitch("onSwitchChange",function(event,state){
	        if(!state){
	        	singleOption.series[0].pointer.show=false;
	        	singleOption.series[0].axisLine.lineStyle.opacity=0;
	        	singleOption.series[0].detail.offsetCenter=[0,"10%"];
	        	singleOption.series[0].detail.rich.a.fontSize=vm.chartSingle.prefixFontSize;
	        	singleOption.series[0].detail.rich.b.fontSize=vm.chartSingle.suffixFontSize;
	        	$("#singlePrefixFontSize").attr("disabled",false);
	        	$("#singleSuffixFontSize").attr("disabled",false);
	        	singleOption.series[0].axisTick.show=false;
	        	singleOption.series[0].splitLine.show=false;
	        	$("#isAxisLabelShow").bootstrapSwitch('state', false);
	        	$("#isThreshold").bootstrapSwitch('state', false);
	        	singleOption.series[0].axisLabel.show=false;
	        	singleOption.series[1].axisTick.show=false;
	        	singleOption.series[1].splitLine.show=false;
	        	singleOption.series[1].axisLine.lineStyle.opacity=0;
	        	vm.chartSingle.isGauge=0;
	        }else{
	        	singleOption.series[0].pointer.show=true;
	        	singleOption.series[0].axisLine.lineStyle.opacity=1;
	        	singleOption.series[0].detail.offsetCenter=[0,"50%"];
	        	singleOption.series[0].detail.textStyle.fontSize=20;
	        	singleOption.series[0].detail.rich.a.fontSize=20;
	        	singleOption.series[0].detail.rich.b.fontSize=20;
	        	$("#singlePrefixFontSize").attr("disabled",true);
	        	$("#singleSuffixFontSize").attr("disabled",true);
	        	singleOption.series[0].axisTick.show=true;
	        	singleOption.series[0].splitLine.show=true;
	        	vm.chartSingle.isGauge=1;
	        	vm.chartSingle.valueFontSize=20;
	        }
	    })
	    
	    //是否显示阈值
	    $("#isThreshold").bootstrapSwitch("onSwitchChange",function(event,state){
	    	//如果未开启仪表盘功能 则不允许开启
	    	if(vm.chartSingle.isGauge==0){
	    		$(this).bootstrapSwitch('state', false);
	    		return;
	    	}
	        if(state){
	        	singleOption.series[1].axisTick.show=true;
	        	singleOption.series[1].splitLine.show=true;
	        	singleOption.series[1].axisLine.lineStyle.opacity=1;
	        	vm.chartSingle.isThreshold=1;
	        }else{
	        	singleOption.series[1].axisTick.show=false;
	        	singleOption.series[1].splitLine.show=false;
	        	singleOption.series[1].axisLine.lineStyle.opacity=0;
	        	vm.chartSingle.isThreshold=0;
	        }
	    })
	    
	    //是否显示详细刻度
	    $("#isAxisLabelShow").bootstrapSwitch("onSwitchChange",function(event,state){
	    	//如果未开启仪表盘功能 则不允许开启
	    	if(vm.chartSingle.isGauge==0){
	    		$(this).bootstrapSwitch('state', false);
	    		return;
	    	}
	        if(state){
	        	singleOption.series[0].axisLabel.show=true;
	        	vm.chartSingle.isScale=1;
	        }else{
	        	singleOption.series[0].axisLabel.show=false;
	        	vm.chartSingle.isScale=0;
	        }
	    })
	    
	    //初始化单值表颜色选择
		$('.singleColor').each(function() {
            $(this).minicolors({
                control: $(this).attr('data-control') || 'hue',
                defaultValue: $(this).attr('data-defaultValue') || '',
                inline: $(this).attr('data-inline') === 'true',
                letterCase: $(this).attr('data-letterCase') || 'lowercase',
                opacity: $(this).attr('data-opacity'),
                position: $(this).attr('data-position') || 'bottom left',
                change: function(hex, opacity) {
                    if (!hex)
                        return;
                    if (opacity)
                        hex += ', ' + opacity;
                    try {
                        console.log(hex);
                    } catch (e) {
                    }
                },
                theme: 'bootstrap'
            });

        });
	});
	
	//注册select组件
	Vue.component('multiselect', window.VueMultiselect.default);
	//引用vuelidate组件
	Vue.use(window.vuelidate.default);
	//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-v-values
	var required = window.validators.required;
	
	
	
	var vm = new Vue({
		el:'#dashboardView',
		data:{
			dashboardSetting:false,
			databaseType:[],
			chartType:[],
			dashboard:{
				databaseType:'',
				chartType:'',
				title:'',
				describe:''
			},
			chartStatistical:{
				lineWidth:1,
				fill:1,
				legendXPosition:{},
				legendYPosition:{},
				lines:1,
				points:0,
				bars:0,
				yaxes:0,
				legend:1,
				yaxes:1,
				xaxis:1,
				yaxis:0,
				yaxisName:'',
				yaxesName:'',
				xaxisName:''
			},
			showType:"line",
			chartTable:{
				timeFormat:''
			},
			chartSingle:{
				colorValue:'#333333',
				valueFontSize:50,
				decimal:0,
				prefix:'',
				prefixFontSize:30,
				colorPrefix:'#333333',
				suffix:'',
				suffixFontSize:60,
				colorSuffix:'#333333',
				isGauge:0,
				minValue:0,
				maxValue:100,
				isThreshold:0,
				isScale:0,
				fontSizeDis:1,
				thresholdValues:[
					{color:'#ff0000',value:0.6},
					{color:'#ffff00',value:0.8},
					{color:'#00ff00',value:1}
				]
			},
			lineWidths:[1,2,3,4,5,6,7,8,9],
			fills:[1,2,3,4,5,6,7,8,9],
			legendXPositions:[],
			legendYPositions:[],
			timeFormats:[],
			chart:false,
			set:true,
			query:true
		},
		methods:{
			getDatabaseType: function(event) {
				$.getJSON(baseURL + "sys/dict/all?type=databaseType&_"+$.now(), function(r){
					var databaseType = [];
					for (var i = 0; i < r.data.length; i++) {
						databaseType.push(r.data[i]);
					}
					vm.databaseType = databaseType;
					if(vm.dashboard.databaseType==''||vm.dashboard.databaseType==undefined){
						vm.dashboard.databaseType=databaseType[0];
					}
				});
			},
			getChartType: function(event) {
				$.getJSON(baseURL + "sys/dict/all?type=chartType&_"+$.now(), function(r){
					var chartType = [];
					for (var i = 0; i < r.data.length; i++) {
						chartType.push(r.data[i]);
					}
					vm.chartType = chartType;
					if(vm.dashboard.chartType==''||vm.dashboard.chartType==undefined){
						vm.dashboard.chartType=chartType[0];
					}
				});
			},
			getTimeFormat: function(event) {
				$.getJSON(baseURL + "sys/dict/all?type=timeFormat&_"+$.now(), function(r){
					var timeFormat = [];
					for (var i = 0; i < r.data.length; i++) {
						timeFormat.push(r.data[i]);
					}
					vm.timeFormats = timeFormat;
				});
			},
			getLegendXPosition: function(event) {
				$.getJSON(baseURL + "sys/dict/all?type=legendXPosition&_"+$.now(), function(r){
					var legendXPosition = [];
					for (var i = 0; i < r.data.length; i++) {
						legendXPosition.push(r.data[i]);
						if ("center" == r.data[i].code) { 
	                        vm.chartStatistical.legendXPosition = r.data[i];
	                    }
					}
					vm.legendXPositions = legendXPosition;
					
				});
			},
			getLegendYPosition: function(event) {
				$.getJSON(baseURL + "sys/dict/all?type=legendYPosition&_"+$.now(), function(r){
					var legendYPosition = [];
					for (var i = 0; i < r.data.length; i++) {
						legendYPosition.push(r.data[i]);
						if ("top" == r.data[i].code) { 
	                        vm.chartStatistical.legendYPosition = r.data[i];
	                    }
					}
					vm.legendYPositions = legendYPosition;
				});
			},
			/*点线柱图相关*/
			//设置左Y轴名称
			setLeftYName:function(){
				option.yAxis[0].name=vm.chartStatistical.yaxesName;
				myChart.setOption(option);
			},
			//设置右Y轴名称
			setRightYName:function(){
				option.yAxis[1].name=vm.chartStatistical.yaxisName;
				myChart.setOption(option);
			},
			//设置X轴名称
			setXName:function(){
				option.xAxis[0].name=vm.chartStatistical.xaxisName;
				myChart.setOption(option);
			},
			/*单值图相关*/
			//设置仪表盘最大值
			setSingleMaxValue:function (){
				singleOption.series[0].max=vm.chartSingle.maxValue;
				singleChart.setOption(singleOption);
			},
			//设置仪表盘最小值
			setSingleMinValue:function (){
				singleOption.series[0].min=vm.chartSingle.minValue;
				singleChart.setOption(singleOption);
			},
			//设置数值字体大小
			setSingleFontSize:function(){
				singleOption.series[0].detail.textStyle.fontSize=vm.chartSingle.valueFontSize;
				if(vm.chartSingle.isGauge==1){
					singleOption.series[0].detail.rich.a.fontSize=vm.chartSingle.valueFontSize;
					singleOption.series[0].detail.rich.b.fontSize=vm.chartSingle.valueFontSize;
				}
				singleChart.setOption(singleOption);
			},
			//设置前缀文字大小
			setSinglePrefixFontSize:function(){
				singleOption.series[0].detail.rich.a.fontSize=vm.chartSingle.prefixFontSize;
				singleChart.setOption(singleOption);
			},
			//设置后缀文字大小
			setSingleSuffixFontSize:function(){
				singleOption.series[0].detail.rich.b.fontSize=vm.chartSingle.suffixFontSize;
				singleChart.setOption(singleOption);
			},
			//设置值颜色
			setSingleColor:function(ele){
				vm.chartSingle.colorValue=$(this.$refs.singleColor).val();
				singleOption.series[0].detail.textStyle.color=vm.chartSingle.colorValue;
				singleChart.setOption(singleOption);
			},
			//设置前缀值颜色
			setSingleColorPrefix:function(ele){
				vm.chartSingle.colorPrefix=$(this.$refs.colorPrefix).val();
				singleOption.series[0].detail.rich.a.color=vm.chartSingle.colorPrefix;
				singleChart.setOption(singleOption);
			},
			//设置后缀值颜色
			setSingleColorSuffix:function(ele){
				vm.chartSingle.colorSuffix=$(this.$refs.colorSuffix).val();
				singleOption.series[0].detail.rich.b.color=vm.chartSingle.colorSuffix;
				singleChart.setOption(singleOption);
			},
			//设置单值图小数点保留位数
			setSingleDecimal:function(){
				singleOption.series[0].data=[{"value":Number(10).toFixed(vm.chartSingle.decimal)}];
				singleChart.setOption(singleOption);
			},
			//删除阈值
			removeThreshold:function(index){
				vm.chartSingle.thresholdValues.splice(index,1)
			},
			//新增阈值
			addThreshold:function(){
				vm.chartSingle.thresholdValues.push({color:'#00ff00',value:1})
			},
			changeSetShow:function(val){
				vm.query=true;
				vm.set=true;
				vm.chart=true;
				if(val==1){
					vm.query=false;
				}else if(val==2){
					vm.chart=false;
				}else{
					vm.set=false;
				}
			}
		},
		created: function(){
			// 初始化进入执行方法
			this.getDatabaseType();
			this.getLegendYPosition();
			this.getLegendXPosition();
			this.getTimeFormat();
			this.getChartType();
		},
		watch:{
			'chartStatistical':{
				handler(newVal,oldVal){
					option.legend.x=vm.chartStatistical.legendXPosition.code;
					option.legend.y=vm.chartStatistical.legendYPosition.code;
					option.series.forEach(function(value,key,arr){
				    	if(value.type=='line'){
				    		value.areaStyle.opacity=(vm.chartStatistical.fill-1)/10;
				    	}
				    })
				    option.series.forEach(function(value,key,arr){
				    	if(value.type=='line'){
				    		value.lineStyle.width=vm.chartStatistical.lineWidth;
				    	}
				    })
					myChart.setOption(option);
				},
				deep:true
			},
			'chartSingle':{
				handler(newVal,oldVal){
					singleChart.setOption(singleOption);
				},
				deep:true
			}
		}
	})
</script>