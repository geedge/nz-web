<script>
var filePushChild = Vue.extend({
    template: `  
    	<div>
	    	<div class="form-group">
			   	<div class="col-sm-2 control-label"><@spring.message 'mission.path'/></div>
			   	<div class="col-sm-9">
 	 			    <input type="file" class=""></input>
 	 			    <span class="required-symbol">*</span>
			        <div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>		        
			    </div>
			</div>
	    
	        <div class="form-group">
    			<div class="col-sm-2 control-label"><@spring.message 'mission.pushDestination'/></div>
    			<div class="col-sm-9">
        			<input type="text" class="form-control" v-model="param.destPath"/>
        			<span class="required-symbol">*</span>
			        <div class="form-control_error-msg" v-if=""><@spring.message 'validate.common.required'/></div>
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
  data () {
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
       /*  if (newV.name.length === 0) {
          return false
        }
        if (newV.age.length === 0) {
          return false
        }
 */
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
var filePush = Vue.extend({
    template: `
    	<div class="" v-show="vm.mission.type==1">
        <filePushChild v-for="(item,index) in items"
          :key="index"
          :index="index"
          :items="items"
          @deleteIndex="del"
          @uploadData="getData"
          v-model="vm.mission.param">
        </filePushChild>
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
        filePushChild: filePushChild
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
          console.log(vm.mission.type+"    tttttttttttttt")
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
Vue.component('filepush', filePush)
new Vue({
    el: '#filepush',
    data: {
        
    }
})
</script>