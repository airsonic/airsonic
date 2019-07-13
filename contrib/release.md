Release Steps
=============

1. Ensure changelog is up to date

2. Create a new minor branch if not already exists. Checkout branch

        git checkout -b release-X.Y

3. Bump the maven pom

        mvn versions:set -DnewVersion=X.Y.Z-RELEASE

4. Commit maven pom changes


5. Create a new tag

        git tag -s vX.Y.Z -m 'Release vX.Y.Z' 

6. Package

        mvn clean verify -P docker

7. Sign sha256sums file

        gpg2 --clearsign artifacts-checksums.sha

8. push up branch and tag

        git push origin vX.Y.Z
        git push -u origin release-X.Y

9. Create new release on github

   - Draft new Relase
   - Choose existing tag
   - Title is "Airsonic X.Y.Z"
   - Contents are the relevant entry of the CHANGELOG.md file
   - Upload `airsonic.war` and `artifacts-checksums.sha`

10. Update latest docker tag

        docker tag airsonic/airsonic:X.Y.Z-RELEASE airsonic/airsonic:latest

11. Docker login with airsonic credentials in `airsonic-passwords` repo

        docker login

12. Push images

        docker push airsonic/airsonic:X.Y.Z-RELEASE
        docker push airsonic/airsonic:latest

13. Checkout master branch and bump maven version to next snapshot version

        git checkout master
        mvn versions:set -DnewVersion=X.Y+1.0-SNAPSHOT

14. Git commit and push
