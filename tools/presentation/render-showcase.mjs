/** Build the repository presentation from canonical, genuine app captures.
 * Run from any directory. Requires Sharp; APTURA_SHARP_MODULE may point to it.
 */
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const sharp=require(process.env.APTURA_SHARP_MODULE || 'sharp');
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const dest=path.join(root,'docs/assets/showcase');
const review=path.join(root,'build/review/repository-showcase');
await fs.mkdir(review+'/exports',{recursive:true});
await fs.mkdir(review+'/studies',{recursive:true});
const C={ink:'#07171C',paper:'#EEECE5',cyan:'#00D7E4',ice:'#9DF5F1',white:'#F3F2EB',muted:'#9DB1B7',teal:'#006D75'};
const inputs=new Map(),placements=[];
const hash=b=>crypto.createHash('sha256').update(b).digest('hex');
const sourceHash=(rel,b)=>hash(/\.(svg|mjs)$/.test(rel)?Buffer.from(b.toString('utf8').replaceAll('\r\n','\n')):b);
async function read(rel){const b=await fs.readFile(path.join(root,rel));inputs.set(rel,sourceHash(rel,b));return b;}
export function findPngPayloads(svg){
 const matches=[...svg.matchAll(/href="data:image\/png;base64,([A-Za-z0-9+/=]+)"/g)];
 if(!matches.length)throw Error('No embedded native PNGs found');
 return matches.map(m=>Buffer.from(m[1],'base64'));
}
async function capture(file,w,h){
 const svg=await read(file),all=findPngPayloads(svg.toString('utf8')),matches=[];
 for(const b of all){const m=await sharp(b).metadata();if(m.width===w&&m.height===h)matches.push(b);}
 if(matches.length!==1)throw Error(`Expected exactly one ${w}x${h} capture in ${file}; found ${matches.length}`);
 return matches[0];
}
const base='distribution/play-store/';
const phone=base+'editable/phone/';
const video=await capture(phone+'02_own_the_moment.svg',1920,956);
const library=await capture(phone+'01_every_frame.svg',1080,1740);
const tablet=await capture(base+'editable/tablet/01_library_dark.svg',2560,1600);
const eq=await capture(phone+'03_feel_the_scene.svg',1080,1562);
const cap=await capture(phone+'04_every_word.svg',1080,966);
const capStyle=await capture(phone+'04_every_word.svg',1080,730);
const precision=await capture(phone+'07_keep_that_part.svg',1080,1790);
const appearance=await capture(phone+'05_a_little_more_you.svg',1080,980);
const logo=await read('docs/assets/logo.svg');
const art=await read('docs/assets/showcase/source/optical-glass.webp');
const uri=(b,type='png')=>`data:image/${type};base64,${b.toString('base64')}`;
const image=(b,x,y,w,h,type='png',more='')=>`<image x="${x}" y="${y}" width="${w}" height="${h}" href="${uri(b,type)}" ${more}/>`;
const esc=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;');
const text=(x,y,size,s,fill=C.ink,weight=400,more='')=>`<text x="${x}" y="${y}" font-family="Arial,sans-serif" font-size="${size}" font-weight="${weight}" fill="${fill}" ${more}>${esc(s)}</text>`;
const bold=(x,y,size,s,fill=C.ink)=>text(x,y,size,s,fill,700,'letter-spacing="-4"');
const italic=(x,y,size,s,fill=C.teal)=>`<text x="${x}" y="${y}" font-family="Georgia,serif" font-size="${size}" font-style="italic" letter-spacing="-5" fill="${fill}">${esc(s)}</text>`;
let seq=0,canvas={width:0,height:0};
function start(w,h,bg=C.paper){canvas={width:w,height:h};return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}"><defs><filter id="shadow" x="-40%" y="-40%" width="180%" height="190%"><feDropShadow dx="0" dy="15" stdDeviation="19" flood-color="#001A20" flood-opacity=".22"/></filter><linearGradient id="fade" x1="0" y1="0" x2="1" y2="0"><stop stop-color="#07171C"/><stop offset="1" stop-color="#07171C" stop-opacity="0"/></linearGradient></defs><rect width="${w}" height="${h}" fill="${bg}"/>`;}
function brand(x,y,size=60,color=C.ink){return image(logo,x,y,size,size,'svg+xml')+text(x+size+25,y+size*.73,size*.42,'APTURA',color,600,'letter-spacing="5"');}
async function ui(b,x,y,w,{r=25,frame=true,label=''}={}){
 const m=await sharp(b).metadata(),h=w*m.height/m.width,n='screen'+(++seq);
 placements.push({label,x,y,width:w,height:h,sourceWidth:m.width,sourceHeight:m.height,canvas:{...canvas}});
 return `${frame?`<rect x="${x-5}" y="${y-5}" width="${w+10}" height="${h+10}" rx="${r+5}" fill="#091115" stroke="#35545D" stroke-opacity=".48" stroke-width="2" filter="url(#shadow)"/>`:''}<defs><clipPath id="${n}"><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${r}"/></clipPath></defs><g clip-path="url(#${n})">${image(b,x,y,w,h)}</g>`;
}
async function crop(b,rect){return sharp(b).extract(rect).png().toBuffer();}
const outputs=[];
async function save(name,svg,w,h,{study=false}={}){
 svg+='</svg>';
 await fs.writeFile(review+'/exports/'+name+'.svg',svg);
 const buf=await sharp(Buffer.from(svg)).removeAlpha().webp({quality:89,effort:6,smartSubsample:true}).toBuffer();
 const file=(study?review+'/studies/':dest+'/')+name+'.webp';await fs.writeFile(file,buf);
 const m=await sharp(buf).metadata();
 if(m.width!==w||m.height!==h||m.hasAlpha)throw Error('Unexpected export format: '+name);
 if(!study)outputs.push({file:path.relative(root,file).replaceAll('\\','/'),width:w,height:h,bytes:buf.length,sha256:hash(buf)});
}

