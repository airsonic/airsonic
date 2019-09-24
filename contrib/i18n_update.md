# I18n update steps

1. Clone the airsonic/airsonic repo
```sh
git clone https://github.com/airsonic/airsonic.git
```

2. Add weblate remote repo
```sh
git remote add weblate https://hosted.weblate.org/git/airsonic/airsonic/ ; git remote update weblate
```

3. Create new `update_translations` branch
```sh
git checkout -b update_translations
```

4. Merge changes from remote weblate
```sh
git merge weblate/master
```

5. Fix possible conflicts (always prefer airsonic master over weblate master)

6. Fix characters encoding
```sh
go build contrib/i18n_fix_encoding.go
mv "i18n_fix_encoding" "airsonic-main/src/main/resources/org/airsonic/player/i18n"
cd "airsonic-main/src/main/resources/org/airsonic/player/i18n"
./i18n_fix_encoding
rm "./i18n_fix_encoding"
git add .
git commit -m "Fix i18n characters encoding"
```

7. Push to github in order to open a Pull Request
```sh
git push origin update_translations
```
