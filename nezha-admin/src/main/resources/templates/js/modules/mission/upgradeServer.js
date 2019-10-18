<script>
/* DC升级或第三方项目升级部署  */
var upgradeServerChild = Vue.extend({
    template: `  
    	<div>
		    <div class="form-group">
			   	<div class="col-sm-2 control-label"><@spring.message 'mission.path'/></div>
			   	<div class="col-sm-9">
 	 			    <input type="file" class="" name="" @change="getFile($event)"></input>
 	 			    <span class="required-symbol">*</span>
	        		<div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>		        
			    </div>
			</div>
			<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.overwriteDirectory'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model='param.cover'/>
		        	<span class="required-symbol">*</span>
	        		<div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>		
		        </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.startupFileOrCommand'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model='param.startupFile'/>
		        	<span class="required-symbol">*</span>
	        		<div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>		
		        </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.pidFile'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model='param.pidFile'/>
		        	<span class="required-symbol">*</span>
	        		<div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>		
		        </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.owner'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model='param.userName'/>
		        </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.groups'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model='param.groupName'/>
		        </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.filePermissions'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model='param.permisson'/>
		        </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.waitingTime'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model='param.maxWaitTime'/>&nbsp&nbsp<@spring.message 'mission.second'/>
		        </div>
	    	</div>
	    	<div  class="form-group">
			   	<div class="col-sm-2 control-label">
			   		<@spring.message 'mission.createLogo'/>
			   	</div>
			   	<div class="col-sm-9">
			   		<label class="radio-inline">
							<input type="radio" value="1" v-model='param.isCreateCover'/> <@spring.message 'mission.create'/>
					</label>
					<label class="radio-inline">
						<input type="radio" value="0" v-model='param.isCreateCover'/> <@spring.message 'mission.doNotCreate'/>
					</label>
			    </div>
	    	</div>
	    	<div  class="form-group">
			   	<div class="col-sm-2 control-label">
			   		<@spring.message 'mission.uncompressionID'/>
			   	</div>
			   	<div class="col-sm-9">
			   		<label class="radio-inline">
							<input type="radio" value="1" v-model='param.isAbs'/> <@spring.message 'mission.absolutePath'/>
					</label>
					<label class="radio-inline">
						<input type="radio" value="0" v-model='param.isAbs'/> <@spring.message 'mission.relativePath'/>
					</label>
			    </div>
	    	</div>
	    	<div  class="form-group">
			   	<div class="col-sm-2 control-label">
			   		<@spring.message 'mission.forceEnforcement'/>
			   	</div>
			   	<div class="col-sm-9">
			   		<label class="radio-inline">
							<input type="radio" value="1" v-model='param.forceExec'/> <@spring.message 'mission.yes'/>
					</label>
					<label class="radio-inline">
						<input type="radio" value="0" v-model='param.forceExec'/> <@spring.message 'mission.no'/>
					</label>
			    </div>
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.deleteFiles'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<textarea class="form-control" v-model='param.deleteTmpStr'></textarea>
		        </div>	
	    	</div>
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.startupParameters'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<textarea class="form-control" v-model='param.execParamsTmpStr'></textarea>
		        </div>	
	    	</div>
	    	
	    	<div class="form-group">
		    	<div class="col-sm-2 control-label">
		    		<@spring.message 'mission.backUp'/>
		    	</div>
		    	<div class="col-sm-9">
		        	<label class="radio-inline">
							<input type="radio" value="1" v-model="param.isBackup" @change="addData"/> <@spring.message 'mission.yes'/>
					</label>
					<label class="radio-inline">
						<input type="radio" value="0" v-model="param.isBackup"/> <@spring.message 'mission.no'/>
					</label>
		        </div>	
	    	</div>
	    	<div v-show='param.isBackup==1'>
	            <div v-for="backup,index in param.backups">
	                <div class="form-group">
				    	<div class="col-sm-2 control-label">
				    		<@spring.message 'mission.filesThatNeedToBeBackedUp'/>
				    	</div>
				    	<div class="col-sm-9">
				        	<input type="text" class="form-control" v-model="backup.backUp"/>
				        </div>
			    	</div>
			    	<div class="form-group">
				    	<div class="col-sm-2 control-label">
				    		<@spring.message 'mission.pathToBackup'/>
				    	</div>
				    	<div class="col-sm-9">
				        	<input type="text" class="form-control" v-model="backup.backUpTo"/>
				        </div>
			    	</div>
			    	<div class="form-group">
				    	<div class="col-sm-2 control-label">
				    		<@spring.message 'mission.compressionID'/>
				    	</div>
				    	<div class="col-sm-9">
				        	<label class="radio-inline">
									<input type="radio" value="1" v-model="backup.isAbs"/> <@spring.message 'mission.yes'/>
							</label>
							<label class="radio-inline">
								<input type="radio" value="0" v-model="backup.isAbs"/> <@spring.message 'mission.no'/>
							</label>
				        </div>	
			    	</div>
			    	<div class="form-group">
				    	<div class="col-sm-2 control-label">
				    		<@spring.message 'mission.excludeFile'/>
				    	</div>
				    	<div class="col-sm-9">
				        	<textarea class="form-control" v-model="backup.exceptTmpStr"></textarea>
				        </div>	
			    	</div>
			    	
			    	<div class="form-group">
						<a class="btn btn-primary" @click="deleteBackup(index)" title="<@spring.message 'common.delete'/>"><i class="fa fa-trash-o"></i></a>
					</div>
	            </div>
	            
	            <div class="form-group">
					<a class="btn btn-primary" @click="addBackup" title="<@spring.message 'common.add'/>"><i class="fa fa-plus"></i></a>
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
    	  isBackup:0,
    	  cover:'',
    	  isAbs:0,
    	  isCreateCover:0,
    	  userName:'',
    	  deleteTmpStr:'',
    	  groupName:'',
    	  permisson:'',
    	  maxWaitTime:'',
    	  startupFile:'',
    	  forceExec:0,
    	  pidFile:'',
    	  execParamsTmpStr:'',
    	  backups:[]
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
	    },
	    addBackup: function(){
	    	this.param.backups.push({
	    		  backUp:'',
				  backUpTo:'',
				  isAbs:0,
				  exceptTmpStr:''
	    	})
	    },
	    deleteBackup:function(index){
	    	this.param.backups.splice(index, 1)
	    	console.log(this.param.backups)
	    	if(this.param.backups.length==0){
	    		this.param.isBackup=0;
	    	}
	    },
	    getFile:function(event){
	    	this.$emit('uploadFile',event,this.index)
	    },
	    addData:function(){
	    	this.param.backups.push({
	    		backUp:"",
				backUpTo:"",
				isAbs:0,
				exceptTmpStr:""
	    	})
	    }
	  },
	  mounted:function(){
		  this.param = {...this.items[this.index]}
	  }
	})
	// 构建一个父组件 
var upgradeServer = Vue.extend({
    template: `
    	<div class="" >
        <upgradeServerChild v-for="(item,index) in items"
          :key="index"
          :index="index"
          :items="items"
          @deleteIndex="del"
          @uploadData="getData"
          @uploadFile="getPushFile">
        </upgradeServerChild>
        <a class="btn btn-primary" @click="add" title="<@spring.message 'common.add'/>"><i class="fa fa-plus"></i></a>
      </div>
    `,
    data () {
	    return {
	      items: [{
	    	  isBackup:0,
	    	  cover:'',
	    	  isAbs:0,
	    	  isCreateCover:0,
	    	  userName:'',
	    	  deleteTmpStr:'',
	    	  groupName:'',
	    	  permisson:'',
	    	  maxWaitTime:'',
	    	  startupFile:'',
	    	  forceExec:0,
	    	  pidFile:'',
	    	  execParamsTmpStr:'',
	    	  backups:[]
	      }]
	    }
	  },
    // 局部注册子组件
    components: {
        upgradeServerChild: upgradeServerChild
    },
    methods: {
        //  add param
        add: function () {
          this.items.push({ 
        	  isBackup:0,
        	  cover:'',
        	  isAbs:0,
        	  isCreateCover:0,
        	  userName:'',
        	  deleteTmpStr:'',
        	  groupName:'',
        	  permisson:'',
        	  maxWaitTime:'',
        	  startupFile:'',
        	  forceExec:0,
        	  pidFile:'',
        	  execParamsTmpStr:'',
        	  backups:[]
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
          /*if(vm.mission.param.paramForUpgrades==null||vm.mission.param.paramForUpgrades==undefined){
        	  vm.mission.param.paramForUpgrades={}
          }
          vm.mission.param.paramForUpgrades=this.items;*/
          var param={
        		  paramForUpgrades:{}
          }
          param.paramForUpgrades=this.items;
          this.$emit('param',this.param)
          console.log('I got the data:', JSON.stringify(this.items))
        },
        dataReset:function(){
        	this.items=[{
        		  isBackup:0,
	          	  cover:'',
	          	  isAbs:0,
	          	  isCreateCover:0,
	          	  userName:'',
	          	  deleteTmpStr:'',
	          	  groupName:'',
	          	  permisson:'',
	          	  maxWaitTime:'',
	          	  startupFile:'',
	          	  forceExec:0,
	          	  pidFile:'',
	          	  execParamsTmpStr:'',
	          	  backups:[]
  	      	}]
        },
        getPushFile:function(val,index){
        	this.$emit('files',val,index)
        }
      }
})

// 注册到全局
Vue.component('upgrade-server', upgradeServer)
</script>