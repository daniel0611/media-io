$(() => {
    if (getMovie() === null || getMovie() === "") {
        $('body').html('');
        $('#error').text('Could not get Movie Data');
        return
    }

    const movie = JSON.parse(getMovie());
    setTitle(movie);
    setPlayerSource(movie);
    setDescription(movie)
});

function setDescription(movie) {
    $('#descriptionText').text(movie['description']);

    $('#releaseDate').append(new Date(movie['releaseDate']).toString());
}

function setPlayerSource(movie) {
    const url = movie['downloadUrl'];

    $('#player').append(`<source src="${url}" type="video/mp4">`);
}

function setTitle(movie) {
    const episode = movie['episodeTitle'];
    const series = movie['seriesTitle'];
    const channel = movie['tvChannel'];
    const title = `${episode} - ${series} - ${channel}`;

    $('head').append(`<title>${title}</title>`);
    $('#title').text(title);
}

function getMovie() {
    const params = new URLSearchParams(window.location.search);
    return params.get('data')
}