// First composition: a quiet editorial launch image with real player and library.
let s=start(1600,900);
s+=brand(72,72,72)+text(75,232,21,'THE PERSONAL VIDEO PLAYER',C.teal,500,'letter-spacing="3"');
s+=bold(65,367,96,'Every frame.')+italic(64,511,133,'All yours.');
s+=text(75,588,31,'Your own videos. Beautifully offline.',C.ink);
s+=text(75,799,20,'NO ADS. NO ACCOUNTS. NO TRACKING.',C.teal,500,'letter-spacing="1"');
s+=await ui(video,646,225,880,{r:28,label:'hero-player'});
await save('hero-desktop',s,1600,900);

// A distinct alternate: picture first, with a dark technical-instrument treatment.
let alternative=start(1600,900,C.ink);
alternative+=brand(72,54,70,C.white)+bold(70,270,109,'A little cinema.',C.white)+italic(67,410,141,'A lot of control.',C.ice);
alternative+=await ui(video,72,466,760,{r:28,label:'alternate-player'});
alternative+=await ui(precision,1100,95,420,{r:26,label:'alternate-precision'});
alternative+=text(75,883,20,'OFFLINE VIDEO · ANDROID · OPEN SOURCE',C.muted,400,'letter-spacing="2"');
await save('hero-instrument-study',alternative,1600,900,{study:true});

s=start(900,1120);
s+=brand(54,45,64)+text(56,206,19,'THE PERSONAL VIDEO PLAYER',C.teal,500,'letter-spacing="2"');
s+=bold(47,333,103,'Every frame.')+italic(47,471,140,'All yours.');
s+=text(56,539,29,'Your own videos. Beautifully offline.',C.ink);
s+=await ui(video,55,616,790,{r:24,label:'mobile-hero-player'});
s+=text(56,1071,20,'NO ADS. NO ACCOUNTS. NO TRACKING.',C.teal,500,'letter-spacing="1"');
await save('hero-mobile',s,900,1120);

