# Media-IO

[![Build Status](https://travis-ci.com/daniel0611/media-io.svg?branch=master)](https://travis-ci.com/daniel0611/media-io)

This application downloads episodes of series that are specified in the config.json from german public television broadcasters like "ARD", "ZDF", "SWR" and many more. It uses [mediathekview/MLib](https://github.com/mediathekview/MLib) to discover all video files meaning it is like [Mediathekview](https://github.com/mediathekview/MediathekView) but without a ui, because everything is configured by a config file.

Note that this project is quite finished and has everything I want or need. Thats why I am not actively working on it but if you find a bug or want a new feature you are still free to open a issue or pull request!

## Configuring with the `config.json` file

Media-IO gets configured by a `config.json` file in the work directory. If you want to have a other filename you can pass a path with the `-c` or `--config` cli flag.

A basic configuration might look like this:
```json
{
  "downloadDirectory": "./download/",
  "minimumSize": 50,
  "minimumLength": 20,
  "maxDaysOld": 0,
  "filters": [
    {
      "tvChannel": "swr",
      "seriesTitle": "Hannes und der Bürgermeister"
    }
  ]
}
```

Note that not all properties were used in this example!

Here is a explanation of what all of these properties do:

- `downloadDirectory`(required): Path where all downloaded video files will be stored. Can be either relative to the current directory or absolute. The files will be stored in subdirectories in following format: `<downloadDirectory>/<tvChannel>/<seriesTitle>/<episodeTitle>-<date>,<fileextension>` .
- `minimumSize`: Is the minimum size in MB that video files have to get downloaded. This is designed to get rid of some pre-tv specials and trailers. If 0 is passed the check is disabled.
- `minimumLength`: Is the minimum length in minutes that video files have to get downloaded. Same reason as above. If 0 is passed the check is disabled.
- `maxDaysOld`: Is the maximum age of video files in days. Video files older than that won't be downloaded. This is because some files stay on the servers but are years old and probably unwanted.  Again if 0 is passed the check is disabled.
- `filters`(required): This determines which series should actually be downloaded. As it is a array a indefinite number of filters may be passed. Here is what sub-properties a filter has:
    - `tvChannel` (required): The channel which broadcasts this series. Matched literally but not case sensitive. E.g. `ard`, `zdf`, `swr` or any other german public tv channel.
    - `seriesTitle`: A regex matching the title of the series you want to download with this filter. E.g. `Hannes und der Bürgermeister` or `Der Tatort`. Defaults to `.+`.
    - `episodeTitle`: A regex matching the title of the episodes you want to download. Needed if a series exists in various versions for disabled people and you only want a specific version, but not all. In this case negative lookaheads are your friend. Defaults to `.+`.
- `movieDataSource`: The source of the list containing information about all video files. Defaults to https://verteiler1.mediathekview.de/Filmliste-akt.xz

## Execution modes

### Download and stop

This is the default mode. The config file is read, the video file list is downloaded from the remote, if updated, locally missing video files are downloaded and the application stops. To use it you just need to start the application with no flags or options.

### Interactive mode

This mode is designed to download single and irregularity video files like movies or single episodes of a series. Another use case is to test your regexes you want to use in the config file.

 You start it by adding the `-i` or `--interactive` flag.

### Server mode (UNFINISHED, not being worked on!)

By using the `serve` command you can start a local webserver and watch your downloaded videos using a browser. It is theoretically working but the ui is very simple and ugly. Just watching the files with the video player of your choice is a way better experience!!!

## Manually include videos using `include.json`

If you want to manually include a video into the system because it didn't show up or is from a different platform you can do it with a `include.json`.

Create a file called `include.json` in your download directory. The content of this file might look like this:

```json
[
  {
    "downloadUrl": "https://pdodswr-a.akamaihd.net/swr/swr-fernsehen/hannes-und-der-buergermeister/2018/10/1064318.l.mp4",
    "tvChannel": "SWR",
    "seriesTitle": "Hannes und der Bürgermeister",
    "episodeTitle": "\"Kindsköpfe\" / \"Das Gummiboot\"",
    "releaseDate": 1540324800000,
    "description": "Im Hinblick auf kommende Wahlen soll der Bürgermeister beim Kindergartenfest einen guten Eindruck machen, so die Order seiner Frau.",
    "length": 30,
    "size": 421
  }
]
```

This is only a example to show how it works. You can add more entries to the array as you wish. Note that every of the properties that the object in the above example has is mandatory. The names are pretty self explaining.