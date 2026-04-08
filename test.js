const fs = require('fs');
const code = fs.readFileSync('d:/загрузки/shajara/src/main/resources/static/v7/familytree.js', 'utf8');
const rx = /replace\(["']\{rounded\}|replaceAll\(["']\{rounded\}/g;
const m = code.match(rx);
console.log('matches:', m);