// Adaptive UI: intentionally matched physical composition, no mixed-ratio table.
s=start(1600,930,C.ink);
s+=text(65,78,22,'01 / A HOME FOR YOUR VIDEOS',C.ice,500,'letter-spacing="3"');
s+=text(64,128,24,'Find a file. Pick up a story. Keep everything local.',C.muted);
s+=await ui(tablet,500,190,1040,{r:28,label:'library-tablet'});
s+=await ui(library,60,197,400,{r:29,label:'library-phone'});
await save('library-desktop',s,1600,930);
s=start(900,1380,C.ink);
s+=text(59,87,23,'01 / YOUR PERSONAL LIBRARY',C.ice,500,'letter-spacing="2"');
s+=await ui(library,90,144,720,{r:30,label:'mobile-library'});
await save('library-mobile',s,900,1380);

// Controls: large native detail crops instead of entire towering bottom sheets.
s=start(1600,1200,C.ink);
s+=text(60,80,22,'02 / THE DETAILS MAKE THE DIFFERENCE',C.ice,500,'letter-spacing="3"');
s+=text(60,150,22,'SOUND',C.muted,500,'letter-spacing="3"')+text(820,150,22,'CAPTIONS + PRECISION',C.muted,500,'letter-spacing="3"');
s+=await ui(eq,60,190,650,{r:27,label:'controls-sound'});
s+=await ui(capStyle,820,190,720,{r:27,label:'controls-caption-style'});
s+=await ui(await crop(precision,{left:0,top:0,width:1080,height:560}),820,720,720,{r:27,label:'controls-loop'});
s+=text(60,1164,24,'Five bands. Your balance.',C.white)+text(820,1164,24,'Readable captions. Repeatable moments.',C.white);
await save('controls-desktop',s,1600,1200);
const bands=await crop(eq,{left:0,top:620,width:1080,height:942});
s=start(900,1630,C.ink);
s+=text(59,82,22,'02 / PLAYBACK, YOUR WAY',C.ice,500,'letter-spacing="2"');
s+=text(59,166,42,'Fine-tune the sound.',C.white,600);
s+=await ui(bands,60,215,780,{r:27,label:'mobile-equalizer-bands'});
s+=text(59,963,42,'Make every word clear.',C.white,600);
s+=await ui(capStyle,60,1008,780,{r:27,label:'mobile-caption-style'});
await save('controls-mobile',s,900,1630);

// Prepared share card: separate from the README's product-led hero.
let social=start(1280,640);social+=image(art,0,0,1280,640,'webp');
social+=brand(56,46,62)+bold(52,267,84,'Every frame.')+italic(52,382,120,'All yours.');
social+=text(60,457,27,'The offline video player for Android.',C.ink);
social+=image(await read('docs/assets/logo-mark.svg'),837,168,320,320,'svg+xml');
social+=text(60,577,19,'YOUR FILES. YOUR PACE. YOUR PRIVACY.',C.teal,500,'letter-spacing="1"');
const socialBuffer=await sharp(Buffer.from(social+'</svg>')).removeAlpha().png({compressionLevel:9}).toBuffer();
await fs.writeFile(dest+'/social-preview.png',socialBuffer);
outputs.push({file:'docs/assets/showcase/social-preview.png',width:1280,height:640,bytes:socialBuffer.length,sha256:hash(socialBuffer),social:true});
inputs.set('tools/presentation/render-showcase.mjs',sourceHash('render-showcase.mjs',await fs.readFile(fileURLToPath(import.meta.url))));

// Source views for readers who want to inspect a full native capture.
for(const [name,b] of [['native-library',library],['native-tablet',tablet],['native-player',video],['native-audio',eq],['native-captions',cap],['native-precision',precision],['native-appearance',appearance]]){
 const m=await sharp(b).metadata(),buf=await sharp(b).removeAlpha().webp({quality:93,effort:6}).toBuffer();
 const file=dest+'/'+name+'.webp';await fs.writeFile(file,buf);
 outputs.push({file:path.relative(root,file).replaceAll('\\','/'),width:m.width,height:m.height,bytes:buf.length,sha256:hash(buf),native:true});
}
await fs.writeFile(dest+'/manifest.json',JSON.stringify({schema:1,generator:'tools/presentation/render-showcase.mjs',inputs:Object.fromEntries(inputs),outputs,placements},null,2)+'\n');
console.log(JSON.stringify({outputs:outputs.length,primaryBytes:outputs.filter(x=>!x.native).reduce((s,x)=>s+x.bytes,0),directory:dest}));
