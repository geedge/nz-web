<script>
var filePushChild = Vue.extend({
    template: `  
    	<div>
	    	<div class="form-group">
			   	<div class="col-sm-2 control-label"><@spring.message 'mission.path'/></div>
			   	<div class="col-sm-9">
 	 			    <input type="file" class="" name="file" @change="getFile($event)"></input>
 	 			    <span class="required-symbol">*</span>
			        <div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>		        
			    </div>
			</div>
	    
	        <div class="form-group">
    			<div class="col-sm-2 control-label"><@spring.message 'mission.pushDestination'/></div>
    			<div class="col-sm-9">
        			<input type="text" class="form-control" v-model="param.destPath"/>
        			<span class="required-symbol">*</span>
			        <div class="form-control_error-msg" ><@spring.message 'validate.common.required'/></div>
    			</div>
    		</div>
		
    		<div class="form-group">
		   		<div class="col-sm-2 control-label"><@spring.message 'mission.owner'/></div>
		   		<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model="param.username"/>
		    	</div>
			</div>


    		<div class="form-group">
		   		<div class="col-sm-2 control-label"><@spring.message 'mission.groups'/></div>
		   		<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model="param.groupName"/>
		    	</div>
			</div>


    		<div class="form-group">
		   		<div class="col-sm-2 control-label"><@spring.message 'mission.filePermissions'/></div>
		   		<div class="col-sm-9">
		        	<input type="text" class="form-control" v-model="param.permisson"/>
		    	</div>
			</div>


    		<div  class="form-group">
		   		<div class="col-sm-2 control-label"><@spring.message 'mission.overwrite'/></div>
		   		<div class="col-sm-9">
			   		<label class="radio-inline">
						<input type="radio" value="1" v-model="param.isCover"/> <@spring.message 'mission.yes'/>
					</label>
					<label class="radio-inline">
						<input type="radio" value="0" v-model="param.isCover"/> <@spring.message 'mission.no'/>
					</label>
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
  data : function () {
    return {
      param: {
    	destPath: '',
    	fileName: '',
    	groupName:'',
    	isCover:0,
    	permisson:'',
    	username:''
      }
    }
  },
  watch: {
    param: {
      handler (newV, oldV) {
    	//这里加判断 如果输入框中没有填写内容贼不允许添加新的组件
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
    getFile:function(event){
    	this.$emit('uploadFile',event,this.index)
    }
  },
  mounted:function(){
	  this.param = {...this.items[this.index]}
  }
})

// 构建一个父组件
var filePush = Vue.extend({
    template: `
    	<div class="" v-show="vm.mission.type==1">
        <filePushChild v-for="(item,index) in items"
          :key="index"
          :index="index"
          :items="items"
          @deleteIndex="del"
          @uploadData="getData"
          @uploadFile="getPushFile">
        </filePushChild>
        <a class="btn btn-primary" @click="add" title="<@spring.message 'common.add'/>"><i class="fa fa-plus"></i></a>
      </div>
    `,
    data : function () {
	    return {
	      items: [{destPath: '',
	      	fileName: '',
	    	groupName:'',
	    	isCover:0,
	    	permisson:'',
	    	username:''
	      }]
	    }
	  },
    // 局部注册子组件
    components: {
        filePushChild: filePushChild
    },
    methods: {
        //  add param
        add: function () {
          this.items.push({destPath:'',
          	fileName: '',
        	groupName:'',
        	isCover:0,
        	permisson:'',
        	username:''})
        },
        // delete param
        del: function (index) {
          //  仅剩一个组件的时候不允许删除
          if (this.items.length !== 1) {
            this.items.splice(index, 1)
            console.log('deleted:', JSON.stringify(this.items))
          }
        },
        //  get the data from child
        getData: function (val) {
          let index = val.index
          this.items[index] = val.data
          vm.mission.param=this.items;
          console.log('I got the data:', JSON.stringify(this.items))
        },
        getPushFile:function(val,index){
        	vm.files[index]=event.target.files[0]
        },
        dataReset:function(){
        	this.items=[{destPath: '',
    	      	fileName: '',
    	    	groupName:'',
    	    	isCover:0,
    	    	permisson:'',
    	    	username:''
    	      }]
        }
      }
})

// 注册到全局
Vue.component('filepush', filePush)
</script>