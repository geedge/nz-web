<script>
var filePushChild = Vue.extend({
    template: `  
    	<div>
	    	<div class="form-group">
			   	<div class="col-sm-2 control-label">文件</div>
			   	<div class="col-sm-10">
 	 			    <input type="file" class=""></input>		        
			    </div>
			</div>
	    
	        <div class="form-group">
    			<div class="col-sm-2 control-label">推送目的地</div>
    			<div class="col-sm-10">
        			<input type="text" class="form-control" v-model="param.destPath"/>
    			</div>
    		</div>
		
    		<div class="form-group">
		   		<div class="col-sm-2 control-label">属主</div>
		   		<div class="col-sm-10">
		        	<input type="text" class="form-control" v-model="param.username"/>
		    	</div>
			</div>


    		<div class="form-group">
		   		<div class="col-sm-2 control-label">属群</div>
		   		<div class="col-sm-10">
		        	<input type="text" class="form-control" v-model="param.groupName"/>
		    	</div>
			</div>


    		<div class="form-group">
		   		<div class="col-sm-2 control-label">文件权限</div>
		   		<div class="col-sm-10">
		        	<input type="text" class="form-control" v-model="param.permisson"/>
		    	</div>
			</div>


    		<div  class="form-group">
		   		<div class="col-sm-2 control-label">覆盖</div>
		   		<div class="col-sm-10">
			   		<label class="radio-inline">
						<input type="radio" value="1" v-model="param.isCover"/> 是
					</label>
					<label class="radio-inline">
						<input type="radio" value="0" v-model="param.isCover"/> 否
					</label>
		    	</div>
			</div>	
			<div class="form-group" v-show="items.length>1">
				<input type="button" class="btn btn-primary" @click="deleteparam" value="删除"/>
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
        <input type="button" class="btn btn-primary" @click="add" value="新增"/>
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