$(() => {
    const channel = getChannel();
    const series = getSeries();

    if (channel === null)
        loadChannels();
    else if (series === null)
        loadSeries(channel);
    else
        loadEpisodes(channel, series);

    initBackButton(channel, series)
});

function loadChannels() {
    $.ajax({
        url: "/api/getTvChannels", success: function (result) {
            result = result.sort((a, b) => a > b);
            const r = $('#result');

            r.html(''); // delete loading message

            result.forEach((t) => r.append(`<p class="container"><a href="javascript:setChannel('${t}')" class="channel">${t}</a></p>`));
        }, error: (x) => console.log("error: " + x)
    })
}

function loadSeries(channel) {
    $.ajax({
        url: "/api/getSeries?channel=" + encodeURI(channel), success: function (result) {
            result = result.sort((a, b) => a > b);
            const r = $('#result');

            r.html(''); // delete loading message

            result.forEach((t) => r.append(`<p class="container">${channel}<span class="arrow">➤</span><a href="javascript:setSeries('${t}')">${t}</a></p>`));
        }, error: (x) => console.log("error: " + x)
    })
}

function loadEpisodes(channel, series) {
    $.ajax({
        url: "/api/getEpisodes?series=" + encodeURI(series), success: function (result) {
            result = result.sort((a, b) => a['episodeTitle'] > b['episodeTitle']);
            const r = $('#result');

            r.html(''); // delete loading message

            result.forEach((t) => {
                const arr = '<span class="arrow">➤</span>';
                const encoded = encodeURI(JSON.stringify(t));
                const url = '/player.html?data=' + encoded;

                r.append(`<p class="container">${channel}${arr}${series}${arr}<a href="${url}">${t["episodeTitle"]}</a></p>`)
            });
        }, error: (x) => console.log("error: " + x)
    })
}

// noinspection JSUnusedGlobalSymbols
function setChannel(chan) {
    let url = window.location.href;
    if (url.indexOf('?') > -1) {
        url += '&channel=' + encodeURI(chan)
    } else {
        url += '?channel=' + encodeURI(chan)
    }
    window.location.href = url;
}

// noinspection JSUnusedGlobalSymbols
function setSeries(series) {
    let url = window.location.href;
    if (url.indexOf('?') > -1) {
        url += '&series=' + encodeURI(series)
    } else {
        url += '?series=' + encodeURI(series)
    }
    window.location.href = url;
}

function initBackButton(channel, series) {
    if (channel !== null) {
        // channel is set and therefore display back button
        let href = "index.html";

        console.log(location.hostname);
        if (series !== null) {
            href += "?channel=" + encodeURI(channel)
        }

        $('#back').append(`<a href="${href}">Go Back</a>`)
    }
}

function getQueryParameters() {
    return new URLSearchParams(window.location.search);
}

function getChannel() {
    const params = getQueryParameters();

    return params.get('channel');
}

function getSeries() {
    const params = getQueryParameters();

    return params.get('series');
}
