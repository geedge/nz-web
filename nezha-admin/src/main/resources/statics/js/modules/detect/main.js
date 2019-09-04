//注册select组件
Vue.component('multiselect', window.VueMultiselect.default);
//引用vuelidate组件
Vue.use(window.vuelidate.default);
//引用校验规则   所有校验规则见   https://vuelidate.netlify.com/#sub-v-values
var required = window.validators.required;
var helpers = window.validators.helpers;
var integer = window.validators.integer;