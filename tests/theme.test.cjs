const test=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const code=fs.readFileSync('frontend/assets/js/theme.js','utf8');
function setup(saved,dark=false,blocked=false){
 const nodes={},listeners={},media={matches:dark,addEventListener:(name,fn)=>media.change=fn};
 const element=()=>({style:{},children:[],classList:{add(){}},setAttribute(){},append(...items){this.children.push(...items);for(const child of items)if(child.id)nodes[child.id]=child;},addEventListener(name,fn){this[name]=fn;}});
 const host=element(),root={dataset:{},style:{}};let value=saved;
 const context={document:{documentElement:root,readyState:'complete',getElementById:id=>nodes[id],createElement:element,querySelector:()=>host,body:host},localStorage:{getItem(){if(blocked)throw Error('blocked');return value;},setItem(k,v){value=v;}},window:{matchMedia:()=>media,addEventListener:(name,fn)=>listeners[name]=fn}};
 vm.runInNewContext(code,context);return {root,media,listeners,select:nodes.themeSelect,saved:()=>value};
}
test('appearance follows system by default and changes live',()=>{const s=setup(null,true);assert.equal(s.root.dataset.theme,'dark');s.media.matches=false;s.media.change();assert.equal(s.root.dataset.theme,'light');});
test('explicit preference persists and stays independent from OS theme',()=>{const s=setup('dark');assert.equal(s.select.value,'dark');s.select.value='light';s.select.change();assert.equal(s.saved(),'light');s.media.matches=true;s.media.change();assert.equal(s.root.dataset.theme,'light');});
test('blocked storage is tolerated and theme syncs between tabs',()=>{const s=setup(null,false,true);assert.equal(s.root.dataset.theme,'light');s.listeners.storage({key:'helpdesk_theme',newValue:'dark'});assert.equal(s.root.dataset.bsTheme,'dark');});
