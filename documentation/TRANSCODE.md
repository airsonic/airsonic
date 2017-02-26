# Setting up Transcoding Binaries

## About Transcoding
Transcoders are used by Libresonic to convert media from their on disk format
to a format that can be consumed by clients. This is done not only for compatibility
but also to save bandwidth when dealing with heavier file types. For example, although your
library might use the flac format, bandwidth can be saved by converting to mp3 before 
transmission.

## Bare Minimum setup (Linux)

*Commands provided below are illustrative*

Install ffmpeg using your distributions package manager. 

```
sudo yum install ffmpeg
```

In the case that ffmpeg is not available, you have two options.
- Add a repository that provides ffmpeg
- Build the binary from source
  - Outdated documentation for this can be found at [TRANSCODE.TXT](developer/TRANSCODE.TXT)

Create a `transcode` directory within your `libresonic.home` directory:

```
mkdir /var/libresonic/transcode
```

Ensure it has the correct permissions:

```
-bash-4.2$ ls -alhd transcode/
drwxr-xr-x. 2 tomcat tomcat 41 Jan  7 13:45 transcode/
```

Within the `transcode` directory symlink to ffmpeg and verify correct permissions
```
-bash-4.2$ cd transcode/
-bash-4.2$ ln -s /usr/bin/ffmpeg
-bash-4.2$ ls -alh
total 4.0K
drwxr-xr-x. 2 tomcat tomcat   41 Jan  7 13:45 .
drwxr--r--. 7 tomcat tomcat 4.0K Feb 23 19:23 ..
lrwxrwxrwx. 1 tomcat tomcat   15 Jan  7 13:45 ffmpeg -> /usr/bin/ffmpeg
```
