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
		        	<input type="text" class="form-control" v-model="param.destPath"/>
		        	<span class="required-symbol">*</span>
			        <div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>		
		        </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.pathToBackup'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model="param.destPath"/>
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
							<input type="radio" value="1" v-model="param.isCover"/> <@spring.message 'mission.absolutePath'/> 
					</label>
					<label class="radio-inline">
						<input type="radio" value="0" v-model="param.isCover"/> <@spring.message 'mission.relativePath'/> 
					</label>
			    </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.excludeFile'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<textarea class="form-control" v-model="param.destPath"></textarea>
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
	// 构建一个父组件 
var upgradeBackup = Vue.extend({
    template: `
    	<div class="" v-show="vm.mission.type==2 && vm.mission.commandEnname!=null && vm.mission.commandEnname.code==1">
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
	      items: [{}],
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
          this.items.push({destPath: '',
          	fileName: '',
        	groupName:'',
        	isCover:0,
        	permisson:'',
        	username:''})
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
Vue.component('upgrade-backup', upgradeBackup)
new Vue({
    el: '#upgradeBackup',
    data: {
        
    }
})
</script>