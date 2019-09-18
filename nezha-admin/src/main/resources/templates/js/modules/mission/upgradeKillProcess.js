<script>
/* 停用进程组件  */
var upgradeKillProcessChild = Vue.extend({
    template: `  
    	<div>
		    <div class="form-group">
		    	<div class="col-sm-2 control-label"><@spring.message 'mission.pidFile'/></div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model="param.destPath"/>
		        	<span class="required-symbol">*</span>
			        <div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>		
	    		</div>
	    	</div>
			<div class="form-group" v-show="items.length>1">
				<a class="btn btn-primary" @click="deleteparam" title="<@spring.message 'common.delete'/>"><i class="fa fa-trash-o"></i></a>
			</div>
    	</div>
    `,
    props: {
    index: {
      type: Number,
      required: true
    },
    items: {
      type: Array,
      default: Array
    }
  },
  data () {
    return {
      param: {
    	pidFiles: ''
      }
    }
  },
  watch: {
	    param: {
	      handler (newV, oldV) {
	        this.$emit('uploadData', {index: this.index, data: newV})
	      },
	      deep: true
	    },
	    items: {
	      handler (newV, oldV) {
	        if (newV.length !== 0) {
	          this.param = {...newV[this.index]}
	        }
	      },
	      deep: true
	    }
	  },
	  methods: {
	    deleteparam: function () {
	      this.$emit('deleteIndex', this.index)
	    }
	  }
	})
	// 构建一个父组件   && vm.mission.commandEnname!=null && vm.mission.commandEnname.code==0  
var upgradeKillProcess = Vue.extend({
    template: `
    	<div v-if="vm.mission.type==2 && vm.mission.commandEnname!=null && vm.mission.commandEnname.code==0 ">
        <upgradeKillProcessChild v-for="(item,index) in items"
          :key="index"
          :index="index"
          :items="items"
          @deleteIndex="del"
          @uploadData="getData">
        </upgradeKillProcessChild>
        <a class="btn btn-primary" @click="add" title="<@spring.message 'common.add'/>"><i class="fa fa-plus"></i></a>
      </div>
    `,
    data () {
	    return {
	      items: [{}],
	      dataRec: []
	    }
	  },
    // 局部注册子组件
    components: {
        upgradeKillProcessChild: upgradeKillProcessChild
    },
    methods: {
        //  add param
        add: function () {
          this.items.push({destPath: ''})
        },
        // delete param
        del: function (index) {
          //  not allow to delete the first
          if (this.items.length !== 1) {
            this.items.splice(index, 1)
            console.log('deleted:', JSON.stringify(this.items))
          }
        },
        //  get the data from child
        getData: function (val) {
          let index = val.index
          this.items[index] = val.data
          console.log('I got the data:', JSON.stringify(this.items))
        }
      }
})

// 注册到全局
Vue.component('upgrade-kill-process', upgradeKillProcess)
new Vue({
    el: '#upgradeKillProcess',
    data: {
        
    }
})
</script>