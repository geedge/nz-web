<script>
/* 单次执行命令组件  */
var singleExecChild = Vue.extend({
    template: `  
    	<div>
		    <div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.executeCommand'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model="param.execCmd"/>
		        	<span class="required-symbol">*</span>
			        <div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>		
		        </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.owner'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model="param.username"/>
		        </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.maximumWaitingTime'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model="param.maxWaitTime"/>&nbsp&nbsp秒
		        </div>
	    	</div>
	    	<div  class="form-group">
			   	<div class="col-sm-2 control-label">
			   		<@spring.message 'mission.postbackID'/>
			   	</div>
			   	<div class="col-sm-9">
			   		<label class="radio-inline">
							<input type="radio" value="1" v-model="param.returnFlag"/> <@spring.message 'mission.yes'/>
					</label>
					<label class="radio-inline">
						<input type="radio" value="0" v-model="param.returnFlag"/> <@spring.message 'mission.no'/>
					</label>
			    </div>
	    	</div>
	    	<div class="form-group" v-show="param.returnFlag==1">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.postFile'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model="param.returnPath"/>
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
    	execCmd: '',
    	username:'',
    	maxWaitTime:null,
    	returnFlag:0,
    	returnPath:''
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
var singleExec = Vue.extend({
    template: `
    	<div class="" v-show="vm.mission.type==2&&vm.mission.commandEnname!=null && vm.mission.commandEnname.code=='single_exec'">
        <singleExecChild v-for="(item,index) in items"
          :key="index"
          :index="index"
          :items="items"
          @deleteIndex="del"
          @uploadData="getData">
        </singleExecChild>
        <a class="btn btn-primary" @click="add" title="<@spring.message 'common.add'/>"><i class="fa fa-plus"></i></a>
      </div>
    `,
    data () {
	    return {
	      items: [{
	    	  execCmd: '',
	      	  username:'',
	      	  maxWaitTime:null,
	      	  returnFlag:0,
	      	  returnPath:''
	      }],
	      dataRec: []
	    }
	  },
    // 局部注册子组件
    components: {
        singleExecChild: singleExecChild
    },
    methods: {
        //  add param
        add: function () {
          this.items.push({
	    	  execCmd: '',
	      	  username:'',
	      	  maxWaitTime:null,
	      	  returnFlag:0,
	      	  returnPath:''
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
          if(vm.mission.param.paramForExecuteInstructs==null||vm.mission.param.paramForExecuteInstructs==undefined){
        	  vm.mission.param.paramForExecuteInstructs={}
          }
          vm.mission.param.paramForExecuteInstructs=this.items;
          console.log('I got the data:', JSON.stringify(this.items))
        },
        dataReset:function(){
        	this.items=[{
	    	  execCmd: '',
	      	  username:'',
	      	  maxWaitTime:null,
	      	  returnFlag:0,
	      	  returnPath:''
	      }]
        }
      }
})

// 注册到全局
Vue.component('single-exec', singleExec)
</script>