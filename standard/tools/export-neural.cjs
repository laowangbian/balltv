// MIT-licensed WebSR real-life CNN S weights, adapted from WebGPU to GLES.
const fs=require('fs'),crypto=require('crypto'),base=process.argv[2]||require('path').resolve(__dirname,'..'),dir=base+'/app/src/main/assets/neural';
const raw=fs.readFileSync(base+'/third-party/websr/cnn-2x-s-rl.json','utf8'),weights=JSON.parse(raw),passes=[];
const layers=['conv2d_tf','conv2d_1_tf','conv2d_2_tf','conv2d_last_tf'];
const number=x=>Number.isInteger(x)?x+'.0':String(x);
const vec=v=>'vec4('+v.map(number).join(',')+')';
for(let i=0;i<4;i++){
 const layer=weights.layers[layers[i]],bind=i===0?'RGB':'L'+(i-1),save=i===3?'SUBCONV1':'L'+i;
 let shader='// Copyright (c) 2023 Sam Bhattacharyya. MIT license.\n// Modified 2026-11-10: GLES shader export of real-life trained weights.\nprecision highp float;varying vec2 vTex;uniform vec2 uStep;uniform sampler2D u'+bind+';\nvoid main(){vec4 res='+vec(layer.bias)+';\n';
 for(let tap=0;tap<9;tap++){
  const x=Math.floor(tap/3)-1,y=tap%3-1;
  shader+='vec4 p'+tap+'=texture2D(u'+bind+',vTex+vec2('+number(x)+','+number(y)+')*uStep);\n';
  const matrix=k=>'mat4('+layer.weights.slice(k*16,k*16+16).map(number).join(',')+')';
  shader+='res+='+matrix(tap)+'*'+(i===0?'p'+tap:'max(p'+tap+',vec4(0.))')+';\n';
  if(i!==0)shader+='res+='+matrix(tap+9)+'*max(-p'+tap+',vec4(0.));\n';
 }
 shader+='gl_FragColor=res;}\n';const file='pass'+i+'.glsl';fs.writeFileSync(dir+'/'+file,shader);passes.push({file,binds:[bind],save});
}
fs.writeFileSync(dir+'/model.json',JSON.stringify({name:'RealLife CNN S 2x',source:'https://github.com/sb2702/websr',sha256:crypto.createHash('sha256').update(raw).digest('hex'),license:'MIT',passes},null,2));
