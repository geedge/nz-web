<script>
/* 备份组件  */
var upgradeBackupChild = Vue.extend({
    template: `  
    	<div>
		    <div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.filesThatNeedToBeBackedUp'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model="param.backup"/>
		        	<span class="required-symbol">*</span>
			        <div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>		
		        </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.pathToBackup'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model="param.backupTo"/>
		        	<span class="required-symbol">*</span>
			        <div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>		
		        </div>
	    	</div>
	    	<div  class="form-group">
			   	<div class="col-sm-2 control-label">
			   		<@spring.message 'mission.compressionID'/> 
			   	</div>
			   	<div class="col-sm-9">
			   		<label class="radio-inline">
							<input type="radio" value="1" v-model="param.isAbs"/> <@spring.message 'mission.absolutePath'/> 
					</label>
					<label class="radio-inline">
						<input type="radio" value="0" v-model="param.isAbs"/> <@spring.message 'mission.relativePath'/> 
					</label>
			    </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.excludeFile'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<textarea class="form-control" v-model="param.exceptTmpStr"></textarea>
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
    	  backup: '',
    	  backupTo:'',
    	  isAbs:0,
    	  exceptTmpStr:''
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
	  },
	  mounted:function(){
		  this.param = {...this.items[this.index]}
	  }
	})
	// 构建一个父组件 
var upgradeBackup = Vue.extend({
    template: `
    	<div class="">
        <upgradeBackupChild v-for="(item,index) in items"
          :key="index"
          :index="index"
          :items="items"
          @deleteIndex="del"
          @uploadData="getData">
        </upgradeBackupChild>
        <a class="btn btn-primary" @click="add" title="<@spring.message 'common.add'/>"><i class="fa fa-plus"></i></a>
      </div>
    `,
    data () {
	    return {
	      items: [{
	    	  backup: '',
	    	  backupTo:'',
	    	  isAbs:0,
	    	  exceptTmpStr:''
	      }],
	      dataRec: []
	    }
	  },
    // 局部注册子组件
    components: {
        upgradeBackupChild: upgradeBackupChild
    },
    methods: {
        //  add param
        add: function () {
          this.items.push({ 
        	  backup: '',
        	  backupTo:'',
        	  isAbs:0,
        	  exceptTmpStr:''
          })
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
          /*if(vm.mission.param.paramForExecuteInstructs==null||vm.mission.param.paramForExecuteInstructs==undefined){
        	  vm.mission.param.paramForExecuteInstructs={}
          }
          vm.mission.param.paramForExecuteInstructs=this.items;*/
          var param={
        		  paramForExecuteInstructs:{}
          }
          param.paramForExecuteInstructs=this.items
          this.$emit('param',this.param)
          console.log('I got the data:', JSON.stringify(this.items))
        },
        dataReset:function(){
        	this.items=[{
        	  backup: '',
          	  backupTo:'',
          	  isAbs:0,
          	  exceptTmpStr:''
  	      }]
        }
      }
})

// 注册到全局
Vue.component('upgrade-backup', upgradeBackup)
</script>