OrgChart.remote._fromReqDTO = function (nodes, roots, configs, callback) {
    fetch('/api/layout1/calculate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nodes, roots, configs })
    })
        .then(r => r.json())
        .then(result => callback(result))
        .catch(err => console.error(err));
};