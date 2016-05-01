Upgrade ffmpeg
  http://ffmpegmac.net/
  http://www.activeobjects.no/subsonic/forum/viewtopic.php?t=5466&highlight=
  FFmpeg now accesses x264 presets via libx264. This extends functionality by introducing several new libx264 options including -preset, -tune, and -profile. You can read more detailed information about these options with "x264 --fullhelp".
  The syntax has changed so be sure to update your commands. Example:
  ffmpeg -i input -vcodec libx264 -preset fast -tune film -profile main -crf 22 -threads 0 output
  Use baseline profile
  ffmpeg -ss %o -i %s -async 1 -b %bk -s %wx%h -ar 44100 -ac 2 -v 0 -f flv -vcodec libx264 -preset fast -threads 0 -

  http://mewiki.project357.com/wiki/X264_Settings
  One-step audio encoding without lame
  More compact transcoding format.
  Create new transcoding packs.
  Downsampling using only ffmpeg?
  Test h264 with Android.
  Create universal binary for mac.
  Update all installers.

HOW TO BUILD FFMPEG (Ubuntu 11.04)
----------------------------------
http://ubuntuforums.org/showthread.php?t=786095

sudo apt-get remove ffmpeg x264 libx264-dev
sudo apt-get update
sudo apt-get install build-essential checkinstall git libfaac-dev libjack-jackd2-dev \
  libmp3lame-dev libopencore-amrnb-dev libopencore-amrwb-dev libsdl1.2-dev libtheora-dev \
  libva-dev libvdpau-dev libvorbis-dev libx11-dev libxfixes-dev libxvidcore-dev texi2html \
  yasm zlib1g-dev

cd
cd projects
git clone git://git.videolan.org/x264
cd x264
./configure --enable-static
make
sudo checkinstall --pkgname=x264 --pkgversion="3:$(./version.sh | awk -F'[" ]' '/POINT/{print $4"+git"$5}')" --backup=no --deldoc=yes --fstrans=no --default

cd
cd projects
sudo apt-get remove libvpx-dev
git clone git://review.webmproject.org/libvpx
cd libvpx
./configure
make
sudo checkinstall --pkgname=libvpx --pkgversion="1:$(date +%Y%m%d%H%M)-git" --backup=no --deldoc=yes --fstrans=no --default

cd
cd projects
git clone git://git.videolan.org/ffmpeg
cd ffmpeg
./configure --disable-ffplay --disable-ffprobe --disable-ffserver --enable-gpl --enable-nonfree --enable-postproc \
  --enable-pthreads --enable-libfaac --enable-libmp3lame --enable-libvpx --enable-libtheora --enable-libvorbis \
  --enable-libx264 --enable-libxvid --enable-zlib --enable-libopencore-amrnb --enable-libopencore-amrwb \
  --enable-libvpx --enable-version3 --enable-bzlib \
  --enable-static --disable-shared --extra-libs=-static --extra-cflags=--static
make
sudo checkinstall --pkgname=ffmpeg --pkgversion="5:$(date +%Y%m%d%H%M)-git" --backup=no --deldoc=yes --fstrans=no --default
hash x264 ffmpeg


HOW TO BUILD LAME (Ubuntu 11.04)
----------------------------------
cd
cd projects
sudo apt-get install nasm
wget http://downloads.sourceforge.net/project/lame/lame/3.98.4/lame-3.98.4.tar.gz
tar xzvf lame-3.98.4.tar.gz
cd lame-3.98.4
./configure --enable-nasm --disable-shared
make


