<script>
	var echartsDefaultData=true; //此标记代表 echarts图使用的是假数据
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
	 	var previewWidth = (top.$(".content").width()-50) + 'px';
	 	var previewHeight = (top.$(".content").height()*0.35) + 'px';
	 	document.getElementById("chart").style.width = previewWidth;
	 	document.getElementById("chart").style.height = previewHeight;
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
        document.getElementById("singleChart").style.width = previewWidth;
	 	document.getElementById("singleChart").style.height = previewHeight;
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
			    if(vm.chartStatistical.points==0&&vm.chartStatistical.bars==0&&vm.chartStatistical.lines==1){
			    	$("#isLine").bootstrapSwitch('state', true);
			    }
	        }else{
			    vm.showType='line';
	        	vm.chartStatistical.points=0;
	        	vm.chartStatistical.bars=0;
	        	vm.chartStatistical.lines=1;
	        	$("#isScatter").bootstrapSwitch('state', false);
		        $("#isBar").bootstrapSwitch('state', false);
		        option.series.forEach(function(value,key,arr){
			    	value.type='line';
			    })
			    myChart.setOption(option);
	        }
	    })
		
	    //是否为柱图
	    $("#isBar").bootstrapSwitch("onSwitchChange",function(event,state){
	    	if(!state){
	        	option.series.forEach(function(value,key,arr){
			    	value.type='';
			    })
			    if(vm.chartStatistical.points==0&&vm.chartStatistical.lines==0&&vm.chartStatistical.bars==1){
			    	$("#isBar").bootstrapSwitch('state', true);
			    }
	        }else{
			    vm.showType='bar';
	        	vm.chartStatistical.points=0;
	        	vm.chartStatistical.bars=1;
	        	vm.chartStatistical.lines=0;
	        	$("#isScatter").bootstrapSwitch('state', false);
		        $("#isLine").bootstrapSwitch('state', false);
		        option.series.forEach(function(value,key,arr){
			    	value.type='bar';
			    })
		        myChart.setOption(option);
	        }
	    })
	    
	    //是否为点图
	    $("#isScatter").bootstrapSwitch("onSwitchChange",function(event,state){
	    	if(!state){
	        	option.series.forEach(function(value,key,arr){
			    	value.type='';
			    })
			    if(vm.chartStatistical.bars==0&&vm.chartStatistical.lines==0&&vm.chartStatistical.points==1){
			    	$("#isScatter").bootstrapSwitch('state', true);
			    }
	        }else{
			    vm.showType='scatter';
	        	vm.chartStatistical.points=1;
	        	vm.chartStatistical.bars=0;
	        	vm.chartStatistical.lines=0;
	        	$("#isLine").bootstrapSwitch('state', false);
		        $("#isBar").bootstrapSwitch('state', false);
		        option.series.forEach(function(value,key,arr){
			    	value.type='scatter';
			    })
		        myChart.setOption(option);
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
	    $("#isPaging").bootstrapSwitch("onSwitchChange",function(event,state){
	    	$("#isPaging").bootstrapSwitch('state', false);
	       /* if(!state){
	        	$("#tablePageSize").attr("disabled",true);
	        	vm.chartTable.scroll=0;
	        }else{
	        	$("#tablePageSize").attr("disabled",false);
	        	vm.chartTable.scroll=1;
	        }*/
	    })
	    
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
			dashboards:[],
			dashboardSetting:false,
			databaseType:[],
			chartType:[],
			tableDatas:[],
			dashboard:{
				id:null,
				databaseType:'',
				chartType:'',
				title:'',
				describe:''
			},
			userSqls:[{
				sql:'',		// 用户自定义sql查询语句
				type:'',		// 图表类型
				vTime:'',		// 时间变量字段
				describe:'series 1'   //查询数据描述
			}],
			chartStatistical:{
				lineWidth:1,
				fill:1,
				legendXPosition:{},
				legendYPosition:{},
				lines:1,
				points:0,
				bars:0,
				yaxis:0,
				legend:1,
				yaxes:1,
				xaxis:1,
				yaxisName:'',
				yaxesName:'',
				xaxisName:''
			},
			showType:"line",
			chartTable:{
				timeFormat:'',
				scroll:0,
				pageSize:10,
				fontSize:5,
				vName:'Value',
				decimal:0,
				tName:'Time'
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
			query:true,
			sqlFlag:false
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
					vm.chartTable.timeFormat=timeFormat[0];
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
				singleOption.series[0].data=[{"value":Number(singleValue).toFixed(vm.chartSingle.decimal)}];
				singleChart.setOption(singleOption);
			},
			//设置阈值颜色
			setThresholdColor:function(index){
				vm.chartSingle.thresholdValues[index].color=event.target.value;
				this.resetThresholdColor();
			},
			resetThresholdColor:function(){
				var newA=[];
				for(var i=0;i<vm.chartSingle.thresholdValues.length;i++){
					var newB=[];
					newB.push(vm.chartSingle.thresholdValues[i].value);
					newB.push(vm.chartSingle.thresholdValues[i].color);
					newA.push(newB);
				}
				singleOption.series[1].axisLine.lineStyle.color=newA;
				singleChart.setOption(singleOption);
			},
			//删除阈值
			removeThreshold:function(index){
				var tvs=[];
				tvs=vm.chartSingle.thresholdValues;
				tvs.splice(index,1)
				vm.chartSingle.thresholdValues=[];
				this.$nextTick(()=>{
					vm.chartSingle.thresholdValues = tvs;
					this.resetThresholdColor();
			    })
			},
			//新增阈值
			addThreshold:function(){
				vm.chartSingle.thresholdValues.push({color:'#00ff00',value:1});
				this.resetThresholdColor();
			},
			//修改用户sql 设置legend
			changeLegend:function(index){
				vm.userSqls[index].describe=event.target.value;
			},
			//删除用户sql
			removeQuery:function(index){
				vm.userSqls.splice(index,1)
			},
			//用户填写完sql获取后台数据信息
			queryData:function(index){
				var sql=event.target.value;
				//判断输入框是否有sql 没有的话不进行后台数据查询  且将echarts中的数据清空
				if(sql!=''&&sql!=undefined){
					$.ajax({
						type:"GET",
					    url: baseURL + "dashboard/queryData",
					    data:{'sql':sql,'timeFormat':vm.chartTable.timeFormat.code,'chartType':vm.dashboard.chartType.code},
					    success: function(r){
					    	if(r.msg=='success'){
						    	if(vm.dashboard.chartType.code==1){
				            		//判断是否替换假数据
				            		if(echartsDefaultData){
				            			//数据为动态的时候将echarts数据状态修改
				            			echartsDefaultData=false;
				            			var data=[];
				            			for(var i=0;i<r.result.length;i++){
				            				var d={value:[r.result[i].time,r.result[i].value]}
				            				data.push(d);
				            			}
				            		    option.series[index].data=data;
				 	 	                myChart.setOption(option);
				            		}else{
				            			var seriesData=option.series.find( element => element.id == index);
				            			//可能为重复的数据操作 需要进行判断
				            			if(seriesData==''||seriesData==undefined){
				            				var data=[];
					            			for(var i=0;i<r.result.length;i++){
					            				var d={value:[r.result[i].time,r.result[i].value]}
					            				data.push(d);
					            			}
				            				var data={
				    	            				id:index,
				    	    			            name:'',
				    	    			            type:vm.showType,
				    	    			            yAxisIndex: 0,
				    	    			            showSymbol: false,
				    	    	                    lineStyle:{
				    	    	                        width:1
				    	    	                    },
				    	    	                    areaStyle:{
				    	    	                        opacity:0
				    	    	                    },
				    	    			            data:data
				    	    			        }
				    	            			option.series.push(data);
				            			}else{
				            				var data=[];
					            			for(var i=0;i<r.result.length;i++){
					            				var d={value:[r.result[i].time,r.result[i].value]}
					            				data.push(d);
					            			}
				            				seriesData.data=data;
				            			}
				            			myChart.setOption(option);
				            		}
			            		}else if(vm.dashboard.chartType.code==2){
			            			var decimal = vm.chartSingle.decimal;
			            			singleValue=r.result[0].value;
			            			singleOption.series[0].data=[{"value":(singleValue).toFixed(decimal)}];
			            			singleChart.setOption(singleOption);
			            		}else if(vm.dashboard.chartType.code==3){
			            			vm.tableDatas=r.result;
			            		}
						    	vm.sqlFlag=true;
						    }else{
						    	vm.sqlFlag=false;
						    	layer.msg(r.msg)
						    }
					    	
						}
					});
				}
			},
			//新增用户sql填写
			addNewQuery:function(){
				vm.userSqls.push({
					dbId:'',		
					sql:'',		
					chartId:'',		
					type:'',		
					vTime:'',		
					describe:'series'+ (vm.userSqls.length+1)  
				})
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
			},
			dateF:function(){
				var sql=vm.userSqls[0].sql;
				//判断输入框是否有sql 没有的话不进行后台数据查询  且将echarts中的数据清空
				if(sql!=''&&sql!=undefined){
					$.ajax({
						type:"GET",
					    url: baseURL + "dashboard/queryData",
					    data:{'sql':sql,'timeFormat':vm.chartTable.timeFormat.code,'chartType':vm.dashboard.chartType.code},
					    success: function(r){
					    	if(vm.dashboard.chartType.code==3){
		            			vm.tableDatas=r.result;
		            		}
						}
					});
				}
			},
			savePanelInfo:function(){
				if(!vm.sqlFlag){
					layer.msg(" sql error ")
					return
				}
				var formdata={};
				formdata.name=vm.dashboard.title;
				formdata.chartType=vm.dashboard.chartType.code;
				var data={};
				if(vm.dashboard.chartType.code==1){
					data=JSON.parse(JSON.stringify(vm.chartStatistical));
					data.legendXPosition=vm.chartStatistical.legendXPosition.code;
					data.legendYPosition=vm.chartStatistical.legendYPosition.code;
				}else if(vm.dashboard.chartType.code==2){
					data=JSON.parse(JSON.stringify(vm.chartSingle));
				}else if(vm.dashboard.chartType.code==3){
					data=JSON.parse(JSON.stringify(vm.chartTable));
					data.timeFormat=vm.chartTable.timeFormat.code;
				}
				data.userSqls=vm.userSqls;
				data.describe=vm.dashboard.describe;
				delete data.series;
				formdata.data=JSON.stringify(data);
				if(vm.dashboard.id!=null&&vm.dashboard.id!=undefined&&vm.dashboard.id!=''){
					formdata.id=vm.dashboard.id;
				}
				$.ajax({
					type:vm.dashboard.id == null ? "POST" : "PUT",
					contentType:'application/json',
				    url: vm.dashboard.id == null?baseURL + "dashboard/save":baseURL + "dashboard/update",
				    data:JSON.stringify(formdata),
				    success: function(r){
				    	if(r.msg=='success'){
				    		location.href=baseURL+'modules/dashboard/index.html'
				    	}else{
				    		layer.msg(r.msg)
				    	}
				    }
				})
			},
			modifyDashboard:function(id){
				$.ajax({
					type:"GET",
				    url: baseURL + "dashboard/querySingleDashboard",
				    data:{"id":id},
				    success: function(r){
				    	vm.sqlFlag=true;
				    	vm.dashboard.id=r.data.id;
				    	vm.dashboard.title=r.data.name;
				    	for(let v of vm.chartType) {  
				            if(v.code==r.data.chartType){
				            	vm.dashboard.chartType=v;
				            }
				        };  
				    	var obj=JSON.parse(r.data.data);
				    	vm.userSqls=obj.userSqls;
				    	delete obj.userSqls;
				    	vm.dashboard.describe=obj.describe;
				    	if(vm.dashboard.chartType.code==1){
				    		vm.chartStatistical=obj;
				    		for(let v of vm.legendXPositions) {  
					            if(v.code==obj.legendXPosition){
					            	vm.chartStatistical.legendXPosition=v;
					            }
					        };  
					        for(let v of vm.legendYPositions) {  
					            if(v.code==obj.legendYPosition){
					            	vm.chartStatistical.legendYPosition=v;
					            }
					        };  
						}else if(vm.dashboard.chartType.code==2){
							vm.chartSingle=obj;
							data=JSON.parse(JSON.stringify(vm.chartSingle));
						}else if(vm.dashboard.chartType.code==3){
							vm.chartTable=obj;
							for(let v of vm.timeFormats) {  
					            if(v.code==obj.timeFormat){
					            	vm.chartTable.timeFormat=v;
					            }
					        }; 
						}
				    	var a=JSON.parse(obj.series);
				    	var nSeries=[];
				    	if(vm.dashboard.chartType.code==1){
                			for(var j=0;j<a.length;j++){
                				var userQueryResult=a[j];
                				var data=[];
                				for(var m=0;m<userQueryResult.length;m++){
		            				var d={value:[userQueryResult[m].time,userQueryResult[m].value]}
		            				data.push(d);
		            			}
                				var data={
            			            name:vm.userSqls[j].describe,
            			            type:vm.chartStatistical.lines==1?'line':vm.chartStatistical.bars==1?'bar':vm.chartStatistical.points==1?'scatter':'line',
            			            yAxisIndex: 0,
            			            showSymbol: false,
            	                    lineStyle:{
            	                        width:vm.chartStatistical.linewidth
            	                    },
            	                    areaStyle:{
            	                        opacity:vm.chartStatistical.fill/10
            	                    },
            			            data:data
            			        }
                				nSeries.push(data);
                				option.series=nSeries;
         	 	                myChart.setOption(option);
        			        }
				    	}else if(vm.dashboard.chartType.code==3){
				    		vm.tableDatas=JSON.parse(obj.series);
				    	}else if(vm.dashboard.chartType.code==2){
				    		var decimal = vm.chartSingle.decimal;
	            			singleValue=a[0].value;
	            			singleOption.series[0].data=[{"value":(singleValue).toFixed(decimal)}];
				    	}	
				    	vm.dashboardSetting=true;
				    }
				})
			},
			removeDashboard:function(id){
				if(id!=null &&id!=undefined){
					$.ajax({
						type:"DELETE",
					    url: baseURL + "dashboard/delete",
					    data:{"id":id},
					    success: function(r){
					    	location.href=baseURL+'modules/dashboard/index.html'
					    }
					})
				}
			},
			returnMenu:function(){
				location.href=baseURL+'modules/dashboard/index.html'
			},
			queryDashboards:function(){
				$.ajax({
					type:"GET",
					contentType:'application/json',
				    url: baseURL + "dashboard/list",
				    success: function(r){
				    	vm.handleDashboards(r);
				    }
				})
			},
			handleDashboards:function(r){
				vm.dashboards=r.data;
		    	vm.$nextTick(function () {
		    		var statisticals=new Array();
			    	for(var i=0;i<r.data.length;i++){
//			    		vm.dashboards[i].idName='chart'+vm.dashboards[i].id;
						var datas=new Array();
						var obj=JSON.parse(r.data[i].data);
						// 折线图等 生成echarts
		        		if(r.data[i].chartType==1){
		        			var a=JSON.parse(obj.series);
							var nSeries=[];
							for(var j=0;j<a.length;j++){
								var userQueryResult=a[j];
								var data=[];
								for(var m=0;m<userQueryResult.length;m++){
									var d={value:[userQueryResult[m].time,userQueryResult[m].value]}
									data.push(d);
								}
								var data={
									name:obj.userSqls[j].describe,
									type:obj.lines==1?'line':obj.bars==1?'bar':obj.points==1?'scatter':'line',
									yAxisIndex: 0,
									showSymbol: false,
									lineStyle:{
										width:obj.linewidth
									},
									areaStyle:{
										opacity:obj.fill/10
									},
									data:data
								}
								nSeries.push(data);
							}
		        			
		        			// 基于准备好的dom，初始化echarts实例
							var chartId=r.data[i].name;
		        	        var mychartHandle = echarts.init(document.getElementById(chartId));
		
		        			var colors = ['#5793f3', '#d14a61', '#DC143C'];
		
		        			tOption = {
		        			    color: colors,
		        			    tooltip: {
		        			        trigger: 'item',
		        			        axisPointer: {
		        			            type: 'cross'
		        			        }
		        			    },
		        			    legend:{
		        			    	show:obj.legend==1?true:false,
		        			    	x:obj.legendXPosition,
		        			    	y:obj.legendYPosition
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
		        			        	name:obj.xaxisName,
		        			        	type:'time'
		        			        }
		        			    ],
		        			    yAxis: [
		        			        {
		        			        	show:obj.yaxes==1?true:false,
		        			            type: 'value',
		        			            name: obj.yaxesName,
		        			            position: 'left',
		        			            axisLine: {
		        			                lineStyle: {
		        			                 //   color: colors[1]
		        			                }
		        			            },
		        			            axisLabel: {
		        			                formatter: ''
		        			            }
		        			        },
		        			        {
		        			        	show:obj.yaxis==1?true:false,
		        			            type: 'value',
		        			            name: obj.yaxisName,
		        			            position: 'right',
		        			            axisLine: {
		        			                lineStyle: {
		        			                //    color: colors[1]
		        			                }
		        			            }
		        			        }
		        			    ],
		        			    series: nSeries
		        			};
		        			// 使用刚指定的配置项和数据显示图表。
		        	        mychartHandle.setOption(tOption);
		        	        statisticals.push(mychartHandle);
		        		}else if(r.data[i].chartType==3){
		        			//这里是初始化表格
		        		}else if(r.data[i].chartType==2){
		        			var cA=[];
		    				for(var m=0;i<obj.thresholdValues.length;m++){
		    					if(obj.thresholdValues[m]==undefined || obj.thresholdValues[m]==''){
		    						break;
		    					}
		    					var cB=[];
		    					var tValue=obj.thresholdValues[m].value;
		    					var tColor=obj.thresholdValues[m].color;
		    					cB.push(tValue);
		    					cB.push(tColor);
		    					cA.push(cB);
		    				}
		    				
		    				var currentSingle=JSON.parse(JSON.parse(JSON.stringify(r.data[i].data)));
		        			var chartId=r.data[i].name;
		        			var singleChartHandle = echarts.init(document.getElementById(chartId));
		        			var angle = [220, -40]
		        			tSingleOption = {
		        			    backgroundColor: '#fff',
		        			    tooltip: {
		        			        show: false,
		        			        formatter: "{a} <br/>{b} : {c}%"
		        			    },
		        			    series: [{
		        			        name: '最外层环仪表盘',
		        			        type: "gauge",
		        			        min: obj.minValue,
		        			        max: obj.maxValue,
		        			        startAngle: angle[0],
		        			        endAngle: angle[1],
		        			        splitNumber: 20,
		        			        axisLine: {
		        			            lineStyle: {
		        			                color: [
		        			                    [1, "#c1c3c5"]
		        			                ],
		        			                width: 10,
		        			                opacity:obj.isGauge==1?1:0
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
		        			            show:obj.isScale==1?true:false,
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
		        			            show: obj.isScale==1?true:false,
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
		        			            	console.log("~~~~~~~~~~~~~")
		        			            	console.log(currentSingle)
		        			            	return '{a| '+currentSingle.prefix+'} '+v+' {b| '+currentSingle.suffix+'}';
		        			            },
		        			            offsetCenter:obj.isGauge==1?[0, "50%"]:[0, "10%"],
		        			            textStyle: {
		        			                fontSize: obj.valueFontSize,
		        			                color: obj.colorValue
		        			            },
		        			            rich:{
		        			                a:{
		        			                    fontSize:obj.isGauge==1?obj.valueFontSize:obj.prefixFontSize,
		        			                    color:obj.colorPrefix,
		        			                    textShadowOffsetX:10
		        			                },
		        			                b:{
		        			                    fontSize:obj.isGauge==1?obj.valueFontSize:obj.suffixFontSize,
		        			                    color:obj.colorSuffix
		        			                }
		        			            }
		        			        },
		        			        title: {
		        			            show: false
		        			        },
		        			        pointer: {
		        			            show:obj.isGauge==1?true:false,
		        			            length: '85%'
		        			        },
		        			        data: [{value: (JSON.parse(obj.series)[0].value).toFixed(obj.decimal)}]
		        			    },{
		        			        name: "内环仪表盘四段颜色",
		        			        type: "gauge",
		        			        min: 0,
		        			        max: 100,
		        			        radius: '69%',
		        			        startAngle: angle[0],
		        			        endAngle: angle[1],
		        			        splitNumber: 10,
		        			        axisLine: {
		        			            lineStyle: {
		        			                color: obj.isThreshold==1?cA:[],
		        			                width: 30,
		        			                opacity:obj.isThreshold==1?1:0
		        			            }
		        			        },
		        			        axisTick: {
		        			            show: obj.isThreshold==1?true:false
		        			        },
		        			        axisLabel: {
		        			            show: false
		        			        },
		        			        splitLine: {
		        			            show: obj.isThreshold==1?true:false,
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
		        			singleChartHandle.setOption(tSingleOption);
		        			statisticals.push(singleChartHandle);
		        		}
					}
		        	
		    		gridster = $(".gridster > ul").gridster({
		    	        widget_margins: [20, 20], // 模块的间距 [上下,左右]
		    	        widget_base_dimensions: [150, 150], //模块的宽高 [宽,高]
		    	        min_cols:6,     //设置最小列数
		    	        max_cols:10,     //设置最大列数
		    	        draggable: {
		    	        	handle: 'header' //模块内定义拖动的元素<header>，这里也支持jquery选择器，如"span.drag_handle"
		    	        }, 
		    	        serialize_params: function($w, wgd) {
		    	            return { id: $w[0].id, x: wgd.row, y: wgd.col,  width: wgd.size_x*150, height: wgd.size_y*150 }
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
		    					statisticals.forEach(function(value,key,arr){
		    						$(value).resize()
		    				    })
		    					
		    				}
		    			},
		    	    }).data('gridster');
		    		statisticals.forEach(function(value,key,arr){
		    			$(value).resize()
		    		})
	    	    })
			},
			modifyPosition:function(){
				var allChartInfos = gridster.serialize();
				$.ajax({
	                type: "PUT",//方法类型
	                dataType: "json",//预期服务器返回的数据类型
	                url: baseURL + "dashboard/modifyShape",//url
	                contentType: 'application/json; charset=UTF-8',
	                data: JSON.stringify(allChartInfos),//JSON.stringify(allChartInfos)
	                success: function (result) {
	                	layer.msg("<@spring.message 'common.success'/>")
	                },
		            error : function(result) {
		            	layer.msg(result.msg)
		            }
	            })
				
			},
			dbOpsShow: function(event) {
				$(event.currentTarget).find(".db-ops").css("display", "inline-block");
			},
			dbOpsHide: function(event) {
				$(event.currentTarget).find(".db-ops").css("display", "none");
			}
		},
		created: function(){
			// 初始化进入执行方法
			this.getDatabaseType();
			this.getLegendYPosition();
			this.getLegendXPosition();
			this.getTimeFormat();
			this.getChartType();
			this.queryDashboards();
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
					this.$nextTick(function(){
						singleChart.setOption(singleOption);
						$('.singleColor').each(function() {
				            $(this).minicolors({
				                control: $(this).attr('data-control') || 'hue',
				                defaultValue: $(this).attr('data-defaultValue') || '',
				                inline: $(this).attr('data-inline') === 'true',
				                letterCase: $(this).attr('data-letterCase') || 'lowercase',
				                opacity: $(this).attr('data-opacity'),
				                position: $(this).attr('data-position') || 'bottom left',
				                change: function(hex, opacity) {
				                	console.log(hex)
				                	console.log(opacity)
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
				},
				deep:true
			},
			'dashboardSetting':{
				handler(newVal,oldVal){
					if(newVal){
						if(vm.chartStatistical.lines==1){
							$("#isScatter").bootstrapSwitch('state', false);
							$("#isLine").bootstrapSwitch('state', true);
					        $("#isBar").bootstrapSwitch('state', false);
						}else if(vm.chartStatistical.points==1){
							$("#isScatter").bootstrapSwitch('state', true);
							$("#isLine").bootstrapSwitch('state', false);
					        $("#isBar").bootstrapSwitch('state', false);
						}else if(vm.chartStatistical.bars==1){
							$("#isScatter").bootstrapSwitch('state', false);
							$("#isLine").bootstrapSwitch('state', false);
					        $("#isBar").bootstrapSwitch('state', true);
						}
						if(vm.chartStatistical.yaxes==1){
							$("#leftYOpen").bootstrapSwitch('state', true);
						}else{
							$("#leftYOpen").bootstrapSwitch('state', false);
						}
						if(vm.chartStatistical.legend==1){
							$("#legendShow").bootstrapSwitch('state', true);					
						}else{
							$("#legendShow").bootstrapSwitch('state', false);
						}
						if(vm.chartStatistical.yaxis==1){
							$("#rightYOpen").bootstrapSwitch('state', true);
						}else{
							$("#rightYOpen").bootstrapSwitch('state', false);
						}
						if(vm.chartStatistical.xaxis==1){
							$("#xOpen").bootstrapSwitch('state', true);
						}else{
							$("#xOpen").bootstrapSwitch('state', false);
						}
						
						if(vm.chartSingle.isGauge==1){
							$("#isGauge").bootstrapSwitch('state', true);
						}else{
							$("#isGauge").bootstrapSwitch('state', true);
						}
						
						if(vm.chartSingle.isThreshold==1){
							$("#isThreshold").bootstrapSwitch('state', true);					
						}else{
							$("#isThreshold").bootstrapSwitch('state', true);
						}
						
						if(vm.chartSingle.isScale==1){
							$("#isAxisLabelShow").bootstrapSwitch('state', true);
						}else{
							$("#isAxisLabelShow").bootstrapSwitch('state', true);
						}
					}
				},
				deep:true
			}
		}
	})
</script>