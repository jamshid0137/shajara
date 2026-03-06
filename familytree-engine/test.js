import { FamilyTree } from './src/index.js';

const nodes = [
    { id: 1, name: 'King George VI' },
    { id: 2, name: 'Queen Elizabeth', pids: [1] },
    
    { id: 3, name: 'Queen Elizabeth II', mid: 2, fid: 1 },
    { id: 4, name: 'Prince Philip', pids: [3] },
    
    { id: 6, name: 'King Charles III', mid: 3, fid: 4 },
    { id: 7, name: 'Princess Anne', mid: 3, fid: 4 }
];

try {
    // Mock document
    global.document = {
        querySelector: () => ({
            innerHTML: '',
            style: {},
            appendChild: () => {},
            addEventListener: () => {},
            clientWidth: 1000,
            clientHeight: 1000,
        }),
        createElementNS: () => ({
            style: {},
            classList: { add: () => {} },
            appendChild: () => {},
            setAttribute: () => {},
            addEventListener: () => {},
        }),
        body: { clientWidth: 1000, clientHeight: 1000 }
    };
    global.window = {
        addEventListener: () => {}
    };

    const chart = new FamilyTree('dummy', { nodes });
    console.log("SUCCESS!");
} catch (e) {
    console.error("ERROR:");
    console.error(e);
}
